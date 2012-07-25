/**
 *                    
 * @author grog (at) myrobotlab.org
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

public class PinData implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(PinData.class);
	public static final int DIGITAL_VALUE = 1; // normalized with Arduino 
	public static final int ANALOG_VALUE = 3;  // normalized with Arduino 
	
	//public long time; 	// time of creation
	public int pin; 	// address
	public int type; 	// FIXME - rename type ?
	public int value; // address
	//public int type; // 0 Binary 1 Analog ?
	public String source;

	public PinData() {
	}
	public PinData(int pin, int type, int value, String source) {
		this.pin = pin;
		this.type = type;
		this.value = value;
		this.source = source;
	}

	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("{");
		ret.append("\"pin\":" + "\"" + pin + "\"");
		ret.append("\"function\":" + "\"" + type + "\"");
		ret.append("\"value\":" + "\"" + value + "\"");
		ret.append("}");
		return ret.toString();
	}

}