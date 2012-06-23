/*
 *  RoombaComm Interface
 *
 *  Copyright (c) 2006 Tod E. Kurt, tod@todbot.com, ThingM
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General
 *  Public License along with this library; if not, write to the
 *  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA  02111-1307  USA
 *
 */

package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialException;
import org.myrobotlab.serial.SerialService;
import org.myrobotlab.serial.UnsupportedCommOperationException;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author greg
 * 
 */
public class AFMotorShield extends Service implements SerialService {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(AFMotorShield.class
			.getCanonicalName());

	SerialDevice serial;

	public AFMotorShield(String n) {
		super(n, AFMotorShield.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	@Override
	public String getToolTip() {
		return "AF Motor Shield Service";
	}

	@Override
	public boolean setSerialPortParams(int baud, int dataBits, int stopBits,
			int parity) {
		try {
			serial.setSerialPortParams(baud, dataBits, stopBits, parity);
		} catch (UnsupportedCommOperationException e) {
			logException(e);
			return false;
		}
		return true;
	}

	/**
	 * Connect to a port (for serial, portid is serial port name, for net,
	 * portid is url?)
	 * 
	 * @return true on successful connect, false otherwise
	 */
	public boolean connect(String deviceName) {
		try {
			serial = SerialDeviceFactory.getSerialDevice(deviceName, 57600, 8, 1, 0);
		} catch (SerialException e) {
			 logException(e);
			 return false;
		}
		return true;
	}

	
	public void disconnect() {
		serial.close();
	}

	@Override
	public void send(int[] data) throws IOException {
		// TODO Auto-generated method stub
		
	}


}
