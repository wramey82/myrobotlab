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

public class ServiceDirectoryUpdate implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger
			.getLogger(ServiceDirectoryUpdate.class);

	public int ID;
	public String login;
	public String password;
	public String hostname; // globally unique name of an instance of some
							// Service
	public String type; // TODO enums - INIT | RECONNECT
	public int servicePort; // service port
	public ArrayList<ServiceEntry> serviceEntryList_; // service directory
														// update request - this
														// is passed between
														// operators to maintain
														// a relevant copy of
														// the ServiceDirectory
	public ArrayList<Property> propertyList_; // operator name the sdu came from
	public String remoteHostname; // globally unique name of an instance of some
									// Service
	public int remoteServicePort; // service port

	// option constants

	// ctors begin ----
	public ServiceDirectoryUpdate() {

		login = new String();
		password = new String();
		hostname = new String();
		serviceEntryList_ = new ArrayList<ServiceEntry>();
		propertyList_ = new ArrayList<Property>();
		remoteHostname = new String();

	}

	public ServiceDirectoryUpdate(final ServiceDirectoryUpdate other) {
		this();
		set(other);
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final ServiceDirectoryUpdate other) {
		ID = other.ID;
		login = other.login;
		password = other.password;
		hostname = other.hostname;
		servicePort = other.servicePort;
		serviceEntryList_ = other.serviceEntryList_;
		propertyList_ = other.propertyList_;
		remoteHostname = other.remoteHostname;
		remoteServicePort = other.remoteServicePort;

	}

	// assignment end ---

	public static String scope() {
		String ret = new String("myrobotlab");
		return ret;
	};

	public static String name() {
		if (LOG.isDebugEnabled()) {
			StringBuilder logString = new StringBuilder(
					"ServiceDirectoryUpdate.name()");
			LOG.debug(logString);
		} // if

		String ret = new String("ServiceDirectoryUpdate");
		return ret;
	};

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<ServiceDirectoryUpdate");
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"login\":" + "\"" + login + "\"");
		ret.append("\"password\":" + "\"" + password + "\"");
		ret.append("\"hostname\":" + "\"" + hostname + "\"");
		ret.append("\"servicePort\":" + "\"" + servicePort + "\"");
		ret.append("\"serviceEntryList\":" + "\""
				+ serviceEntryList_.toString() + "\"");
		ret
				.append("\"propertyList\":" + "\"" + propertyList_.toString()
						+ "\"");
		ret.append("\"remoteHostname\":" + "\"" + remoteHostname + "\"");
		ret.append("\"remoteServicePort\":" + "\"" + remoteServicePort + "\"");

		// ret.append("</ServiceDirectoryUpdate>");
		ret.append("}");
		return ret.toString();
	}

}