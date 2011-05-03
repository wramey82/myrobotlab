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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.service.GUIService;

public class SoccerGameGUI extends ServiceGUI {

	public final static Logger LOG = Logger.getLogger(SoccerGameGUI.class
			.getCanonicalName());
	static final long serialVersionUID = 1L;

	JLabel boundPos = null;

	JLabel timeValue = new JLabel("20");
	JLabel playersConnected = new JLabel("6");

	DigitalButton attachButton = null;
	JButton takeControl = null;
	JSlider slider = null;

	BasicArrowButton forward = new BasicArrowButton(BasicArrowButton.NORTH);
	BasicArrowButton back = new BasicArrowButton(BasicArrowButton.SOUTH);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	Keyboard keyboard = null;

	JComboBox controller = null;

	public SoccerGameGUI(String name, GUIService myService) {
		super(name, myService);

		left.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider.setValue(slider.getValue() - 1);
			}
		});
		right.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider.setValue(slider.getValue() + 1);
			}
		});

		keyboard = new Keyboard();
		// build input begin ------------------

		gc.gridx = 0;
		gc.gridy = 0;
		display.add(new JLabel("time remaining : "), gc);

		gc.gridx = 1;
		gc.gridy = 0;
		display.add(timeValue, gc);

		gc.gridx = 0;
		gc.gridy = 1;
		display.add(new JLabel("players connected: "), gc);

		gc.gridx = 1;
		gc.gridy = 1;
		display.add(playersConnected, gc);

		gc.gridx = 0;
		gc.gridy = 2;
		ImageIcon pic = new ImageIcon("soccerball.jpg");
		display.add(new JLabel(pic), gc);

		// input begin
		JPanel input = new JPanel(new GridBagLayout());

		gc.gridx = 1;
		gc.gridy = 0;
		input.add(forward, gc);

		gc.gridx = 0;
		gc.gridy = 1;
		input.add(left, gc);

		gc.gridx = 2;
		gc.gridy = 1;
		input.add(right, gc);

		gc.gridx = 1;
		gc.gridy = 2;
		input.add(back, gc);

		// input end

		gc.gridx = 0;
		gc.gridy = 3;

		takeControl = new JButton("take control player");
		JTextField playerValue = new JTextField("4");

		display.add(takeControl, gc);
		gc.gridx = 1;
		gc.gridy = 3;

		display.add(playerValue, gc);

		// re-using gc
		gc.gridx = 1;
		gc.gridy = 4;
		display.add(input, gc);

		gc.gridx = 0;
		gc.gridy = 4;

		JButton p = new JButton(
				"<html><table><tr><td align=\"center\">click here</td></tr><tr><td align=\"center\">for keyboard</td></tr><tr><td align=\"center\">control</td></tr></table></html>");
		// p.setBorder(new BevelBorder(BevelBorder.RAISED));

		display.add(p, gc);

		p.addKeyListener(keyboard);

	}

	public class Keyboard implements KeyListener {
		public void keyPressed(KeyEvent keyEvent) {
			LOG.error("Pressed" + keyEvent);
			if (keyEvent.getKeyCode() == 38) {
				myService.send("tilt", "move", 1);
			} else if (keyEvent.getKeyCode() == 40) {
				myService.send("tilt", "move", -1);
			}

		}

		public void keyReleased(KeyEvent keyEvent) {
			LOG.error("Released" + keyEvent);
		}

		public void keyTyped(KeyEvent keyEvent) {
			LOG.error("Typed" + keyEvent);
		}

		private void printIt(String title, KeyEvent keyEvent) {
			int keyCode = keyEvent.getKeyCode();
			String keyText = KeyEvent.getKeyText(keyCode);
			LOG.error(title + " : " + keyText + " / " + keyEvent.getKeyChar());
		}
	};

	public void attachGUI() {
	}

	/*
	private JSlider getAnalogValue() {
		if (slider == null) {
			slider = new JSlider(0, 180, 90);
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					boundPos.setText("" + slider.getValue());

					if (myService != null) {
						myService.send(boundServiceName, "moveTo", new Integer(
								slider.getValue()));
					} else {
						LOG.error("can not send message myService is null");
					}
				}
			});

		}
		return slider;
	}
	*/
	
	private class DigitalButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (getText().equals("attach")) {
				setText("detach");
				// myService.send(boundServiceName, "attach",
				// controller.getSelectedItem().toString(),
				// pin.getSelectedItem());
			} else {
				setText("attach");
				myService.send(boundServiceName, "detach", null);
			}
		}
	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}

	public Vector<String> getAllServoControllers() {
		Vector<String> v = new Vector<String>();
		v.addElement(""); // the "no interface" selection
		ConfigurationManager cm = myService.getHostCFG();
		Vector<String> sm = cm.getServiceVector();
		for (int i = 0; i < sm.size(); ++i) {
			Vector<String> intfcs = cm.getInterfaces(sm.get(i));
			if (intfcs == null)
				continue;
			for (int j = 0; j < intfcs.size(); ++j) {
				if (intfcs.get(j).compareTo(
						"org.myrobotlab.service.interfaces.ServoController") == 0) {
					v.addElement(sm.get(i));
				}
			}

		}
		return v;
	}

}