package org.myrobotlab.service;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;

public class MagaBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(MagaBot.class.getCanonicalName());

	public MagaBot(String n) {
		super(n, MagaBot.class.getCanonicalName());
	}

	SerialDevice serialDevice = null;
	private boolean isInitialized = false;

	public void init(String serialPortName) {
		if (!isInitialized) {
			try {
				serialDevice = SerialDeviceFactory.getSerialDevice(serialPortName, 9600, 8, 1, 0);
				serialDevice.open();
				isInitialized = true;
			} catch (SerialDeviceException e) {
				logException(e);
			}
		}

	}

	/*
	 * xicombd: - '1' to Assisted Navigation - 'w' to go forward - 's' to go
	 * backward - 'a' to go left - 'd' to go right - 'p' to stop - '2' to
	 * Obstacle Avoidance - '3' to start Line Following
	 * 
	 * 'i' if the ir sensors are activated
	 */
	public void sendOrder(String o) {
		try {
			serialDevice.write(o);
		} catch (IOException e) {
			logException(e);
		}
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		MagaBot template = new MagaBot("template");
		template.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

}
