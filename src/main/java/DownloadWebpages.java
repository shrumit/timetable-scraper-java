/*
 * Copyright (C) Shrumit Mehta 2016
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
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class DownloadWebpages {

	static final String url = "http://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm";
	static final int TIMEOUT = 10 * 1000;

	public static void main(String[] args) throws IOException {

		// get list of subjects
		Document doc = Jsoup.connect(url).get();
		Element subjectInput = doc.getElementById("inputSubject");
		Elements subjectCodes = subjectInput.getElementsByTag("option");

		// download and store each subject's webpage
		for (int i = 0; i < subjectCodes.size(); i++) {
			String code = subjectCodes.get(i).val();
			if (code.length() == 0)
				continue;

			String content = downloadCoursePage(code);

			if (content.length() < 3000 && content.contains("captcha")) {
				System.out.println("Please solve captcha and try again.");
				return;
			}

			// write to file
			File file = new File("dump\\" + code);
			file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

			System.out.println("Downloaded:" + i + ":" + code);
		}
		System.out.println("Finished.");
	}

	static String downloadCoursePage(String courseCode) throws IOException {

		/* This doesn't work for some reason */
		// Map<String, String> formData = new HashMap<>();
		// formData.put("subject", courseCode);
		// formData.put("Designation", "Any");
		// formData.put("catalognbr", "");
		// formData.put("CourseTime", "All");
		// formData.put("Component", "All");
		// formData.put("time", "");
		// formData.put("end_time", "");
		// formData.put("day", "m");
		// formData.put("day", "tu");
		// formData.put("day", "w");
		// formData.put("day", "th");
		// formData.put("day", "f");
		// formData.put("Campus", "Any");
		// formData.put("command", "search");
		// Document doc = Jsoup.connect(url).data(formData).timeout(TIMEOUT)
		// .post();

		Document doc = Jsoup.connect(url).data("subject", courseCode)
				.data("Designation", "Any").data("catalognbr", "")
				.data("CourseTime", "All").data("Component", "All")
				.data("time", "").data("end_time", "").data("day", "m")
				.data("day", "tu").data("day", "w").data("day", "th")
				.data("day", "f").data("Campus", "Any")
				.data("command", "search").maxBodySize(0).timeout(10 * 1000).post();

		return doc.toString();
	}
}
