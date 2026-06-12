package tsj;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tsj.model.Component;
import tsj.model.Course;
import tsj.model.Metadata;
import tsj.model.Section;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingJob {

    // Regex to shorten name
    static final Pattern shortname_regex = Pattern.compile("(.{1,4}).* (\\d{4}\\w{0,1}).*");
    // Regex for selecting course code suffix
    static final Pattern suffix_regex = Pattern.compile(".*\\d{4}(\\w).*");

    Logger logger;

    List<Course> courses = new ArrayList<>();
    Metadata.MetadataBuilder metadataBuilder = new Metadata.MetadataBuilder();

    public ParsingJob(Logger logger) {
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
            Course course = new Course(names.get(i).text(), courses.size());
            logger.info("Parsing course " + course.name);
            Elements rows = tables.get(i).select("tbody").first().select("> tr");

            Map<String, Map<String, Section>> compMap = new LinkedHashMap<>();

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
                if (!compMap.containsKey(compName)) {
                    // encountered a new component
                    compMap.put(compName, new LinkedHashMap<>());
                }
                if (!compMap.get(compName).containsKey(sectionName)) {
                    // encountered a new section
                    Section section = new Section(sectionName, number, location, instructor, campus, delivery);
                    compMap.get(compName).put(sectionName, section);
                    metadataBuilder.submitCampusType(campus);
                    metadataBuilder.submitDeliveryType(delivery);
                }

                if (startTime.length() == 0 || endTime.length() == 0)
                    continue;

                Section section = compMap.get(compName).get(sectionName);

                // figure out which days have the timeslot and add it to the Section
                for (Section.Days day : days) {
                    section.addTime(startTime, endTime, day, logger);
                }
            } // done parsing all sections of the course's table

            // remove empty sections and components
//            compMap.entrySet().removeIf(comp -> {
//                comp.getValue().entrySet().removeIf(sec -> !sec.getValue().hasTimeslots());
//                return comp.getValue().isEmpty();
//            });

            // convert map to Component objects and add to course
            compMap.forEach((k, v) -> {
                Component comp = new Component(k);
                comp.sections.addAll(v.values());
                course.components.add(comp);
            });

            if (!course.components.isEmpty())
                courses.add(course);
            else
                logger.warning("Skipping due to all empty components " + course.name);
        }
    }

    public void saveOutput(String outputDir, String outputView, String outputSearch, String outputMetadata) throws
            IOException {
        CommonUtils.saveToFile(produceViewDataJson(), outputDir, outputView, logger);
        CommonUtils.saveToFile(produceSearchDataJson(), outputDir, outputSearch, logger);
        CommonUtils.saveToFile(produceMetadataJson(), outputDir, outputMetadata, logger);
        logger.info(String.format("Saved output for %s courses to %s", courses.size(), outputDir));
    }

    public String produceMetadataJson() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(metadataBuilder.build());
    }

    public String produceViewDataJson() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(courses);
    }

    public String produceSearchDataJson() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        StringJoiner termA = new StringJoiner(",", "[", "]");
        StringJoiner termB = new StringJoiner(",", "[", "]");

        for (Course course : courses) {
            String suffix;
            Matcher m = suffix_regex.matcher(course.name);
            // if match found, assign to suffix, else assign ""
            if (m.find())
                suffix = m.group(1);
            else
                suffix = "";

            switch (suffix) {
                // Fall term
                case "A", "F", "W", "Q", "R" -> termA.add(gson.toJson(course));

                // Winter term
                case "B", "G", "X", "S", "T" -> termB.add(gson.toJson(course));

                // Both terms
                case "", "E", "Y", "Z", "U" -> {
                    termA.add(gson.toJson(course));
                    termB.add(gson.toJson(course));
                }
                default -> throw new IllegalArgumentException("Unexpected suffix: " + course.name);
            }
        }
        return "[" + termA + "," + termB + "]";
    }
}
