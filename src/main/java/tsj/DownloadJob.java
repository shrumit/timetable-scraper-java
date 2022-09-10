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

    static final int NETWORK_RETRY_LIMIT = 5;
    static final int CAPTCHA_RETRY_LIMIT = 10;

    Logger logger;
    BlockingQueue<String> subjects;
    String outputDir;

    static boolean inCaptchaState = false; // doesn't need to be volatile because it's only accessed inside synchronized blocks
    boolean captchaStateOwner = false;

    public DownloadJob(Logger logger, BlockingQueue<String> courseCodes, String outputDir) {
        this.logger = logger;
        this.subjects = courseCodes;
        this.outputDir = outputDir;
    }

    // runs until subjects is empty
    @Override
    public void run() {

        logger.info("Started DownloadJob");

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
            logger.severe("Fatal error in DownloadJob thread. Throwing RuntimeException.");
            logger.severe(e.toString());
            throw new RuntimeException();
        }

        logger.info("Ending DownloadJob normally");
    }

    private Document downloadCoursePageWithRetry(String subject) throws IOException, InterruptedException {
        for (int captchaRetry = 0; captchaRetry < CAPTCHA_RETRY_LIMIT; captchaRetry++) {

            // go into wait when another thread is doing captcha retries
            synchronized (DownloadJob.class) {
                if (inCaptchaState && !captchaStateOwner) {
                    logger.info("Waiting because inCaptchaState");
                    DownloadJob.class.wait();
                    logger.info("Done waiting.");
                }
            }

            Document page = downloadCoursePage(subject);

            // check for captcha page
            if (!page.getElementsContainingOwnText("captcha").isEmpty()) {
                logger.info(String.format("Detected captcha when downloading %s. captchaRetry:%s/%s", subject, captchaRetry, CAPTCHA_RETRY_LIMIT - 1));

                if (!captchaStateOwner) {
                    synchronized (DownloadJob.class) {
                        // this is the first thread to detect captcha hence it becomes captchaStateOwner. only the captchaStateOwner may enable/disable inCaptchaState
                        if (!inCaptchaState) {
                            logger.info("Enabling inCaptchaState");
                            captchaStateOwner = true;
                            inCaptchaState = true;
                        } else {
                            // although this thread also downloaded a captcha page, another thread beat it for becoming captchaStateOwner
                            // continue so that this thread ends up at wait() with the other threads
                            captchaRetry--;
                            continue;
                        }
                    }
                }

                logger.info("Sleeping " + CAPTCHA_SLEEP_SECONDS + " seconds.");
                Thread.sleep(TimeUnit.SECONDS.toMillis(CAPTCHA_SLEEP_SECONDS));

            } else {
                // captchaStateOwner exits inCaptchaState and notifies waiting threads
                if (captchaStateOwner) {
                    synchronized (DownloadJob.class) {
                        logger.info("Disabling inCaptchaState and notifying");
                        inCaptchaState = false;
                        captchaStateOwner = false;
                        DownloadJob.class.notifyAll();
                    }
                }
                return page;
            }
        }

        throw new IOException("Too many captcha retries");
    }

    private Document downloadCoursePage(String subject) throws IOException {

        for (int networkRetries = 0; networkRetries < NETWORK_RETRY_LIMIT; networkRetries++) {
            try {
                Connection connection = Jsoup.connect(URL).timeout(0).maxBodySize(0);
                connection.request().requestBody(
                        String.format("subject=%s&Designation=Any&catalognbr=&CourseTime=All&Component=All&time=&end_time=&day=m&day=tu&day=w&day=th&day=f&LocationCode=Any&command=search",
                                subject));

                return connection.post();
            } catch (IOException e) {
                logger.warning(String.format("Exception when downloading %s networkRetries:%s/%s %s", subject, networkRetries, NETWORK_RETRY_LIMIT - 1, e));
            }
        }

        throw new IOException("Too many errored retries");
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
