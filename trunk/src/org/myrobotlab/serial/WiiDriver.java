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


package org.myrobotlab.serial;

import gnu.io.CommDriver;
import gnu.io.CommPort;

import org.apache.log4j.Logger;
import org.myrobotlab.service.Wii;

public class WiiDriver implements CommDriver {

	public final static Logger LOG = Logger.getLogger(WiiDriver.class.getCanonicalName());
	private WiiCommPort wiiport;

	public WiiDriver(Wii wii) {
		wiiport = new WiiCommPort();
		wiiport.setWii(wii);
	}

	// @Override - only in Java 1.6
	public CommPort getCommPort(String arg0, int arg1) {
		LOG.info("getCommPort");

		return wiiport;
	}

	// @Override - only in Java 1.6
	public void initialize() {
		LOG.info("BinaryDriver.initialize");
	}

}