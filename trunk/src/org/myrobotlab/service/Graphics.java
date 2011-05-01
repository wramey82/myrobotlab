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

import java.awt.Color;
import java.awt.Dimension;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;

public class Graphics extends Service {

	public final static Logger LOG = Logger.getLogger(Graphics.class.getCanonicalName());
	
	public String guiServiceName = null;
	
	public Graphics(String n) {
		super(n, Graphics.class.getCanonicalName());
	}

	// in order for a graphics service to work it needs to be associated with a GUIServic
	// this is how to associate it
	public void attach(String guiServiceName)
	{
		this.guiServiceName = guiServiceName;
	}
	
	@Override
	public void loadDefaultConfiguration() {
		cfg.set("width", 320);
		cfg.set("height", 240);		
	}
	
	public void createGraph ()
	{
		createGraph(cfg.getInt("width"), cfg.getInt("height"));
	}

	public void createGraph (Integer x, Integer y)
	{
		send(guiServiceName, "createGraph", new Dimension(x, y));
	}
	
	// wrappers begin --------------------------
	public void drawLine(Integer x1, Integer y1, Integer x2, Integer y2)
	{
		send(guiServiceName, "drawLine", x1, y1, x2, y2);
	}

	public void drawString(String str, Integer x, Integer y)
	{
		send(guiServiceName, "drawString", str, x, y);
	}

	public void drawRect(Integer x, Integer y, Integer width, Integer height)
	{
		send(guiServiceName, "drawRect", x, y, width, height);
	}

	public void fillOval(Integer x, Integer y, Integer width, Integer height)
	{
		send(guiServiceName, "fillOval", x, y, width, height);
	}

	public void fillRect(Integer x, Integer y, Integer width, Integer height)
	{
		send(guiServiceName, "fillRect", x, y, width, height);
	}
	
	public void clearRect(Integer x, Integer y, Integer width, Integer height)
	{
		send(guiServiceName, "clearRect", x, y, width, height);
	}

	public void setColor(Color c)
	{
		send(guiServiceName, "setColor", c);
	}

	
	// wrappers end --------------------------

	public void refreshDisplay ()
	{
		send(guiServiceName, "refreshDisplay");		
	}
	/*
	 *    publishing points begin --------------------------------------------
	 *    This would be fore static routes and invoking - but it is abondoned for non-static routes and message "send"ing with
	 *    more robuts parameter handling
	 */
	// sent to GUIService - TODO - this message node CAN NOT be private (unfortunately) because the invoking is only allowed to touch 
	// public methods -  This is a little confusing, because the "user" of this function might think it does something immediately
	// in actuality it does something only when it is INVOKED
	public Dimension createGraph (Dimension d)
	{
		return d;
	}
	
	/*
	 *    publishing points end --------------------------------------------
	 */
	
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		GUIService gui = new GUIService("gui");
		Graphics graph = new Graphics("graph");
		
		// manual intervention - clear screen
		
		gui.startService();
		graph.startService();
		
		gui.display();
		graph.attach(gui.name);
		graph.createGraph(640, 480);
		graph.setColor(new Color(0x666666));
		graph.drawLine(20, 20, 300, 300);
	}

	@Override
	public String getToolTip() {
		return "a graphics service encapsulating Java swing graphic methods";
	}
	
}
