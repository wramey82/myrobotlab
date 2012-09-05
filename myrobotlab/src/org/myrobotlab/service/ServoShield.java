package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class ServoShield extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(ServoShield.class.getCanonicalName());

	public ServoShield(String n) {
		super(n, ServoShield.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public boolean setType(String type)
	{
		return false;
		
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		ServoShield template = new ServoShield("template");
		template.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
