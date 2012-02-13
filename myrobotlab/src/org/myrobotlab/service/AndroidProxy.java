package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class AndroidProxy extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(AndroidProxy.class.getCanonicalName());

	public AndroidProxy(String n) {
		super(n, AndroidProxy.class.getCanonicalName());
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
		
		AndroidProxy template = new AndroidProxy("proxy");
		template.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
