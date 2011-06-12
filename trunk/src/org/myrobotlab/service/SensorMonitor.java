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

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.PinData;

public class SensorMonitor extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(SensorMonitor.class
			.getCanonicalName());

	public HashMap<String, Alert> alerts = new HashMap<String, Alert>();

	public SensorMonitor(String n) {
		super(n, SensorMonitor.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public final class Alert {
		public static final int BOUNDRY = 1;

		public static final int STATE_LOW = 2;
		public static final int STATE_HIGH = 3;

		public Alert(String n, int min, int max, int type, int state,
				int targetPin) {
			this.name = n;
			this.min = min;
			this.max = max;
			this.type = type;
			this.state = state;
			this.targetPin = targetPin;
		}

		public String name;
		public int min;
		public int max;
		public int type;
		public int state;
		public PinData pinData;
		public int targetPin;
	}

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		SensorMonitor sm = new SensorMonitor("sensors");
		Arduino arduino = new Arduino("arduino");
		GUIService gui = new GUIService("gui");
		arduino.startService();
		gui.startService();
		sm.startService();
		gui.display();
	}

	public final void addAlert(String n, int min, int max, int type, int state,
			int targetPin) {
		alerts.put(n, new Alert(n, min, max, type, state, targetPin));
	}

	// TODO - an Attach for the ArduinoGUI - so not "automatic" pinRead
	// read pinData notify -> checkSensorData
	// add new Alert -> when pin 4 > 35

	// sensorInput - an input point for sensor info

	public void sensorInput(PinData pinData) {
		// spin through alerts
		// if map(pin).hasKey()
		// for each rule check

		Iterator<String> it = alerts.keySet().iterator();
		while (it.hasNext()) {
			Boolean publish = false;
			String serviceName = it.next();
			Alert alert = alerts.get(serviceName);
			// LOG.warn(alert.min + " " + alert.max + " " + alert.state + " " +
			// pinData.value + " " + pinData.pin + " " + alert.targetPin);
			// transition across boundary -- begin
			if (pinData.pin == alert.targetPin && pinData.value < alert.min
					&& alert.state == Alert.STATE_HIGH) {
				publish = true;
				alert.state = Alert.STATE_LOW;
			}

			if (pinData.value > alert.max && alert.state == Alert.STATE_LOW) {
				publish = true;
				alert.state = Alert.STATE_HIGH;
			}
			// transition across boundary -- end

			if (publish) {
				invoke("publish", alert);
			}

		}
		invoke("publishSensorData", pinData);

	}

	public Alert publish(Alert alert) {
		return alert;
	}

	// output
	public PinData publishSensorData(PinData pinData) {
		// TODO - wrap with more info if possible
		return pinData;
	}

	@Override
	public String getToolTip() {
		return "<html>sensor monitor - capable of displaying sensor information in a crude oscilliscope fasion</html>";
	}
	
}
