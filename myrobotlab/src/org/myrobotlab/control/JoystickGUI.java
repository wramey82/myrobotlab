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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.myrobotlab.service.Joystick;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;

public class JoystickGUI extends ServiceGUI implements ActionListener{

	static final long serialVersionUID = 1L;

	JComboBox outputFormat = new JComboBox(); 
	JButton startJoystick = null;
	Joystick myJoystick = null;
	
	private JoystickCompassPanel xyPanel, zrzPanel, hatPanel;

	
	public JoystickGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		display.setLayout(new BorderLayout());
		
		// PAGE_START
		JPanel page_start = new JPanel();
		//page_start.setLayout(new BoxLayout(page_start, BoxLayout.X_AXIS)); // horizontal box
															// layout
		// three CompassPanels in a row
		hatPanel = new JoystickCompassPanel("POV");
		page_start.add(hatPanel);

		xyPanel = new JoystickCompassPanel("xy");
		page_start.add(xyPanel);

		zrzPanel = new JoystickCompassPanel("zRz");
		page_start.add(zrzPanel);
		
		display.add(page_start, BorderLayout.PAGE_START);
		
		// CENTER
		JPanel center = new JPanel();
		
		JPanel input = new JPanel();
		TitledBorder title;
		title = BorderFactory.createTitledBorder("input");
		input.setBorder(title);
		input.add(controllers);
		center.add(input);

		JPanel output = new JPanel();
		title = BorderFactory.createTitledBorder("output");
		output.setBorder(title);
		output.add(outputFormat);
		outputFormat.addItem("0 to 180");
		outputFormat.addItem("-1.0 to 1.0");
		outputFormat.addItem("0.0 to 1.0");
		outputFormat.setSelectedItem(null);
		center.add(output);
				
		// PAGE_END
		JPanel page_end = new JPanel();
		JoystickButtonsPanel buttonsPanel = new JoystickButtonsPanel();
		display.add(buttonsPanel, BorderLayout.PAGE_END);

		
		display.add(center, BorderLayout.CENTER);
        myJoystick = (Joystick)Runtime.getService(boundServiceName).service;
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == controllers)
		{
			log.info("changed to");
		}
		//myService.send(boundServiceName, "setType", e.getActionCommand());
	}
	
	JComboBox controllers = new JComboBox();
	
	TreeMap<String, Integer> controllerNamess;
	// FIXME - is get/set state interact with Runtime registry ??? 
	// it probably should
	public void getState(Joystick joy)
	{
		if (joy != null)
		{
			
			controllers.removeAll();
			
			controllerNamess = joy.getControllerNames();
			Iterator<String> it = controllerNamess.keySet().iterator();
			
			controllers.addItem("");
			while (it.hasNext()) {
				String name = it.next();
				controllers.addItem(name);
			}	
			
			controllers.addActionListener(this);
			controllers.setSelectedItem(null);
		}
		
	}


	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <- Class.forName(type)
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Joystick.class); 
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Joystick.class);
	}

}
