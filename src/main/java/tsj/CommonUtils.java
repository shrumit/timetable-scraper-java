package tsj;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class CommonUtils {

    public static String saveToFile(String body, String dirname, String filename, Logger logger) throws IOException {
        File dir = new File(dirname);
        if (!dir.exists()) {
            logger.info("Created directory:" + dir.getCanonicalPath());
            dir.mkdir();
        }

        File output = new File(dirname + File.separator + filename);
        if (!output.exists()) {
            output.createNewFile();
        }
        FileWriter fw = new FileWriter(output.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(body);
        bw.close(); // also closes fw
        return output.getCanonicalPath();
    }

}
