package tsj.model;

import java.util.logging.Logger;

public class Subject {
    boolean atMain = false, atKings = false, atHuron = false;
    String code;
    String name;
    int id;

    public Subject(Logger logger, String code, String name, String campusListRaw, int id) {
        this.code = code;
        this.name = name;
        this.id = id;
        String[] split = campusListRaw.trim().split(" ");
        if (split.length == 0) {
            logger.severe(String.format("campusClassRaw parsed as empty for: %s %s. campusClassRaw: %s", code, name, campusListRaw));
            throw new IllegalArgumentException();
        }

        for (String s : split) {
            if (s.equals("Any")) continue;
            switch (s) {
                case "MAIN" -> atMain = true;
                case "KINGS" -> atKings = true;
                case "HURON" -> atHuron = true;
                default ->
                        logger.warning(String.format("Unknown campus identifier: %s. code: %s, name: %s", s, code, name));
            }
        }
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Subject subject = (Subject) o;
//        return Objects.equals(code, subject.code);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hashCode(code);
//    }
}
