package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class ChumbyBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(ChumbyBot.class.getCanonicalName());

	OpenCV camera = new OpenCV("camera");
	Servo servo = new Servo("pan");
	Arduino arduino = new Arduino("uBotuino");
	SensorMonitor sensors = new SensorMonitor("sensors");
	RemoteAdapter remote = new RemoteAdapter("remote");
	Speech speech = new Speech("speech");

	transient Thread behavior = null;
	
	public abstract class Behavior implements Runnable
	{

		
	}
	
	public void startBot ()
	{
		behavior = new Thread(new ChumbyBot.Explore(),"behavior");
		behavior.start();
	}
	
	public class Explore extends Behavior
	{
		@Override
		public void run() { // execute non Runnable
			// start forward - some speed - DifferentialDrive?
			// wait on IR Event
			// stop
			// say something relevant e.g. "wall @ 25 cm"
			// check left
			// check right
			// determine correction
			// say something relevant e.g. "clear right"
			// turn appropriate direction 
			// continue loop/explore
			
			try {
			
			speech.startService();
			//speech.cfg.set("isATT", true);
			speech.speak("I am about to start");
			remote.startService();
			camera.startService();
			arduino.startService();
			sensors.startService();
			servo.startService();
			
			servo.attach(arduino.name, 12);

			Thread.sleep(10000);
			
			while (true)
			{
					Thread.sleep(1000);
					
					
					servo.moveTo(10);
					Thread.sleep(4000);		
					servo.moveTo(90);
					Thread.sleep(4000);
					servo.moveTo(170);
					Thread.sleep(4000);
					servo.moveTo(90);
					
				
			}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	public synchronized void onEvent (Object data)
	{
		
	}
	
	public ChumbyBot(String n) {
		super(n, ChumbyBot.class.getCanonicalName());
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
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		
		ChumbyBot chumbybot = new ChumbyBot("chumbybot");
		chumbybot.startService();
		chumbybot.startBot();
				
	}


}
