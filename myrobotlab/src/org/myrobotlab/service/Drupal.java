package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Drupal extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Drupal.class.getCanonicalName());

	public Drupal(String n) {
		super(n, Drupal.class.getCanonicalName());
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
		
		Drupal template = new Drupal("template");
		template.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
