package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.i2c.AdafruitLEDBackpack;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.pickToLight.KitRequest;
import org.slf4j.Logger;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

// cycle through single display
// cycle through (list) of displays
// cycle through all displays
// cycle time

// bin calls
// boxList calls
// setAllBoxesLEDs (on off)
// setBoxesOn(String list)
// setBoxesOff(String list)
// getBesSwitchState()
// displayString(boxlist, str)
// ZOD

public class PickToLight extends Service implements GpioPinListenerDigital {

	private static final long serialVersionUID = 1L;

	transient public RasPi raspi;
	transient public WebGUI webgui;

	public final static Logger log = LoggerFactory.getLogger(PickToLight.class);
	// item# SensorBox
	HashMap<Integer, SensorBox> sensorBoxes = new HashMap<Integer, SensorBox>();
	public Worker worker;

	String messageGoodPick = "YES";

	int rasPiBus = 1;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("raspi", "RasPi", "raspi");
		peers.put("webgui", "WebGUI", "web server interface");
		return peers;
	}

	public class SensorBox {
		Integer bus;
		Integer address;

		public AdafruitLEDBackpack display;

		public String display(String str) {
			return display.display(str);
		}

		public SensorBox(int bus, int address) {
			this.bus = bus;
			this.address = address;
			try {

				display = new AdafruitLEDBackpack(bus, address);
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
		
		

		// FIXME - add PCFBLAHBLAH
	}
	
	public void cycleMsgOnDisplay(Integer address, String msg, Integer delay)
	{
		if (sensorBoxes.containsKey(address)) {
			SensorBox sb = sensorBoxes.get(address);
			sb.display.cycleOn(msg, delay);
		} else {
			log.error(String.format("cycleMsgOnDisplay could not find sensor box %d"));
		}
		
	}
	
	public void cycleMsgOff(Integer address)
	{
		if (sensorBoxes.containsKey(address)) {
			SensorBox sb = sensorBoxes.get(address);
			sb.display.cycleOff();
		} else {
			log.error(String.format("cycleMsgOff could not find sensor box %d"));
		}
	}
	

	public String kitToLight(KitRequest kit) {
		return "";
	}

	// single monolithic worker thread
	// think of it like the SwingUtilities thread 
	// easy 1 thread maintenance
	public class Worker extends Thread {
		
		ConcurrentHashMap<String, Object[]> workQueue = new ConcurrentHashMap<String, Object[]>();
		
		public boolean isWorking = false;

		public int idleSleep = 100;
		public int delay;
		public String msg = "helo";

		public Worker(int delay, String msg) {
			this.msg = msg;
			this.delay = delay;
		}

		public void run() {
			try {
				isWorking = true;
				long workTime = 0;
				
				while (isWorking) {
					if (workQueue.size() == 0){
						sleep(idleSleep);
					} else {
	
						workTime = System.currentTimeMillis();
						for (Map.Entry<String, Object[]> o : workQueue.entrySet()) {
							String task = o.getKey();
							
							switch (task){
							case "cycleDisplay":
								break;
								
							}
							// o.getValue().display(msg);
							//int pickCount = (int) (1 + Math.random() * 26);
							//o.getValue().display(pickCount + "");
							//sleep(delay);
							// o.getValue().display("    ");
							// sleep(delay);
						}

					}
					TreeMap<Integer, SensorBox> sorted = new TreeMap<Integer, SensorBox>(sensorBoxes);
					for (Map.Entry<Integer, SensorBox> o : sorted.entrySet()) {
						// o.getValue().display(msg);
						int pickCount = (int) (1 + Math.random() * 26);
						o.getValue().display(pickCount + "");
						sleep(delay);
						// o.getValue().display("    ");
						// sleep(delay);
					}

				}
			} catch (Exception e) {
				isWorking = false;
			}
		}
	}

	public PickToLight(String n) {
		super(n);
		webgui = (WebGUI) createPeer("webgui");
		raspi = (RasPi) createPeer("raspi");
	}

	public void cycle(int delay, String msg) {
		cycleStop();
		worker = new Worker(delay, msg);
		worker.start();
	}

	public void cycleStop() {
		if (worker != null) {
			worker.isWorking = false;
			worker.interrupt();
			worker = null;
		}
	}

	@Override
	public String getDescription() {
		return "Pick to light system";
	}

	public boolean kitToLight(String xmlKit) {
		return true;
	}

	public void createSensorBoxes() {

		Integer[] devices = scanI2CDevices();

		log.info(String.format("found %d devices", devices.length));

		for (int i = 0; i < devices.length; ++i) {
			int deviceAddress = devices[i];
			// FIXME - kludge to work with our prototype
			// addresses of displays are above 100
			if (deviceAddress > 100) {
				createSensorBox(1, deviceAddress);
			}

		}

		// FIXME - kludge gor proto-type hardware
		createPCF8574(rasPiBus, 0x27);
	}

	public boolean createSensorBox(int bus, int address) {
		log.info(String.format("adding sensor box %d %d", bus, address));
		SensorBox box = new SensorBox(bus, address);
		sensorBoxes.put(address, box);
		return true;
	}
	
	public String display(Integer address, String msg) {
		if (sensorBoxes.containsKey(address)) {
			sensorBoxes.get(address).display(msg);
			return msg;
		} else {
			String err = String.format("display could not find sensorbox %d", address);
			log.error(err);
			return err;
		}
	}

	// FIXME normalize splitting code
	public String display(String boxList, String value) {
		if (boxList == null) {
			log.error("box list is null");
			return "box list is null";
		}
		String[] list = boxList.split(" ");
		for (int i = 0; i < list.length; ++i) {
			try {
				String strKey = list[i].trim();
				Integer key = Integer.parseInt(strKey);
				if (sensorBoxes.containsKey(key)) {
					sensorBoxes.get(key).display(value);
				} else {
					log.error(String.format("display could not find sensorbox %s", strKey));
				}
			} catch (Exception e) {
				Logging.logException(e);
			}
		}

		return boxList;

	}

	/**
	 * DEPRECATED 
	 * @return sorted box list in a string with space delimited
	 */
	public String getBoxList() {
		StringBuffer sb = new StringBuffer();
		Map<Integer, SensorBox> treeMap = new TreeMap<Integer, SensorBox>(sensorBoxes);

		boolean append = false;

		for (Map.Entry<Integer, SensorBox> o : treeMap.entrySet()) {
			sb.append((append ? " " : ""));
			sb.append(o.getKey());
			append = true;
		}

		return sb.toString();
	}

	// LOWER LEVEL PIN CALLS

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		// display pin state on console

		System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " [" + event.getPin().getName() + "]" + " = " + event.getState());
		GpioPin pin = event.getPin();

		if (pin.getName().equals("GPIO 0")) {
			sensorBoxes.get("01").display.blinkOff("ok");
		} else if (pin.getName().equals("GPIO 1")) {
			sensorBoxes.get("02").display.blinkOff("ok");
		} else if (pin.getName().equals("GPIO 2")) {
			sensorBoxes.get("03").display.blinkOff("ok");
		} else if (pin.getName().equals("GPIO 3")) {
			sensorBoxes.get("04").display.blinkOff("ok");
		}
		// if (pin.getName().equals(anObject))
	}

	public PCF8574GpioProvider createPCF8574(Integer bus, Integer address) {
		try {

			// System.out.println("<--Pi4J--> PCF8574 GPIO Example ... started.");

			log.info(String.format("PCF8574 - begin - bus %d address %d", bus, address));

			// create gpio controller
			GpioController gpio = GpioFactory.getInstance();

			// create custom MCP23017 GPIO provider
			// PCF8574GpioProvider gpioProvider = new
			// PCF8574GpioProvider(I2CBus.BUS_1,
			// PCF8574GpioProvider.PCF8574A_0x3F);
			PCF8574GpioProvider gpioProvider = new PCF8574GpioProvider(bus, address);

			// provision gpio input pins from MCP23017
			GpioPinDigitalInput myInputs[] = { gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_00), gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_01),
					gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_02), gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_03),
					gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_04), gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_05),
					gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_06), gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_07) };

			// create and register gpio pin listener
			gpio.addListener(this, myInputs);
			/*
			 * gpio.addListener(new GpioPinListenerDigital() {
			 * 
			 * @Override public void
			 * handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent
			 * event) { // display pin state on console
			 * System.out.println(" --> GPIO PIN STATE CHANGE: " +
			 * event.getPin() + " = " + event.getState()); } }, myInputs);
			 */

			/*
			 * // provision gpio output pins and make sure they are all LOW at
			 * startup GpioPinDigitalOutput myOutputs[] = {
			 * gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_04,
			 * PinState.LOW), gpio.provisionDigitalOutputPin(gpioProvider,
			 * PCF8574Pin.GPIO_05, PinState.LOW),
			 * gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_06,
			 * PinState.LOW) };
			 * 
			 * // on program shutdown, set the pins back to their default state:
			 * HIGH //gpio.setShutdownOptions(true, PinState.HIGH, myOutputs);
			 * 
			 * // keep program running for 20 seconds for (int count = 0; count
			 * < 10; count++) { gpio.setState(true, myOutputs);
			 * Thread.sleep(1000); gpio.setState(false, myOutputs);
			 * Thread.sleep(1000); }
			 * 
			 * // stop all GPIO activity/threads by shutting down the GPIO
			 * controller // (this method will forcefully shutdown all GPIO
			 * monitoring threads and scheduled tasks) // gpio.shutdown();
			 */
			log.info(String.format("PCF8574 - end - bus %d address %d", bus, address));

			return gpioProvider;

		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
	}

	public Integer[] scanI2CDevices() {
		// raspi's "now" have 1 bus = 0x01
		return raspi.scanI2CDevices(rasPiBus);
	}

	public void displayI2CAddresses() {
		TreeMap<Integer, SensorBox> sorted = new TreeMap<Integer, SensorBox>(sensorBoxes);

		for (Map.Entry<Integer, SensorBox> o : sorted.entrySet()) {
			// o.getValue().display(msg);
			o.getValue().display(o.getKey() + "");
		}
	}

	public void startService() {
		super.startService();
		raspi.startService();
		webgui.startService();

		createSensorBoxes();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		
		String msg = "happy holidays merry christmas happy new year";
		
		log.info(String.format("[%s]",msg.substring(5, 9)));

		PickToLight pickToLight = new PickToLight("pickToLight");
		pickToLight.startService();
		String binList = pickToLight.getBoxList();
		pickToLight.display(binList, "helo");

		pickToLight.display("01", "1234");
		pickToLight.display(" 01 02 03 ", "1234  1");
		pickToLight.display("01 03", " 1234");
		pickToLight.display(binList, "1234 ");

		Runtime.createAndStart("web", "WebGUI");

		// Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
