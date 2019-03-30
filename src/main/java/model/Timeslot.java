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

package model;

public class Timeslot {
	public int day;
	public int start;
	public int len;
	public int timebit;
	public String str1;
	public String str2;
	public int id;
	
	public Timeslot(int day, String start, String end, String str1, String str2, int id) {
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