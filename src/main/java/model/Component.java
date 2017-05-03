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

public class Component {
	public String name;
	public ArrayList<Section> sections;

	public Component() {
		this("");
	}

	public Component(String name) {
		this.name = name;
		sections = new ArrayList<Section>();
	}

	public void add(Section sec) {
		if ((!sec.name.equals("")) && (sec.timeslots.size() > 0))
			sections.add(sec);
	}

	public void append(Component in) {

	}
}