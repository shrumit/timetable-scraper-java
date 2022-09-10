package tsj.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Metadata {
    public String time;
    public Metadata() {
        ZonedDateTime now = ZonedDateTime.now();
        time = now.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static Metadata now() {
        return new Metadata();
    }
}