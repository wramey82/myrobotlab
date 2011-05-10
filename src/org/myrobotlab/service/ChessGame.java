/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.chess.HMove;
import org.myrobotlab.framework.Service;

public class ChessGame extends Service {

	public final static Logger LOG = Logger.getLogger(ChessGame.class.getCanonicalName());


	public ChessGame(String n) {
		super(n, ChessGame.class.getCanonicalName());
	}
	
	public String makeMove(HMove m)
	{
		String t = m.toString();
		// if Ne6-F7 - remove
		LOG.info(t);
		if (t.length() == 6)
		{
			t = t.substring(1);
		}
		LOG.info(t);

		//t = Util.removeChar(t, '-');
		t = (t.substring(0,2) + t.substring(3));
		LOG.info(t);
		//t = t.toUpperCase();
		t = t.toLowerCase(); 
		t = t + "Z";
		LOG.info(t);
		
		return t;
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		
		ChessGame chessBoardService = new ChessGame("chessBoardService");
		chessBoardService.startService();

		//OpenCV camera = new OpenCV("camera");
		//camera.startService();
		
		Logging log = new Logging("log");
		log.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

	@Override
	public String getToolTip() {
		return "used to generate pulses";
	}


}
