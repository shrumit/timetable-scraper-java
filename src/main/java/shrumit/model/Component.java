package shrumit.model;

import java.util.ArrayList;
import java.util.List;

public class Component {
    public String name;
    public List<Section> sections;

    public Component() {
        this("");
    }

    public Component(String name) {
        this.name = name;
        sections = new ArrayList<Section>();
    }

}