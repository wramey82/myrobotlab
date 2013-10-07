package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;

public class RasPi extends Service  {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RasPi.class.getCanonicalName());
	transient public final GpioController gpio = GpioFactory.getInstance();
	GpioPinDigitalOutput gpio01;
	GpioPinDigitalOutput gpio03;
	
	private boolean initialized = false;
	
	public RasPi(String n) {
		super(n, RasPi.class.getCanonicalName());	
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public void init()
	{
		if (initialized)
		{
			return;
		}
		gpio01 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
		gpio03 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);
		

		// provision gpio pin #02 as an input pin with its internal pull down resistor enabled
        //final GpioPinDigitalInput myButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
        
        /*
		GpioPinDigitalInput myButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02,             // PIN NUMBER
                "MyButton",                   // PIN FRIENDLY NAME (optional)
                PinPullResistance.PULL_DOWN); // PIN RESISTANCE (optional)
       */
		initialized = true;
	}
	
	public void blinkTest()
	{
		gpio01.blink(500, 15000);
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
