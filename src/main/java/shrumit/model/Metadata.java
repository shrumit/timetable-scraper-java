package shrumit.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Metadata {
    public String time;
    public Metadata() {
        ZonedDateTime now = ZonedDateTime.now();
        time = now.format(DateTimeFormatter.RFC_1123_DATE_TIME);
        System.out.println(time);
    }
}