package shrumit.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class Course implements Comparable<Course> {
    @Expose
    public int id;
    @Expose
    public String name;
    public List<Component> components;

    public Course() {
        this(".placeholder");
    }

    public Course(String name) {
        this(-1, name);
    }

    public Course(int id, String name) {
        this.id = id;
        this.name = name;
        components = new ArrayList<Component>();
    }

    @Override
    public int compareTo(Course o) {
        return name.compareTo(o.name);
    }
}