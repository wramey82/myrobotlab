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
	transient public final GpioController gpio = GpioFactory.getInstance();
	
	public RasPi(String n) {
		super(n, RasPi.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.ERROR);

		int i = 0;
		
		Runtime.createAndStart(String.format("ras%d", i), "Runtime");
		Runtime.createAndStart(String.format("rasPi%d", i), "RasPi");
		Runtime.createAndStart(String.format("rasGUI%d",i), "GUIService");
		Runtime.createAndStart(String.format("rasPython%d",i), "Python");
		//Runtime.createAndStart(String.format("rasClock%d",i), "Clock");
		Runtime.createAndStart(String.format("rasRemote%d", i), "RemoteAdapter");
	}


}
