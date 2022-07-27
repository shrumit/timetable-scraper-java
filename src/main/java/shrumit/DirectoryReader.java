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

@SuppressWarnings("ConstantConditions")
public class DirectoryReader {

    static final String outputView = "master.json";
    static final String outputSearch = "search.json";
    static final String outputMetadata = "metadata.json";

    public static String parse(String inputDirName, Logger logger) throws IOException {
        long time_start = System.nanoTime();

        if (inputDirName == null) {
            throw new IllegalArgumentException("inputDirName cannot be null");
        }

        String outputDirname = inputDirName.replace("dump", "coutput");

        // Retrieve files in directory
        File dir = new File(inputDirName);
        File[] fileList = dir.listFiles();
        Arrays.sort(fileList);

        logger.info("Number of files:" + fileList.length);

        List<Course> courses = new ArrayList<>();

        for (File file : fileList) {
            courses.addAll(parseHTML(file, logger));
        }

        Collections.sort(courses);

        writeToFile(produceViewDataJson(courses), outputDirname, outputView, logger);
        writeToFile(produceSearchDataJson(courses), outputDirname, outputSearch, logger);
        writeToFile(produceMetadataJson(), outputDirname, outputMetadata, logger);

        long time_end = System.nanoTime();
        logger.info("Parsed " + courses.size() + " courses in " + TimeUnit.NANOSECONDS.toMinutes(time_end-time_start) + " seconds");

        return outputDirname;
    }

    private static void writeToFile(String body, String dirname, String filename, Logger logger) throws IOException {
        File dir = new File(dirname);
        if (!dir.exists()) {
            logger.info("Created directory:" + dir.getCanonicalPath());
            dir.mkdir();
        }

        File output = new File(dirname + "/" + filename);
        if (!output.exists()) {
            output.createNewFile();
        }
        FileWriter fw = new FileWriter(output.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(body);
        bw.close();
    }
}
