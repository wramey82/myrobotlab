package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import topcodes.Scanner;
import topcodes.TopCode;


public class TopCodes extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(TopCodes.class.getCanonicalName());
	Scanner scanner;
	
	public TopCodes(String n) {
		super(n, TopCodes.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general topcodes";
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

	@Override
	public void startService()
	{
		super.releaseService();
		scanner = new Scanner();
	}
	
	List<TopCode> scan(BufferedImage img)
	{
		return scanner.scan(img);
	}

	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		TopCodes topcodes = new TopCodes("topcodes");
		topcodes.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
