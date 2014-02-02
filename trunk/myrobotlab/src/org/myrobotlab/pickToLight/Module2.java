package org.myrobotlab.pickToLight;

import java.util.HashMap;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import com.pi4j.io.i2c.I2CFactory;

public class Module2 {

	public final static Logger log = LoggerFactory.getLogger(Module2.class);

	// TODO abtract out Pi4J
	transient private com.pi4j.io.i2c.I2CBus i2cbus;
	transient private com.pi4j.io.i2c.I2CDevice device;

	protected Address2 address = new Address2();
	protected String type;
	protected String version; // hardware version
	protected String state;

	private String lastValue = "";
	static HashMap<String, Byte> translation = new HashMap<String, Byte>();

	static private boolean translationInitialized = false;
	CycleThread ct = null;

	public class BlinkThread extends Thread {
		public int number = 5;
		public int delay = 100;
		public String value = "";
		public boolean leaveOn = true;

		public void run() {
			int count = 0;
			while (count < 5) {
				display(value);
				Service.sleep(delay);
				display("   ");
				Service.sleep(delay);
				++count;
			}

			if (leaveOn) {
				display(value);
			}
		}
	}

	public static void initTranslation() {

		if (translationInitialized) {
			return;
		}
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
		translation.put("p", (byte) 115);
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

		translationInitialized = true;
	}

	public static byte translate(char c) {
		byte b = 0;
		String s = String.valueOf(c).toLowerCase();
		if (translation.containsKey(s)) {
			b = translation.get(s);
		}
		return b;
	}

	public byte[] writeDisplay(byte[] data) {
		if (device == null) {
			log.error("device is null");
			return data;
		}

		try {
			device.write(0x00, data, 0, 16);
		} catch (Exception e) {
			Logging.logException(e);
		}

		return data;
	}

	public String display(String str) {
		lastValue = str;
		if (translationInitialized) {
			initTranslation();
		}
		// d1 d2 : d3 d4
		byte[] display = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		if (str == null || str == "") {
			writeDisplay(display);
			return str;
		}

		if (str.length() < 4) {
			str = String.format("%4s", str);
		}

		display[0] = translate(str.charAt(0));
		display[2] = translate(str.charAt(1));
		display[6] = translate(str.charAt(2));
		display[8] = translate(str.charAt(3));

		writeDisplay(display);

		return str;
	}

	public int getI2CAddress() {
		return address.getI2CAddress();
	}

	public int getI2CBus() {
		return address.getI2CBus();
	}

	public void blinkOff(String msg) {
		log.info(String.format("blinkOff %s", msg));
		BlinkThread b = new BlinkThread();
		b.value = msg;
		b.leaveOn = false;
		b.start();
	}

	public void blinkOn(String value) {
		BlinkThread b = new BlinkThread();
		b.value = value;
		b.start();
	}

	public class CycleThread extends Thread {
		public boolean isRunning = false;
		int delay = 300;
		String msg;

		public CycleThread(String msg, int delay) {
			this.msg = "    " + msg + "    ";
			this.delay = delay;
		}

		public void run() {
			isRunning = true;
			try {
				while (isRunning) {
					// start with scroll on page
					for (int i = 0; i < msg.length() - 3; ++i) {
						display(msg.substring(i, i + 4));
						sleep(delay);
					}
				}
			} catch (InterruptedException e) {
				isRunning = false;
			}
		}
	}

	public void cycle(String msg, int delay) {
		if (ct != null) {
			cycleStop();
		}
		ct = new CycleThread(msg, delay);
		ct.start();
	}

	public void cycle(String msg) {
		if (ct != null) {
			cycleStop();
		}
		ct = new CycleThread(msg, 300);
		ct.start();
	}

	public void cycleStop() {
		if (ct != null) {
			ct.isRunning = false;
		}
	}

	public Module2(int bus, int i2cAddress) {
		try {

			address.setI2CBus(bus);
			address.setI2CAddress(i2cAddress);

			if (Platform.isArm()) {
				// create I2C communications bus instance
				i2cbus = I2CFactory.getInstance(bus);

				// create I2C device instance
				device = i2cbus.getDevice(i2cAddress);
			}

			if (translationInitialized) {
				initTranslation();
			}

		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	// FIXME - add PCFBLAHBLAH
}
