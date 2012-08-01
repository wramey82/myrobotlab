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

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.nio.ShortBuffer;

import org.myrobotlab.openni.Points3DPanel;
import org.myrobotlab.openni.PointsShape;
import org.myrobotlab.service.OpenNI;
import org.myrobotlab.service.interfaces.GUI;

public class OpenNIGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;

	PointsShape ptsShape = new PointsShape();
	Points3DPanel panel3d = new Points3DPanel(ptsShape);

	
	public OpenNIGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	
	public void init() {
		//openni = (OpenNI)Runtime.getService(boundServiceName).service;
		
		display.setLayout(new BorderLayout());
		display.add(panel3d, BorderLayout.CENTER);
		
		/*
		JPanel viewer = new JPanel();
		viewer.setLayout(new BorderLayout());
		viewer.add(panel3d, BorderLayout.CENTER);
		*/
		
	}

	
	public void getState(OpenNI openni)
	{
		if (openni != null)
		{
	
		}
		
	}

	
	public void publishFrame (ShortBuffer depthBuffer)
	{
		ptsShape.updateDepthCoords(depthBuffer);  
	}
	
	@Override
	public void attachGUI() {
		// subscribe & ask for the initial state of the service
		subscribe("publishState", "getState", OpenNI.class); 
		subscribe("publishFrame", "publishFrame", ShortBuffer.class); 
		myService.send(boundServiceName, "publishState");
		
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", OpenNI.class);
	}

}
