package shrumit;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException {
        System.out.println("Hello World!");

        String dirname = null;
        try {
            dirname = CoursePageDownloader.download();
        } catch (Exception e) {
            TraceWriter.trace("Exception calling CoursePageDownloader.download()");
            e.printStackTrace(System.out);
            System.exit(1);
        }

        try {
            CoursePageScraper.scrape(dirname);
        } catch (Exception e) {
            TraceWriter.trace("Exception calling CoursePageDownloader.scrape()");
            e.printStackTrace(System.out);
            System.exit(1);
        }

        System.out.println("Bye");
        System.exit(0);
    }
}
