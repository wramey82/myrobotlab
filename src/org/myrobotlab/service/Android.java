package org.myrobotlab.service;

import android.hardware.Sensor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
public class Android extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Android.class.getCanonicalName());

	public Android(String n) {
		super(n, Android.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {	
	}
	
	@Override
	public String getToolTip() {
		return "used as a general android";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Android android = new Android("android");
		android.startService();
		
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
		
	}


}
