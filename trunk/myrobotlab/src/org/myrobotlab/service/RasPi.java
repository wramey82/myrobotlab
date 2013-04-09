package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;

public class RasPi extends Service  {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RasPi.class.getCanonicalName());
	//GpioController gpio = GpioFactory.getInstance();
	
	public RasPi(String n) {
		super(n, RasPi.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Runtime.createAndStart("raspiRun", "Runtime");
		Runtime.createAndStart("raspi", "RasPi");
		Runtime.createAndStart("rasGUI", "GUIService");
		Runtime.createAndStart("remote", "RemoteAdapter");
		

	}


}
