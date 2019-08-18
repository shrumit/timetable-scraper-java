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

package model;

public class Section {
	
	public String name;
	public String number;
	public String location;
	public String instructor;
	// Time stored in a bitmap where 1 bit = 30min occupied interval
 	// LSB is the 30min interval starting at 8AM. Intervals go up to 10PM (i.e. 28 LSBs utilized)
	public int[] timebits = new int[5];

	public Section(String name) {
		this.name = name;
	}

	public Section(String name, String number, String location, String instructor) {
		this.name = name;
		this.number = number;
		this.location = location;
		this.instructor = instructor;
	}

	// returns true is at least one timebit is nonzero
	public boolean hasTimeslots() {
		for (int n : timebits)
			if (n != 0) return true;
		return false;
	}

	public void addTime(String startStr, String endStr, int dayIdx) {
		if (dayIdx > 4 || dayIdx < 0)
			throw new IllegalArgumentException("dayIdx not proper");
		int start = convertStringToInterval(startStr);
		int len = convertStringToInterval(endStr) - start;
		int mask = 0;
		mask = ~mask;
		mask <<= len;
		mask = ~mask;
		mask <<=  start;
		if ((timebits[dayIdx] & mask) > 0)
			throw new IllegalArgumentException("Section addTime overlap");
		timebits[dayIdx] |= mask;
	}
	
	// converts String to 30min interval index, starting at 7:00 AM
	private int convertStringToInterval(String str) {
		int total = 0;
		String[] one = str.split(" ");
		if (one[1].equals("PM"))
			total += 24;
		String[] two = one[0].split(":");
		int hour = Integer.parseInt(two[0]);
		int minute = Integer.parseInt(two[1]);
		if (hour == 12)
			hour = 0;
		total += 2 * (hour - 7);
		if (minute != 0)
			total += 1;

		return total;
	}
	
}
