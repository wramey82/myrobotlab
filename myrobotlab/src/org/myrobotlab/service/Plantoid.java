package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

public class Plantoid extends Service {

	// tracking ?

	private static final long serialVersionUID = 1L;

	// peer services
	transient public Servo d3, d4, d5, d6;
	transient public Arduino arduino;
	transient public OpenCV IRCamera, camera;
	transient public Keyboard keyboard;

	// system specific data
	@Element
	public String port = "/dev/ttyACM0";
	public int d3Pin = 3;
	@Element
	public int d4Pin = 4;
	@Element
	public int d5Pin = 5;
	@Element
	public int d6Pin = 6;
	
	public final static Logger log = LoggerFactory.getLogger(Plantoid.class.getCanonicalName());

	public Plantoid(String n) {
		super(n, Plantoid.class.getCanonicalName());

		reserve("arduino", "Arduino", "Plantoid has one arduino controlling all the servos and sensors");
		reserve("d3", "Servo", "one of the driving servos");
		reserve("d4", "Servo", "one of the driving servos");
		reserve("d5", "Servo", "one of the driving servos");
		reserve("d6", "Servo", "one of the driving servos");

		reserve("keyboard", "Keyboard", "for keyboard control");

		reserve("webgui", "WebGUI", "plantoid gui");

		// reserve("pilotcam","OpenCV", "One of the cameras");
		// reserve("ircamera","OpenCV", "One of the cameras");

	}

	@Override
	public String getToolTip() {
		return "the plantoid service";
	}

	@Override
	public void startService() {
		super.startService();

		arduino = (Arduino) startReserved("arduino");
		arduino.connect(port);

		d3 = (Servo) startReserved("d3");
		d4 = (Servo) startReserved("d4");
		d5 = (Servo) startReserved("d5");
		d6 = (Servo) startReserved("d6");
		
		attach();
		
		// start sensor data
	}

	public boolean connect(String port) {
		this.port = port;
		arduino = (Arduino) startReserved("arduino");
		arduino.connect(port);
		arduino.broadcastState();
		return true;
	}
	
	public void spin(Integer power)
	{
		int s = 90 - power;
		d3.moveTo(s);
		d4.moveTo(s);
		d5.moveTo(s);
		d6.moveTo(s);
	}
	
	public void stop()
	{
		d3.moveTo(90);
		d4.moveTo(90);
		d5.moveTo(90);
		d6.moveTo(90);
	}
	
	public void detach()
	{
		d3.detach();
		d4.detach();
		d5.detach();
		d6.detach();
	}

	public void attach()
	{
		arduino.servoAttach(d3.getName(), d3Pin);
		arduino.servoAttach(d4.getName(), d4Pin);
		arduino.servoAttach(d5.getName(), d5Pin);
		arduino.servoAttach(d6.getName(), d6Pin);
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Plantoid plantoid = (Plantoid) Runtime.create("plantoid", "Plantoid");
		plantoid.connect("COM9");
		plantoid.startService();
		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
