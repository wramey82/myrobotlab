package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class IPCamera extends Service {

	private static final long serialVersionUID = 1L;
	
	public URL url = null;

	public final static Logger LOG = Logger.getLogger(IPCamera.class.getCanonicalName());

	public IPCamera(String n) {
		super(n, IPCamera.class.getCanonicalName());
	}
	
	public boolean connect (String cameraURL)
	{
		try {
			url = new URL(cameraURL);
	        URLConnection yc = url.openConnection();
	        BufferedReader in = new BufferedReader(
	                                new InputStreamReader(
	                                yc.getInputStream()));
	        String inputLine;

	        while ((inputLine = in.readLine()) != null) 
	            System.out.println(inputLine);
	        in.close();
		} catch (Exception e) {
			logException(e);
			return false;
		}
		return true;
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		IPCamera foscam = new IPCamera("foscam");
		
		foscam.startService();
		foscam.connect("http://admin:zardoz7@192.168.0.59/get_status.cgi");
				
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
