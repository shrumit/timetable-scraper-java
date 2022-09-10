package tsj;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.*;

public class App
{

    static final String outputView = "master.json";
    static final String outputSearch = "search.json";
    static final String outputMetadata = "metadata.json";

    static final String storageDirPrefix = "dump";
    static final String outputDirPrefix = "coutput";

    static final int THREADS = 3;

    public static void main( String[] args ) throws IOException, ExecutionException, InterruptedException {
        System.out.println("Hello World!");

        String runId = args.length == 0 ? dateTimeString() : args[0];
        System.out.println(runId);

        Logger logger = createLogger(runId);
        logger.info("Logger started. runId:" + runId);


        String storageDir = storageDirPrefix + runId;
        String outputDir = outputDirPrefix + runId;


        // get subjects
        List<String> subjectsList = DownloadJob.fetchSubjects();
        subjectsList.subList(1,subjectsList.size()).clear();
        logger.info("subjectsList.size():" + subjectsList.size());
        BlockingQueue<String> subjects = new ArrayBlockingQueue<String>(subjectsList.size());
        subjects.addAll(subjectsList);

        // spawn and execute DownloadJobs in parallel
        CompletableFuture<Void>[] cfs = new CompletableFuture[1];
        for (int i = 0; i < cfs.length; i++) {
            cfs[i] = CompletableFuture.runAsync(new DownloadJob(logger, subjects, storageDir));
            logger.info("Spawned runnable:" + cfs[i].toString());
        }

        // wait for all of them
        try {
            CompletableFuture.allOf(cfs).get();
        } catch (Exception e) {
            logger.severe(e.toString());
            throw e;
        }

        // parse
        ParsingJob pj = new ParsingJob(logger);

        pj.parseFromDir(storageDir);
        pj.saveOutput(outputDir, outputView, outputSearch, outputMetadata);

//        boolean enableConcurrentMode = System.getenv("ENABLE_CONCURRENT_MODE") != null && System.getenv("ENABLE_CONCURRENT_MODE").equalsIgnoreCase("true");
//        enableConcurrentMode = true;
//        logger.log(Level.INFO, "enableConcurrentMode:" + enableConcurrentMode);
//
//        CoursePageDownloader downloader = new CoursePageDownloader(logger);
//
//        if (enableConcurrentMode) {
//            try {
//                AsyncReader ar = new AsyncReader(logger, outputDir, outputView, outputSearch, outputMetadata);
//                var arCf = CompletableFuture.runAsync(ar);
//                downloader.downloadAndStream(ar.getQueue());
//                ar.stop();
//                arCf.get();
//            } catch (Exception e) {
//                logger.log(Level.SEVERE, "Exception in concurrent mode: " + e.getMessage(), e);
//                System.exit(1);
//            }
//        }
//        else {
//            try {
//                downloader.downloadAndSave(storageDir);
//            } catch (Exception e) {
//                logger.log(Level.SEVERE, "Exception calling CoursePageDownloader.download(): " + e.getMessage(), e);
//                System.exit(1);
//            }
//
//            try {
//                DirectoryReader dr = new DirectoryReader(logger, outputDir, outputView, outputSearch, outputMetadata);
//                dr.parse(storageDir);
//            } catch (Exception e) {
//                logger.log(Level.SEVERE, "Exception calling CoursePageDownloader.scrape(): " + e.getMessage(), e);
//                System.exit(1);
//            }
//        }

        logger.info("Program ending normally");
        System.out.println("Bye");
        System.exit(0);
    }

    private static String dateTimeString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HHmm");
        return LocalDateTime.now().format(formatter);
    }

    private static Logger createLogger(String runId) throws IOException {
        Logger logger = Logger.getLogger(runId);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        var formatter = new MyFormatter();

        var fileHandler = new FileHandler(runId + ".log");
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(formatter);
        logger.addHandler(fileHandler);

        var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        return logger;
    }

    static class MyFormatter extends SimpleFormatter {
        @Override
        public String format(LogRecord record) {
            return super.format(record).replaceAll("[\\t\\n\\r]+"," ") + System.lineSeparator();
        }
    }
}
