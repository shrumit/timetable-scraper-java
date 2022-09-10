package tsj;

import tsj.model.Course;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tsj.ParsingUtils.*;

public class AsyncReader implements Runnable {

    private BlockingQueue<String> queue;

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

    public BlockingQueue<String> getQueue() {
        return queue;
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
        System.out.println("AsyncReader stop requested");

        // do not write results to file if thread was interrupted
        if (Thread.currentThread().isInterrupted())
            return;

        Collections.sort(courses);

        // assign ids
        int id = 0;
        for (var course : courses)
            course.id = id++;

        try {
            CommonUtils.saveToFile(ParsingUtils.produceViewDataJson(courses), outputDir, outputView, logger);
            CommonUtils.saveToFile(ParsingUtils.produceSearchDataJson(courses), outputDir, outputSearch, logger);
            CommonUtils.saveToFile(ParsingUtils.produceMetadataJson(), outputDir, outputMetadata, logger);

        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Exception calling parseHTML: " + e.getMessage(), e);
        }
    }

    public void stop() {
        stopRequested = true;
    }
}
