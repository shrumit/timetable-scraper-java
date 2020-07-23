
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class DownloadWebpages {

	static final String url = "http://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm";
	static final int TIMEOUT = 10 * 1000;
	static final String folderPrefix = "dump";
	static final int startIdx = 0;

	public static void main(String[] args) throws IOException, InterruptedException {
		long time_start = System.nanoTime();

		// get list of subjects
		Document doc = Jsoup.connect(url).get();
		Element subjectInput = doc.getElementById("inputSubject");
		Elements subjectCodes = subjectInput.getElementsByTag("option");
		System.out.println("Number of subjects:" + subjectCodes.size());

		String dirname = folderPrefix + dateString();
		
	    File dir = new File(dirname);
	    if (!dir.exists()){
	    	System.out.println("Created directory:" + dir.getCanonicalPath());
	        dir.mkdir();
	    }
		
		// download and store each subject's webpage
		for (int i = startIdx; i < subjectCodes.size(); i++) {
			String code = subjectCodes.get(i).val();
			if (code.length() == 0)
				continue;

			System.out.println("Attempting:" + i + ":" + code);
			String content = downloadCoursePage(code);

			if (content.length() < 3000 && content.contains("captcha")) {
				System.out.println("Captcha");
				Thread.sleep(30 * 1000);
				i--;
				continue;
			}

			// write to file
//			File file = new File(folder + "\\" + code); // WINDOWS
			File file = new File(dirname + "/" + code); // LINUX
			file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

			System.out.println("Downloaded:" + i + ":" + code);
//			Thread.sleep(2000);
		}
		System.out.println("Finished.");
		System.out.println("Dirname:" + dirname);
		long time_end = System.nanoTime();
		System.out.println((time_end - time_start) / 6e+10 + "minutes");
	}

	static String downloadCoursePage(String courseCode) throws IOException {
		Document doc = Jsoup.connect(url).data("subject", courseCode).data("Designation", "Any").data("catalognbr", "")
				.data("CourseTime", "All").data("Component", "All").data("time", "").data("end_time", "")
				.data("day", "m").data("day", "tu").data("day", "w").data("day", "th").data("day", "f")
				.data("Campus", "Any").data("command", "search").maxBodySize(0).timeout(30 * 1000).get();

//		System.out.println(doc.toString());
		return doc.toString();
	}
	
	public static String dateString() {
	    LocalDate today = LocalDate.now();
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy");
	    return today.format(formatter);		
	}

}
