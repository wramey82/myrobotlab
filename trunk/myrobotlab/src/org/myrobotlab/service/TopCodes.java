package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import topcodes.Scanner;
import topcodes.TopCode;


public class TopCodes extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(TopCodes.class.getCanonicalName());
	Scanner scanner = new Scanner();
	
	public TopCodes(String n) {
		super(n, TopCodes.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general topcodes";
	}

	
	List<TopCode> scan(BufferedImage img)
	{
		return scanner.scan(img);
	}

	List<TopCode> scan(String filename)
	{
		try {
			BufferedImage img;
			img = ImageIO.read(new File(filename));
			return scanner.scan(img);
		} catch (IOException e) {
			error(e.getMessage());
			Logging.logException(e);
		}
		
		return null;
	}

	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		TopCodes topcodes = new TopCodes("topcodes");
		topcodes.startService();			
		topcodes.scan("somepicture.jpg");
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
