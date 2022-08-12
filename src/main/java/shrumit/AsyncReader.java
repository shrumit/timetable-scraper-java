package shrumit;

import shrumit.model.Course;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static shrumit.ParsingUtils.*;

public class AsyncReader implements Runnable {

    public BlockingQueue<String> queue;

    volatile boolean stopRequested;

    Logger logger;
    String outputDir;
    String outputView;
    String outputSearch;
    String outputMetadata;

    public AsyncReader(Logger logger, String outputDir, String outputView, String outputSearch, String outputMetadata) {
        this.logger = logger;
        this.outputDir = outputDir;
        this.outputView = outputView;
        this.outputSearch = outputSearch;
        this.outputMetadata = outputMetadata;
        queue = new ArrayBlockingQueue<>(500);
    }

    @Override
    public void run() {
        List<Course> courses = new ArrayList<>();

        while (!stopRequested) {
            String page = null;
            try {
                page = queue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                break;
            }
            if (page != null) {
                try {
                    courses.addAll(parseHTML(page, logger));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Exception calling parseHTML: " + e.getMessage(), e);
                    return;
                }
            }
        }

        // do not write results to file if thread was interrupted
        if (Thread.currentThread().isInterrupted())
            return;

        Collections.sort(courses);

        try {
            FileUtils.writeToFile(ParsingUtils.produceViewDataJson(courses), outputDir, outputView, logger);
            FileUtils.writeToFile(ParsingUtils.produceSearchDataJson(courses), outputDir, outputSearch, logger);
            FileUtils.writeToFile(ParsingUtils.produceMetadataJson(), outputDir, outputMetadata, logger);

        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Exception calling parseHTML: " + e.getMessage(), e);
        }
    }

    public void stop() {
        stopRequested = true;
    }
}
