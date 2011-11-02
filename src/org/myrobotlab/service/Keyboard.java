package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Keyboard extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Keyboard.class.getCanonicalName());

	public Keyboard(String n) {
		super(n, Keyboard.class.getCanonicalName());
	}
		
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	public String keyCommand (String cmd)
	{
		return cmd;
	}
	
	
	@Override
	public String getToolTip() {
		return "keyboard";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Keyboard template = new Keyboard("keyboard");
		template.startService();

		Logging log = new Logging("log");
		log.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

	

}
