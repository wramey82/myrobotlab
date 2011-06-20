package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.PinAlert;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.SensorData;

public class ChumbyBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(ChumbyBot.class.getCanonicalName());

	OpenCV camera = new OpenCV("camera");
	Servo servo = new Servo("pan");
	Arduino arduino = new Arduino("uBotino");
	SensorMonitor sensors = new SensorMonitor("sensors");
	RemoteAdapter remote = new RemoteAdapter("remote");
	Speech speech = new Speech("speech");
	Motor left = new Motor("left");
	Motor right = new Motor("right");
	
	transient Thread behavior = null;
	
	private final Object lock = new Object(); 

	
	public abstract class Behavior implements Runnable
	{

		
	}
	
	public void startBot ()
	{		
		speech.startService();
		//speech.cfg.set("isATT", true);
		//speech.speak("I am about to start");
		remote.startService();
		camera.startService();
		arduino.startService();
		sensors.startService();
		servo.startService();

		arduino.setSerialPort("/dev/ttyUSB0");
		
		// arduino to sensor monitor
		arduino.notify(SensorData.publishPin, sensors.name, "sensorInput", PinData.class);
		
		// sensor monitor to chumbybot
		sensors.notify("publishPinAlert", this.name, "publishPinAlert", PinAlert.class);
		
		arduino.analogReadPollingStart(0);

		behavior = new Thread(new ChumbyBot.Explore(),"behavior");
		behavior.start();
	}
	
	public class Explore extends Behavior
	{
		@Override
		public void run() { 
			try {
			
			// execute non Runnable
			// start forward - some speed - DifferentialDrive?
			// no encoder - ? start timer ?
			right.move(0.5f);
			left.move(0.5f);

			PinAlert alert = new PinAlert();
			alert.threshold = 600;
			alert.pinData.pin = 0;
			sensors.addAlert(alert);
			
			// wait on IR Event
			synchronized (lock) {
				lock.wait(); 
			}

			// stop
			right.stop();
			left.stop();
						
			// say something relevant e.g. "wall @ 25 cm"
			speech.speak("Excuse me. I believe something is in my way");

			// check left
			servo.moveTo(20);
			Thread.sleep(300);
			// wait for servo to stop
			// check IR - 
			int leftRange = sensors.getLastValue(0);
			
			// check right
			servo.moveTo(160);
			Thread.sleep(300);
			
			int rightRange = sensors.getLastValue(0);

			// if both values are under - must backup or rotate base right
			
			if (rightRange > leftRange)
			{
				speech.speak("moving right");
				left.move(0.3f);
				Thread.sleep(400);
				
			} else {
				speech.speak("moving left");
				right.move(0.3f);
				Thread.sleep(400);				
			}

			servo.moveTo(90);
			speech.speak("checking forward range");
			Thread.sleep(300);				
			int forward = sensors.getLastValue(0);
			if (forward < 100)
			{
				speech.speak("forward");
			} else {
				speech.speak("not safe to go forward");
			}
			
			
			while (true)
			{
				/*
					Thread.sleep(1000);
					servo.moveTo(10);
					Thread.sleep(4000);		
					servo.moveTo(90);
					Thread.sleep(4000);
					servo.moveTo(170);
					Thread.sleep(4000);
					servo.moveTo(90);
					*/
				Thread.sleep(1000);
					
				
			}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	public void publishPinAlert (PinAlert alert)
	{
		synchronized (lock) {
			lock.notifyAll(); 
		}
		
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
