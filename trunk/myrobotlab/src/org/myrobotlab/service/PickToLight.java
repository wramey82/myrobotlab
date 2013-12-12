package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.i2c.AdafruitLEDBackpack;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class PickToLight extends Service implements GpioPinListenerDigital {

	private static final long serialVersionUID = 1L;

	transient public final GpioController gpio = GpioFactory.getInstance();

	// the 2 pins for I2C on the raspberry
	GpioPinDigitalOutput gpio01;
	GpioPinDigitalOutput gpio03;

	
	public final static Logger log = LoggerFactory.getLogger(PickToLight.class);
	//      item#   bin
	HashMap<String, Bin> bins = new HashMap<String, Bin>();
	public Worker worker;

	String messageGoodPick = "YES";
	
	// global device identifier
	int id = 1;

	public class Bin {
		public String itemNum;
		public AdafruitLEDBackpack display;

		// public PFBLAHBLAH
		// public HashMap<String,Device> devices = new HashMap<String,Device>();

		public String display(String str) {
			return display.display(str);
		}

		public Bin(String itemNum) {
			this.itemNum = itemNum;
		}

		public boolean addDisplay(int bus, int address) {
			try {

				display = new AdafruitLEDBackpack(bus, address);
				return true;
			} catch (Exception e) {
				Logging.logException(e);
				return false;
			}
		}
	}

	public class Worker extends Thread {
		public boolean isWorking = false;

		public int delay;
		public String msg = "helo";

		public Worker(int delay, String msg) {
			this.msg = msg;
			this.delay = delay;
		}

		public void run() {
			try {
				isWorking = true;
				TreeMap<String, Bin> sorted = new TreeMap<String, Bin>(bins);
				while (isWorking) {
					for (Map.Entry<String, Bin> o : sorted.entrySet()) {
						//o.getValue().display(msg);
						int pickCount = (int) (1 + Math.random()*26);
						o.getValue().display(pickCount+"");
						sleep(delay);
						//o.getValue().display("    ");
						//sleep(delay);
					}

				}
			} catch (Exception e) {
				isWorking = false;
			}
		}
	}

	public PickToLight(String n) {
		super(n);
		
		gpio01 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
		gpio03 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);
		
		// FIXME - do this with reservations !!!
		// arduinoToPort.put(String.format("%s-arduino-1", getName()),
		// "/dev/ttyACM0");
		loadDefaultMap();
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

	public void loadDefaultMap() {

		// remember its HEX !!!
		addBin("01", 1, 0x70);
		addBin("02", 1, 0x72);
		addBin("03", 1, 0x73);
		addBin("04", 1, 0x75);
		createPCF8574(1, 0x20);
	}

	public boolean addBin(String itemNum, int bus, int address) {
		log.info(String.format("adding bin %s %d %d", itemNum, bus, address));
		Bin bin = new Bin(itemNum);
		bin.addDisplay(bus, address);
		bins.put(itemNum, bin);
		return true;
	}

	// bin calls
	// binList calls
	// setAllbinesLEDs (on off)
	// setbinesOn(String list)
	// setbinesOff(String list)
	// getbinesSwitchState()
	// displayString(binlist, str)
	// ZOD

	public String display(String binList, String value) {
		if (binList == null) {
			log.error("bin list is null");
			return "bin list is null";
		}
		String[] list = binList.split(" ");
		for (int i = 0; i < list.length; ++i) {
			try {
				String key = list[i].trim();
				if (bins.containsKey(key)){
					bins.get(key).display(value);
				} else {
					log.error(String.format("display could not find bin %s", key));
				}
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
		
		return binList;

	}

	/**
	 * @return sorted bin list in a string with space delimited  
	 */
	public String getBinList() {
		StringBuffer sb = new StringBuffer();
		Map<String, Bin> treeMap = new TreeMap<String, Bin>(bins);

		boolean append = false;
		
		for (Map.Entry<String, Bin> o : treeMap.entrySet()) {
			sb.append((append?" ":""));
			sb.append(o.getKey());
			append = true;
		}

		return sb.toString();
	}

	// LOWER LEVEL PIN CALLS

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		PickToLight pickToLight = new PickToLight("pickToLight");
		pickToLight.startService();
		String binList = pickToLight.getBinList();
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

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        // display pin state on console
    	
        System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " [" +  event.getPin().getName() + "]" + " = "
                + event.getState());
        GpioPin pin = event.getPin();
        
        if (pin.getName().equals("GPIO 0")) {
        	bins.get("01").display.blinkOff("ok");
        } else if (pin.getName().equals("GPIO 1")) {
        	bins.get("02").display.blinkOff("ok");
        } else if (pin.getName().equals("GPIO 2")) {
        	bins.get("03").display.blinkOff("ok");
        } else if (pin.getName().equals("GPIO 3")) {
        	bins.get("04").display.blinkOff("ok");
        }
//        if (pin.getName().equals(anObject))
    }
    
    
    public PCF8574GpioProvider createPCF8574(Integer bus, Integer address)  {
    	try {
        
        //System.out.println("<--Pi4J--> PCF8574 GPIO Example ... started.");
    		
    	log.info(String.format("PCF8574 - begin - bus %d address %d", bus, address));
        
        // create gpio controller
        GpioController gpio = GpioFactory.getInstance();
        
        // create custom MCP23017 GPIO provider
        //PCF8574GpioProvider gpioProvider = new PCF8574GpioProvider(I2CBus.BUS_1, PCF8574GpioProvider.PCF8574A_0x3F);
        PCF8574GpioProvider gpioProvider = new PCF8574GpioProvider(bus, address);
        
        // provision gpio input pins from MCP23017
        GpioPinDigitalInput myInputs[] = {
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_00),
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_01),
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_02),
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_03),
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_04),
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_05),
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_06),
                gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_07)
            };
        
        // create and register gpio pin listener
        gpio.addListener(this, myInputs);
        /*
        gpio.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = "
                        + event.getState());
            }
        }, myInputs);
        */
        
        /*
        // provision gpio output pins and make sure they are all LOW at startup
        GpioPinDigitalOutput myOutputs[] = { 
            gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_04, PinState.LOW),
            gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_05, PinState.LOW),
            gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_06, PinState.LOW)
          };

        // on program shutdown, set the pins back to their default state: HIGH 
        //gpio.setShutdownOptions(true, PinState.HIGH, myOutputs);
        
        // keep program running for 20 seconds	
        for (int count = 0; count < 10; count++) {
            gpio.setState(true, myOutputs);
            Thread.sleep(1000);
            gpio.setState(false, myOutputs);
            Thread.sleep(1000);
        }
        
        // stop all GPIO activity/threads by shutting down the GPIO controller
        // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
       // gpio.shutdown();
        */
    	log.info(String.format("PCF8574 - end - bus %d address %d", bus, address));

    	return gpioProvider;
    	
    	} catch(Exception e) {
    		Logging.logException(e);
    	}
    	
    	return null;
    }


}
