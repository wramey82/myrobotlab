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

package org.myrobotlab.fileLib;

import java.util.Date;

import org.apache.log4j.Logger;

public class File {
	// @SuppressWarnings("serial")
	private final static Logger LOG = Logger.getLogger(File.class);

	public int ID;
	public String filename; //
	public String path; //
	public Date creation; //
	public int size; // Size
	public long modified; //
	public Date lastAccess; //
	public int checksum; // Not Implemented
	public int attributes; // Attributes of a file on the files system

	// option constants

	// ctors begin ----
	public File() {

		filename = new String();
		path = new String();
		creation = new Date();
		// modified = new Date();
		lastAccess = new Date();

	}

	public File(final File other) {
		this();
		set(other);
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final File other) {
		ID = other.ID;
		filename = other.filename;
		path = other.path;
		creation = other.creation;
		size = other.size;
		modified = other.modified;
		lastAccess = other.lastAccess;
		checksum = other.checksum;
		attributes = other.attributes;

	}

	// assignment end ---

	public static String scope() {
		String ret = new String("fileLib");
		return ret;
	};

	public static String name() {
		if (LOG.isDebugEnabled()) {
			StringBuilder logString = new StringBuilder("File.name()");
			LOG.debug(logString);
		} // if

		String ret = new String("File");
		return ret;
	};

	// todo format string interface more than one compact, ini, xml
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("<File>");
		ret.append("<ID>" + ID + "</ID>");
		ret.append("<FileName>" + filename + "</FileName>");
		ret.append("<Path>" + path + "</Path>");
		ret.append("<Creation>" + creation + "</Creation>");
		ret.append("<Size>" + size + "</Size>");
		ret.append("<Modified>" + modified + "</Modified>");
		ret.append("<LastAccess>" + lastAccess + "</LastAccess>");
		ret.append("<CheckSum>" + checksum + "</CheckSum>");
		ret.append("<Attributes>" + attributes + "</Attributes>");

		ret.append("</File>");
		return ret.toString();
	}

}