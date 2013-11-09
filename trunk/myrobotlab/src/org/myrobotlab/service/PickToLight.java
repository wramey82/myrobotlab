package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class PickToLight extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(PickToLight.class.getCanonicalName());

	// FIXME - this could be eliminated with Runtime.getServicesOfType("Arduino")
	// public HashMap<String, Arduino> arduinos = new HashMap<String, Arduino>();
	public HashMap<String, String> arduinoToPort = new HashMap<String, String>();

	// device type identifiers
	public final static String LED_01 = "LED_01";
	public final static String SWITCH_01 = "SWITCH_01";
	
	public Worker worker;
	
	// global device identifier
	int id = 1;

	public class Device {
		public int id;
		public String arduino;
		public Integer pin;
		public String type;
		public String bin;

		public Device(String arduino, Integer pin, String type, String bin) {
			this.arduino = arduino;
			this.pin = pin;
			this.type = type;
			this.bin = bin;
		}
	}

	HashMap<Integer, Device> devices = new HashMap<Integer, Device>();
	//       bin#            type   id
	HashMap<String, HashMap<String,Device>> binMap = new HashMap<String, HashMap<String,Device>>();
	
	
	public class Worker extends Thread {
		public boolean isWorking = false;
		
		public int delay;
		public int increment;
		
		public Worker(int delay, int increment)
		{
			this.delay = delay;
			this.increment = increment;	
		}
		
		public void run(){
			try{
			isWorking = true;
			Map<String, HashMap<String, Device>> sorted = new TreeMap<String, HashMap<String, Device>>(binMap);
			while (isWorking)
			{
				for (Map.Entry<String, HashMap<String, Device>> o : sorted.entrySet()) {
					//sb.append(o.getKey());
					setDevice(o.getKey(), LED_01, 1);
					sleep(delay);
					setDevice(o.getKey(), LED_01, 0);
					sleep(delay);
				} 
				
			}
			} catch(Exception e){
				isWorking = false;
			}
		}
	}

	public PickToLight(String n) {
		super(n, PickToLight.class.getCanonicalName());
		// FIXME - do this with reservations !!!
		arduinoToPort.put(String.format("%s-arduino-1", getName()), "/dev/ttyACM0");
		loadDefaultMap();
	}
	
	public void cycle(int delay)
	{
		cycleStop();
		worker = new Worker(delay, 0);
		worker.start();
	}
	
	public void cycleStop()
	{
		if (worker != null){
			worker.isWorking = false;
			worker.interrupt();
			worker = null;
		}
	}

	@Override
	public String getDescription() {
		return "Pick to light system";
	}

	public String addSwitch(String arduino, String bin, Integer pin) {
		return addDevice(arduino, bin, pin, SWITCH_01);
	}

	public String addLED(String arduino, String bin, Integer pin) {
		return addDevice(arduino, bin, pin, LED_01);
	}

	public String addDevice(String arduino, String bin, Integer pin, String type) {
		info("addDevice %s %s %d %s", arduino, bin, pin, type);
		if (pin == null) {
			return error("can not add switch with null pin");
		}

		if (!SWITCH_01.equals(type) && !LED_01.equals(type)) {
			error(String.format("cant add device %s invalid type", type));
		}

		Device device = new Device(arduino, pin, type, bin);

		devices.put(id, device);
		
		HashMap<String,Device> typeDevice = null;
		if (binMap.containsKey(bin))
		{
			typeDevice = binMap.get(bin);
		} else {
			typeDevice = new HashMap<String,Device>();
			binMap.put(bin, typeDevice);
		}

		typeDevice.put(type, device);
		
		if (!arduinoToPort.containsKey(arduino))
		{
			error("arduino %s specified but not found int arduinoToPort", arduino);
		}
		
		return String.format("added device bin %s pin %d type %s", bin, pin, type);
	}

	public boolean kitToLight(String xmlKit) {
		return true;
	}

	public void startService() {
		super.startService();
		for (Map.Entry<String, String> o : arduinoToPort.entrySet()) {
			Arduino arduino = (Arduino)Runtime.createAndStart(o.getKey(), "Arduino");
			arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
			arduino.connect(o.getValue());
		} 
	}
	
	/*
	public boolean connect()
	{
		return connect("arduino.1", "/dev/ttyACM0");
	}
	*/
	
	public boolean connect(String arduinoName, String portName)
	{
		Arduino arduino = (Arduino)Runtime.createAndStart(arduinoName, "Arduino");
		arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
		arduino.connect(portName);
		return arduino.isConnected();
	}

	public void reconnect(String arduino, String port) {
		Arduino a = (Arduino)Runtime.getService(arduino);
		a.connect(port);
	}
	

	public void loadDefaultMap() {

		addDevice(String.format("%s-arduino-1", getName()), "17", 8, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "17", 9, LED_01);

		addDevice(String.format("%s-arduino-1", getName()), "18", 7, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "18", 6, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "23", 5, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "23", 4, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "24", 3, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "24", 2, SWITCH_01);

		addDevice(String.format("%s-arduino-1", getName()), "22", 14, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "22", 15, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "21", 16, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "21", 17, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "20", 18, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "20", 19, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "19", 20, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "19", 21, SWITCH_01);

		addDevice(String.format("%s-arduino-1", getName()), "02", 22, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "02", 23, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "03", 24, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "03", 25, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "04", 26, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "04", 27, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "05", 28, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "05", 29, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "06", 30, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "06", 31, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "07", 32, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "07", 33, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "08", 34, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "08", 35, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "09", 36, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "09", 37, LED_01);

		addDevice(String.format("%s-arduino-1", getName()), "10", 38, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "10", 39, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "11", 40, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "11", 41, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "12", 42, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "12", 43, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "13", 44, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "13", 45, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "14", 46, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "14", 47, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "15", 48, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "15", 49, LED_01);
		addDevice(String.format("%s-arduino-1", getName()), "16", 50, SWITCH_01);
		addDevice(String.format("%s-arduino-1", getName()), "16", 51, LED_01);

	}

	// bin CALLS
	// setAllbinesLEDs (on off)
	// setbinesOn(String list)
	// setbinesOff(String list)
	// getbinesSwitchState()
	// ZOD
	
	public String setLEDsOn()
	{
		return setLEDsOn(null);
	}
	
	public String setLEDsOn(String binList) {
		log.info("setBinLEDsOn request");
		return setDevice(binList, LED_01, 1);
	}
	
	public String setLEDsOff()
	{
		return setLEDsOff(null);
	}
	
	public String setLEDsOff(String binList) {
		log.info("setBinLEDsOff request");
		return setDevice(binList, LED_01, 0);
	}
	
	/**
	 * @return sorted bin list in a string with space delimitation 
	 */
	public String getBinList()
	{
		StringBuffer sb = new StringBuffer();
		Map<String, HashMap<String, Device>> treeMap = new TreeMap<String, HashMap<String, Device>>(binMap);
		
		for (Map.Entry<String, HashMap<String, Device>> o : treeMap.entrySet()) {
			sb.append(" ");
			sb.append(o.getKey());
		} 
		
		return sb.toString();
	}
	
	public String setDevice(String binList, String type, Integer value)
	{
		info("setDevice request %s, %s, %d", binList, type, value);
		if (binList == null)
		{
			binList = getBinList();
		}
		String[] bins = binList.split(" ");
		for (int i = 0; i < bins.length; ++i) {
			
			String bin  = bins[i].trim();

			if (bin.length() > 0 && binMap.containsKey(bin)) {
				HashMap<String, Device> deviceMap = binMap.get(bin);
				if (!deviceMap.containsKey(type))
				{
					error("bin %s type %s does not exits", bin, type);
					continue;
				}
				
				Device device = deviceMap.get(type);
				//Arduino arduino = getArduino(arduinoName);
				// FIXME WRONG WRONG WRONG - no non-dynamic reference to arduino
				Arduino arduino = (Arduino)Runtime.getService(device.arduino);
				if (arduino == null)
				{
					error("can not get arduino %s", device.arduino);
				} else {
					info("setDevice request bin %s type %s pin %d value %d", bin, type, device.pin, value);
					arduino.digitalWrite(device.pin, value);
				}
			} else {
				error(String.format("could not find bin %s", bin));
			}

		}

		return String.format("%s.digitalWrite(%s)", String.format("%s-arduino-1", getName()), binList);
	}

	// LOWER LEVEL PIN CALLS

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		PickToLight pickToLight = new PickToLight("pickToLight");
		pickToLight.arduinoToPort.clear();
		//pickToLight.arduinoToPort.put(String.format("%s-arduino-1", getName()), "COM4");
		pickToLight.startService();
		
		/*
		Arduino arduino.1 = (Arduino) Runtime.createAndStart("arduino.1", "Arduino");
		arduino.1.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
		arduino.1.connect("COM4");
		*/
		pickToLight.setLEDsOff();
		
		pickToLight.setLEDsOff("3");
		
		log.info(pickToLight.setLEDsOn("1 3 5 6 7 "));

		// log.info(pickToLight.turnLEDsOn("arduino.1", "2 3 4 10"));

		Runtime.createAndStart("web", "WebGUI");

		// Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
