package tsj;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tsj.model.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;
import java.io.IOException;

public class DataManager {

    // Regex to shorten name
    static final Pattern shortname_regex = Pattern.compile("(.{1,4}).* (\\d{4}\\w{0,1}).*");
    // Regex for selecting course code suffix
    static final Pattern suffix_regex = Pattern.compile(".*\\d{4}(\\w).*");

    Logger logger;

    Map<String, Course> courses = new HashMap<>();
    int nextUnusedCourseId = 0;
    Metadata.MetadataBuilder metadataBuilder = new Metadata.MetadataBuilder();

    // The purpose of this class is to decouple the data extraction from the data processing.
    // This class processes and stores rows of information parsed from the HTML webpage.
    public DataManager(Logger logger) {
        this.logger = logger;
    }

    public void submitRow(String courseName,
                          String componentName,
                          String sectionName,
                          String number,
                          String instructor,
                          String campus,
                          String delivery,
                          String location,
                          List<Section.Days> days,
                          String startTime,
                          String endTime
    ) {

        System.out.printf("submitRow: courseName=%s, componentName=%s, sectionName=%s, "
                        + "number=%s, instructor=%s, campus=%s, delivery=%s, location=%s, "
                        + "days=%s, startTime=%s, endTime=%s%n",
                courseName, componentName, sectionName, number, instructor, campus,
                delivery, location, days, startTime, endTime);

        if (!courses.containsKey(courseName)) {
            courses.put(courseName, new Course(courseName, nextUnusedCourseId++));
        }

        Course course = courses.get(courseName);

        // get or add new component
        Component comp = course.getComponent(componentName);
        if (comp == null) {
            comp = new Component(componentName);
            course.addComponent(comp);
        }

        // get or add new component
        Section sec = comp.getSection(sectionName);
        if (sec == null) {
            sec = new Section(sectionName, number, location, instructor, campus, delivery);
            comp.addSection(sec);
        }

        // no need to add times if empty
        if (startTime.isEmpty() || endTime.isEmpty()) {
            logger.info(String.format("Skipping row with empty startTime/endTime. courseName: %s, number: %s", course.name, number));
            return;
        }

        // add times denoted by the row
        for (Section.Days day : days) {
            sec.addTime(startTime, endTime, day, logger);
        }

        metadataBuilder.submitCampusType(campus);
        metadataBuilder.submitDeliveryType(delivery);
    }

    public void saveOutput(String outputDir, String viewFileName, String searchFileName, String metadataFileName) throws
            IOException {
        List<Course> coursesList = new ArrayList<>(courses.values());

        coursesList.removeIf(course -> {
            // remove courses with empty components
            if (course.components.isEmpty()) {
                logger.info("Removing course with no components: " + course.name);
                return true;
            }
            return false;
        });

        CommonUtils.saveToFile(produceViewDataJson(coursesList), outputDir, viewFileName, logger);
        CommonUtils.saveToFile(produceSearchDataJson(coursesList), outputDir, searchFileName, logger);
        CommonUtils.saveToFile(produceMetadataJson(), outputDir, metadataFileName, logger);
        logger.info(String.format("Saved output for %s courses to %s", courses.size(), outputDir));
    }

    private String produceMetadataJson() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(metadataBuilder.build());
    }

    private String produceViewDataJson(List<Course> coursesList) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(coursesList);
    }

    private String produceSearchDataJson(List<Course> coursesList) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        StringJoiner termA = new StringJoiner(",", "[", "]");
        StringJoiner termB = new StringJoiner(",", "[", "]");

        for (Course course : coursesList) {
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
