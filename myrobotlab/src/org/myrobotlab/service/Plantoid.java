package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;


public class Plantoid extends Service {

	private static final long serialVersionUID = 1L;
	
	transient public Servo leftX, rightX, leftY, rightY;
	transient public Arduino arduino;
	transient public OpenCV IRCamera, camera;
	
	//:192.168.0.25 
	
	public String leftXName = "leftX";
	public String rightXName = "rightX";
	public String leftYName = "leftY";
	public String rightYName = "rightY";
	public String arduinoName = "arduino";
	public String IRCameraName = "IRCamera";
	public String cameraName = "camera";
	
	@Element
	private String port = null; // not normalized :(

	public final static Logger log = LoggerFactory.getLogger(Plantoid.class.getCanonicalName());
	
	public Plantoid(String n) {
		super(n, Plantoid.class.getCanonicalName());	
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override 
	public void startService()
	{
		super.startService();
		
		boolean startup = true;
		
		startup &= attach(arduino, port);
		/*
		startup &= attach(IRCamera);
		startup &= attachServos(x, xPin, y, yPin);
		startup &= attachPIDs(xpid, ypid);
		*/
		
		if (startup) {
			info("tracking ready");
		} else {
			error("tracking could not initialize properly");
		}
	}
	
	public boolean attach(Arduino duino, String inSerialPort)
	{
		if (arduino != null)
		{
			log.info("arduino already attached");
			return true;
		}
		port = inSerialPort;
		
		info("attaching Arduino");
		if (duino!= null)
		{
			arduinoName = duino.getName();
		}
		arduino = (Arduino) Runtime.createAndStart(arduinoName, "Arduino", duino);
		
		if (!arduino.isConnected())
		{
			if (port == null)
			{
				error("no serial port specified for Arduino");
				return false;
			}
			arduino.setSerialDevice(port);
		}
		
		Service.sleep(500);
		
		if (!arduino.isConnected())
		{
			error("Arduino is not connected!");
			return false;
		}
		
		arduino.broadcastState();
		return true;
	} 
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Plantoid plantoid = (Plantoid)Runtime.create("plantoid", "Plantoid");
		plantoid.setPort("COM9");
		plantoid.startService();
		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("webgui", "WebGUI");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
