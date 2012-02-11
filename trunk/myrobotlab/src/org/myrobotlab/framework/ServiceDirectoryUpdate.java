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
import java.net.URL;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class ServiceDirectoryUpdate implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(ServiceDirectoryUpdate.class);

	public int ID;
	/*
	public String login;
	public String password;
	public String hostname; // globally unique name of an instance of some
	*/
	public URL url;
	public URL remoteURL;
	public String type; // TODO enums - INIT | RECONNECT
	public int servicePort;
	public ServiceEnvironment serviceEnvironment;
	public ArrayList<Property> propertyList; // operator name the sdu came from

}