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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.PinAlert;
import org.myrobotlab.service.data.PinData;

public class SensorMonitor extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(SensorMonitor.class
			.getCanonicalName());

	public HashMap<Integer, PinAlert> alerts = new HashMap<Integer, PinAlert>();

	public SensorMonitor(String n) {
		super(n, SensorMonitor.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		SensorMonitor sm = new SensorMonitor("sensors");
		Arduino arduino = new Arduino("arduino");
		GUIService gui = new GUIService("gui");
		arduino.startService();
		gui.startService();
		sm.startService();
		gui.display();
	}

	public final void addAlert(PinAlert alert) {
		alerts.put(alert.pinData.pin, alert);
	}
	
	public final void addAlert(String n, int min, int max, int type, int state,
			int targetPin) {
		alerts.put(targetPin, new PinAlert(n, min, max, type, state, targetPin));
	}

	// TODO - an Attach for the ArduinoGUI - so not "automatic" pinRead
	// read pinData notify -> checkSensorData
	// add new PinAlert -> when pin 4 > 35

	// sensorInput - an input point for sensor info

	public void sensorInput(PinData pinData) {
		// spin through alerts
		// if map(pin).hasKey()
		// for each rule check

		if (alerts.containsKey(pinData.pin))
		{
			PinAlert alert = alerts.get(pinData.pin);
			/*
			// transition across boundary -- begin
			if (pinData.pin == alert.targetPin && pinData.value < alert.min
					&& alert.state == PinAlert.STATE_HIGH) {
				publish = true;
				alert.state = PinAlert.STATE_LOW;
			}

			if (pinData.value > alert.max && alert.state == PinAlert.STATE_LOW) {
				publish = true;
				alert.state = PinAlert.STATE_HIGH;
			}
			// transition across boundary -- end
			 * 
			 */
			// time
			if (alert.threshold < pinData.value)
			{
				alert.pinData = pinData;
				invoke("publishPinAlert", alert);				
				alerts.remove(pinData.pin);
			}
		}

		invoke("publishSensorData", pinData);

	}

	public void removeAlert(String name)
	{
		if (alerts.containsKey(name))
		{
			alerts.remove(name);
		} else {
			LOG.error("remoteAlert " + name + " not found");
		}
		
	}
	
	public PinAlert publishPinAlert(PinAlert alert) {
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
