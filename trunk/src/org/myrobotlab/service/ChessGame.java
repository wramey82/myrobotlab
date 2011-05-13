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
import org.myrobotlab.control.ChessGameGUI;
import org.myrobotlab.framework.Service;

public class ChessGame extends Service {

	public final static Logger LOG = Logger.getLogger(ChessGame.class.getCanonicalName());


	public ChessGame(String n) {
		super(n, ChessGame.class.getCanonicalName());
	}

	public HMove makeHMove(HMove m)
	{
		
		return m;
	}
	
	public String makeMove(HMove m)
	{
		String t = m.toString();
		LOG.info(t);

		if (t.length() == 6)
		{
			t = t.substring(1);
		}

		t = (t.substring(0,2) + t.substring(3));

		t = "x" + t + "z";
		t = t.toLowerCase(); 
		
		LOG.info(t);
		
		return t;
	}
	
	public HMove inputHMove (HMove s)
	{
		return s;
	}

	
	public String inputMove (String s)
	{
		return s;
	}
	
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		LOG.info(ChessGameGUI.cleanMove("a2-a3q"));		
		
		ChessGame chess1 = new ChessGame("chess1");
		chess1.startService();

		ChessGame chess2 = new ChessGame("chess2");
		chess2.startService();
		
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
