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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;
import org.myrobotlab.service.Runtime;

import org.myrobotlab.service.AFMotorShield;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.interfaces.GUI;

public class AFMotorShieldGUI extends ServiceGUI implements ListSelectionListener {

	public final static Logger LOG = Logger.getLogger(AFMotorShieldGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	private AFMotorShield myAFMotorShield = null;

	JComboBox ttyPort = new JComboBox(new String[] { "" }); 
	Keyboard keyboard = null;
	ActionListener portActionListener = null;
	JButton stop = new JButton("stop");
	JButton test = new JButton("test");
	ButtonListener buttonLisener = null;

	public AFMotorShieldGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
		myAFMotorShield = (AFMotorShield)Runtime.getService(boundServiceName).service;
	}
	
	class ButtonListener implements ActionListener {
	  ButtonListener() {
	  }

	  public void actionPerformed(ActionEvent e) {
		  LOG.info(e.getActionCommand());
		  myService.send(boundServiceName, e.getActionCommand());
	  }
	}
	
	public void init() {

		keyboard = new Keyboard();
		buttonLisener = new ButtonListener();
		// build input begin ------------------

		gc.gridx = 0;
		gc.gridy = 0;

		gc.gridx = 0;
		++gc.gridy;
		display.add(new JLabel("port : "), gc);
		++gc.gridx;
		display.add(ttyPort, gc);
		++gc.gridx;
		display.add(stop, gc);
		stop.addActionListener(buttonLisener);
		++gc.gridx;
		display.add(test, gc);
		test.addActionListener(buttonLisener);
		
		++gc.gridy;
		gc.gridx = 0;
		++gc.gridy;
		JButton keyboardButton = new JButton(
				"<html><table><tr><td align=\"center\">click here</td></tr><tr><td align=\"center\">for keyboard</td></tr><tr><td align=\"center\">control</td></tr></table></html>");
		display.add(keyboardButton, gc);
		keyboardButton.addKeyListener(keyboard);

		gc.gridx = 1;
		
		 portActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				String newPort = (String) cb.getSelectedItem();
				myService.send(boundServiceName, "setPort", newPort);
			}
		};

		ttyPort.addActionListener(portActionListener);
		

		++gc.gridy;
		++gc.gridy;
		++gc.gridy;
		gc.gridx = 0;

		gc.gridx = 1;
	}

	public class Keyboard implements KeyListener {
		public void keyPressed(KeyEvent keyEvent) {
			myService.send(boundServiceName, "keyCommand", KeyEvent
					.getKeyText(keyEvent.getKeyCode()));
		}

		public void keyReleased(KeyEvent keyEvent) {
			// LOG.error("Released" + keyEvent);
		}

		public void keyTyped(KeyEvent keyEvent) {
			// LOG.error("Typed" + keyEvent);
		}

		private void printIt(String title, KeyEvent keyEvent) {
			int keyCode = keyEvent.getKeyCode();
			String keyText = KeyEvent.getKeyText(keyCode);
		}
	};
	
	public void getState(AFMotorShield roomba)
	{
		if (roomba != null)
		{
			setPorts(roomba.getPorts());			
		}
	
	}
	

	/**
	 * setPorts is called by getState - which is called when the Arduino changes port state
	 * is NOT called by the GUI component
	 * @param p
	 * FIXME - there should be a corresponding gui element for the serial.Port ie serial.PortGUI such
	 */
	public void setPorts(ArrayList<String> p) {
		//ttyPort.removeAllItems();
		
		//ttyPort.addItem(""); // the null port
		// ttyPort.removeAllItems();
		for (int i = 0; i < p.size(); ++i) {
			String n = p.get(i);
			LOG.info(n);
			ttyPort.addItem(n);
		}

		if (myAFMotorShield != null)
		{
			// remove and re-add the action listener
			// because we don't want a recursive event
			// when the Service changes the state
			ttyPort.removeActionListener(portActionListener);
			ttyPort.setSelectedItem(myAFMotorShield.getPortName());
			ttyPort.addActionListener(portActionListener);
		}

	}

	
	public void attachGUI() {
		sendNotifyRequest("publishState", "getState", Arduino.class);
		myService.send(boundServiceName, "publishState");	
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishState", "getState", Arduino.class);	
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}


}