package shrumit;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CoursePageDownloader {

    // for debugging
    static final int startIdx = 0;
    static final int limit = Integer.MAX_VALUE;

    static final String URL = "https://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm";
    static final long CAPTCHA_SLEEP_SECONDS = 30;

    static final int ERROR_RETRY = 5;
    static final int CAPTCHA_RETRY = 10;

    Logger logger;

    public CoursePageDownloader(Logger logger) {
        this.logger = logger;
    }

    public void downloadAndSave(String storageDir) throws Exception {
        long time_start = System.nanoTime();

        // get list of subjects
        List<String> subjectCodes = getSubjects();
        logger.info("Number of subjects:" + subjectCodes.size());

        // make dir if not present
        File dir = new File(storageDir);
        if (!dir.exists()) {
            logger.info("Created directory:" + dir.getCanonicalPath());
            dir.mkdir();
        }

        // download and store each subject's webpage
        for (int i = startIdx; i < Math.min(subjectCodes.size(), limit); i++) {
            String code = subjectCodes.get(i);
            if (code.length() == 0)
                continue;

            logger.info("Attempting:" + i + ":" + code);

            String content = downloadCoursePageWithRetry(code, logger);
            String filepath = FileUtils.writeToFile(content, storageDir, code, logger);

            logger.info("Downloaded:" + i + ":" + code + " to " + filepath);
        }

        logger.info("Finished. Dirname:" + storageDir);

        long time_end = System.nanoTime();
        logger.info(TimeUnit.NANOSECONDS.toMinutes(time_end - time_start) + " minutes");
    }

    public void downloadAndStream(BlockingQueue<String> outputQueue) throws IOException, ExecutionException, InterruptedException {
        // add courseCode to a queue
        BlockingQueue<String> subjectCodes = new LinkedBlockingQueue<>();
        subjectCodes.addAll(getSubjects());
        logger.info("Number of subjects:" + subjectCodes.size());

        // spawn parallel downloaders
        CompletableFuture<Void>[] cfs = new CompletableFuture[3];
        for (int i = 0; i < cfs.length; i++) {
            int finalI = i;
            cfs[i] = CompletableFuture.runAsync(() -> {
                try {
                    String code = subjectCodes.poll();
                    while (code != null) {
                        logger.info("Runnable " + finalI + " processing " + code);
                        String content = downloadCoursePageWithRetry(code, logger);
                        outputQueue.put(content);

                        code = subjectCodes.poll();
                    }
                } catch (InterruptedException | IOException e) {
                    logger.log(Level.SEVERE, "Exception in runnable: " + e.getMessage(), e);
                    throw new CompletionException(e);
                }
            });

            logger.info("Spawned runnable:" + cfs[i].toString());
        }

        // wait for all of them
        CompletableFuture.allOf(cfs).get();
        logger.info("Finished. subjectCodes.size():" + subjectCodes.size());
    }

    private static List<String> getSubjects() throws IOException {
        Document doc = Jsoup.connect(URL).get();
        Element inputSubject = doc.getElementById("inputSubject");
        Elements codes = inputSubject.getElementsByTag("option");
        List<String> list = new ArrayList<>();
        for (var code : codes)
            list.add(code.val());

        return list;
    }

    private static String downloadCoursePageWithRetry(String code, Logger logger) throws IOException, InterruptedException {
        for (int captchaRetry = 0; captchaRetry < CAPTCHA_RETRY; captchaRetry++) {
            String content = "";
            boolean success = false;
            for (int errorRetry = 0; errorRetry < ERROR_RETRY; errorRetry++) {
                try {
                    content = downloadCoursePage(code);
                    success = true;
                    break;
                } catch (SocketTimeoutException e) {
                    logger.info("SocketTimeoutException. Retrying. " + e.getMessage());
                } catch (IOException e) {
                    logger.info("IOException. Retrying. " + e.getMessage());
                }
            }

            if (!success) {
                throw new IOException("Too many errored retries");
            }

            // check for captcha page
            if (content.length() < 3000 && content.contains("captcha")) {
                logger.info("Captcha. Sleeping " + CAPTCHA_SLEEP_SECONDS + " seconds.");
                Thread.sleep(TimeUnit.SECONDS.toMillis(CAPTCHA_SLEEP_SECONDS));
            } else {
                return content;
            }
        }

        throw new IOException("Too many captcha retries");
    }

    private static String downloadCoursePage(String courseCode) throws IOException {
        Connection connection = Jsoup.connect(URL).timeout(0).maxBodySize(0);
        connection.request().requestBody(
                String.format("subject=%s&Designation=Any&catalognbr=&CourseTime=All&Component=All&time=&end_time=&day=m&day=tu&day=w&day=th&day=f&LocationCode=Any&command=search",
                        courseCode));

        return connection.post().toString();
    }
}
