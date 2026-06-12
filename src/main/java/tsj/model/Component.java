package tsj.model;

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

    public Section getSection(String sectionName) {
        for (Section s : sections) {
            if (s.name.equals(sectionName))
                return s;
        }
        return null;
    }

    public void addSection(Section section) {
        sections.add(section);
    }
}