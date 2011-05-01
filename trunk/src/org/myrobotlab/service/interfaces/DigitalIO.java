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

/**
 * @author gperry
 * 
 */
public interface DigitalIO {

	/*
	 * String representation of function signatures to be used on the consumer
	 * of the interface.
	 */
	public final static String digitalWrite = "digitalWrite";
	public final static String digitalRead = "digitalRead";
	public final static String getType = "getType";

	/**
	 * digitalWrite will provide a simple mechanism to call into an arbitrary
	 * device which can support digital writes. For devices which have
	 * specialized addressing schemes a wrapper must be created which will
	 * simplify the address to a single integer value. This may involve
	 * constructing a map to support more complicated address schemes.
	 * 
	 * @param address
	 *            - the address on the device which value will be written too
	 * @param value
	 *            - the value to be written, typically 1 or 255 range
	 * @return void
	 */
	// public void digitalWrite(Integer address, Integer value);
	public void digitalWrite(IOData io);

	/**
	 * digitalRead will provide a simple mechanism to call into an arbitrary
	 * device which can support digital reads. For devices which have
	 * specialized addressing schemes a wrapper must be created which will
	 * simplify the address to a single integer value. This may involve
	 * constructing a map to support more complicated address schemes.
	 * 
	 * @param address
	 *            - the address on the device which value will be written too
	 * @return Integer - the value read
	 */
	// public Integer digitalRead(Integer address);
	public void digitalReadPollStart(Integer address);

	public void digitalReadPollStop(Integer address);

	/**
	 * getType will return the canonical class name of the controller that
	 * supports this interface. e.g. org.myrobotlab.service.Arduino or
	 * com.someone.service.PICAxe
	 * 
	 * @return String - the canonical class name of the controller supporting
	 *         the DigitalIO interface
	 */
	public String getType();

}
