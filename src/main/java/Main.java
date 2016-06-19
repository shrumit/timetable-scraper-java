import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

	public static void main(String[] args) throws IOException {

		Pattern regex = Pattern.compile("(.*) -.*");

		int count = 0;

		File dir = new File("dump1");
		File[] fileList = dir.listFiles();

		List<Course> master_list = new ArrayList<>();

		if (fileList != null) {

			// for each file in directory
			for (File file : fileList) {
				Document doc = Jsoup.parse(file, "UTF-8", "");
				Elements courseList = doc.getElementsByClass("table-striped");

				// for each course in file
				for (Element course : courseList) {
					Course data = new Course();
					List<Component> comp_list = new ArrayList<>();
					List<Section> section_list = new ArrayList<>();

					// extract course name
					Matcher m = regex.matcher(course
							.getElementsByTag("caption").first().text());
					m.find();
					data.name = m.group(1);

					Component temp_comp = new Component();
					Section temp_section = new Section();

					Element body = course.select("tbody").first();
					Elements rows = body.getElementsByTag("tr");

					boolean flag = false;

					// per row of table
					for (Element row : rows) {
						if (flag) {
							flag = !flag;
							continue;
						}

						System.out.println("in row");

						Elements td = row.getElementsByTag("td");

						// make new temp_comp if required
						if (temp_comp.name == null)
							temp_comp = new Component(td.get(1).text());
						else if (!td.get(1).text().equals(temp_comp.name)) {
							comp_list.add(temp_comp);
							temp_comp = new Component(td.get(1).text());
						}

						// make new temp_section if required
						if (temp_section.number == -1)
							temp_section = new Section();
						else if (!td.get(0).text().equals(temp_section.number)) {
							temp_section = new Section();
							section_list.add(temp_section);
						}

						System.out.println("td get 3:" + td.get(3));
						Integer[] days = parse_days(td.get(3));
						System.out.println("days len:" + days.length);

						for (Integer day : days) {
							if (day == null)
								break;
							Timeslot ts = new Timeslot();
							ts.day = (int) day;
							ts.startTime = parse_time(td.get(4).text());
							ts.endTime = parse_time(td.get(5).text());
							System.out.println("ts.day" + ts.day);
							temp_section.add(ts);
						}
						flag = !flag;
					}
					section_list.add(temp_section);
					temp_comp.sections = section_list.toArray(new Section[1]);
					comp_list.add(temp_comp);
					data.components = comp_list.toArray(new Component[1]);
					master_list.add(data);
					System.out.println("ADDED");
				}
			}
		} else {
			System.out.println("Not a directory");
		}

		System.out.println("master list size " + master_list.size());
		Gson gson = new Gson();

		File output = new File("output.txt");
		if (!output.exists()) {
			output.createNewFile();
		}
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(gson.toJson(master_list));
		bw.close();

	}// end of method main

	static Integer[] parse_days(Element e) {
		List<Integer> list = new ArrayList<Integer>();
		Elements tds = e.getElementsByTag("tr").first().getElementsByTag("td");
		int count = 1;
		for (Element td : tds) {
			// System.out.println("td text:" + td.text()+":");
			if (td.text().matches("M|Tu|W|Th|F"))
				list.add(count);
			count++;
		}
		System.out.println(list.toString());
		return list.toArray(new Integer[1]);
	}// end of method parse_days

	static int parse_time(String time) {
		return 1;
	}

}// end of class Main

class Course {
	String name;
	Component[] components;
}

class Component {
	String name;
	Section[] sections;

	Component() {
		name = null;
		ArrayUtils.nullToEmpty(sections);
	}

	Component(String name) {
		this.name = name;
		ArrayUtils.nullToEmpty(sections);
	}

	void add(Section e) {
		List<Section> temp = new ArrayList<Section>(Arrays.asList(sections));
		temp.add(e);
	}

}

class Section {
	int number;
	List<Timeslot> timeslots;

	Section() {
		number = -1;
		timeslots = new ArrayList<Timeslot>();
	}

	void add(Timeslot e) {
		timeslots.add(e);
	}
}

class Timeslot {
	int day;
	int startTime;
	int endTime;
}