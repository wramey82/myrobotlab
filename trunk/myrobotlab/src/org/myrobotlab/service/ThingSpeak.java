package org.myrobotlab.service;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.data.Pin;

/**
 * @author GroG & 
 * 
 *         References : http://community.thingspeak.com/documentation/api/
 * 
 */
public class ThingSpeak extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(ThingSpeak.class.getCanonicalName());
	// http://api.thingspeak.com/update?key=AO4DMKQZY4RLWNNU&field1=pin&field2=A0&field3=value&field4=345&status=boink6

	String updateURL = "http://api.thingspeak.com/update";
	String writeKey = "AO4DMKQZY4RLWNNU";
	String readKey = "";


	public ThingSpeak(String n) {
		super(n, ThingSpeak.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public Integer update(Object[] data) {
		String result = "0";
		try {
			for (int i = 0; i < data.length; ++i)
			{
				Object o = data[i];
				String url = String.format("%s?key=%s&field1=%s", updateURL, writeKey, o.toString());
				HTTPRequest request = new HTTPRequest(url);
				result = request.getString();
				log.info(String.format("ThingSpeak returned %s",result));
			}
		} catch (IOException e) {
			Service.logException(e);
		}

		return Integer.parseInt(result);
	}
	
	public Integer update (Integer data)
	{
		return update (new Object[]{data});
	}
	
	public Integer update (String data)
	{
		return update (new Object[]{data});
	}
	
	public Integer update(Pin pin)
	{
		return update(new Object[]{pin.value});
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		//Logger.getRootLogger().setLevel(Level.WARN);

		ThingSpeak thingSpeak = new ThingSpeak("thingSpeak");
		thingSpeak.update(33);
		thingSpeak.startService();
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
