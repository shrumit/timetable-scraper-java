/*
 * Copyright (C) Shrumit Mehta 2017
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.*;

public class Main {

	static final String inputDir = "dump";
	static final String outputMaster = "master.json";
	static final String outputSearch = "search.json";

	public static void main(String[] args) throws IOException {

		long time_start = System.nanoTime();

		int count = 0;

		// Retrieve files in directory
		File dir = new File(inputDir);
		File[] fileList = dir.listFiles();

		List<Course> master_list = new ArrayList<>();
		StringBuilder termA = new StringBuilder("[[");
		StringBuilder termB = new StringBuilder("[");

		// For search data arrays
		Gson gsonX = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.create();

		// Regex to shorten name
		Pattern shortname_regex = Pattern
				.compile("(.{1,4}).* (\\d{4}\\w{0,1}).*");

		// Regex for selecting course code suffix
		Pattern suffix_regex = Pattern.compile(".*\\d{4}(\\w).*");

		if (fileList != null) {

			// for each file in directory
			for (File file : fileList) {
				Document doc = Jsoup.parse(file, "UTF-8", "");
				Elements courseNames = doc.getElementsByTag("h4");
				Elements courseList = doc.getElementsByClass("table-striped");
				if (courseNames.size() != courseList.size()){
					System.out.println("Size mismatch.");
					return;
				}
				
				// for each course in file
				for (int i = 0; i < courseNames.size(); i++) {
					Element course = courseList.get(i);
					Course c = new Course();

					c.id = count;
					c.text = courseNames.get(i).text();

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

						// Get str1 and str2, short versions of course name and
						// section
						Matcher m = shortname_regex.matcher(c.text);
						m.find();
						String str1 = m.group(1) + " " + m.group(2);
						String str2 = tempcomp.name + " " + tempsect.name;
						// System.out.println(str1 + str2);

						// Get days and make timeslot per day
						Elements days = td.get(3).getElementsByTag("td");
						for (int j = 1; j < days.size(); j++) {
							if (!days.get(j).text().equals("\u00a0")) {
								Timeslot tempts = new Timeslot(j - 1, start,
										end, str1, str2, count);
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
		File output = new File(outputMaster);
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

		// Save search
		output = new File(outputSearch);
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

}