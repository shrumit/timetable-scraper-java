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
    public static void main( String[] args ) throws IOException {
        System.out.println("Hello World!");

        String runId = args.length == 0 ? dateTimeString() : args[0];
        System.out.println(runId);

        Logger logger = createLogger(runId);
        logger.info("Logger started. runId:" + runId);

        String dirname = null;
        try {
            CoursePageDownloader downloader = new CoursePageDownloader(logger);
            dirname = downloader.download(runId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception calling CoursePageDownloader.download(): " + e.getMessage(), e);
            System.exit(1);
        }

        try {
            DirectoryReader.parse(dirname, logger);
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
