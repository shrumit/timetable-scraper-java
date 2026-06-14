package tsj;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import tsj.model.Section;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

        /* For each course, download and parse */
        for (String url : urls) {
            Document doc = Jsoup.connect(url)
                    .cookie("CFID", creds.cfid())
                    .cookie("CFTOKEN", creds.cftoken())
                    .get();

            // parse each course offering
            for (Element table : doc.select("table.table-hover")) {

                // parse course name
                Element heading = table.previousElementSibling();
                while (heading != null && !heading.tagName().equals("h4")) {
                    heading = heading.previousElementSibling();
                }
                // fallback: search up to the wrapping div if needed.
                if (heading == null) {
                    Element parent = table.parent();
                    if (parent != null) heading = parent.selectFirst("h4");
                }
                String courseName = heading != null ? heading.text().trim() : "";
                if (courseName.isEmpty()) {
                    logger.severe("courseName not parsed. doc.outerHTML():" + doc.outerHtml());
                    throw new RuntimeException();
                }
                logger.info("Now parsing " + courseName);

                int numRows = 0;

                // parse each row of table
                for (Element row : table.select("> tbody > tr:not(.active)")) {
                    // Direct child <td> cells only — avoids the nested days/times table.
                    Elements cells = row.children();
                    if (cells.size() < 11) {
                        logger.warning("malformed row:" + row.outerHtml());
                        logger.warning("cells.size():" + cells.size());
                        continue; // skip malformed/empty rows
                    }

//                    int idx = 0;
//                    for (var c : cells) {
//                        int i = idx++;
//                        System.out.println(i + ":" + c.text().trim());
//                        System.out.println(i + ":" + c.outerHtml());
//                    }

                    String componentName = cells.get(0).text().trim();
                    String sectionName   = cells.get(1).text().trim();
                    String number        = cells.get(2).text().trim();
                    String instructor    = cells.get(3).text().trim();   // may be empty

                    // cells.get(4) = Requisites and Constraints, cells.get(6) = Credit Units, get(7) = Status, get(8) = Waitlist

                    String daysRaw   = "";
                    String startTime = "";
                    String endTime   = "";
                    String location  = "";
                    Element dtl = cells.get(5).selectFirst("table tr");
                    if (dtl != null) {
                        Elements dtlCells = dtl.children();
                        if (dtlCells.size() >= 3) {
                            daysRaw  = dtlCells.get(0).text();
                            location = dtlCells.get(2).text().trim();

                            String[] times = dtlCells.get(1).text().split("-");
                            startTime = times[0].trim();
                            endTime   = times.length > 1 ? times[1].trim() : "";
                        }
                    }

                    String campus   = cells.get(9).text().trim();
                    String delivery = cells.get(10).text().trim();

                    List<Section.Days> days = parseDays(daysRaw);
                    if (days.isEmpty()) {
                        logger.warning("Parsed days was empty:" + table.outerHtml());
                    }

                    dm.submitRow(courseName, componentName, sectionName, number,
                            instructor, campus, delivery, location,
                            days, startTime, endTime);
                }

                logger.info("Finished parsing " + courseName);
            }
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
                case "M"  -> result.add(Section.Days.MONDAY);
                case "Tu" -> result.add(Section.Days.TUESDAY);
                case "W"  -> result.add(Section.Days.WEDNESDAY);
                case "Th" -> result.add(Section.Days.THURSDAY);
                case "F"  -> result.add(Section.Days.FRIDAY);
            }
        }
        return result;
    }
}
