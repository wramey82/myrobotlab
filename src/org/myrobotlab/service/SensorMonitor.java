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
	public final static Logger LOG = Logger.getLogger(SensorMonitor.class.getCanonicalName());

	public HashMap<String, PinAlert> alerts = new HashMap<String, PinAlert>();
	public HashMap<String, PinAlert> alerts_nameIndex = new HashMap<String, PinAlert>();
	public HashMap<String, PinData> lastValue = new HashMap<String, PinData>();

	public Speech speech = null;
	
	public SensorMonitor(String n) {
		super(n, SensorMonitor.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}


	public final void addAlert(PinAlert alert) {
		if (alert.pinData.source == null)
		{
			LOG.error("addAlert adding alert with no source controller - will be based on pin only ! " + alert.pinData.pin);
		}
		alerts.put(makeKey(alert.pinData), alert);
		alerts_nameIndex.put(alert.name, alert);
	}
	
	
	public final void addAlert(String source, String name, int min, int max, int type, int state,
			int targetPin) {
		PinAlert pa = new PinAlert(name, min, max, type, state, targetPin);
		alerts.put(makeKey(source, targetPin), pa);
		alerts_nameIndex.put(name, pa);
	}

	// TODO - an Attach for the ArduinoGUI - so not "automatic" pinRead
	// read pinData notify -> checkSensorData
	// add new PinAlert -> when pin 4 > 35

	final static public String makeKey(PinData pinData)
	{
		return makeKey(pinData.source, pinData.pin);
	}
	
	
	final static public String makeKey(String source, Integer pin)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(source);
		sb.append("_");
		sb.append(pin);
		return sb.toString();
	}
	
	// sensorInput - an input point for sensor info

	public void sensorInput(PinData pinData) {
		// spin through alerts
		// if map(pin).hasKey()
		// for each rule check

		String key = makeKey(pinData);
		
		if (alerts.containsKey(key))
		{
			PinAlert alert = alerts.get(key);
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
				invoke("publishPinAlertText", alert);				
				alerts.remove(key);
			}
		}

		if (!lastValue.containsKey(key))
		{
			lastValue.put(key, pinData);
		}
		
		PinData last = lastValue.get(key);
		last.value = pinData.value;
		//last.pin = pinData.pin;
		//last.function = pinData.function;
		//last.source = pinData.source;
		
		invoke("publishSensorData", pinData);

	}

	public int getLastValue(String source, Integer pin)
	{
		String key = makeKey(source, pin);
		if (lastValue.containsKey(key))
		{
			return lastValue.get(key).value;
		}
		LOG.error("getLastValue for pin " + key + " does not exist");
		return -1;
	}
	
	public void removeAlert(String name)
	{
		if (alerts_nameIndex.containsKey(name))
		{
			alerts.remove(name);
			alerts_nameIndex.remove(name);
		} else {
			LOG.error("remoteAlert " + name + " not found");
		}
		
	}
	
	public PinAlert publishPinAlert(PinAlert alert) {
		return alert;
	}
	
	public String publishPinAlertText(PinAlert alert) {		
		return alert.name;
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

	/*
	 * publishing point to add trace data to listeners (like the gui)
	 */
	public PinData addTraceData (PinData pinData)
	{
		return pinData;
	}
	
	public static void main(String[] args) throws InterruptedException {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		SensorMonitor sm = new SensorMonitor("sensors");
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		//Arduino arduino1 = new Arduino("arduino1");
		//RemoteAdapter remote = new RemoteAdapter("remote");
		//Speech speech = new Speech("speech");
		//sm.speech = speech;
		//arduino.startService();
		//arduino1.startService();
		//remote.startService();
		//speech.startService();
		sm.startService();

		Servo neck = new Servo("neck");
		neck.startService();
		neck.attach("arduino", 4);

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		neck.moveTo(179);
		neck.moveTo(0);
		neck.moveTo(160);
		neck.moveTo(90);
		neck.moveTo(0);
		
		for (int j = 0; j < 30; ++j)
		{
			int i;
			for (i = 0; i < 160; i+=10)
			{
				neck.moveTo(i);
				Thread.sleep(300);
			}
			for (i = 160; i > 0; i-=10)
			{
				neck.moveTo(i);
				Thread.sleep(300);
			}
		}

		
	}	
}
