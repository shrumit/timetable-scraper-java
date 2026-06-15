package tsj;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tsj.model.Section;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// parser for the Academic Timetable site (legacy)
public class ATParser {
    Logger logger;
    DataManager dm;

    public ATParser(Logger logger, DataManager dm) {
        this.logger = logger;
    }

    public void parseFromDir(String inputDir) throws IOException {
        File[] fileList = CommonUtils.getFilesInDir(inputDir);
        if (fileList.length == 0) {
            throw new IllegalArgumentException("inputDir is nonexistent or empty");
        }

        logger.info("Number of files in dir " + inputDir + " :" + fileList.length);

        for (File file : fileList) {
            parseFromFile(file);
        }
    }

    public void parseFromFile(File file) throws IOException {
        logger.info("Parsing file " + file.getName());

        Document doc = Jsoup.parse(file, "UTF-8", "");

        Elements names = doc.getElementsByTag("h4");
        Elements tables = doc.getElementsByClass("table-striped");

        if (names.size() != tables.size()) {
            throw new IllegalArgumentException("Size mismatch");
        }

        logger.info("No. of courses in page:" + names.size());

        // for each course in file
        for (int i = 0; i < names.size(); i++) {
            String courseName = names.get(i).text();
            logger.info("Parsing course " + courseName);
            Elements rows = tables.get(i).select("tbody").first().select("> tr");

            // for every row in course table
            for (Element row : rows) {

                /* read values */
                Elements td = row.select("> td");
                String sectionName = td.get(0).text();
                String compName = td.get(1).text();
                String number = td.get(2).text();
                String location = td.get(6).text();
                String instructor = td.get(7).text();
                String campus = td.get(10).text();
                String delivery = td.get(11).text();

                String startTime = td.get(4).text().trim();
                String endTime = td.get(5).text().trim();
                // fix exceptions
                if (startTime.equals("7:00 AM"))
                    startTime = "8:00 AM";
                if (endTime.equals("10:30 PM"))
                    endTime = "10:00 PM";

                Elements daysCols = td.get(3).getElementsByTag("td");
                List<Section.Days> days = new ArrayList<>();
                for (int j = 1; j < daysCols.size(); j++) {
                    String dayText = daysCols.get(j).text();
                    if (dayText.isBlank() || dayText.equals("\u00a0"))
                        continue;
                    days.add(Section.Days.values()[j - 1]);
                }

                /* store values */
                dm.submitRow(courseName, null, compName, sectionName, number, instructor, campus, delivery, location, days, startTime, endTime);
            } // done parsing all sections of the course's table

        }
    }
}
