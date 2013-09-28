package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class PickToLight extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(PickToLight.class.getCanonicalName());

	// FIXME MUST REMOVE FOR MULTIPLES !!!!
	public transient Arduino arduino01;

	public final static String DEVICE_LED = "DEVICE_LED";
	public final static String DEVICE_SWITCH = "DEVICE_SWITCH";

	public class Device {
		public String arduino;
		public Integer pin;
		public String type;
		public String bin;

		public Device(String arduino, Integer pin, String type, String bin) {
			this.pin = pin;
			this.type = type;
			this.bin = bin;
		}
	}

	HashMap<Integer, Device> devices = new HashMap<Integer, Device>();
	HashMap<String, Device> binMap = new HashMap<String, Device>();

	public PickToLight(String n) {
		super(n, PickToLight.class.getCanonicalName());
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public Arduino getArduino(String arduinoName) {
		Arduino arduino = (Arduino) Runtime.getService(arduinoName);

		if (arduino == null) {
			error("can't get arduino %s", arduinoName);
			return null;
		}

		if (!arduino.isConnected()) {
			error("arduino %s not connected", arduinoName);
			return null;
		}

		return arduino;

	}

	public String addSwitch(String arduino, String bin, Integer pin) {
		return addDevice(arduino, bin, pin, DEVICE_SWITCH);
	}

	public String addLED(String arduino, String bin, Integer pin) {
		return addDevice(arduino, bin, pin, DEVICE_LED);
	}

	public String addDevice(String arduino, String bin, Integer pin, String type) {
		if (pin == null) {
			return error("can not add switch with null pin");
		}

		if (!DEVICE_SWITCH.equals(type) && !DEVICE_SWITCH.equals(type)) {
			return error(String.format("cant add device %s invalid type", type));
		}

		Device device = new Device(arduino, pin, type, bin);

		devices.put(pin, device);
		binMap.put(bin, device);

		return String.format("added device bin %s pin %d type %s", bin, pin, type);
	}

	public boolean kitToLight(String xmlKit) {
		return true;
	}

	// INITIALIZATION

	public void startService() {
		super.startService();
		arduino01 = (Arduino) Runtime.createAndStart("arduino01", "Arduino");
		arduino01.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
		arduino01.connect("/dev/ttyACM0");
		loadDefaultMap();
	}

	public void reconnect(String port) {
		arduino01.connect(port);
	}

	public void loadDefaultMap() {

		addDevice("arduino01", "17", 8, DEVICE_SWITCH);
		addDevice("arduino01", "17", 9, DEVICE_LED);

		addDevice("arduino01", "18", 7, DEVICE_LED);
		addDevice("arduino01", "18", 6, DEVICE_SWITCH);
		addDevice("arduino01", "23", 5, DEVICE_LED);
		addDevice("arduino01", "23", 4, DEVICE_SWITCH);
		addDevice("arduino01", "24", 3, DEVICE_LED);
		addDevice("arduino01", "24", 2, DEVICE_SWITCH);

		addDevice("arduino01", "22", 14, DEVICE_LED);
		addDevice("arduino01", "22", 15, DEVICE_SWITCH);
		addDevice("arduino01", "21", 16, DEVICE_LED);
		addDevice("arduino01", "21", 17, DEVICE_SWITCH);
		addDevice("arduino01", "20", 18, DEVICE_LED);
		addDevice("arduino01", "20", 19, DEVICE_SWITCH);
		addDevice("arduino01", "19", 20, DEVICE_LED);
		addDevice("arduino01", "19", 21, DEVICE_SWITCH);

		addDevice("arduino01", "2", 22, DEVICE_SWITCH);
		addDevice("arduino01", "2", 23, DEVICE_LED);
		addDevice("arduino01", "3", 24, DEVICE_SWITCH);
		addDevice("arduino01", "3", 25, DEVICE_LED);
		addDevice("arduino01", "4", 26, DEVICE_SWITCH);
		addDevice("arduino01", "4", 27, DEVICE_LED);
		addDevice("arduino01", "5", 28, DEVICE_SWITCH);
		addDevice("arduino01", "5", 29, DEVICE_LED);
		addDevice("arduino01", "6", 30, DEVICE_SWITCH);
		addDevice("arduino01", "6", 31, DEVICE_LED);
		addDevice("arduino01", "7", 32, DEVICE_SWITCH);
		addDevice("arduino01", "7", 33, DEVICE_LED);
		addDevice("arduino01", "8", 34, DEVICE_SWITCH);
		addDevice("arduino01", "8", 35, DEVICE_LED);
		addDevice("arduino01", "9", 36, DEVICE_SWITCH);
		addDevice("arduino01", "9", 37, DEVICE_LED);

		addDevice("arduino01", "10", 38, DEVICE_SWITCH);
		addDevice("arduino01", "10", 39, DEVICE_LED);
		addDevice("arduino01", "11", 40, DEVICE_SWITCH);
		addDevice("arduino01", "11", 41, DEVICE_LED);
		addDevice("arduino01", "12", 42, DEVICE_SWITCH);
		addDevice("arduino01", "12", 43, DEVICE_LED);
		addDevice("arduino01", "13", 44, DEVICE_SWITCH);
		addDevice("arduino01", "13", 45, DEVICE_LED);
		addDevice("arduino01", "14", 46, DEVICE_SWITCH);
		addDevice("arduino01", "14", 47, DEVICE_LED);
		addDevice("arduino01", "15", 48, DEVICE_SWITCH);
		addDevice("arduino01", "15", 49, DEVICE_LED);
		addDevice("arduino01", "16", 50, DEVICE_SWITCH);
		addDevice("arduino01", "16", 51, DEVICE_LED);

	}

	// bin CALLS
	// setAllbinesLEDs (on off)
	// setbinesOn(String list)
	// setbinesOff(String list)
	// getbinesSwitchState()
	// ZOD
	public String setBinsOn(String binList) {
		log.info("turnLedsOn request");

		String[] bins = binList.split(" ");
		for (int i = 0; i < bins.length; ++i) {

			if (binMap.containsKey(bins[i])) {
				Device device = binMap.get(bins[i]);
				//Arduino arduino = getArduino(arduinoName);
				// FIXME WRONG WRONG WRONG - no non-dynamic reference to arduino
				arduino01.digitalWrite(device.pin, 1);
			} else {
				return error(String.format("could not find bin %s", bins[i]));
			}

		}

		return String.format("%s.digitalWrite(%s)", "arduino01", binList);
	}

	// LOWER LEVEL PIN CALLS

	// getPINState
	public Integer getSwitchState(String arduinoName, Integer pin) {
		Arduino arduino = getArduino(arduinoName);
		// arduino.pinMode(address, value);
		return null;
	}

	// refactor - rename to turnOnPins
	public String turnLEDsOff(String arduinoName, String listOfLEDNumbers) {
		return switchLEDs(arduinoName, listOfLEDNumbers, 0);
	}

	// refactor - rename to turnOnPins
	public String turnLEDsOn(String arduinoName, String listOfLEDNumbers) {
		return switchLEDs(arduinoName, listOfLEDNumbers, 1);
	}

	// ??? THROW FOR ERROR ???//
	public String switchLEDs(String arduinoName, String listOfLEDNumbers, int value) {
		log.info("turnLedsOn request");

		Arduino arduino = getArduino(arduinoName);

		String[] leds = listOfLEDNumbers.split(" ");
		for (int i = 0; i < leds.length; ++i) {
			try {
				int address = Integer.parseInt(leds[i]);
				arduino.digitalWrite(address, value);
			} catch (NumberFormatException e) {
				Logging.logException(e); // TODO error handles exception ?
				error(e.getMessage());
			}
		}

		return String.format("%s.digitalWrite(%s)", arduinoName, listOfLEDNumbers);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		PickToLight pickToLight = (PickToLight) Runtime.createAndStart("pickToLight", "PickToLight");
		Arduino arduino01 = (Arduino) Runtime.createAndStart("arduino01", "Arduino");
		arduino01.connect("COM3");

		// log.info(pickToLight.turnLEDsOn("arduino01", "2 3 4 10"));

		Runtime.createAndStart("web", "WebGUI");

		// Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
