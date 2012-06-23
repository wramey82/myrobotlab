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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;


import org.myrobotlab.service.Clock;
import org.myrobotlab.service.Clock.PulseDataType;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.Runtime;

public class ClockGUI extends ServiceGUI implements ActionListener{

	static final long serialVersionUID = 1L;
	JButton startClock = null;
    ButtonGroup group = new ButtonGroup();
    
	JRadioButton none = new JRadioButton("none");
	JRadioButton increment = new JRadioButton("increment");
	JRadioButton integer = new JRadioButton("integer");
	JRadioButton string = new JRadioButton("string");

	JTextField interval = new JTextField("1000");
	JTextField pulseDataString = new JTextField(10);
	JIntegerField pulseDataInteger = new JIntegerField(10);
	
	Clock myClock = null;
	
	public ClockGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	
	ActionListener setType = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			//myService.send(boundServiceName, "setType", ((JRadioButton) e.getSource()).getText()); - look ma no message !
			myClock.setType(((JRadioButton) e.getSource()).getText());
		}
	};

	public void init() {
		
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 2;

		display.add(getstartClockButton(), gc);
		gc.gridwidth = 1;
		++gc.gridx;
		display.add(new JLabel("  interval  "), gc);
		++gc.gridx;
		display.add(interval, gc);
		++gc.gridx;
		display.add(new JLabel("  ms  "), gc);
		
		
		// build filters begin ------------------
		JPanel pulseData = new JPanel(new GridBagLayout());
		TitledBorder title;
		title = BorderFactory.createTitledBorder("pulse data");
		pulseData.setBorder(title);
		
		none.setActionCommand("none");
		none.setSelected(true);
		none.addActionListener(setType);

		increment.setActionCommand("increment");
		increment.addActionListener(setType);

		integer.setActionCommand("integer");
		integer.addActionListener(setType);

		string.setActionCommand("string");
		string.addActionListener(setType);
		
	     //Group the radio buttons.
        group.add(none);
        group.add(increment);
        group.add(integer);
        group.add(string);
        
        // reuse gc
        gc.gridx = 0;
        gc.gridy = 0;
        pulseData.add(none, gc);
        ++gc.gridy;
        pulseData.add(increment, gc);
        ++gc.gridy;        
        pulseData.add(integer, gc);
        ++gc.gridx;
        pulseData.add(pulseDataInteger, gc);
        gc.gridx = 0;        
        ++gc.gridy;
        pulseData.add(string, gc);
        ++gc.gridx;        
        pulseData.add(pulseDataString, gc);

        // reuse gc
		gc.gridx = 0;
		gc.gridy = 2;
        display.add(pulseData, gc);
        
        myClock = (Clock)Runtime.getService(boundServiceName).service;
		
	}

	public JButton getstartClockButton() {
		if (startClock == null) {
			startClock = new JButton("start clock");
			startClock.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (startClock.getText().compareTo("start clock") == 0) {
						startClock.setText("stop clock");
												
						myClock.interval = Integer.parseInt(interval.getText());
						myClock.pulseDataInteger = Integer.parseInt(pulseDataInteger.getText());
						myClock.pulseDataString = pulseDataString.getText();
								
						// set the state of the bound service - whether local or remote 
						myService.send(boundServiceName, "setState", myClock); // double set on local
						// publish the fact you set the state - 
						// TODO - should this a function which calls both functions ? 
						myService.send(boundServiceName, "publishState"); // TODO - bury in Service.SetState?
						myService.send(boundServiceName, "startClock"); 
						
					} else {
						startClock.setText("start clock");
						myService.send(boundServiceName, "stopClock");
					}
				}

			});

		}

		return startClock;

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		myService.send(boundServiceName, "setType", e.getActionCommand());
	}
	
	// FIXME - is get/set state interact with Runtime registry ??? 
	// it probably should
	public void getState(Clock c)
	{
		// Setting the display fields based on incoming Clock data
		// if the Clock is local - the actual clock is sent
		// if the Clock is remote - a data proxy is sent
		if (c != null)
		{
			if (c.pulseDataType == PulseDataType.increment)
			{
				increment.setSelected(true);
			} else if (c.pulseDataType == PulseDataType.integer)
			{
				integer.setSelected(true);
				
			} else if (c.pulseDataType == PulseDataType.string)
			{
				string.setSelected(true);
				
			} else if (c.pulseDataType == PulseDataType.none)
			{
				none.setSelected(true);			
			}
			
			pulseDataString.setText(c.pulseDataString);

			pulseDataInteger.setInt(c.pulseDataInteger);
			
			interval.setText((c.interval + ""));
			
			/*
			 * WARNING - you can not base accurate data on a transient member !!!
			if (c.myClock != null) // this is transient it will always be null if remote !
			{
				startClock.setText("stop clock");
			} else {
				startClock.setText("start clock");
			}
			*/
			
			if (c.isClockRunning)
			{
				startClock.setText("stop clock");				
			} else {
				startClock.setText("start clock");				
			}
			
		}
		
	}


	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <- Class.forName(type)
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Clock.class); 
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Clock.class);
	}

}
