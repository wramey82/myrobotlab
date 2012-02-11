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

public class IOData implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(IOData.class);

	public int ID;
	public int address; // address
	public int value; // value

	// option constants

	// ctors begin ----
	public IOData() {
	}

	public IOData(int address, int value) {
		this.address = address;
		this.value = value;
	}

	public IOData(final IOData other) {
		this();
		set(other);
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final IOData other) {
		ID = other.ID;
		address = other.address;
		value = other.value;

	}

	// assignment end ---

	public static String scope() {
		String ret = new String("hardware");
		return ret;
	};

	public static String name() {
		if (LOG.isDebugEnabled()) {
			StringBuilder logString = new StringBuilder("IOData.name()");
			LOG.debug(logString);
		} // if

		String ret = new String("IOData");
		return ret;
	};

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<IOData");
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"address\":" + "\"" + address + "\"");
		ret.append("\"value\":" + "\"" + value + "\"");

		// ret.append("</IOData>");
		ret.append("}");
		return ret.toString();
	}

}