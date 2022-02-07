package shrumit.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class Course {
    @Expose
    public int id;
    @Expose
    public String name;
    public List<Component> components;

    public Course() {
        components = new ArrayList<Component>();
    }

    public Course(int id, String name) {
        this.id = id;
        this.name = name;
        components = new ArrayList<Component>();
    }

}