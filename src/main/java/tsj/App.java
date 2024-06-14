package tsj;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;

public class App {

    static final String OutputMaster = "master.json";
    static final String OutputSearch = "search.json";
    static final String OutputMetadata = "metadata.json";

    static final String StorageDirPrefix = "dump";
    static final String OutputDirPrefix = "coutput";

    static final int DEFAULT_THREADS = 3;

    private static Options createOptions() {
        final Options options = new Options();

        Option logToFile = Option.builder("logToFile").build();
        options.addOption(logToFile);

        Option runId = Option.builder("runId").hasArg().build();
        options.addOption(runId);

        Option downloadThreads = Option.builder("downloadThreads").hasArg().build();
        options.addOption(downloadThreads);

        Option parseOnly = Option.builder("parseOnly").build();
        options.addOption(parseOnly);

        return options;
    }

    public record SubjectEvent(int idx, String subject) { }

    public static void main(String[] args) throws Exception {
        System.out.println("Hello World!");

        CommandLine cmd = new DefaultParser().parse(createOptions(), args);

        // determine runId
        String runId = cmd.getOptionValue("runId");
        if (runId == null) runId = dateTimeString();
        System.out.println(runId);

        // init logger
        Logger logger = createLogger(runId, cmd.hasOption("logToFile"));
        logger.info("Logger started. runId:" + runId);

        // determine dirnames
        String storageDir = StorageDirPrefix + runId;
        String outputDir = OutputDirPrefix + runId;

        // determine threads count
        String threadsArg = cmd.getOptionValue("downloadThreads");
        int threads = threadsArg != null ? Integer.parseInt(threadsArg) : DEFAULT_THREADS;

        // determine parseOnly
        boolean parseOnly = false;
        if (cmd.hasOption("parseOnly"))
            parseOnly = true;

        // manual overrides for testing
//        parseOnly = true;
//        storageDir = "dump123";
//        outputDir = "coutput123";

        // download
        if (!parseOnly) {
            download(logger, threads, storageDir);
        }

        // parse
        parse(logger, storageDir, outputDir);


        logger.info("Program ending normally");
        System.out.println("Bye");
        System.exit(0);
    }

    private static void download(Logger logger, int threads, String storageDir) throws Exception {
        // get subjects
        List<String> subjectsList = DownloadJob.fetchSubjects();
        logger.info("subjectsList.size():" + subjectsList.size());
        BlockingQueue<SubjectEvent> subjectsChannel = new ArrayBlockingQueue<>(subjectsList.size());

        for (int i = 0; i < subjectsList.size(); i++) {
            subjectsChannel.add(new SubjectEvent(i, subjectsList.get(i)));
        }

        // spawn and execute DownloadJobs in parallel
        CompletableFuture<Void>[] cfs = new CompletableFuture[threads];
        for (int i = 0; i < cfs.length; i++) {
            cfs[i] = CompletableFuture.runAsync(new DownloadJob(
                    logger, storageDir, subjectsChannel, subjectsList.size()));
            logger.info("Spawned runnable:" + cfs[i].toString());
        }

        // wait for all of them
        try {
            CompletableFuture.allOf(cfs).get();
            logger.info("All runnables ended");
        } catch (Exception e) {
            logger.severe(e.toString());
            throw e;
        }
    }

    private static void parse(Logger logger, String storageDir, String outputDir) throws Exception {
        ParsingJob pj = new ParsingJob(logger);

        pj.parseFromDir(storageDir);
        pj.saveOutput(outputDir, OutputMaster, OutputSearch, OutputMetadata);
    }

    private static String dateTimeString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HHmm");
        return LocalDateTime.now().format(formatter);
    }

    private static Logger createLogger(String runId, boolean addFileHandler) throws IOException {
        Logger logger = Logger.getLogger(runId);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        var formatter = new MyFormatter();

        if (addFileHandler) {
            var fileHandler = new FileHandler(runId + ".log");
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        }

        var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        return logger;
    }

    static class MyFormatter extends SimpleFormatter {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss Z");

        @Override
        public String format(LogRecord record) {
            // see https://github.com/openjdk/jdk17/blob/4afbcaf55383ec2f5da53282a1547bac3d099e9d/src/java.logging/share/classes/java/util/logging/SimpleFormatter.java

            ZonedDateTime zdt = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

            String className = record.getSourceClassName();
            String methodName = record.getSourceMethodName();

            String source = String.format(
                    "%s#%s",
                    className != null ? className : "<unknown>",
                    methodName != null ? methodName : "<unknown>"
            );

            String thread = Thread.currentThread().getName();
            String message = formatMessage(record);

            String throwable = "";
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }

            return String.format(
                    "[%s] %s %s %s :::: %s %s\n",
                    zdt.format(dateFormat),
                    record.getLevel(),
                    source,
                    thread,
                    message,
                    throwable);
        }
    }
}
