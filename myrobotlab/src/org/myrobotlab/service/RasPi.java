package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

public class RasPi extends Service implements GpioPinListenerDigital {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RasPi.class.getCanonicalName());
	transient public final GpioController gpio = GpioFactory.getInstance();

	// the 2 pins for I2C on the raspberry
	GpioPinDigitalOutput gpio01;
	GpioPinDigitalOutput gpio03;

	HashMap<String, Device> devices = new HashMap<String, Device>();
	static HashMap<String, Byte> translation = new HashMap<String, Byte>();

	boolean initialized = false;

	Tester tester = null;

	public static class Device {
		public I2CBus bus;
		public I2CDevice device;
		public String type;
	}

	public RasPi(String n) {
		super(n);
		gpio01 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
		gpio03 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);
		// gpio01.
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void init() {
		if (initialized) {
			return;
		}
		gpio01 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
		gpio03 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);

		initTranslation();

		// provision gpio pin #02 as an input pin with its internal pull down
		// resistor enabled
		// final GpioPinDigitalInput myButton =
		// gpio.provisionDigitalInputPin(RaspiPin.GPIO_02,
		// PinPullResistance.PULL_DOWN);

		/*
		 * GpioPinDigitalInput myButton =
		 * gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, // PIN NUMBER
		 * "MyButton", // PIN FRIENDLY NAME (optional)
		 * PinPullResistance.PULL_DOWN); // PIN RESISTANCE (optional)
		 */
		initialized = true;
	}

	public void blinkTest() {
		gpio01.blink(500, 15000);
	}

	public static void MCP23017() throws InterruptedException, IOException {

		System.out.println("<--Pi4J--> MCP23017 GPIO Example ... started.");

		// create gpio controller
		final GpioController gpio = GpioFactory.getInstance();

		// create custom MCP23017 GPIO provider
		final MCP23017GpioProvider gpioProvider = new MCP23017GpioProvider(I2CBus.BUS_0, 0x21);

		// provision gpio input pins from MCP23017
		GpioPinDigitalInput myInputs[] = { gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A0, "MyInput-A0", PinPullResistance.PULL_UP),
				gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A1, "MyInput-A1", PinPullResistance.PULL_UP),
				gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A2, "MyInput-A2", PinPullResistance.PULL_UP),
				gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A3, "MyInput-A3", PinPullResistance.PULL_UP),
				gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A4, "MyInput-A4", PinPullResistance.PULL_UP),
				gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A5, "MyInput-A5", PinPullResistance.PULL_UP),
				gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A6, "MyInput-A6", PinPullResistance.PULL_UP),
				gpio.provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A7, "MyInput-A7", PinPullResistance.PULL_UP), };

		// create and register gpio pin listener
		gpio.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				// display pin state on console
				System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
			}
		}, myInputs);

		// provision gpio output pins and make sure they are all LOW at startup
		GpioPinDigitalOutput myOutputs[] = { gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B0, "MyOutput-B0", PinState.LOW),
				gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B1, "MyOutput-B1", PinState.LOW),
				gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B2, "MyOutput-B2", PinState.LOW),
				gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B3, "MyOutput-B3", PinState.LOW),
				gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B4, "MyOutput-B4", PinState.LOW),
				gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B5, "MyOutput-B5", PinState.LOW),
				gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B6, "MyOutput-B6", PinState.LOW),
				gpio.provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B7, "MyOutput-B7", PinState.LOW) };

		// keep program running for 20 seconds
		for (int count = 0; count < 10; count++) {
			gpio.setState(true, myOutputs);
			Thread.sleep(1000);
			gpio.setState(false, myOutputs);
			Thread.sleep(1000);
		}

		// stop all GPIO activity/threads by shutting down the GPIO controller
		// (this method will forcefully shutdown all GPIO monitoring threads and
		// scheduled tasks)
		gpio.shutdown();
	}

	// private GpioStateMonitor monitor = null;

	public void predatorBomb(int value) {
		for (int i = 0; i < value; ++i) {
			sleep(70);
			// SevenSegment(0, i);
		}
	}

	public void writeRaw(int busAddress, int deviceAddress, byte d0, byte d1, byte d2, byte d3, byte d4, byte d5, byte d6, byte d7, byte d8, byte d9, byte d10, byte d11, byte d12,
			byte d13, byte d14, byte d15) {
		try {
			log.info("--------writeRaw begin -------------");

			log.info(String.format("test %d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d", d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15));
			I2CDevice device = getDevice(busAddress, deviceAddress);
			device.write(0x00, new byte[] { d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15 }, 0, 16);

			log.info("--------writeRaw end-------------");
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public I2CDevice getDevice(int busAddress, int deviceAddress) {
		try {
			String key = String.format("%d.%d", busAddress, deviceAddress);
			if (!devices.containsKey(key)) {
				// FIXME -- remove put in createDevice
				I2CBus bus = I2CFactory.getInstance(busAddress);
				log.info("getDevice 70");
				I2CDevice device = bus.getDevice(deviceAddress);

				Device d = new Device();
				d.bus = bus;
				d.device = device;
				d.type = "display";

				devices.put(key, d);
				return d.device;

			} else {
				return devices.get(key).device;
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
	}

	/*
	 * public int PCF8574SetValue(int busAddress, int deviceAddress, int pin,
	 * double value) {
	 * 
	 * PCF8574GpioProvider pcf = (PCF8574GpioProvider) getDevice(busAddress,
	 * deviceAddress); pcf. pcf.setValue(pin, value);
	 * 
	 * }
	 */

	public String listDevices(int busAddress) {
		StringBuffer sb = new StringBuffer();
		try {
			/* From its name we can easily deduce that it provides a communication link between ICs (integrated circuits). I2C is multimaster and can support a maximum of 112 devices on the bus. The specification declares that 128 devices can be connected to the I2C bus, but it also defines 16 reserved addresses. */
			I2CBus bus = I2CFactory.getInstance(busAddress);
			
			for (int i = 0; i < 128; ++i) {
				I2CDevice device = bus.getDevice(i);
				if (device != null) 
				{
					sb.append(i);
					sb.append(" ");
				}
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
		
		return sb.toString();
	}

	// FIXME - make backpack work as real I2C device
	public I2CDevice createDevice(int busAddress, int deviceAddress, String type) {

		try {

			String key = String.format("%d.%d", busAddress, deviceAddress);
			I2CBus bus = I2CFactory.getInstance(busAddress);

			// PCF8574GpioProvider pcf = new PCF8574GpioProvider(busAddress,
			// deviceAddress);
			// I2CDevice device = bus.getDevice(deviceAddress);

			// PCF8574GpioProvider p = new PCF8574GpioProvider(busAddress,
			// deviceAddress);
			// p.setValue(pin, value)
			
			if ("com.pi4j.gpio.extension.pcf.PCF8574GpioProvider".equals(type)){
				Device d = new Device();
				d.bus = bus;
				d.device = (I2CDevice) new PCF8574GpioProvider(busAddress, deviceAddress);
				d.type = d.device.getClass().getCanonicalName();// "PCF8574GpioProvider";
																// // full type name
				devices.put(key, d);
				return d.device;
			} else {
				log.error("could not create device %s", type);
				return null;
			}

		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
	}
	
	boolean initPCF = false;
	
	/*
	public boolean setPCF8574AsInput(Integer busAddress, Integer deviceAddress)
	{
		PCF8574GpioProvider gpioProvider = (PCF8574GpioProvider)getDevice(busAddress, deviceAddress);
		if (gpioProvider == null)
		{
			gpioProvider = (PCF8574GpioProvider)createDevice(busAddress, deviceAddress, "com.pi4j.gpio.extension.pcf.PCF8574GpioProvider");
		}
		
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
		
	}
	*/
	
    public static void testPCF8574(Integer busAddress, Integer deviceAddress)  {
    	try {
        
        //System.out.println("<--Pi4J--> PCF8574 GPIO Example ... started.");
    		
    	log.info(String.format("PCF8574 - begin - bus %d address %d", busAddress, deviceAddress));
        
        // create gpio controller
        GpioController gpio = GpioFactory.getInstance();
        
        // create custom MCP23017 GPIO provider
        //PCF8574GpioProvider gpioProvider = new PCF8574GpioProvider(I2CBus.BUS_1, PCF8574GpioProvider.PCF8574A_0x3F);
        PCF8574GpioProvider gpioProvider = new PCF8574GpioProvider(busAddress, deviceAddress);
        
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
        gpio.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = "
                        + event.getState());
            }
        }, myInputs);
        
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
    	log.info(String.format("PCF8574 - end - bus %d address %d", busAddress, deviceAddress));

    	} catch(Exception e) {
    		Logging.logException(e);
    	}
    }

	// FIXME LOW LEVEL ANY I2C READ OR WRITE !!!
	public byte I2CWrite(int busAddress, int deviceAddress, byte value) {
		I2CDevice device = getDevice(busAddress, deviceAddress);
		
		if (device == null){
			error("bus %d device %d not valid", busAddress, deviceAddress);
			return -1;
		}

		try {
			device.write(value);
			return value;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return -1;
	}

	public void displayClear(int busAddress, int deviceAddress) {
		try {
			I2CDevice device = getDevice(busAddress, deviceAddress);
			device.write(0x00, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
			device.write(0x00, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
			device.write(0x00, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
			device.write(0x00, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	public static void initTranslation() {

		translation.put("", (byte) 0);
		translation.put(" ", (byte) 0);

		translation.put(":", (byte) 3);
		// translation.put("of", (byte)0)

		translation.put("a", (byte) 119);
		translation.put("b", (byte) 124);
		translation.put("c", (byte) 57);
		translation.put("d", (byte) 94);
		translation.put("e", (byte) 121);
		translation.put("f", (byte) 113);
		translation.put("g", (byte) 111);
		translation.put("h", (byte) 118);
		translation.put("i", (byte) 48);
		translation.put("J", (byte) 30);
		translation.put("k", (byte) 118);
		translation.put("l", (byte) 56);
		translation.put("m", (byte) 21);
		translation.put("n", (byte) 84);
		translation.put("o", (byte) 63);
		translation.put("P", (byte) 115);
		translation.put("q", (byte) 103);
		translation.put("r", (byte) 80);
		translation.put("s", (byte) 109);
		translation.put("t", (byte) 120);
		translation.put("u", (byte) 62);
		translation.put("v", (byte) 98);
		translation.put("x", (byte) 118);
		translation.put("y", (byte) 110);
		translation.put("z", (byte) 91);

		translation.put("-", (byte) 64);
		// translation.put("dot", (byte)???);

		translation.put("0", (byte) 63);
		translation.put("1", (byte) 6);
		translation.put("2", (byte) 91);
		translation.put("3", (byte) 79);
		translation.put("4", (byte) 102);
		translation.put("5", (byte) 109);
		translation.put("6", (byte) 125);
		translation.put("7", (byte) 7);
		translation.put("8", (byte) 127);
		translation.put("9", (byte) 111);
	}

	public static byte translate(char c) {
		byte b = 0;
		String s = String.valueOf(c).toLowerCase();
		if (translation.containsKey(s)) {
			b = translation.get(s);
		}
		return b;
	}

	public int displayDigit(int busAddress, int deviceAddress, int i) {

		String data = String.format("%d", i);
		;
		displayString(busAddress, deviceAddress, data);

		return i;
	}

	public String displayString(int busAddress, int deviceAddress, String data) {

		initTranslation();
		// d1 d2 : d3 d4
		byte[] display = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		if (data == null || data == "") {
			writeDisplay(busAddress, deviceAddress, display);
			return data;
		}

		if (data.length() < 4) {
			data = String.format("%4s", data);
		}

		display[0] = translate(data.charAt(0));
		display[2] = translate(data.charAt(1));
		display[6] = translate(data.charAt(2));
		display[8] = translate(data.charAt(3));

		writeDisplay(busAddress, deviceAddress, display);

		return data;
	}

	public byte[] writeDisplay(int busAddress, int deviceAddress, byte[] data) {
		I2CDevice device = getDevice(busAddress, deviceAddress);

		if (device == null) {
			log.error(String.format("bad device bus %d device %d", busAddress, deviceAddress));
			return data;
		}

		try {
			device.write(0x00, data, 0, 16);
		} catch (Exception e) {
			Logging.logException(e);
		}

		return data;
	}

	public boolean init7SegmentDisplay(int busAddress, int deviceAddress) {
		try {
			I2CDevice device = getDevice(busAddress, deviceAddress);
			if (device == null) {
				return false;
			}

			device.write(0x21, (byte) 0x00);
			device.write(0x81, (byte) 0x00);
			device.write(0xEF, (byte) 0x00);
			device.write(0x00, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);

		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public void startTester() {
		int busAddress = 1;

		init7SegmentDisplay(busAddress, 0x70);
		init7SegmentDisplay(busAddress, 0x71);
		init7SegmentDisplay(busAddress, 0x72);
		init7SegmentDisplay(busAddress, 0x73);
		init7SegmentDisplay(busAddress, 0x74);
		init7SegmentDisplay(busAddress, 0x75);

		tester = new Tester();
		tester.start();

	}

	public void stopTester() {
		tester.interrupt();
		tester.running = false;
		tester = null;
	}

	public class Tester extends Thread {
		public boolean running = true;
		public int busAddress = 1;

		public void run() {
			while (running) {
				// for (int j = 0; j < repeat; ++j) {
				for (int deviceAddress = 0; deviceAddress < 6; ++deviceAddress) {
					for (int i = 0; i < 100; ++i) {
						displayDigit(busAddress, 0x70 + deviceAddress, i);
						try {
							Thread.sleep(30);
						} catch (InterruptedException e) {
							running = false;
						}
					}

					displayDigit(busAddress, 0x70 + deviceAddress, (int) (Math.random() * 9999));
				}
				// }
			}
		}

	}
	

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		/*
		 * RasPi.displayString(1, 70, "1");
		 * 
		 * RasPi.displayString(1, 70, "abcd");
		 * 
		 * RasPi.displayString(1, 70, "1234");
		 * 
		 * 
		 * //RasPi raspi = new RasPi("raspi");
		 */

		// raspi.writeDisplay(busAddress, deviceAddress, data)

		int i = 0;

		Runtime.createAndStart(String.format("ras%d", i), "Runtime");
		Runtime.createAndStart(String.format("rasPi%d", i), "RasPi");
		Runtime.createAndStart(String.format("rasGUI%d", i), "GUIService");
		Runtime.createAndStart(String.format("rasPython%d", i), "Python");
		// Runtime.createAndStart(String.format("rasClock%d",i), "Clock");
		Runtime.createAndStart(String.format("rasRemote%d", i), "RemoteAdapter");
	}


}
