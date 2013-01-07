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

package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.IOData;

public interface AnalogIO {

	/*
	 * String representation of function signatures to be used on the consumer
	 * of the interface.
	 */
	public final static String analogWrite = "analogWrite";
	public final static String analogRead = "analogRead";
	public final static String getType = "getType";

	/**
	 * analogWrite will provide a simple mechanism to call into an arbitrary
	 * device which can support analog writes. For devices which have
	 * specialized addressing schemes a wrapper must be created which will
	 * simplify the address to a single integer value. This may involve
	 * constructing a map to support more complicated address schemes.
	 * 
	 * @param io
	 *            - and IOData contains 2 relevant members address - the address
	 *            on the device which value will be written too value - the
	 *            value to be written, typically 1 or 255 range
	 * @return void
	 */
	// public void analogWrite(Integer address, Integer value);
	public IOData analogWrite(IOData io);

}
