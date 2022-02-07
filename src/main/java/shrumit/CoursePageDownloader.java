package shrumit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class CoursePageDownloader {

    // for debugging
    static final int startIdx = 0;
    static final int limit = Integer.MAX_VALUE;

    static final String url = "http://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm";
    static final long CAPTCHA_SLEEP_SECONDS = 30;
    static final String folderPrefix = "dump";

    public static String download() throws Exception {
        long time_start = System.nanoTime();

        // get list of subjects
        Document doc = Jsoup.connect(url).get();
        Element subjectInput = doc.getElementById("inputSubject");
        Elements subjectCodes = subjectInput.getElementsByTag("option");
        TraceWriter.trace("Number of subjects:" + subjectCodes.size());

        String dirname = folderPrefix + dateString();

        File dir = new File(dirname);
        if (!dir.exists()){
            TraceWriter.trace("Created directory:" + dir.getCanonicalPath());
            dir.mkdir();
        }

        // download and store each subject's webpage
        for (int i = startIdx; i < Math.min(subjectCodes.size(), limit); i++) {
            String code = subjectCodes.get(i).val();
            if (code.length() == 0)
                continue;

            TraceWriter.trace("Attempting:" + i + ":" + code);
            String content = "";
            boolean success = false;
            for (int retry = 0; i < 5; i++) {
                try {
                    content = downloadCoursePage(code);
                    success = true;
                } catch (SocketTimeoutException e) {
                    TraceWriter.trace("Socket timeout exception. Retrying");
                } finally {
                    break;
                }
            }
            if (!success) {
                throw new Exception("Too many retries");
            }

            if (content.length() < 3000 && content.contains("captcha")) {
                TraceWriter.trace("Captcha. Sleeping " + CAPTCHA_SLEEP_SECONDS + " seconds.");
                Thread.sleep(TimeUnit.SECONDS.toMillis(CAPTCHA_SLEEP_SECONDS));
                i--; // retry same i
                continue;
            }

            // write to file
			File file = new File(dirname + "\\" + code); // WINDOWS
//            File file = new File(dirname + "/" + code); // LINUX
            file.createNewFile();
            FileWriter fw = new FileWriter(file.getCanonicalPath());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close(); // also closes fw

            TraceWriter.trace("Downloaded:" + i + ":" + code + " to " + file.getCanonicalPath());
        }
        TraceWriter.trace("Finished. Dirname:" + dirname);

        long time_end = System.nanoTime();
        TraceWriter.trace(TimeUnit.NANOSECONDS.toMinutes(time_end - time_start) + " minutes");

        return dirname;
    }

    private static String downloadCoursePage(String courseCode) throws IOException {
        Document doc = Jsoup.connect(url).data("subject", courseCode).data("Designation", "Any").data("catalognbr", "")
                .data("CourseTime", "All").data("Component", "All").data("time", "").data("end_time", "")
                .data("day", "m").data("day", "tu").data("day", "w").data("day", "th").data("day", "f")
                .data("Campus", "Any").data("command", "search").timeout(0).maxBodySize(0).get();

//		TraceWriter.write(doc.toString());
        return doc.toString();
    }

    private static String dateString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HHMM");
        return LocalDateTime.now().format(formatter);
    }
}
