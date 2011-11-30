package org.myrobotlab.attic;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.ServiceFactory;

public class Chumby extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Chumby.class.getCanonicalName());

	public Chumby(String n) {
		super(n, Chumby.class.getCanonicalName());
	}
	
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used to generate pulses";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		ServiceFactory ServiceFactory = new ServiceFactory("services");
		ServiceFactory.startService();

		Chumby chumby = new Chumby("chumby");
		chumby.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
