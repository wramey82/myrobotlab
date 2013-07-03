package org.myrobotlab.service;

import it.sauronsoftware.cron4j.Scheduler;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Cron extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Cron.class.getCanonicalName());
	private Scheduler scheduler = new Scheduler();
	
	public final static String EVERY_MINUTE = "* * * * *";
	
	public Cron(String n) {
		super(n, Cron.class.getCanonicalName());	
		scheduler.start();
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public void addScheduledEvent(String cron, String serviceName, String method)
	{
		addScheduledEvent(cron, serviceName, method, (Object[])null);
	}
	
	public void addScheduledEvent(String cron, String serviceName, String method, Object ... data)
	{
		final Message msg = createMessage(serviceName, method, data);
		
		scheduler.schedule(cron, new Runnable() {
			public void run() {
				out(msg);
			}
		});
	}
	
	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Cron cron = new Cron("cron");
		cron.startService();	
		
		Log log = new Log("log");
		log.startService();
		
		cron.addScheduledEvent("0 6 * * 1,3,5","arduino","digitalWrite", 13, 1);
		cron.addScheduledEvent("0 7 * * 1,3,5","arduino","digitalWrite", 12, 1);
		cron.addScheduledEvent("0 8 * * 1,3,5","arduino","digitalWrite", 11, 1);

		cron.addScheduledEvent("59 * * * 1,3,5","arduino","digitalWrite", 13, 0);
		cron.addScheduledEvent("59 * * * 1,3,5","arduino","digitalWrite", 12, 0);
		cron.addScheduledEvent("59 * * * 1,3,5","arduino","digitalWrite", 11, 0);

		//cron.addScheduledEvent(EVERY_MINUTE, "log", "log");
		// west wall | back | east wall
		
		// 1. doug - find location where checked in ----
		// 2. take out security token from DL broker's response
		// 3. Tony - status ? and generated xml responses - "update" looks ok
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
