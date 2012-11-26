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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

import org.apache.log4j.Logger;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.interfaces.GUI;

public class InMoovGUI extends ServiceGUI implements ActionListener{

	static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(InMoovGUI.class.getCanonicalName());

	JLayeredPane imageMap;
	
	public InMoovGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	public void init() {
		
		imageMap = new JLayeredPane();
		imageMap.setPreferredSize(new Dimension(692,688));

		// set correct arduino image
		JLabel image = new JLabel();

		ImageIcon dPic = Util.getImageIcon("InMoov/body.png"); // FIXME - shortType/image.png
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));
		display.add(imageMap);
	}


	public void getState(InMoov moov)
	{
	
	}


	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <- Class.forName(type)
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", InMoovGUI.class); 
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", InMoovGUI.class);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
