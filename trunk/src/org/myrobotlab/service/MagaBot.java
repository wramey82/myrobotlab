package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class MagaBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(MagaBot.class.getCanonicalName());

	public MagaBot(String n) {
		super(n, MagaBot.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	Arduino arduino = new Arduino("arduino");
	private boolean isInitialized = false;
	
	public void init(String serialPortName)
	{
		if (!isInitialized)
		{
			arduino.startService();
			arduino.setSerialPort(serialPortName);
			arduino.setSerialPortParams(9600);
			isInitialized = true;
		}
		
	}
	/*
	 * xicombd: - '1' to Assisted Navigation 
- 'w' to go forward 
- 's' to go backward
- 'a' to go left
- 'd' to go right
- 'p' to stop
- '2' to Obstacle Avoidance
- '3' to start Line Following
  
'i' if the ir sensors are activated
	 * 
	 */
	public void sendOrder(String o)
	{
		arduino.serialSend(o);
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		MagaBot template = new MagaBot("template");
		template.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
