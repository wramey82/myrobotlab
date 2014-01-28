package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.pickToLight.ControllerType;
import org.myrobotlab.pickToLight.KitRequest;
import org.myrobotlab.pickToLight.ModuleControl;
import org.myrobotlab.pickToLight.ObjectFactory;
import org.slf4j.Logger;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

// bin calls
// moduleList calls
// setAllBoxesLEDs (on off)
// setBoxesOn(String list)
// setBoxesOff(String list)
// getBesSwitchState()
// displayString(boxlist, str)
// ZOD

/**
 * @author GroG
 * 
 *         C:\mrl\myrobotlab>xjc -d src -p org.myrobotlab.pickToLight
 *         PickToLightTypes.xsd
 * 
 */
public class PickToLight extends Service implements GpioPinListenerDigital {

	private static final long serialVersionUID = 1L;

	transient public RasPi raspi;
	transient public WebGUI webgui;

	transient public Worker worker;
	
	public final static Logger log = LoggerFactory.getLogger(PickToLight.class);

	ObjectFactory of = new ObjectFactory();

	// item# Module
	static HashMap<String, ModuleControl> modules = new HashMap<String, ModuleControl>();
	transient HashMap<String, Worker> workers = new HashMap<String, Worker>();
	
	private String mode = "kitting";

	
	String messageGoodPick = "GOOD";

