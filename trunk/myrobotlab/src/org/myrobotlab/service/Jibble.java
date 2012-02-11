package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Jibble extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Jibble.class.getCanonicalName());

	public Jibble(String n) {
		super(n, Jibble.class.getCanonicalName());
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
		
		Jibble template = new Jibble("template");
		template.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
