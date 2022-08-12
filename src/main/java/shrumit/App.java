package shrumit;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class App
{

    static final String outputView = "master.json";
    static final String outputSearch = "search.json";
    static final String outputMetadata = "metadata.json";

    static final String storageDirPrefix = "dump";
    static final String outputDirPrefix = "coutput";

    public static void main( String[] args ) throws IOException {
        System.out.println("Hello World!");

        String runId = args.length == 0 ? dateTimeString() : args[0];
        System.out.println(runId);

        Logger logger = createLogger(runId);
        logger.info("Logger started. runId:" + runId);

        String storageDir = storageDirPrefix + runId;
        String outputDir = outputDirPrefix + runId;


        try {
            CoursePageDownloader downloader = new CoursePageDownloader(logger, storageDir);
            downloader.download(runId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception calling CoursePageDownloader.download(): " + e.getMessage(), e);
            System.exit(1);
        }

        try {
            DirectoryReader dr = new DirectoryReader(logger);
            dr.parse(storageDir, outputDir, outputView, outputSearch, outputMetadata);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception calling CoursePageDownloader.scrape(): " + e.getMessage(), e);
            System.exit(1);
        }

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
        var fileHandler = new FileHandler(runId + ".log");
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        return logger;
    }
}
