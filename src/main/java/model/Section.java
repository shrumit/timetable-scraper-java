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

import java.util.ArrayList;
import java.util.List;

public class Section {
	public String name;
	public String number;
	public String location;
	public String instructor;
	public String startTime;
	public String endTime;

	public List<Timeslot> timeslots;

	public Section() {
	}

	public Section(String name, String number, String location, String instructor, String startTime, String endTime) {
		this.name = name;
		this.number = number;
		this.location = location;
		this.instructor = instructor;
		this.startTime = startTime;
		this.endTime = endTime;
		timeslots = new ArrayList<Timeslot>();
	}

}
