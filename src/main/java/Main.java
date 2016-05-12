import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import com.google.gson.Gson;

public class Main {

	public static void main(String[] args) throws IOException {

		List<Data> termA = new ArrayList<>();
		List<Data> termB = new ArrayList<>();

		Pattern regex = Pattern.compile(".*\\d{4}(\\w).*");

		int count = 0;

		File dir = new File("dump");
		File[] dirListing = dir.listFiles();
		if (dirListing != null) {
			for (File file : dirListing) {
				Document doc = Jsoup.parse(file, "UTF-8", "");
				Elements course_names = doc.getElementsByTag("caption");
				for (Element e : course_names) {

					// assign params to Data object
					Data temp = new Data();
					temp.id = count;
					temp.text = e.text();

					String suffix;
					Matcher m = regex.matcher(e.text());
					
					// if match found, assign to suffix, else assign ""
					if (m.find())
						suffix = m.group(1);
					else
						suffix = "";

					// A term
					if (suffix.equals("A") || suffix.equals("F")
							|| suffix.equals("W") || suffix.equals("Q")
							|| suffix.equals("R"))
						termA.add(temp);
					// B term
					else if (suffix.equals("B") || suffix.equals("G")
							|| suffix.equals("X") || suffix.equals("S")
							|| suffix.equals("T"))
						termB.add(temp);
					// Both terms
					else if (suffix.equals("") || suffix.equals("E")
							|| suffix.equals("Y") || suffix.equals("Z")
							|| suffix.equals("U")) {
						termA.add(temp);
						termB.add(temp);
					} else
						System.out.println(temp.text);

					count++;
				}

			}
		} else {
			System.out.println("Not a directory");
		}

		
		// Console printing is glitchy. Print one set at a time
		
		System.out.println(new Gson().toJson(termA));
		System.out.println("a count:" + termA.size());
		
		System.out.println(new Gson().toJson(termB));
		System.out.println("b count:" + termB.size());
		
		System.out.println("total count:" + count);

	}
}

class Data {
	public int id;
	public String text;
}