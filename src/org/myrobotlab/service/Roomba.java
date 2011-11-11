package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Roomba extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Roomba.class.getCanonicalName());

	public Roomba(String n) {
		super(n, Roomba.class.getCanonicalName());
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
		
		Roomba template = new Roomba("template");
		template.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
