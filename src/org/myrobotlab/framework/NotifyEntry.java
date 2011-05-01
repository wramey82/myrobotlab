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

public class NotifyEntry implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(NotifyEntry.class);

	public int ID;
	public String outMethod_; // the keyed out method
	public String name; // globally unique name of Service a Message will be
						// sent to
	public String inMethod_; // the method which will be invoked from the
								// Message
	public Class[] paramTypes = null; // the parameter type of the inMethod - named
								// parameterType vs dataType, because this will
								// always specify parameters not return types

	// option constants

	// ctors begin ----
	public NotifyEntry() {
		outMethod_ = new String();
		name = new String();
		inMethod_ = new String();
	}

	public NotifyEntry(final NotifyEntry other) {
		this();
		set(other);
	}

	// ctors end ----

	public void set(final NotifyEntry other) {
		ID = other.ID;
		outMethod_ = other.outMethod_;
		name = other.name;
		inMethod_ = other.inMethod_;
		paramTypes = other.paramTypes;

	}

	// assignment end ---

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"outMethod\":" + "\"" + outMethod_ + "\"");
		ret.append("\"name\":" + "\"" + name + "\"");
		ret.append("\"inMethod\":" + "\"" + inMethod_ + "\"");
		ret.append("\"paramType\":" + "\"" + paramTypes + "\"");
		ret.append("}");
		return ret.toString();
	}

}