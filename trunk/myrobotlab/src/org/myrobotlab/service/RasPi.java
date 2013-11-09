package org.myrobotlab.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
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

public class RasPi extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RasPi.class.getCanonicalName());
	transient public final GpioController gpio = GpioFactory.getInstance();
	GpioPinDigitalOutput gpio01;
	GpioPinDigitalOutput gpio03;

	private boolean initialized = false;
	
	public static class Device {
		public I2CBus bus;
		public I2CDevice device;
		public String type;
	}

	public HashMap<String, Device> devices = new HashMap<String, Device>();
	
	public RasPi(String n) {
		super(n, RasPi.class.getCanonicalName());
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

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] my_int_to_bb_le(int myInteger) {
		return ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN).putInt(myInteger).array();
	}

	public static int my_bb_to_int_le(byte[] byteBarray) {
		return ByteBuffer.wrap(byteBarray).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	public static byte[] my_int_to_bb_be(int myInteger) {
		return ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
	}

	public static int my_bb_to_int_be(byte[] byteBarray) {
		return ByteBuffer.wrap(byteBarray).order(ByteOrder.BIG_ENDIAN).getInt();
	}

	// byte[] buffer = new byte[]{5, 1, 2, 3, 123, 115, 6, 114 };

	byte[] bytes = new byte[16];

	public void SevenSegment(byte d0, byte d1, byte d2, byte d3, byte d4, byte d5, byte d6, byte d7) {
		try {

			/*
			log.info("--------SevenSegment begin-------------");
			final GpioController gpio = GpioFactory.getInstance();

			log.info("I2CFactory bus 0");
			bus = I2CFactory.getInstance(I2CBus.BUS_0);
			log.info("getDevice 70");
			device = bus.getDevice(0x70);

			byte[] buffer = new byte[] { d0, d1, d2, d3, d4, d5, d6, d7 };
			// byte[] buffer = my_int_to_bb_be(value);
			// byte[] buffer = new byte[]{(byte)address, (byte)value};

			// log.info(String.format("writing %d %s", address,
			// bytesToHex(buffer)));

			for (int i = 0; i < buffer.length; ++i) {
				bytes[i] = (byte) (buffer[i] & 0xFF);
				bytes[i + 1] = (byte) ((buffer[i] >> 8) & 0xFF);
			}

			log.info(String.format("buffer  %s", bytesToHex(buffer)));
			log.info(String.format("writing %s", bytesToHex(bytes)));

			device.write(buffer, 0, buffer.length);
			// device.write(address, (byte)value);
			// device.write(0x00, buffer, 0, 32);
			// device.write((byte) address);
			// device.write((byte) value);

			log.info("finished");
			*/
			/*
			 * log.info(String.format("AdafruitLEDBackpack %d %d", I2CBus.BUS_0,
			 * 0x70 ));
			 * 
			 * final AdafruitLEDBackpack gpioProvider = new
			 * AdafruitLEDBackpack(I2CBus.BUS_0, 0x70);
			 * 
			 * //log.info("provisionDigitalOutputPin");
			 * //gpio.provisionDigitalOutputPin(gpioProvider,
			 * MCP23017Pin.GPIO_B0, "MyOutput-B0", PinState.LOW); gpioProvider.
			 * 
			 * log.info("provisionDigitalOutputPin");
			 * 
			 * // provision gpio output pins and make sure they are all LOW at
			 * startup GpioPinDigitalOutput myOutputs[] = {
			 * gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_04,
			 * PinState.LOW), gpio.provisionDigitalOutputPin(gpioProvider,
			 * PCF8574Pin.GPIO_05, PinState.LOW),
			 * gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_06,
			 * PinState.LOW) };
			 * 
			 * 
			 * gpio.setState(true, arg1); // on program shutdown, set the pins
			 * back to their default state: HIGH //
			 * gpio.setShutdownOptions(true, PinState.HIGH, myOutputs);
			 * 
			 * // keep program running for 20 seconds
			 * log.info("--------begin setState begin-------------");
			 * 
			 * for (int count = 0; count < 10; count++) {
			 * log.info("setState myOutputs true"); gpio.setState(true,
			 * myOutputs);
			 * 
			 * 
			 * Thread.sleep(1000); log.info("setState myOutputs false");
			 * gpio.setState(false, myOutputs); Thread.sleep(1000); }
			 */
			/*
			 * 
			 * self.i2c = Adafruit_I2C(address) self.address = address
			 * self.debug = debug
			 * 
			 * # Turn the oscillator on
			 * self.i2c.write8(self.__HT16K33_REGISTER_SYSTEM_SETUP | 0x01,
			 * 0x00)
			 * 
			 * # Turn blink off self.setBlinkRate(self.__HT16K33_BLINKRATE_OFF)
			 * 
			 * # Set maximum brightness self.setBrightness(15)
			 * 
			 * # Clear the screen self.clear()
			 */

			log.info("--------SevenSegment end-------------");
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public I2CDevice getDevice(int busAddress, int deviceAddress)
	{
		try {
		String key = String.format("%d.%d", busAddress, deviceAddress);
		if (!devices.containsKey(key)){
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
		} catch(Exception e) {
			Logging.logException(e);
		}
		
		return null;
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

	public int displayDigit(int busAddress, int deviceAddress, int i) {
		try {
			I2CDevice device = getDevice(busAddress, deviceAddress);

			switch (i) {

			case 0:
				log.info("------------0 --------------------");
				device.write(0x00, new byte[] { 63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				break;

			case 1:
				log.info("------------1 --------------------");
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------1 --------------------");
				break;

			case 2:
				log.info("------------2 --------------------");
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------2 --------------------");
				break;

			case 3:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------3 --------------------");
				break;

			case 4:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------4 --------------------");
				break;

			case 5:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------5 --------------------");
				break;

			case 6:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 125, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------6 --------------------");
				break;

			case 7:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 125, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 125, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 125, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 7, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------7 --------------------");
				break;

			case 8:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 7, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 7, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 7, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 127, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------8 --------------------");
				break;

			case 9:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 127, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 127, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 127, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 111, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------9 --------------------");
				break;

			case 10:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 111, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 63, 0, 111, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 111, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------10 --------------------");
				break;

			case 11:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 63, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------11 --------------------");
				break;

			case 12:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 6, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------12 --------------------");
				break;

			case 13:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 91, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------13 --------------------");
				break;

			case 14:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 79, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------14 --------------------");
				break;

			case 15:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 102, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------15 --------------------");
				break;

			case 16:
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 109, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				device.write(0x00, new byte[] { 63, 0, 63, 0, 0, 0, 6, 0, 125, 0, 0, 0, 0, 0, 0, 0 }, 0, 16);
				log.info("------------16 --------------------");
				break;

			}
			;

		} catch (Exception e) {
			Logging.logException(e);
		}

		return i;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		int i = 0;

		Runtime.createAndStart(String.format("ras%d", i), "Runtime");
		Runtime.createAndStart(String.format("rasPi%d", i), "RasPi");
		Runtime.createAndStart(String.format("rasGUI%d", i), "GUIService");
		Runtime.createAndStart(String.format("rasPython%d", i), "Python");
		// Runtime.createAndStart(String.format("rasClock%d",i), "Clock");
		Runtime.createAndStart(String.format("rasRemote%d", i), "RemoteAdapter");
	}

}
