package tsj.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Metadata {
    public String time;
    List<String> campusTypes;
    List<String> deliveryTypes;

    public Metadata(List<String> campusTypes, List<String> deliveryTypes) {
        ZonedDateTime now = ZonedDateTime.now();
        time = now.format(DateTimeFormatter.RFC_1123_DATE_TIME);

        this.campusTypes = campusTypes;
        this.deliveryTypes = deliveryTypes;
    }
}