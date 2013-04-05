package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;

public class RaspPi extends Service  {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RaspPi.class.getCanonicalName());
	GpioController gpio = GpioFactory.getInstance();
	
	public RaspPi(String n) {
		super(n, RaspPi.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		RaspPi raspi = new RaspPi("raspi");
		raspi.startService();

	}


}
