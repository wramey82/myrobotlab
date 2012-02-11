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
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.serial.Port;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SerialPort;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author greg
 * 
 */
public class AFMotorShield extends Service implements SerialPort {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(AFMotorShield.class
			.getCanonicalName());

	Port serial;

	public AFMotorShield(String n) {
		super(n, AFMotorShield.class.getCanonicalName());
		serial = new Port(this);
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	@Override
	public String getToolTip() {
		return "AF Motor Shield Service";
	}

	@Override
	public String readSerial(byte[] s) {

		return null;
	}

	@Override
	public boolean setSerialPortParams(int baud, int dataBits, int stopBits,
			int parity) {
		serial.setSerialPortParams(baud, dataBits, stopBits, parity);
		return true;
	}

	//
	// --------------------- BEGIN ORIGINAL ROOMBACOMM ---------------
	//

	/**
	 * List available ports
	 * 
	 * @return a list available portids, if applicable or empty set if no ports,
	 *         or return null if list is not enumerable
	 */
	public String[] listPorts() {
		return serial.getPorts().toArray(new String[serial.getPorts().size()]);
	}

	public ArrayList<String> getPorts() {
		return serial.getPorts();
	}

	/**
	 * Connect to a port (for serial, portid is serial port name, for net,
	 * portid is url?)
	 * 
	 * @return true on successful connect, false otherwise
	 */
	public boolean connect(String portid) {
		serial.setPort(portid, 9600, 8, 1, 0);
		if (serial.isReady())
			return true;
		else
			return false;
	}

	
	public void disconnect() {
		serial.releaseSerialPort();
	}


	public boolean send(byte[] bytes) {
		if (serial.isReady()) {
			serial.serialSend(bytes);
			return true;
		}
		return false;
	}

	public boolean send(int b) {
		if (serial.isReady()) {
			serial.serialSend(b);
			return true;
		}
		return false;
	}

	public String getPortName() {
		if (serial != null)
		{
			return serial.getPortName();
		}
		
		return null;
	}

	@Override
	public boolean send(int[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	final public static int SET_MOTOR_SPEED          = 0; 
	final public static int MOTOR_MOVE               = 1; 
	final public static int SERVO_WRITE              = 2; 
	

	final public static int MOTOR_1              = 0; // Yay, I can still make it 0 based ! 
	final public static int MOTOR_2              = 1; 
	
	final public static int FORWARD             = 1; 
	final public static int BACKWARD            = 2; 
	final public static int BRAKE              	= 3;  
	final public static int RELEASE             = 4; 
		
	public void setSpeed(int motorNum, float power) // not 0 based since board number begins with 1
	{
		int p = (int)(255 * power);
		
		serial.serialSend(SET_MOTOR_SPEED, motorNum, p);
	}
	
	
	
	public void motorMove(int motorNum, int direction) // not 0 based since board number begins with 1
	{
		
		serial.serialSend(MOTOR_MOVE, motorNum, direction);
	}
	
	public void servoWrite(int servoNum, int angle) {

		LOG.info("servoWrite (" + servoNum + "," + angle + ")");

		serial.serialSend(SERVO_WRITE, servoNum, angle);

	}
	
	public boolean setPort(String portid, int baud) {
		serial.setPort(portid, baud, 8, 1, 0);
		if (serial.isReady())
			return true;
		else
			return false;
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		AFMotorShield ms = new AFMotorShield("AFMotorShield");
		ms.startService();
		
		//ms.setPort("/dev/ttyUSB0", 9600);
		//ms.setPort("/dev/rfcomm0", 9600);
		ms.setPort("/dev/ttyS50", 9600);

		

		int servoNum = 0;
		
		ms.servoWrite(servoNum, 30);
		ms.servoWrite(servoNum, 90);
		ms.servoWrite(servoNum, 30);
		ms.servoWrite(servoNum, 90);
		
				
		// MOTOR_2; // right motor backward
		// MOTOR_1; // left motor forward
		
		//ms.motorMove(motorNum, RELEASE);
		
		
		ms.motorMove(MOTOR_2, BACKWARD);
		ms.setSpeed(MOTOR_2, 0.25f);
		ms.setSpeed(MOTOR_2, 0);

		ms.motorMove(MOTOR_1, BACKWARD); 
		ms.setSpeed(MOTOR_1, 0.25f);
		ms.setSpeed(MOTOR_1, 0);

		
		
		
		Motor m1 = new Motor("motor1");

		Servo s = new Servo("servo");
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}
}
