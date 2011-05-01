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
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static String BLOCKING = "B";
	public final static String RETURN = "R";
	public final static Logger LOG = Logger.getLogger(Message.class);

	public int ID;
	public String msgID; // unique identifier for this message
	public String timeStamp; // datetimestamp when message is created GMT
	public String name; // globally unique name of destination Service -
						// typically this is filled in from the notifyList
	public String sender; // globally unique name of Service which sent the
							// message
	public String sendingMethod; // the origininating source method of this
									// message
	public ArrayList<RoutingEntry> historyList; // history of the message -
												// routing stops and times
	public String status; // status of the message
	public String msgType; // Broadcast|Blocking|Blocking Return - depricated
	public String method; // requested service method to invoke
	// public String dataClass ; //type class of data - e.g. java.lang.String -
	// should change this name to dataClass
	public String encoding; // depricated (should probably be Option not Text) -
							// limited number of encoding types - type of
							// encoding used on data TODO - should be part of
							// Communicator - not here
	public Object[] data; // data payload - if invoking a service request this
							// would be the parameter (list) - this would the
							// return type data if the message is outbound

	// ctors begin ----
	public Message() {

		msgID = new String();
		timeStamp = new String();
		// hostname = new String();
		// servicePort = new int();
		name = new String();
		sender = new String();
		sendingMethod = new String();
		historyList = new ArrayList<RoutingEntry>();
		status = new String();
		msgType = new String();
		method = new String();
		// dataClass = new String();
		encoding = new String();
		// data = new Object[5]; // TODO - is this pointless anyway ??

	}

	public Message(final Message other) {
		this();
		set(other);
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	final public void set(final Message other) {
		ID = other.ID;
		msgID = other.msgID;
		timeStamp = other.timeStamp;
		name = other.name;
		sender = other.sender;
		sendingMethod = other.sendingMethod;
		historyList = other.historyList;
		status = other.status;
		msgType = other.msgType;
		method = other.method;
		// dataClass = other.dataClass;
		encoding = other.encoding;
		data = other.data;

	}

	// rather silly parameter building but - convienent

	final public void setData(Object param0) {
		this.data = new Object[1];
		this.data[0] = param0;
	}

	final public void setData(Object param0, Object param1) {
		this.data = new Object[2];
		this.data[0] = param0;
		this.data[1] = param1;
	}

	final public void setData(Object param0, Object param1, Object param2) {
		this.data = new Object[3];
		this.data[0] = param0;
		this.data[1] = param1;
		this.data[2] = param2;
	}

	final public void setData(Object param0, Object param1, Object param2,
			Object param3) {
		this.data = new Object[4];
		this.data[0] = param0;
		this.data[1] = param1;
		this.data[2] = param2;
		this.data[3] = param3;
	}

	final public void setData(Object[] params) {
		this.data = params;
	}

	final public String getParameterSignature() {
		if (data == null) {
			return "null";
		}

		String ret = "";
		for (int i = 0; i < data.length; ++i) {
			ret += data[i].getClass().getCanonicalName();
			if (data.length != i + 1) {
				ret += ",";
			}

		}
		return ret;

	}

	// assignment end ---

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<Message");
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"MSGID\":" + "\"" + msgID + "\"");
		ret.append("\"timeStamp\":" + "\"" + timeStamp + "\"");
		ret.append("\"name\":" + "\"" + name + "\"");
		ret.append("\"sender\":" + "\"" + sender + "\"");
		ret.append("\"sendingMethod\":" + "\"" + sendingMethod + "\"");
		ret.append("\"historyList\":" + "\"" + historyList.toString() + "\"");
		ret.append("\"status\":" + "\"" + status + "\"");
		ret.append("\"msgType\":" + "\"" + msgType + "\"");
		ret.append("\"method\":" + "\"" + method + "\"");
		ret.append("\"dataClass\":" + "\"" + getParameterSignature() + "\"");
		ret.append("\"encoding\":" + "\"" + encoding + "\"");
		ret.append("\"data\":" + "\"" + data + "\"");

		// ret.append("</Message>");
		ret.append("}");
		return ret.toString();
	}

}