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
import java.util.Iterator;
import java.util.List;

import com.google.gson.annotations.Expose;

public class Course {
	@Expose
	public int id;
	@Expose
	public String text;
	public List<Component> components;

	public Course() {
		components = new ArrayList<Component>();
	}

	public void add(Component comp) {
		// if component isn't unnamed or empty
		if (!comp.name.equals("") && (comp.sections.size() > 0)) {
			Iterator<Component> iterator = components.iterator();
			// if same named Component exists then append sections to it
			while (iterator.hasNext()) {
				Component cur = iterator.next();
				if (cur.name.equals(comp.name)) {
					cur.sections.addAll(comp.sections);
					return;
				}
			}
			components.add(comp);
		}
	}
}