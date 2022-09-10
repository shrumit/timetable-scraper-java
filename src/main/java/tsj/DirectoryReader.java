package tsj;

import tsj.model.Course;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static tsj.ParsingUtils.*;

public class DirectoryReader {

    Logger logger;
    String outputDir;
    String outputView;
    String outputSearch;
    String outputMetadata;

    public DirectoryReader(Logger logger, String outputDir, String outputView, String outputSearch, String outputMetadata) {
        this.logger = logger;
        this.outputDir = outputDir;
        this.outputView = outputView;
        this.outputSearch = outputSearch;
        this.outputMetadata = outputMetadata;
    }

    public void parse(String storageDir) throws IOException {
        long time_start = System.nanoTime();

        if (storageDir == null) {
            throw new IllegalArgumentException("storageDir cannot be null");
        }

        // Retrieve files in directory
        File dir = new File(storageDir);
        File[] fileList = dir.listFiles();
        Arrays.sort(fileList);

        logger.info("Number of files:" + fileList.length);

        List<Course> courses = new ArrayList<>();

        for (File file : fileList) {
            courses.addAll(parseHTML(file, logger));
        }

        Collections.sort(courses);

        // assign ids
        int id = 0;
        for (var course : courses)
            course.id = id++;


        CommonUtils.saveToFile(produceViewDataJson(courses), outputDir, outputView, logger);
        CommonUtils.saveToFile(produceSearchDataJson(courses), outputDir, outputSearch, logger);
        CommonUtils.saveToFile(produceMetadataJson(), outputDir, outputMetadata, logger);

        long time_end = System.nanoTime();
        logger.info("Parsed " + courses.size() + " courses in " + TimeUnit.NANOSECONDS.toMinutes(time_end-time_start) + " seconds");
    }
}
