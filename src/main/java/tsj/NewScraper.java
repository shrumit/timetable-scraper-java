package tsj;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import tsj.model.Section;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NewScraper {

    Logger logger;
    DataManager dm;
    Login.Creds creds;

    public NewScraper(Logger logger, DataManager dm, Login.Creds creds) {
        this.logger = logger;
        this.dm = dm;
        this.creds = creds;
    }

    public void scrape() throws IOException {

        // 1. Get all subject codes
        Document doc = Jsoup.connect("https://draftmyschedule.uwo.ca/secure/builder.cfm")
                .maxBodySize(40 * 1024 * 1024) // 40 MB limit, up from the default of 2 MB
                .header("content-type", "application/x-www-form-urlencoded")
                .header("cache-control", "no-cache")
                .header("priority", "u=0")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/149.0.0.0 Safari/537.36")
                .cookie("CFID", creds.cfid())
                .cookie("CFTOKEN", creds.cftoken())
                .cookie("FORM_AGREEMENT", "1")
                .get();

        Element select = doc.selectFirst("select#Subject");
        if (select == null) {
            logger.severe("Select element not found.");
            return;
        }

        List<String> subjectCodes = new ArrayList<>();

        Elements options = select.select("option");
        for (int i = 1; i < options.size(); i++) {   // start at 1 to skip "Select a Subject" placeholder
            Element option = options.get(i);
            String cls = option.attr("class");
            String value = option.attr("value");
            String text = option.text().trim();            // normalized innerText

            dm.submitSubject(value, text, cls);
            subjectCodes.add(value);
        }

        // 2. For each subject, get all its courses
        for (int j = 0; j < subjectCodes.size(); j++) {

            String subjectCode = subjectCodes.get(j);
            logger.info(String.format("Downloading subject: %s. (%s of %s)", subjectCode, j, subjectCodes.size() - 1));

            long dlStartTime = System.nanoTime();
            doc = downloadSubjectPage(creds, subjectCode, "");
            long dlEndTime = System.nanoTime();

            List<String> courseNames = new ArrayList<>();
            Elements courseTables = new Elements();

            // the form won't return results if the subject has over 300 courses. so we query by course number prefix and download separately.
            if (doc.text().contains("Unable to display your search results as it exceeds")) {
                logger.info("Exceeds results error. Downloading separately and combining.");
                for (String courseNumPrefix : List.of("1", "2", "3", "4")) {
                    logger.info(String.format("Downloading subject-part: %s-%s.", subjectCode, courseNumPrefix));
                    var sepDoc = downloadSubjectPage(creds, subjectCode, courseNumPrefix);
                    var cn = sepDoc.select("h4").eachText();
                    var ct = sepDoc.select("table.table-hover");
//                    printSepDocLists(cn, ct);
                    if (cn.size() != ct.size() - 1) {
                        logger.severe("mismatched sizes of courseNames and courseTables. outerHTML of page:" + sepDoc.outerHtml());
                        throw new IllegalArgumentException();
                    }
                    courseNames.addAll(cn);
                    courseTables.addAll(ct);
                    courseTables.remove(courseTables.size()-1); // trim the last one
                }
            } else {
                var cn = doc.select("h4").eachText();
                var ct = doc.select("table.table-hover");
//                printSepDocLists(cn, ct);
                if (cn.size() != ct.size() - 1) {
                    logger.severe("mismatched sizes of courseNames and courseTables. outerHTML of page:" + doc.outerHtml());
                    throw new IllegalArgumentException();
                }
                courseNames.addAll(cn);
                courseTables.addAll(ct);
                courseTables.remove(courseTables.size()-1); // trim the last one
            }

            if (courseNames.isEmpty() && !doc.text().contains("Search results: 0 Subject(s)")) {
                logger.severe("zero courses parsed from subject page. outerHTML of page:" + doc.outerHtml());
                throw new IllegalArgumentException();
            }

            // 3. For each course, parse its timetable
            for (int i = 0; i < courseNames.size(); i++) {
                String courseName = courseNames.get(i);
                Element table = courseTables.get(i);

                logger.info(String.format("Parsing course: %s (%s of %s)", courseName, i, courseNames.size() - 1));

                for (Element row : table.select("> tbody > tr:not(.active)")) {
                    // Direct child <td> cells only — avoids the nested days/times table.
                    Elements cells = row.children();
                    if (cells.size() < 11) {
                        logger.warning("malformed row:" + row.outerHtml());
                        logger.warning("cells.size():" + cells.size());
                        continue; // skip malformed/empty rows
                    }

                    String componentName = cells.get(0).text().trim();
                    String sectionName = cells.get(1).text().trim();
                    String number = cells.get(2).text().trim();
                    String instructor = cells.get(3).text().trim();   // may be empty
                    String campus = cells.get(9).text().trim();
                    String delivery = cells.get(10).text().trim();
                    // cells.get(4) = Requisites and Constraints, cells.get(6) = Credit Units, get(7) = Status, get(8) = Waitlist

                    // parse each row of the date/time/location table
                    for (var dtlRow : cells.get(5).select("table tr")) {
                        Elements dtlCells = dtlRow.children();
                        List<Section.Days> days = parseDays(dtlCells.get(0).text());
                        String[] times = dtlCells.get(1).text().split("-");
                        if (days.isEmpty() || times.length == 0) {
                            logger.info(String.format("Skipped row because parsed days/time was empty for course: %s, subject: %s, dtlRow: %s", courseName, subjectCode, dtlRow.outerHtml()));
                            continue;
                        }
                        String startTime = times[0].trim();
                        String endTime = times[1].trim();
                        String location = dtlCells.get(2).text().trim();

                        dm.submitRow(courseName, subjectCode, componentName, sectionName, number,
                                instructor, campus, delivery, location,
                                days, startTime, endTime);
                    }
                }
                logger.info("Finished parsing course: " + courseName);
            }

            long parseEndtime = System.nanoTime();
            String downloadMs = String.valueOf(TimeUnit.NANOSECONDS.toMillis(dlEndTime - dlStartTime));
            String parseMs    = String.valueOf(TimeUnit.NANOSECONDS.toMillis(parseEndtime - dlEndTime));
            logger.info(String.format("Finished parsing subject: %s. DL time: %sms. Parse time: %sms", subjectCode, downloadMs, parseMs));
//            logger.info("Finished parsing subject: " + subjectCode);
        }
    }

    public static String getQueryParamValue(String url, String paramKey) {
        if (url == null) return null;
        String prefix = paramKey + "=";
        for (String param : url.split("[?&]")) {
            if (param.startsWith(prefix)) {
                return param.substring(prefix.length());
            }
        }
        return null;
    }

    private List<Section.Days> parseDays(String raw) {
        // Normalize non-breaking spaces, collapse whitespace
        String cleaned = raw.replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
        List<Section.Days> result = new ArrayList<>();
        if (cleaned.isEmpty()) return result;

        for (String token : cleaned.split(" ")) {
            switch (token) {
                case "M" -> result.add(Section.Days.MONDAY);
                case "Tu" -> result.add(Section.Days.TUESDAY);
                case "W" -> result.add(Section.Days.WEDNESDAY);
                case "Th" -> result.add(Section.Days.THURSDAY);
                case "F" -> result.add(Section.Days.FRIDAY);
            }
        }
        return result;
    }

    // old code, not used
    public List<String> getIndividualCourseURL() throws IOException {
        /* Get list of all individual course URLs */
        Document allCoursesDoc = Jsoup.connect("https://www.westerncalendar.uwo.ca/AllCourses.cfm?SelectedCalendar=Live&ArchiveID=")
                .maxBodySize(40 * 1024 * 1024) // 40 MB limit, up from the default of 2 MB
                .data("SubjectFilter", "")
                .data("SelectedCalendar", "Live")
                .data("ArchiveID", "")
                .data("ShowCourses", "1")
                .post();

        List<String> cacIDs = allCoursesDoc.select("a:containsOwn(More details)").stream()
                .map(a -> a.absUrl("href"))
                .map(a -> getQueryParamValue(a, "CourseAcadCalendarID"))
                .toList();

        List<String> urls = new ArrayList<>();

        for (String c : cacIDs) {
            if (c == null) {
                logger.warning("got a null CourseAcadCalendarID");
                continue;
            }

            urls.add(String.format("https://draftmyschedule.uwo.ca/secure/courses.cfm?CourseAcadCalendarID=%s&SelectedCalendar=Live&referer_link=Course_listing&ArchiveID=", c));
        }

        logger.info("Number of courses found:" + urls.size());

        return urls;
    }

    private Document downloadSubjectPage(Login.Creds creds, String subjectCode, String catalogNbrTyped) throws IOException {
        var doc = Jsoup.connect("https://draftmyschedule.uwo.ca/secure/builder.cfm")
                .maxBodySize(40 * 1024 * 1024) // 40 MB limit, up from the default of 2 MB
                .method(Connection.Method.POST)
                .header("content-type", "application/x-www-form-urlencoded")
                .header("cache-control", "no-cache")
                .header("priority", "u=0")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/149.0.0.0 Safari/537.36")
                .cookie("CFID", creds.cfid())
                .cookie("CFTOKEN", creds.cftoken())
                .cookie("FORM_AGREEMENT", "1")
                .requestBody(String.format("version_id=&Subject=%s&delivery_type=All&catalog_nbr_typed=%s&catalog_nbr=" +
                        "&day=m&day=tu&day=w&day=th&day=f&Designation=Any&start_time=&end_time=" +
                        "&Campus=Any&course_component=LEC&course_component=TUT&course_component=LAB" +
                        "&command_search=search", subjectCode, catalogNbrTyped))
                .execute()
                .parse();
        return doc;
    }

    private void printSepDocLists(List<String> cn, Elements ct) {
        System.out.println("Headings (h4): " + cn.size() + " found");
        for (String heading : cn) {
            System.out.println("  " + heading);
        }

        System.out.println("Tables (table.table-hover): " + ct.size() + " found");
        for (Element table : ct) {
            System.out.println("  " + table.outerHtml());
        }
    }

}
