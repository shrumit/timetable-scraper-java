package shrumit;

import shrumit.model.Course;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static shrumit.ParsingUtils.*;

public class DirectoryReader {

    Logger logger;

    public DirectoryReader(Logger logger) {
        this.logger =  logger;
    }

    public void parse(String storageDir, String outputDir, String outputView, String outputSearch, String outputMetadata) throws IOException {
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

        FileUtils.writeToFile(produceViewDataJson(courses), outputDir, outputView, logger);
        FileUtils.writeToFile(produceSearchDataJson(courses), outputDir, outputSearch, logger);
        FileUtils.writeToFile(produceMetadataJson(), outputDir, outputMetadata, logger);

        long time_end = System.nanoTime();
        logger.info("Parsed " + courses.size() + " courses in " + TimeUnit.NANOSECONDS.toMinutes(time_end-time_start) + " seconds");
    }
}
