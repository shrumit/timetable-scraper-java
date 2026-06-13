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
    // first get all "Courses.cfm?Subject" URLs from this page: https://www.westerncalendar.uwo.ca/Courses.cfm
    // for each of these links:
    // - follow the link
    // - for every `h4 class="courseTitleNoBlueLink`, get the course title and the CourseAcadCalendarID

    final String SUBJECT_CATALOG_URL = "https://www.westerncalendar.uwo.ca/Courses.cfm";

    record Course(String name, String courseAcadCalendarID){
        public String getDFMLink() {
            return String.format("https://draftmyschedule.uwo.ca/secure/courses.cfm?CourseAcadCalendarID=%s&SelectedCalendar=Live&referer_link=Course_listing&ArchiveID=", courseAcadCalendarID);
        }
    };

    Logger logger;
    DataManager dm;
    Login.Creds creds;

    public NewScraper(Logger logger, DataManager dm, Login.Creds creds) {
        this.logger = logger;
        this.dm = dm;
        this.creds = creds;
    }

    public void scrape() throws IOException {
//        Map<String, List<Course>> subjects = new HashMap<>();

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
                System.out.println("got a null");
                continue;
            }

            urls.add(String.format("https://draftmyschedule.uwo.ca/secure/courses.cfm?CourseAcadCalendarID=%s&SelectedCalendar=Live&referer_link=Course_listing&ArchiveID=", c));
        }

        System.out.println("Number of courses found:" + urls.size());

        for (String url : urls) {
            Document doc = Jsoup.connect(url)
                    .cookie("CFID", creds.cfid())
                    .cookie("CFTOKEN", creds.cftoken())
                    .get();

            System.out.println(doc.outerHtml());

            for (Element table : doc.select("table.table-hover")) {
                // Course name = the <h4> sibling just before this table's wrapper.
                Element heading = table.previousElementSibling();
                while (heading != null && !heading.tagName().equals("h4")) {
                    heading = heading.previousElementSibling();
                }
                // Fallback: search up to the wrapping div if needed.
                if (heading == null) {
                    Element parent = table.parent();
                    if (parent != null) heading = parent.selectFirst("h4");
                }
                String courseName = heading != null ? heading.text().trim() : "";

                for (Element row : table.select("tr:not(.active)")) {
                    // Direct child <td> cells only — avoids the nested days/times table.
                    Elements cells = row.children();
                    if (cells.size() < 11) {
                        System.out.println("malformed row:" + row.outerHtml());
                        System.out.println("cells.size():" + cells.size());
                        continue; // skip malformed/empty rows
                    }

                    int idx = 0;
                    for (var c : cells) {
                        int i = idx++;
                        System.out.println(i + ":" + c.text().trim());
                        System.out.println(i + ":" + c.outerHtml());
                    }

                    String componentName = cells.get(0).text().trim();   // LEC
                    String sectionName   = cells.get(1).text().trim();   // 001
                    String number        = cells.get(2).text().trim();   // 11145
                    String instructor    = cells.get(3).text().trim();   // may be empty

                    // cells.get(4) = Requisites and Constraints (ignored)

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

                    // cells.get(6) = Credit Units, get(7) = Status, get(8) = Waitlist
                    String campus   = cells.get(9).text().trim();   // Main
                    String delivery = cells.get(10).text().trim();  // In Person

                    List<Section.Days> days = parseDays(daysRaw);

                    dm.submitRow(courseName, componentName, sectionName, number,
                            instructor, campus, delivery, location,
                            days, startTime, endTime);
                }
            }

//            for (Element block : doc.select("div:has(> table)")) {
//                String courseName = block.selectFirst("h4").text();
//                System.out.println(courseName);
//
//                Element table = block.selectFirst("table");
//
//                // skip the header row (tr.active)
//                for (Element row : table.select("tr:not(.active)")) {
//                    // Direct child <td> cells of this row (avoid the nested days/times table)
//                    Elements cells = row.children().select("td");
//                    if (cells.isEmpty()) continue;
//
//                    String componentName = cells.get(0).text().trim();   // LEC / TUT
//                    String sectionName   = cells.get(1).text().trim();   // 001
//                    String number        = cells.get(2).text().trim();   // 9901
//                    String instructor    = cells.get(3).text().trim();   // A. McAlpine
//
//                    // cells.get(4) = Requisites and Constraints (ignored)
//
//                    // Days/Times/Location lives in the nested table inside cells.get(5)
//                    Element dtl = cells.get(5).selectFirst("table tr");
//                    String daysRaw  = "";
//                    String startTime = "";
//                    String endTime   = "";
//                    String location  = "";
//                    if (dtl != null) {
//                        Elements dtlCells = dtl.children(); // 3 <td>: days, time range, location
//                        daysRaw  = dtlCells.get(0).text();
//                        location = dtlCells.get(2).text().trim();
//
//                        String[] times = dtlCells.get(1).text().split("-");
//                        startTime = times[0].trim();
//                        endTime   = times.length > 1 ? times[1].trim() : "";
//                    }
//
//                    // cells.get(6) = Credit Units, cells.get(7) = Status, cells.get(8) = Waitlist
//                    String campus   = cells.get(9).text().trim();   // Main
//                    String delivery = cells.get(10).text().trim();  // In Person
//
//                    List<Section.Days> days = parseDays(daysRaw);
//
//                    dm.submitRow(courseName, componentName, sectionName, number,
//                            instructor, campus, delivery, location,
//                            days, startTime, endTime);
//                }
//            }

            // TODO: REMOVE after testing
            break;
        }

//        Document catalogPage = Jsoup.connect(SUBJECT_CATALOG_URL).get();
//        Elements subjectAElements = catalogPage.select("a[href^=Courses.cfm?Subject=]");
//
//        // for every subject
//        for (Element s : subjectAElements) {
//            String subjectPageURL = s.attr("abs:href");
//            System.out.println("subjectLink:" + subjectPageURL);
//
//            String subjectName = getQueryParamValue(subjectPageURL, "Subject");
//            if (subjectName == null) {
//                System.out.println("Subject query param not found in: " + s.outerHtml());
//                continue;
//            }
//
//            // download subject page
//            Document sub = Jsoup.connect(subjectPageURL).get();
//
//            // parses all courses in that subject
//            Elements titles = sub.select("h4.courseTitleNoBlueLink");
//            for (Element title : titles) {
//
//                // Course name: own text, excluding the nested <a> text
//                String name = title.ownText().trim();
//
//                // CourseAcadCalendarID from the nested link's href
//                Element link = title.selectFirst("a[href*=CourseAcadCalendarID=]");
//                if (link == null) {
//                    System.out.println("course link not present in: " + title.outerHtml());
//                    continue;
//                }
//
//                String id = getQueryParamValue(link.attr("href"), "CourseAcadCalendarID");
//
//                if (id == null) {
//                    System.out.println("CourseAcadCalendarID query param not found in: " + link.outerHtml());
//                    continue;
//                }
//
//                System.out.println(name + " | " + id);
//                subjects.putIfAbsent(subjectName, new ArrayList<>());
//                subjects.get(subjectName).add(new Course(name, id));
//            }
//
//        }
//        return subjects;
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
                // add "Sa"/"Su" if your data ever includes weekends
            }
        }
        return result;
    }
}
