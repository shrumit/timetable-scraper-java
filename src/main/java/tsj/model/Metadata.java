package tsj.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    public static class MetadataBuilder {
        Map<String, Integer> campusTypes;
        Map<String, Integer> deliveryTypes;

        public MetadataBuilder() {
            campusTypes = new HashMap<>();
            deliveryTypes = new HashMap<>();
        }

        public void submitCampusType(String c) {
            campusTypes.put(c, 1 + campusTypes.getOrDefault(c, 0));
        }

        public void submitDeliveryType(String d) {
            deliveryTypes.put(d, 1 + deliveryTypes.getOrDefault(d, 0));
        }

        public Metadata build() {
            // sort campusTypes by descending frequency
            List<String> ct = new ArrayList<>();
            ct.addAll(campusTypes.keySet());
            Collections.sort(ct, (a, b) -> Integer.compare(campusTypes.get(b), campusTypes.get(a)));

            // sort deliveryTypes by descending frequency
            List<String> dt = new ArrayList<>();
            dt.addAll(deliveryTypes.keySet());
            Collections.sort(dt, (a, b) -> Integer.compare(deliveryTypes.get(b), deliveryTypes.get(a)));
            return new Metadata(ct, dt);
        }

    }
}