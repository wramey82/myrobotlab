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

package org.myrobotlab.dbms;

import java.util.HashMap;

/**
 * @author gperry
 * 
 */
public class Configuration {

	private static boolean HasInstance_ = false;
	private static Configuration ConfigurationRef_ = null;
	private HashMap<String, String> Data_;

	public Configuration() {
		Data_ = new HashMap<String, String>();
		HasInstance_ = true;
		ConfigurationRef_ = this;
		// todo - need a place in AGO - to enumerate configuration
		// set("TablePrefix", "PRE_");
		// set("UseNameSpaceAsPrefix", "true");
	}

	public void set(final String key, final String value) {
		Data_.put(key, value);
	}

	public String get(final String key) {
		if (Data_.get(key) == null)
			return ("");

		return Data_.get(key);
	}

	public static Configuration getInstance() {
		// check static var
		if (HasInstance_) {
			return ConfigurationRef_;
		} else {
			return new Configuration();
		}
	}

	public boolean getBoolean(final String key) {
		String configValue = Data_.get(key);
		if ((configValue != null)
				&& ((configValue.compareToIgnoreCase("T") == 0)
						|| (configValue.compareToIgnoreCase("Y") == 0)
						|| (configValue.compareToIgnoreCase("TRUE") == 0)
						|| (configValue.compareToIgnoreCase("YES") == 0) || (configValue
						.compareToIgnoreCase("1") == 0))) {
			return true;
		}

		return false;
	}

	/**
	 * @return the data
	 */
	public HashMap<String, String> getData() {
		return Data_;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(HashMap<String, String> data) {
		Data_ = data;
	}
}