	// FIXME - you will need to decouple initialization until after raspibus is
	// set
	private int rasPiBus = 0;
	// FIXME - who will update me ?
	private String updateURL;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("raspi", "RasPi", "raspi");
		peers.put("webgui", "WebGUI", "web server interface");
		return peers;
	}

	/**
	 * Worker is a PickToLight level thread which operates over (potentially)
	 * all of the service modules. Displays have their own
	 * 
	 */
	public static class Worker extends Thread {

		public boolean isWorking = false;

		public static final String TASK = "TASK";

		public HashMap<String, Object> data = new HashMap<String, Object>();

		public Worker(String task) {
			super(task);
			data.put(TASK, task);
		}

		public void run() {
			try {

				if (!data.containsKey(TASK)) {
					log.error("task is required");
					return;
				}
				String task = (String) data.get(TASK);
				isWorking = true;

				while (isWorking) {

					switch (task) {

					case "cycleAll":

						TreeMap<String, ModuleControl> sorted = new TreeMap<String, ModuleControl>(modules);
						for (Map.Entry<String, ModuleControl> o : sorted.entrySet()) {
							o.getValue().cycle((String) data.get("msg"));
						}
						isWorking = false;
						break;

					default:
						log.error(String.format("don't know how to handle task %s", task));
						break;
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
		webgui.autoStartBrowser(false);
		webgui.useLocalResources(true);
		raspi = (RasPi) createPeer("raspi");
	}
	
	public String getVersion(){
		return Runtime.getVersion();
	}

	@Override
	public String getDescription() {
		return "Pick to light system";
	}

	public boolean kitToLight(String xmlKit) {
		return true;
	}

	public void createModules() {

		Integer[] devices = scanI2CDevices();

		log.info(String.format("found %d devices", devices.length));

		for (int i = 0; i < devices.length; ++i) {
			int deviceAddress = devices[i];
			// FIXME - kludge to work with our prototype
			// addresses of displays are above 100
			/*
			 * if (deviceAddress > 100) { createModule(1, deviceAddress); }
			 */

			createModule(1, deviceAddress);

		}

		// FIXME - kludge gor proto-type hardware
		/*
		 * if (Platform.isArm()) { createPCF8574(rasPiBus, 0x27); }
		 */
	}

	public boolean createModule(int bus, int address) {
		String key = makeKey(address);
		log.info(String.format("create module key %s (bus %d address %d)", key, bus, address));
		ModuleControl box = new ModuleControl(bus, address);
		modules.put(key, box);
		return true;
	}

	public String display(Integer address, String msg) {
		String key = makeKey(address);
		if (modules.containsKey(key)) {
			modules.get(key).display(msg);
			return msg;
		} else {
			String err = String.format("display could not find module %d", key);
			log.error(err);
			return err;
		}
	}

	public ArrayList<String> displayAddresses() {
		TreeMap<String, ModuleControl> sorted = new TreeMap<String, ModuleControl>(modules);
		ArrayList<String> ret = new ArrayList<String>();

		for (Map.Entry<String, ModuleControl> o : sorted.entrySet()) {
			ModuleControl mc = o.getValue();
			ret.add(o.getKey());
			mc.display(o.getKey());
		}
		return ret;
	}

	// FIXME normalize splitting code
	public String display(String moduleList, String value) {
		if (moduleList == null) {
			log.error("box list is null");
			return "box list is null";
		}
		String[] list = moduleList.split(" ");
		for (int i = 0; i < list.length; ++i) {
			try {
				String strKey = list[i].trim();
				if (strKey.length() > 0) {

					String key = makeKey(Integer.parseInt(strKey));
					if (modules.containsKey(key)) {
						modules.get(key).display(value);
					} else {
						log.error(String.format("display could not find module %s", strKey));
					}
				}
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
		return moduleList;
	}

	/**
	 * single location for key generation - in case other parts are add in a
	 * composite key
	 * 
	 * @param address
	 * @return
	 */
	public String makeKey(Integer address) {
		return String.format("%d.%d", rasPiBus, address);
	}

	public String makeKey(Integer bus, Integer address) {
		return String.format("%d.%d", bus, address);
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		// display pin state on console

		System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " [" + event.getPin().getName() + "]" + " = " + event.getState());
		GpioPin pin = event.getPin();

		if (pin.getName().equals("GPIO 0")) {
			modules.get("01").blinkOff("ok");
		} else if (pin.getName().equals("GPIO 1")) {
			modules.get("02").blinkOff("ok");
		} else if (pin.getName().equals("GPIO 2")) {
			modules.get("03").blinkOff("ok");
		} else if (pin.getName().equals("GPIO 3")) {
			modules.get("04").blinkOff("ok");
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

	public void startService() {
		super.startService();
		raspi.startService();
		webgui.startService();

		createModules();
	}

	public ControllerType getController() {

		ControllerType controller = of.createControllerType();

		controller.setVersion("svnVersion");
		controller.setName(getName());
		try {
			ArrayList<String> addresses = Runtime.getLocalAddresses();
			if (addresses.size() != 1) {
				log.error(String.format("incorrect number of ip addresses %d", addresses.size()));
			} else {
				controller.setIpaddress(addresses.get(0));
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		TreeMap<String, ModuleControl> sorted = new TreeMap<String, ModuleControl>(modules);
		ArrayList<String> ret = new ArrayList<String>();

		for (Map.Entry<String, ModuleControl> o : sorted.entrySet()) {
			ModuleControl mc = o.getValue();
			controller.getModuleList().add(mc.getModule());
			ret.add(o.getKey());
			mc.display(o.getKey());
		}

		return controller;
	}

	// ------------ TODO - IMPLEMENT - BEGIN ----------------------
	public String update(String url) {
		// TODO - auto-update
		return "TODO - auto update";
	}

	public String update() {
		return update(updateURL);
	}

	public void drawColon(Integer bus, Integer address, boolean draw) {

	}

	/**
	 * will let you change the overall brightness of the entire display. 0 is
	 * least bright, 15 is brightest and is what is initialized by the display
	 * when you start
	 * 
	 * @param address
	 * @param level
	 * @return
	 */
	public int setBrightness(Integer address, Integer level) {
		return level;
	}

	public ModuleControl getModule(Integer address) {
		return getModule(rasPiBus, address);
	}

	public ModuleControl getModule(Integer bus, Integer address) {
		String key = makeKey(bus, address);
		if (!modules.containsKey(key)) {
			log.error(String.format("get module - could not find module with key %s", key));
			return null;
		}
		return modules.get(key);
	}

	// ---- cycling message on individual module begin ----
	public void cycle(Integer address, String msg) {
		cycle(address, msg, 300);
	}

	public void cycle(Integer address, String msg, Integer delay) {
		getModule(address).cycle(msg, delay);
	}

	public void cycleStop(Integer address) {
		getModule(address).cycleStop();
	}

	public void cycleAll(String msg) {
		cycleAll(300, msg);
	}

	public void cycleAll(int delay, String msg) {
		TreeMap<String, ModuleControl> sorted = new TreeMap<String, ModuleControl>(modules);
		for (Map.Entry<String, ModuleControl> o : sorted.entrySet()) {
			o.getValue().cycle(msg, delay);
		}
	}

	public void cycleAllStop() {
		TreeMap<String, ModuleControl> sorted = new TreeMap<String, ModuleControl>(modules);
		for (Map.Entry<String, ModuleControl> o : sorted.entrySet()) {
			o.getValue().cycleStop();
		}
	}

	public void startWorker(String key) {
		if (workers.containsKey("cycleAll")) {
			if (worker != null) {
				worker.isWorking = false;
				worker.interrupt();
				worker = null;
			}
		}
	}

	public void stopWorker(String key) {
		if (workers.containsKey("cycleAll")) {
			if (worker != null) {
				worker.isWorking = false;
				worker.interrupt();
				worker = null;
			}
		}
	}

	// ---- cycling message on individual module end ----

	public String kitToLight(KitRequest kit) {
		return "";
	}

	public void writeToDisplay (int address, byte b0, byte b1, byte b2, byte b3 ) {
		try {
			
			
/*
		    sudo i2cset -y 0 0x10 0x80
		    sudo i2cset -y 0 0x38 0 0x17 0xXX 0xXX 0xXX 0xXX i
		    sudo i2cset -y 0 0x10 0x83
*/
			
			// TODO abtract out Pi4J
			I2CBus i2cbus = I2CFactory.getInstance(rasPiBus);
			I2CDevice device = i2cbus.getDevice(address);
			device.write(address, (byte)0x80);
			device.write(address, new byte[]{(byte) 0x38, 0, (byte)0x17, b0, b1, b2, b3}, 0, 4);
			device.write(address, (byte)0x83);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// ------------ TODO - IMPLEMENT - END ----------------------

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		PickToLight pickToLight = new PickToLight("pick.1");
		pickToLight.startService();

		// String binList = pickToLight.getBoxList();
		// pickToLight.display(binList, "helo");
		/*
		 * pickToLight.display("01", "1234"); pickToLight.display(" 01 02 03 ",
		 * "1234  1"); pickToLight.display("01 03", " 1234"); //
		 * pickToLight.display(binList, "1234 ");
		 */

		// Runtime.createAndStart("web", "WebGUI");

		// Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */

	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

}
