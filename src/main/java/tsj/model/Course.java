package tsj.model;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class Course implements Comparable<Course> {
    @Expose
    public int id;
    @Expose
    public String name;
    public List<Component> components;
    public Subject subject;

    public Course(String name, Subject subject, int id) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        components = new ArrayList<>();
    }

    public Component getComponent(String compName) {
        for (Component c : components) {
            if (c.name.equals(compName))
                return c;
        }
        return null;
    }

    public void addComponent(Component comp) {
        components.add(comp);
    }

    @Override
    public int compareTo(Course o) {
        return name.compareTo(o.name);
    }
}