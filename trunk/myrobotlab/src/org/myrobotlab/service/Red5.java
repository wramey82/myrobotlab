package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Red5 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Red5.class.getCanonicalName());

	public Red5(String n) {
		super(n, Red5.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "Red5 video/audio streaming service";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Red5 template = new Red5("template");
		template.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
