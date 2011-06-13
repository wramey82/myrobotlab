package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class ChumbyBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(ChumbyBot.class.getCanonicalName());

	public abstract class Behavior implements Runnable
	{

		
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
		
		/*
		ChumbyBot chumbybot = new ChumbyBot("chumbybot");
		chumbybot.startService();
		*/

		/*
		OpenCV camera = new OpenCV("camera");
		camera.startService();
		camera.capture();
		*/
		
		Arduino arduino = new Arduino("uBotuino");
		arduino.startService();
		
		
		RemoteAdapter remote = new RemoteAdapter("remote");
		remote.startService();
		
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
