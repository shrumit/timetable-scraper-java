package tsj;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DownloadJob implements Runnable {

    static final String URL = "https://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm";
    static final long CAPTCHA_SLEEP_SECONDS = 30;

    static final int ERROR_RETRY = 5;
    static final int CAPTCHA_RETRY = 10;

    Logger logger;
    BlockingQueue<String> subjects;
    String outputDir;

    public DownloadJob(Logger logger, BlockingQueue<String> courseCodes, String outputDir) {
        this.logger = logger;
        this.subjects = courseCodes;
        this.outputDir = outputDir;
    }

    // runs until subjects is empty
    @Override
    public void run() {

        logger.info(String.format("Started DownloadJob on thread %s\n", Thread.currentThread().getName()));

        try {
            while (true) {
                String subject = subjects.poll();
                if (subject == null) {
                    break;
                }

                // download subject page
                Document page = downloadCoursePageWithRetry(subject);
                logger.info(String.format("Downloaded %s to memory", subject));

                // save to file
                String savedFilepath = CommonUtils.saveToFile(page.toString(), outputDir, subject, logger);
                logger.info(String.format("Saved %s to file %s", subject, savedFilepath));
            }
        } catch (Exception e) {
            logger.severe(String.format("Fatal error in DownloadJob thread%s", Thread.currentThread().getName()));
            logger.severe(e.toString());
            throw new RuntimeException();
        }

        logger.info(String.format("Ending DownloadJob normally on thread %s", Thread.currentThread().getName()));
    }

    private Document downloadCoursePageWithRetry(String subject) throws IOException, InterruptedException {
        for (int captchaRetry = 0; captchaRetry < CAPTCHA_RETRY; captchaRetry++) {
            Document page = null;
            for (int errorRetry = 0; errorRetry < ERROR_RETRY; errorRetry++) {
                try {
                    logger.info(String.format("Attempting to download %s. captchaRetry:%s/%s. errorRetry:%s/%s", subject, captchaRetry, CAPTCHA_RETRY - 1, errorRetry, ERROR_RETRY - 1));
                    page = downloadCoursePage(subject);
                    break;
                } catch (IOException e) {
                    logger.info(e.toString());
                }
            }

            if (page == null) {
                throw new IOException("Too many errored retries");
            }

            // check for captcha page
            if (!page.getElementsContainingOwnText("captcha").isEmpty()) {
                logger.info("Captcha. Sleeping " + CAPTCHA_SLEEP_SECONDS + " seconds.");
                Thread.sleep(TimeUnit.SECONDS.toMillis(CAPTCHA_SLEEP_SECONDS));
            } else {
                return page;
            }
        }

        throw new IOException("Too many captcha retries");
    }

    private Document downloadCoursePage(String subject) throws IOException {
        Connection connection = Jsoup.connect(URL).timeout(0).maxBodySize(0);
        connection.request().requestBody(
                String.format("subject=%s&Designation=Any&catalognbr=&CourseTime=All&Component=All&time=&end_time=&day=m&day=tu&day=w&day=th&day=f&LocationCode=Any&command=search",
                        subject));

        return connection.post();
    }

    public static List<String> fetchSubjects() throws IOException {
        Document doc = Jsoup.connect(URL).get();
        Element inputSubject = doc.getElementById("inputSubject");
        Elements codes = inputSubject.getElementsByTag("option");
        List<String> list = new ArrayList<>();
        for (var code : codes) {
            String val = code.val();
            if (val.length() != 0)
                list.add(val);
        }

        return list;
    }
}
