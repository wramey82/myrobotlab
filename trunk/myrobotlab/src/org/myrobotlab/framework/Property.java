/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.framework;

import java.io.Serializable;

import org.apache.log4j.Logger;

public class Property implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(Property.class);

	public int ID;
	public String domain; // value of property
	public String name; // name of property
	public String value; // value of property

	// option constants

	// ctors begin ----
	public Property() {

		domain = new String();
		name = new String();
		value = new String();

	}

	public Property(final Property other) {
		this();
		set(other);
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final Property other) {
		ID = other.ID;
		domain = other.domain;
		name = other.name;
		value = other.value;

	}

	// assignment end ---

	public static String scope() {
		String ret = new String("myrobotlab");
		return ret;
	};

	public static String name() {
		if (LOG.isDebugEnabled()) {
			StringBuilder logString = new StringBuilder("Property.getName()()");
			LOG.debug(logString);
		} // if

		String ret = new String("Property");
		return ret;
	};

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<Property");
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"domain\":" + "\"" + domain + "\"");
		ret.append("\"name\":" + "\"" + name + "\"");
		ret.append("\"value\":" + "\"" + value + "\"");

		// ret.append("</Property>");
		ret.append("}");
		return ret.toString();
	}

}