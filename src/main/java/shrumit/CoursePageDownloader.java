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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CoursePageDownloader {

    // for debugging
    static final int startIdx = 0;
    static final int limit = Integer.MAX_VALUE;

    static final String url = "https://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm";
    static final long CAPTCHA_SLEEP_SECONDS = 30;
    static final String folderPrefix = "dump";

    Logger logger;

    public CoursePageDownloader(Logger logger) {
        this.logger = logger;
    }

    public String download(String runId) throws Exception {
        long time_start = System.nanoTime();

        // get list of subjects
        Document doc = Jsoup.connect(url).get();
        Element subjectInput = doc.getElementById("inputSubject");
        Elements subjectCodes = subjectInput.getElementsByTag("option");
        logger.info("Number of subjects:" + subjectCodes.size());

        String dirname = folderPrefix + runId;

        File dir = new File(dirname);
        if (!dir.exists()){
            logger.info("Created directory:" + dir.getCanonicalPath());
            dir.mkdir();
        }

        // download and store each subject's webpage
        for (int i = startIdx; i < Math.min(subjectCodes.size(), limit); i++) {
            String code = subjectCodes.get(i).val();
            if (code.length() == 0)
                continue;

            logger.info("Attempting to download:" + i + ":" + code);

            String content = "";
            boolean success = false;
            for (int retry = 0; retry < 5; retry++) {
                try {
                    content = downloadCoursePage(code);
                    success = true;
                    break;
                } catch (SocketTimeoutException e) {
                    logger.info("SocketTimeoutException. Retrying. " + e.getMessage());
                } catch (HttpStatusException e) {
                    logger.info("HttpStatusException. Retrying. " + e.getMessage());
                }
            }
            if (!success) {
                throw new Exception("Too many retries");
            }

            if (content.length() < 3000 && content.contains("captcha")) {
                logger.info("Captcha. Sleeping " + CAPTCHA_SLEEP_SECONDS + " seconds.");
                Thread.sleep(TimeUnit.SECONDS.toMillis(CAPTCHA_SLEEP_SECONDS));
                i--; // retry same i
                continue;
            }

            // write to file
            File file = new File(dirname + File.separator + code);
            file.createNewFile();
            FileWriter fw = new FileWriter(file.getCanonicalPath());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close(); // also closes fw

            logger.info("Downloaded:" + i + ":" + code + " to " + file.getCanonicalPath());
        }
        logger.info("Finished. Dirname:" + dirname);

        long time_end = System.nanoTime();
        logger.info(TimeUnit.NANOSECONDS.toMinutes(time_end - time_start) + " minutes");

        return dirname;
    }

    private String downloadCoursePage(String courseCode) throws IOException {
        Connection connection = Jsoup.connect(url).timeout(0).maxBodySize(0);
        connection.request().requestBody(
                String.format("subject=%s&Designation=Any&catalognbr=&CourseTime=All&Component=All&time=&end_time=&day=m&day=tu&day=w&day=th&day=f&LocationCode=Any&command=search",
                courseCode));

        return connection.post().toString();
    }
}
