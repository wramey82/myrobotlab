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
import java.util.Date;
import java.util.HashMap;

public class ServiceEntry implements Serializable {

	// TODO - make immutable?

	private static final long serialVersionUID = 1L;
	public String host;
	public int servicePort;
	public String name;
	public String serviceClass;
	public Date lastModified;
	public Object localServiceHandle;
	public String toolTip;

	public HashMap<String, MethodEntry> methods;
	public HashMap<String, InterfaceEntry> interfaces;

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("{host:");
		sb.append(host);
		sb.append("	,\n");

		sb.append("servicePort:");
		sb.append(servicePort);
		sb.append("	,\n");

		sb.append("name:");
		sb.append(name);
		sb.append("	,\n");

		sb.append("serviceClass:");
		sb.append(serviceClass);
		sb.append("	,\n");

		sb.append("lastModified:");
		sb.append(lastModified);
		sb.append("	,\n");

		sb.append("localServiceHandle:");
		sb.append(localServiceHandle);
		sb.append("	,\n");
		/*
		 * TODO - taken out to prevent dependency of actual parameterTypes into
		 * GUIs for (String key: methods.keySet()) {
		 * sb.append(methods.get(key).toString()); sb.append(",\n"); }
		 */

		for (String key : interfaces.keySet()) {
			sb.append(interfaces.get(key).toString());
			sb.append(",\n");
		}

		sb.append("}");

		return sb.toString();
	}
}
