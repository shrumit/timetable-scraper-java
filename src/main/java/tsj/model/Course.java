package tsj.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class Course implements Comparable<Course> {
    @Expose
    public int id;
    @Expose
    public String name;
    public List<Component> components;

    public Course(String name) {
        this(name, -1);
    }

    public Course(String name, int id) {
        this.id = id;
        this.name = name;
        components = new ArrayList<Component>();
    }

    @Override
    public int compareTo(Course o) {
        return name.compareTo(o.name);
    }
}