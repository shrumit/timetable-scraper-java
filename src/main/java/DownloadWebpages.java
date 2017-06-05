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

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class DownloadWebpages {

	static final String url = "http://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm";

	public static void main(String[] args) throws IOException {

		Document doc = Jsoup.connect(url).get();

		Element subject = doc.getElementById("inputSubject");
		Elements courseCodes = subject.getElementsByTag("option");

		courseCodes.remove(0);

		// processCourse(courseCodes.get(0).val(), courseCodes.get(0).text());

		for (int i = 0; i < courseCodes.size(); i++) {

			Element item = courseCodes.get(i);
			String content = processCourse(item.val());
			System.out.println("dump\\" + item.val());
			File file = new File("dump\\" + item.val());
			file.createNewFile();

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
			System.out.println(item.val() + " done");

		}
	}

	static String processCourse(String courseCode) throws IOException {
		Document doc = Jsoup.connect(url).data("subject", courseCode)
				.data("Designation", "Any").data("catalognbr", "")
				.data("CourseTime", "All").data("Component", "All")
				.data("time", "").data("end_time", "").data("day", "m")
				.data("day", "tu").data("day", "w").data("day", "th")
				.data("day", "f").data("Campus", "Any")
				.data("command", "search").timeout(10 * 1000).post();

		return doc.toString();
	}
}
