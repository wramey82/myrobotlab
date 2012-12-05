package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Mapper extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Mapper.class.getCanonicalName());

	public Mapper(String n) {
		super(n, Mapper.class.getCanonicalName());
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
		
		Mapper mapper = new Mapper("mapper");
		mapper.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
