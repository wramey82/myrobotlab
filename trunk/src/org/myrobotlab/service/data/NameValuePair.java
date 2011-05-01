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

package org.myrobotlab.service.data;

import java.io.Serializable;

import org.apache.log4j.Logger;

public class NameValuePair implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(NameValuePair.class);

	public int ID;
	public String name; // name
	public String value; // value

	// option constants

	// ctors begin ----
	public NameValuePair() {
		name = new String();
		value = new String();
	}

	public NameValuePair(final NameValuePair other) {
		this();
		set(other);
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final NameValuePair other) {
		ID = other.ID;
		name = other.name;
		value = other.value;

	}

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<NameValuePair");
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"name\":" + "\"" + name + "\"");
		ret.append("\"value\":" + "\"" + value + "\"");

		// ret.append("</NameValuePair>");
		ret.append("}");
		return ret.toString();
	}

}