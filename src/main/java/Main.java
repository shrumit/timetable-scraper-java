import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class Main {

	public static void main(String[] args) throws IOException {

		long time_start = System.nanoTime();

		int count = 0;

		// Retrieve files in directory
		File dir = new File("dump");
		File[] fileList = dir.listFiles();

		List<Course> master_list = new ArrayList<>();
		StringBuilder termA = new StringBuilder("[[");
		StringBuilder termB = new StringBuilder("[");
		
		// For search data arrays
		Gson gsonX = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.create();

		// Regex to shorten name
		Pattern shortname_regex = Pattern.compile("(.{1,4}).* (\\d{4}\\w{0,1}).*");

		// Regex for selecting course code suffix
		Pattern suffix_regex = Pattern.compile(".*\\d{4}(\\w).*");
		
		if (fileList != null) {

			// for each file in directory
			for (File file : fileList) {
				Document doc = Jsoup.parse(file, "UTF-8", "");
				Elements courseList = doc.getElementsByClass("table-striped");

				// for each course in file
				for (Element course : courseList) {
					Course c = new Course();

					// extract course name
					c.text = course.getElementsByTag("caption").first().text();
					c.id = count;
					
					Element body = course.select("tbody").first();
					Elements rows = body.select("> tr");

					Component tempcomp = new Component();
					Section tempsect = new Section();

					// For every row in course table
					for (Element row : rows) {
						Elements td = row.select("> td");
						String rowsect = td.get(0).text();
						String rowcomp = td.get(1).text();

						// if new component in current row
						if (!tempcomp.name.equals(rowcomp)) {
							tempcomp.add(tempsect);
							c.add(tempcomp);
							tempcomp = new Component(rowcomp);
							tempsect = new Section(rowsect);
						}
						// if new section in current row
						else if (!tempsect.name.equals(rowsect)) {
							tempcomp.add(tempsect);
							tempsect = new Section(rowsect);
						}

						// Get times
						String start = td.get(4).text();
						String end = td.get(5).text();

						// Get str1 and str2, short versions of course name and section
						Matcher m = shortname_regex.matcher(c.text);
						m.find();
						String str1 = m.group(1) + " " + m.group(2);
						String str2 = tempcomp.name + " " + tempsect.name;
						// System.out.println(str1 + str2);

						// Get days and make timeslot per day
						Elements days = td.get(3).getElementsByTag("td");
						for (int i = 1; i < days.size(); i++) {
							if (!days.get(i).text().equals("\u00a0")) {
								Timeslot tempts = new Timeslot(i-1, start, end,
										str1, str2, count);
								tempsect.timeslots.add(tempts);
							}
						}
					}

					tempcomp.add(tempsect);
					c.add(tempcomp);
					
					master_list.add(c);

					// MAKE SEARCH DATA
					
					String suffix;
					Matcher m = suffix_regex.matcher(c.text);
					// if match found, assign to suffix, else assign ""
					if (m.find())
						suffix = m.group(1);
					else
						suffix = "";

					// A term
					if (suffix.equals("A") || suffix.equals("F")
							|| suffix.equals("W") || suffix.equals("Q")
							|| suffix.equals("R"))
						termA.append(gsonX.toJson(c)).append(",");
					// B term
					else if (suffix.equals("B") || suffix.equals("G")
							|| suffix.equals("X") || suffix.equals("S")
							|| suffix.equals("T"))
						termB.append(gsonX.toJson(c)).append(",");
					// Both terms
					else if (suffix.equals("") || suffix.equals("E")
							|| suffix.equals("Y") || suffix.equals("Z")
							|| suffix.equals("U")) {
						termA.append(gsonX.toJson(c)).append(",");
						termB.append(gsonX.toJson(c)).append(",");
					} else
						System.out.println("Unexpected suffix: " + c.text);
					count++;
				}
			}

		} else {
			System.out.println("Not a directory");
		}

		Gson gson = new Gson();

		// Save master_list objects to file
		File output = new File("master.json");
		if (!output.exists()) {
			output.createNewFile();
		}
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(gson.toJson(master_list));
		bw.close();

		// remove trailing commas
		termA.deleteCharAt(termA.length() - 1);
		termB.deleteCharAt(termB.length() - 1);

		termA.append("],");
		termB.append("]]");

		output = new File("search.json");
		if (!output.exists()) {
			output.createNewFile();
		}

		fw = new FileWriter(output.getAbsoluteFile());
		bw = new BufferedWriter(fw);
		bw.write(termA.toString());
		bw.write(termB.toString());
		bw.close();

		long time_end = System.nanoTime();
		System.out.println("Scrapped " + master_list.size() + " courses in "
				+ (time_end - time_start) / 1000000 + "ms");
	}// end of method main

}// end of class Main

class Course {
	@Expose
	public int id;
	@Expose
	public String text;
	public ArrayList<Component> components;

	Course() {
		components = new ArrayList<Component>();
	}

	public void add(Component comp) {
		// if component isn't unnamed or empty
		if (!comp.name.equals("") && (comp.sections.size() > 0)) {
			Iterator<Component> iterator = components.iterator();
			// if same named Component exists then append sections to it
			while (iterator.hasNext()) {
				Component cur = iterator.next();
				if (cur.name.equals(comp.name)) {
					cur.sections.addAll(comp.sections);
					return;
				}
			}
			components.add(comp);
		}
	}
}

class Component {
	public String name;
	public ArrayList<Section> sections;

	Component() {
		this("");
	}

	Component(String name) {
		this.name = name;
		sections = new ArrayList<Section>();
	}

	public void add(Section sec) {
		if ((!sec.name.equals("")) && (sec.timeslots.size() > 0))
			sections.add(sec);
	}

	public void append(Component in) {

	}
}

class Section {
	public String name;
	public ArrayList<Timeslot> timeslots;

	Section() {
		this("");
	}

	Section(String name) {
		this.name = name;
		timeslots = new ArrayList<Timeslot>();
	}

}

class Timeslot {
	public int day;
	public int start;
	public int len;
	public int timebit;
	public String str1;
	public String str2;
	public int id;
	
	Timeslot(int day, String start, String end, String str1, String str2, int id) {
		this.day = day;
		this.str1 = str1;
		this.str2 = str2;
		this.id = id;
		parseTime(start, end);
	}

	void parseTime(String start, String end) {
		this.start = convertTime(start);
		this.len = convertTime(end) - this.start;
		int mask = 0;
		mask = ~mask;
		mask <<= len;
		mask = ~mask;
		mask <<=  this.start;
		this.timebit = mask;
	}

	int convertTime(String str) {
		int total = 0;
		String[] one = str.split(" ");
		if (one[1].equals("PM"))
			total = 24;
		String[] two = one[0].split(":");
		int hour = Integer.parseInt(two[0]);
		int minute = Integer.parseInt(two[1]);
		if (hour == 12)
			hour = 0;
		total += 2 * (hour - 8);
		if (minute != 0)
			total += 1;

		return total;
	}

}