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

package org.myrobotlab.service;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.android.BluetoothChat;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorData;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 *  Implementation of a Arduino Service connected to MRL through a serial port.  
 *  The protocol is basically a pass through of system calls to the Arduino board.  Data 
 *  can be passed back from the digital or analog ports by request to start polling. The
 *  serial port can be wireless (bluetooth), rf, or wired. The communication protocol
 *  supported is in arduinoSerial.pde - located here :
 *  
 *	Should support nearly all Arduino board types  
 *   
 *   TODO:
 *   Data should be serializable in xml or properties, binary should already work
 *   
 *   References:
 *    <a href="http://www.arduino.cc/playground/Main/RotaryEncoders">Rotary Encoders</a> 
 *   @author GroG
 */

@Root
public class ArduinoBT extends Service implements //SerialPortEventListener,
		SensorData, DigitalIO, AnalogIO, ServoController, MotorController {
	
	public final static Logger LOG = Logger.getLogger(ArduinoBT.class.getCanonicalName());
	private static final long serialVersionUID = 1L;
	
	// debugging
    private static final String TAG = "ArduinoBT";
    private static final boolean D = true;

	// serial uuid but does notw work for simple bt devices
	private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	int mState = STATE_NONE;
	
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	
	// non serializable serial object
//	transient SerialPort deviceName;
//	transient static HashMap<String, CommDriver> customPorts = new HashMap<String, CommDriver>();
    BluetoothAdapter adapter = null;
    // Name of the connected device
	@Element
    String deviceName = null;
  
    private ConnectThread connectThread = null;
    private ConnectedThread connectedThread = null;
    private final Handler handler = null;
 
	@Element
	int baudRate = 115200;
	@Element
	int dataBits = 8;
	@Element
	int parity = 0;
	@Element
	int stopBits = 1;

	// imported Arduino constants FIXME - NORMLIZE / GLOBALIZE
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;
	public static final int OUTPUT = 0x1;
	public static final int INPUT = 0x0;

	public static final int TCCR0B = 0x25; // register for pins 6,7
	public static final int TCCR1B = 0x2E; // register for pins 9,10
	public static final int TCCR2B = 0xA1; // register for pins 3,11

	// serial protocol functions
	public static final int DIGITAL_WRITE = 0;
	public static final int ANALOG_WRITE = 2;
	public static final int ANALOG_VALUE = 3;
	public static final int PINMODE = 4;
	public static final int PULSE_IN = 5;
	public static final int SERVO_ATTACH = 6;
	public static final int SERVO_WRITE = 7;
	public static final int SERVO_SET_MAX_PULSE = 8;
	public static final int SERVO_DETACH = 9;
	public static final int SET_PWM_FREQUENCY = 11;
	public static final int SERVO_READ = 12;
	public static final int ANALOG_READ_POLLING_START = 13;
	public static final int ANALOG_READ_POLLING_STOP = 14;
	public static final int DIGITAL_READ_POLLING_START = 15;
	public static final int DIGITAL_READ_POLLING_STOP = 16;

	// servo related
	public static final int SERVO_ANGLE_MIN = 0;
	public static final int SERVO_ANGLE_MAX = 180;
	public static final int SERVO_SWEEP = 10;
	public static final int MAX_SERVOS = 8; // TODO dependent on board?

	// servos
	boolean[] servosInUse = new boolean[MAX_SERVOS - 1];
	HashMap<Integer, Integer> pinToServo = new HashMap<Integer, Integer>(); 
	HashMap<Integer, Integer> servoToPin = new HashMap<Integer, Integer>(); 

	/**
	 *  list of serial port names from the system which the Arduino service is 
	 *  running
	 */
	public ArrayList<String> portNames = new ArrayList<String>(); 
	
	public ArduinoBT(String n) {
		super(n, ArduinoBT.class.getCanonicalName());
		// get ports - return array of strings
		// set port? / init port
		// detach port
		
		load(); // attempt to load config

		adapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		// attempt to get serial port based on there only being 1
		// or based on previous config
		
		// if there is only 1 port - attempt to initialize it
		portNames = getDeviceNames();
		LOG.info("number of ports " + portNames.size());
		for (int j = 0; j < portNames.size(); ++j) {
			LOG.info(portNames.get(j));
		}

		if (portNames.size() == 1) { // heavy handed?
			LOG.info("only one serial port " + portNames.get(0));
			setPort(portNames.get(0));
		} else if (portNames.size() > 1) {
			if (deviceName != null && deviceName.length() > 0) {
				LOG.info("more than one port - last serial port is "
						+ deviceName);
				setPort(deviceName);
			} else {
				// idea - auto discovery attempting to auto-load arduinoSerial.pde
				LOG.warn("more than one port or no ports, and last serial port not set");
				LOG.warn("need user input to select from " + portNames.size()
						+ " possibilities ");
			}
		}

		for (int i = 0; i < servosInUse.length; ++i) {
			servosInUse[i] = false;
		}

	}
	
	/**
	 * @return the current serials port name or null if not opened
	 */
	public String getPortName() //FIXME - BT MAC address or BT Name ???
	{
		if (deviceName != null)
		{
			return deviceName;
		}
		
		return null;
	}
	
	/**
	 * getPorts returns all serial ports, including the current port being used, 
	 * and any custom added ports (e.g. wii ports) 
	 * 
	 * @return array of port names which are currently being used, or serial, or custom
	 */
	// get BT devices
	public ArrayList<String> getDeviceNames() {
		
		// fill in device names
		ArrayList<String> deviceNames = new ArrayList<String>();

		return deviceNames;
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	/**
	 * serialSend communicate to the arduino using our simple language 3 bytes 3
	 * byte functions - |function name| d0 | d1
	 * 
	 * if outputStream is null: Important note to Fedora 13 make sure
	 * /var/lock/uucp /var/spool/uucp /var/spool/uucppublic and all are chown'd
	 * by uucp:uucp
	 */
	public synchronized void serialSend(int function, int param1, int param2) {
		LOG.info("serialSend fn " + function + " p1 " + param1 + " p2 "
				+ param2);
		// 3 byte Arduino Protocol
		byte data[] = new byte[3];
		data[0] = (byte)function;
		data[1] = (byte)param1;
		data[2] = (byte)param2;

		connectedThread.write(data);
	}

	@ToolTip("sends an array of data to the serial port which an Arduino is attached to")
	public void serialSend(String data) {
		LOG.error("serialSend [" + data + "]");
		serialSend(data.getBytes());
	}

	public synchronized void serialSend(byte[] data) {
		connectedThread.write(data);
	}

	public void setPWMFrequency(IOData io) {
		int freq = io.value;
		int prescalarValue = 0;

		switch (freq) {
		case 31:
		case 62:
			prescalarValue = 0x05;
			break;
		case 125:
		case 250:
			prescalarValue = 0x04;
			break;
		case 500:
		case 1000:
			prescalarValue = 0x03;
			break;
		case 4000:
		case 8000:
			prescalarValue = 0x02;
			break;
		case 32000:
		case 64000:
			prescalarValue = 0x01;
			break;
		default:
			prescalarValue = 0x03;
		}

		serialSend(SET_PWM_FREQUENCY, io.address, prescalarValue);
	}


	/*
	 * Servo Commands Arduino has a concept of a software Servo - and supports
	 * arrays Although Services could talk directly to the Arduino software
	 * servo in order to control the hardware the Servo service was created to
	 * store/handle the details, provide a common interface for other services
	 * regardless of the controller (Arduino in this case but could be any
	 * uController)
	 */
	
	// ---------------------------- Servo Methods Begin -----------------------
	
	/* servoAttach
	 * attach a servo to a pin
	 * @see org.myrobotlab.service.interfaces.ServoController#servoAttach(java.lang.Integer)
	 */
	public boolean servoAttach(Integer pin) { if (deviceName == null) {
			LOG.error("could not attach servo to pin " + pin
					+ " serial port in null - not initialized?");
			return false;
		}
		// deviceName == null ??? make sure you chown it correctly !
		LOG.info("servoAttach (" + pin + ") to " + deviceName
				+ " function number " + SERVO_ATTACH);

		/*
		 * soft servo if (pin != 3 && pin != 5 && pin != 6 && pin != 9 && pin !=
		 * 10 && pin != 11) { LOG.error(pin + " not valid for servo"); }
		 */

		for (int i = 0; i < servosInUse.length; ++i) {
			if (!servosInUse[i]) {
				servosInUse[i] = true;
				pinToServo.put(pin, i);
				servoToPin.put(i, pin);
				serialSend(SERVO_ATTACH, pinToServo.get(pin), pin);
				return true;
			}
		}

		LOG.error("servo " + pin + " attach failed - no idle servos");
		return false;
	}

	public boolean servoDetach(Integer pin) {
		LOG.info("servoDetach (" + pin + ") to " + deviceName
				+ " function number " + SERVO_DETACH);

		if (pinToServo.containsKey(pin)) {
			int removeIdx = pinToServo.get(pin);
			serialSend(SERVO_DETACH, pinToServo.get(pin), 0);
			servosInUse[removeIdx] = false;

			return true;
		}

		LOG.error("servo " + pin + " detach failed - not found");
		return false;

	}

	/*
	 * servoWrite(IOData io) interface that allows routing with a single
	 * parameter TODO - how to "route" to multiple parameters
	 */
	public void servoWrite(IOData io) {
		servoWrite(io.address, io.value);
	}

	// Set the angle of the servo in degrees, 0 to 180.
	// @Override - TODO - make interface - implements ServoController interface
	public void servoWrite(Integer pin, Integer angle) {
		if (deviceName == null) // TODO - remove this only for debugging without
		// Arduino
		{
			return;
		}

		LOG.info("servoWrite (" + pin + "," + angle + ") to "
				+ deviceName + " function number " + SERVO_WRITE);

		if (angle < SERVO_ANGLE_MIN || angle > SERVO_ANGLE_MAX) {
			// LOG.error(pin + " angle " + angle + " request invalid");
			return;
		}

		serialSend(SERVO_WRITE, pinToServo.get(pin), angle);

	}

	// ---------------------------- Servo Methods End -----------------------
	
	// ---------------------- Serial Control Methods Begin ------------------

	public void releaseSerialPort() {
		LOG.debug("releaseSerialPort");
		stop();
	    LOG.info("released port");
	}

	/**
	 * setPort - sets the serial port to the requested port name
	 * and initially attempts to open with 115200 8N1
	 * @param inPortName name of serial port 
	 * 			Linux [ttyUSB0, ttyUSB1, ... S0, S1, ...]
	 * 			Windows [COM1, COM2, ...]
	 * 			OSX [???]
	 * @return if successful
	 * 
	 */
	public boolean setPort(String inPortName) {
		LOG.debug("setPort requesting [" + inPortName + "]");
		//connect();
		return false;
	}

	public String getDeviceString()
	{
		return adapter.getName();
	}
	
	public boolean setBaud(int baudRate)
	{
		if (deviceName == null)
		{
			LOG.error("setBaudBase - deviceName is null");
			return false;
		}
		try {
			// boolean ret = deviceName.set.setBaudBase(baudRate); // doesnt work - operation not allowed
			// boolean ret = setSerialPortParams(baudRate, deviceName.getDataBits(), deviceName.getStopBits(), deviceName.getParity());
			boolean ret = false;
			this.baudRate = baudRate;
			save();
			broadcastState(); // state has changed let everyone know
			return ret;
		} catch (Exception e) {
			Service.logException(e);
		}
		return false;
	}
	
	public int getBaudRate()
	{
		return baudRate;
	}
	
	public boolean setSerialPortParams (int baudRate, int dataBits, int stopBits, int parity)
	{
		if (deviceName == null)
		{
			LOG.error("setSerialPortParams - deviceName is null");
			return false;
		}
		
		/*
		try {
			deviceName.setSerialPortParams(baudRate, dataBits, stopBits, parity);
		} catch (UnsupportedCommOperationException e) {
			Service.logException(e);
		}
		*/
		
		return true;
	}
	
	public void digitalReadPollStart(Integer address) {

		LOG.info("digitalRead (" + address + ") to " + deviceName);
		serialSend(DIGITAL_READ_POLLING_START, address, 0);

	}
	// ---------------------- Serial Control Methods End ------------------
	// ---------------------- Protocol Methods Begin ------------------

	public void digitalReadPollStop(Integer address) {

		LOG.info("digitalRead (" + address + ") to " + deviceName);
		serialSend(DIGITAL_READ_POLLING_STOP, address, 0);

	}

	public void digitalWrite(IOData io) {
		digitalWrite(io.address, io.value);
	}

	public void digitalWrite(Integer address, Integer value) {
		LOG.info("digitalWrite (" + address + "," + value + ") to "
				+ deviceName + " function number " + DIGITAL_WRITE);
		serialSend(DIGITAL_WRITE, address, value);
	}

	public void pinMode(IOData io) {
		pinMode(io.address, io.value);
	}

	public void pinMode(Integer address, Integer value) {
		LOG.info("pinMode (" + address + "," + value + ") to "
				+ deviceName + " function number " + PINMODE);
		serialSend(PINMODE, address, value);
	}

	public void analogWrite(IOData io) {
		analogWrite(io.address, io.value);
	}

	public void analogWrite(Integer address, Integer value) {
		LOG.info("analogWrite (" + address + "," + value + ") to "
				+ deviceName + " function number " + ANALOG_WRITE);
		serialSend(ANALOG_WRITE, address, value);
	}

	public PinData publishPin(PinData p) {
		LOG.info(p);
		return p;
	}

	// DEPRICATE !
	public PinData readServo(PinData p) {
		// TODO - translation back to pin identifier
		// e.g. pin 6 could be servo[0] - sending back we need to put pin back
		// pin is actually servo index until this translation bleh
		p.pin = servoToPin.get(p.pin);
		LOG.info(p);
		return p;
	}

	/*
	public static void addPortName(String n, int portType, CommDriver cpd) {
		// it IS misleading to have addPortName put the port in, but not
		// available through getPortIdentifiers !
		// http://en.wikibooks.org/wiki/Serial_Programming/Serial_Java -
		// The method CommPortIdentifier.addPortName() is misleading,
		// since driver classes are platform specific and their
		// implementations are not part of the public API

		customPorts.put(n, cpd);
		// CommPortIdentifier.addPortName(n, portType, cpd); // this does
		// nothing of relevance - because it does not
		// persist across getPortIdentifier calls
	}
	*/

	// TODO - blocking call which waits for serial return
	// not thread safe - use mutex? - block on expected byte count?
	// @Override - only in Java 1.6 - its only a single reference not all
	// supertypes define it

	public String readSerialMessage(String s) {
		return s;
	}

	boolean rawReadMsg = false;
	int rawReadMsgLength = 4;

	// char rawMsgBuffer

	public void setRawReadMsg(Boolean b) {
		rawReadMsg = b;
	}

	public void setReadMsgLength(Integer length) {
		rawReadMsgLength = length;
	}


	// @Override - only in Java 1.6 - its only a single reference not all
	// supertypes define it
	public String getType() {
		return ArduinoBT.class.getCanonicalName();
	}

	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(Integer pin) {
		serialSend(DIGITAL_READ_POLLING_START, pin, 0); // last param is not
		// used in read
	}

	public void digitalReadPollingStop(Integer pin) {
		serialSend(DIGITAL_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	// force an analog read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void analogReadPollingStart(Integer pin) {
		serialSend(ANALOG_READ_POLLING_START, pin, 0); // last param is not used
		// in read
	}

	public void analogReadPollingStop(Integer pin) {
		serialSend(ANALOG_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	/*
	 * Another means of distributing the data would be to publish to individual
	 * functions which might be useful for some reason in the future - initially
	 * this was started because of the overlap on the Arduino board where the
	 * analog pin addresses overlapped the digital vs 14 - 19 analog pins they
	 * are addressed 0 - 1 with analog reads
	 */

	class MotorData {
		boolean isAttached = false;
	}
	
	HashMap<String, MotorData> motorMap = new HashMap<String, MotorData>();

	// @Override - only in Java 1.6 - its only a single reference not all
	// supertypes define it
	public void motorAttach(String name, Integer PWMPin, Integer DIRPin) {
		// set the pinmodes on the 2 pins
		if (deviceName != null) {
			pinMode(PWMPin, ArduinoBT.OUTPUT);
			pinMode(DIRPin, ArduinoBT.OUTPUT);
		} else {
			LOG.error("attempting to attach motor before serial connection to "
					+ name + " Arduino is ready");
		}

	}

	public void motorDetach(String name) {
		// TODO Auto-generated method stub

	}

	public void motorMove(String name, Integer amount) {
		// TODO Auto-generated method stub

	}

	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getToolTip() {
		return "<html>Arduino is a service which interfaces with an Arduino micro-controller.<br>"
				+ "This interface can operate over radio, IR, or other communications,<br>"
				+ "but and appropriate .PDE file must be loaded into the micro-controller.<br>"
				+ "See http://myrobotlab.org/communication for details";
	}

	public void stopService() {
		super.stopService();
		releaseSerialPort();
	}

	public Vector<Integer> getOutputPins()
	{
		// TODO - base on "type"
		Vector<Integer> ret = new Vector<Integer>();
		for (int i = 2; i < 13; ++i )
		{
			ret.add(i);
		}
		return ret;
	}
	
	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
	
		//Arduino arduino = (Arduino) ServiceFactory.create("arduino", "Arduino");
		ArduinoBT arduino = new ArduinoBT("arduino");
		arduino.startService();

	}

	   /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket, socketType, this);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        //BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        //BluetoothChatService.this.start();
    }



    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice - try 1 instead of MY_UUID_SECURE or Insecure
            try {
            	// Hint: If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
            	// REF : 
            	// http://stackoverflow.com/questions/5308373/how-to-create-insecure-rfcomm-socket-in-android
            	// http://stackoverflow.com/questions/5263144/bluetooth-spp-between-android-and-other-device-uuid-and-pin-questions
            	
                tmp = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                Log.e(TAG, "tmp = " + tmp);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ArduinoBT.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final Service myService;

        public ConnectedThread(BluetoothSocket socket, String socketType, Service myService) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
        	this.myService = myService;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    // bytes = mmInStream.read(buffer);

                    // read and publish bytes
    				byte[] msg = new byte[rawReadMsgLength];
    				int newByte;
    				int numBytes = 0;
    				int totalBytes = 0;

    				// TODO - refactor big time ! - still can't dynamically change
    				// msg length
    				// also need a byLength or byStopString - with options to remove
    				// delimiter
    				while ((newByte = mmInStream.read(buffer)) >= 0) {
    					msg[numBytes] = (byte) newByte;
    					++numBytes;
    					// totalBytes += numBytes;

    					// LOG.info("read " + numBytes + " target msg length " +
    					// rawReadMsgLength);

    					if (numBytes == rawReadMsgLength) {
    						
    						
    	                    // Send the obtained bytes to the UI Activity
    	                    handler.obtainMessage(BluetoothChat.MESSAGE_READ, numBytes, -1, buffer)
    	                            .sendToTarget();
    	 
    						
    						/*
    						 * Diagnostics StringBuffer b = new StringBuffer(); for
    						 * (int i = 0; i < rawReadMsgLength; ++i) {
    						 * b.append(msg[i] + " "); }
    						 * 
    						 * LOG.error("msg" + b.toString());
    						 */
    						totalBytes += numBytes;

    						if (rawReadMsg) {
    							// raw protocol

    							String s = new String(msg);
    							LOG.info(s);
    							invoke("readSerialMessage", s);
    						} else {

    							// mrl protocol

    							PinData p = new PinData();
    							p.time = System.currentTimeMillis();
    							p.function = msg[0];
    							p.pin = msg[1];
    							// java assumes signed
    							// http://www.rgagnon.com/javadetails/java-0026.html
    							p.value = (msg[2] & 0xFF) << 8; // MSB - (Arduino
    															// int is 2 bytes)
    							p.value += (msg[3] & 0xFF); // LSB

    							// if (p.function == SERVO_READ) { COMPLETELY
    							// DEPRICATED !!!
    							// invoke("readServo", p);
    							// } else {
    							if (p.function == ANALOG_VALUE) {
    								p.type = 1;
    							}
    							p.source = myService.getName();
    							invoke(SensorData.publishPin, p);
    							// }
    						}

    						// totalBytes = 0;
    						numBytes = 0;

    						// reset buffer
    						for (int i = 0; i < rawReadMsgLength; ++i) {
    							msg[i] = -1;
    						}

    					}
    				}
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                handler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        handler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

}
