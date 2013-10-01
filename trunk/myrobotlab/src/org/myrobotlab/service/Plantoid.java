package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Plantoid extends Service {

	// tracking ? pan tilt ?

	private static final long serialVersionUID = 1L;

	// peer services
	transient public Servo d3, d4, d5, d2, pan, tilt;
	transient public Arduino arduino;
	transient public OpenCV IRCamera, camera;
	transient public Keyboard keyboard;
	transient public WebGUI webgui;
	transient public JFugue jfugue;
	transient public Speech speech;
	transient public AudioFile audioFile;
		
	// system specific data
	public String port = "/dev/ttyACM0";
	public int d2Pin = 2;
	public int d3Pin = 3;
	public int d4Pin = 4;
	public int d5Pin = 5;
	
	public int panPin = 6;
	public int tiltPin = 7;
	
	// analog read pins
	public int soildMoisture = 0;
	public int tempHumidity = 2;
	public int leftLight = 4;
	public int rightLight = 6;
	public int airQuality = 10;

	private int sampleRate = 8000;

	public final static Logger log = LoggerFactory.getLogger(Plantoid.class.getCanonicalName());

	public Plantoid(String n) {
		super(n, Plantoid.class.getCanonicalName());

		reserve("arduino", "Arduino", "Plantoid has one arduino controlling all the servos and sensors");
		reserve("d3", "Servo", "one of the driving servos");
		reserve("d4", "Servo", "one of the driving servos");
		reserve("d5", "Servo", "one of the driving servos");
		reserve("d2", "Servo", "one of the driving servos");

		reserve("pan", "Servo", "pan servo");
		reserve("tilt", "Servo", "tilt servo");
		
		
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

		try {
			webgui = (WebGUI) startReserved("webgui");
			arduino = (Arduino) startReserved("arduino");
			arduino.connect(port);

			d3 = (Servo) startReserved("d3");
			d4 = (Servo) startReserved("d4");
			d5 = (Servo) startReserved("d5");
			d2 = (Servo) startReserved("d2");

			pan = (Servo) startReserved("pan");
			tilt = (Servo) startReserved("tilt");

			arduino.setSampleRate(sampleRate);
			arduino.analogReadPollingStart(soildMoisture);
			arduino.analogReadPollingStart(tempHumidity);
			arduino.analogReadPollingStart(leftLight);
			arduino.analogReadPollingStart(rightLight);
			arduino.analogReadPollingStart(airQuality);

			attachServos();
			
			stop();
			
		} catch (Exception e) {
			error(e);
		}
	}

	public boolean connect(String port) {
		this.port = port;
		arduino = (Arduino) startReserved("arduino");
		arduino.connect(port);
		arduino.broadcastState();
		return true;
	}

	// 2 and 4 are x, 3 and 5 are Y

	// ------- servos begin -----------
	public void spin(Integer power) {
		int s = 90 - power;
		d3.moveTo(s);
		d4.moveTo(s);
		d5.moveTo(s);
		d2.moveTo(s);
	}
	
	public void moveY(Integer power){
		int s = 90 - power;
		d3.moveTo(90);
		d4.moveTo(s);
		d5.moveTo(90);
		d2.moveTo(s);
	}

	public void moveX(Integer power){
		int s = 90 - power;
		d2.moveTo(s);
		d3.moveTo(90);
		d4.moveTo(s);
		d5.moveTo(90);
	}
	
	public void squareDance(Integer power, Integer time)
	{
		int s = 90 - power;
		moveX(s);
		sleep(time);
		moveY(s);
		sleep(time);
		moveX(-s);
		sleep(time);
		moveX(-s);
		sleep(time);
		stop();
	}
	
	public void stop() {
		d2.moveTo(90);
		d3.moveTo(90);
		d4.moveTo(90);
		d5.moveTo(90);
	}

	public void attachServos() {
		arduino.servoAttach(d2.getName(), d2Pin);
		arduino.servoAttach(d3.getName(), d3Pin);
//		arduino.servoAttach(d4.getName(), d4Pin);
		arduino.servoAttach(d5.getName(), d5Pin);

		arduino.servoAttach(pan.getName(), panPin);
		arduino.servoAttach(tilt.getName(), tiltPin);
}

	public void detachServos() {
		d2.detach();
		d3.detach();
//		d4.detach();
		d5.detach();
		
		pan.detach();
		tilt.detach();
	}
	// ------- servos begin -----------
	
	public void shutdown() {
		detachServos();
		Runtime.releaseAll();
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
