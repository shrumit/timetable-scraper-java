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
    Map<String, Subject> subjects = new HashMap<>();
    int nextUnusedCourseId = 0;
    int nextUnusedSubjectId = 0;
    Metadata.MetadataBuilder metadataBuilder = new Metadata.MetadataBuilder();

    // The purpose of this class is to decouple the data extraction from the data processing.
    // This class processes and stores rows of information parsed from the HTML webpage.
    public DataManager(Logger logger) {
        this.logger = logger;
    }

    public void submitSubject(String subjectCode, String subjectName, String campusListRaw) {
        if (!subjects.containsKey(subjectCode)) {
            subjects.put(subjectCode, new Subject(logger, subjectCode, subjectName, campusListRaw, nextUnusedSubjectId++));
        }
    }

    public void submitRow(String courseName,
                          String subjectCode,
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

        String debugPrint = String.format("submitRow: courseName=%s, subjectCode=%s, componentName=%s, sectionName=%s, "
                        + "number=%s, instructor=%s, campus=%s, delivery=%s, location=%s, "
                        + "days=%s, startTime=%s, endTime=%s",
                courseName, subjectCode, componentName, sectionName, number, instructor, campus,
                delivery, location, days, startTime, endTime);

        var subject = subjects.get(subjectCode);
        if (subject == null) {
            logger.severe(String.format("subjectCode not found. debugPrint: %s", debugPrint));
            throw new IllegalArgumentException("subjectCode not found");
        }

        if (!courses.containsKey(courseName)) {
            courses.put(courseName, new Course(courseName, subject, nextUnusedCourseId++));
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
            sec.addTime(startTime, endTime, day, location, logger);
        }

        metadataBuilder.submitCampusType(campus);
        metadataBuilder.submitDeliveryType(delivery);
    }

    public void saveOutput(String outputDir, String masterFileName, String searchFileName, String metadataFileName, String subjectsFilename) throws
            IOException {
        List<Course> coursesList = new ArrayList<>(courses.values());
        coursesList.sort((a,b) -> Integer.compare(a.id, b.id));
        coursesList.removeIf(course -> {
            // remove courses with empty components
            if (course.components.isEmpty()) {
                logger.info("Removing course with no components: " + course.name);
                return true;
            }
            return false;
        });

        List<Subject> subjectsList = new ArrayList<>(subjects.values());
        subjectsList.sort((a,b) -> Integer.compare(a.id, b.id));

        CommonUtils.saveToFile(produceMasterJson(coursesList), outputDir, masterFileName, logger);
        CommonUtils.saveToFile(produceSearchJson(coursesList), outputDir, searchFileName, logger);
        CommonUtils.saveToFile(produceMetadataJson(metadataBuilder.build()), outputDir, metadataFileName, logger);
        CommonUtils.saveToFile(produceSubjectJson(subjectsList), outputDir, subjectsFilename, logger);
        logger.info(String.format("Saved output to %s. Stats: %s", outputDir, produceStats()));
    }

    private static String produceMetadataJson(Metadata m) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(m);
    }

    private static String produceMasterJson(List<Course> coursesList) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Subject.class, new SubjectSerializer()).create();
        return gson.toJson(coursesList);
    }

    private static String produceSearchJson(List<Course> coursesList) {
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

    private static String produceSubjectJson(List<Subject> subjectsList) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(subjectsList);
    }

    record Stats(long courseCount, long compCount, long sectionCount){};
    private Stats produceStats() {
        long compCount = 0;
        long sectionCount = 0;
        for (var e : courses.entrySet()) {
            compCount += e.getValue().components.size();
            for (var c : e.getValue().components) {
                sectionCount += c.sections.size();
            }
        }

        return new Stats(courses.size(), compCount, sectionCount);
    }
}
