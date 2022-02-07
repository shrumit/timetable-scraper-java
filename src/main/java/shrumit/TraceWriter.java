package shrumit;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TraceWriter {
    public static boolean disabled = false;
    public static void trace(String message) {
        if (disabled)
            return;

        System.out.printf("[%s] %s\n", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), message);
    }
}
