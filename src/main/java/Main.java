
/*
 * Copyright (C) Shrumit Mehta 2019
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

package timetable_scraper_java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import timetable_scraper_java.model.*;

public class Main {

	static final String inputDirName = "dump22_07_2020";
	static final String outputDirName = inputDirName.replace("dump", "coutput");

	static final String outputView = "master.json";
	static final String outputSearch = "search.json";
	static final String outputCompute = "compute.json";
	static final String outputMetadata = "metadata.json";

	// Regex to shorten name
	static final Pattern shortname_regex = Pattern.compile("(.{1,4}).* (\\d{4}\\w{0,1}).*");
	// Regex for selecting course code suffix
	static final Pattern suffix_regex = Pattern.compile(".*\\d{4}(\\w).*");

	public static void main(String[] args) throws IOException {

		long time_start = System.nanoTime();

		// Retrieve files in directory
		File dir = new File(inputDirName);
		File[] fileList = dir.listFiles();
		System.out.println("Number of files:" + fileList.length);

		Document[] docList = new Document[fileList.length];
		for (int i = 0; i < fileList.length; i++)
			docList[i] = Jsoup.parse(fileList[i], "UTF-8", "");

		List<Course> courses = extractCourses(docList);

		produceViewData(courses, outputView);
		produceSearchData(courses, outputSearch);
//		produceComputeData(courses, outputCompute);
		produceMetadata(outputMetadata);

		long time_end = System.nanoTime();
		System.out.println("Parsed " + courses.size() + " courses in " + (time_end - time_start) / 1000000 + "ms");
	}

	private static void produceMetadata(String filename) throws IOException {
		Gson gson = new Gson();
		Metadata metadata = new Metadata();
		writeToFile(gson.toJson(metadata), outputDirName, filename);
	}

	private static void produceViewData(List<Course> courses, String filename) throws IOException {
		Gson gson = new Gson();
		writeToFile(gson.toJson(courses), outputDirName, filename);
	}

	private static void produceSearchData(List<Course> courses, String filename) throws IOException {
		Gson gsonX = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		StringJoiner termA = new StringJoiner(",", "[", "]");
		StringJoiner termB = new StringJoiner(",", "[", "]");

		for (Course course : courses) {
			String suffix;
			Matcher m = suffix_regex.matcher(course.name);
			// if match found, assign to suffix, else assign ""
			if (m.find())
				suffix = m.group(1);
			else
				suffix = "";

			// A term
			if (suffix.equals("A") || suffix.equals("F") || suffix.equals("W") || suffix.equals("Q")
					|| suffix.equals("R"))
				termA.add(gsonX.toJson(course));
			// B term
			else if (suffix.equals("B") || suffix.equals("G") || suffix.equals("X") || suffix.equals("S")
					|| suffix.equals("T"))
				termB.add(gsonX.toJson(course));
			// Both terms
			else if (suffix.equals("") || suffix.equals("E") || suffix.equals("Y") || suffix.equals("Z")
					|| suffix.equals("U")) {
				termA.add(gsonX.toJson(course));
				termB.add(gsonX.toJson(course));
			} else
				System.out.println("Unexpected suffix: " + course.name);
		}

		writeToFile("[" + termA.toString() + "," + termB.toString() + "]", outputDirName, filename);
	}

	private static void produceComputeData(List<Course> courses, String filename) throws IOException {
		Map<String, int[]> map = new LinkedHashMap<>();
		for (Course course : courses) {
			for (int i = 0; i < course.components.size(); i++) {
				for (int j = 0; j < course.components.get(i).sections.size(); j++) {
					map.put(course.id + "-" + i + "-" + j, course.components.get(i).sections.get(j).timebits);
				}
			}
		}
		System.out.println("Final compute map size:" + map.size());
		Gson gson = new Gson();
		writeToFile(gson.toJson(map), outputDirName, filename);
	}

	private static List<Course> extractCourses(Document[] docList) {
		List<Course> courses = new ArrayList<>();
		for (Document doc : docList) {
			Elements names = doc.getElementsByTag("h4");
			Elements tables = doc.getElementsByClass("table-striped");
			if (names.size() != tables.size()) {
				throw new RuntimeException("Size mismatch");
			}

			// for each course in file
			for (int i = 0; i < names.size(); i++) {
				Course course = new Course(courses.size(), names.get(i).text());
				System.out.println("Parsing course:" + course.name);
				Elements rows = tables.get(i).select("tbody").first().select("> tr");

				Map<String, Map<String, Section>> compMap = new LinkedHashMap<>();

				// For every row in course table
				for (Element row : rows) {

					// parse names
					Elements td = row.select("> td");
					String compName = td.get(1).text();
					String sectionName = td.get(0).text();

					if (!compMap.containsKey(compName)) { // encountered a new component
						compMap.put(compName, new LinkedHashMap<String, Section>());
					}
					if (!compMap.get(compName).containsKey(sectionName)) { // encountered a new section
						Section section = new Section(sectionName);
						section.number = td.get(2).text();
						section.location = td.get(6).text();
						section.instructor = td.get(7).text();
						section.campus = td.get(10).text();
						compMap.get(compName).put(sectionName, section);
					}

					// parse time
					String startTime = td.get(4).text().trim();
					String endTime = td.get(5).text().trim();

					if (startTime.length() == 0 || endTime.length() == 0)
						continue;

					// fix exceptions
					if (startTime.equals("7:00 AM"))
						startTime = "8:00 AM";
					if (endTime.equals("10:30 PM"))
						endTime = "10:00 PM";

					Elements days = td.get(3).getElementsByTag("td");
					// figure out which days are being selected
					for (int j = 1; j < days.size(); j++) {
						if (!days.get(j).text().equals("\u00a0")) {
							compMap.get(compName).get(sectionName).addTime(startTime, endTime, j - 1);
						}
					}
				} // done parsing course

				// remove empty sections and components
				compMap.entrySet().removeIf(comp -> {
					comp.getValue().entrySet().removeIf(sec -> {
						return !sec.getValue().hasTimeslots();
					});
					return comp.getValue().isEmpty();
				});

				// convert map to Component objects and add to course
				compMap.forEach((k, v) -> {
					Component comp = new Component(k);
					comp.sections.addAll(v.values());
					course.components.add(comp);
				});

				if (!course.components.isEmpty())
					courses.add(course);
			}
		}
		return courses;
	}

	private static void writeToFile(String body, String dirname, String filename) throws IOException {
		File dir = new File(dirname);
		if (!dir.exists()) {
			System.out.println("Created directory:" + dir.getCanonicalPath());
			dir.mkdir();
		}

		File output = new File(dirname + "/" + filename);
		if (!output.exists()) {
			output.createNewFile();
		}
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(body);
		bw.close();
	}

}
