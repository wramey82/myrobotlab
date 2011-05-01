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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.myrobotlab.service.GUIService;

public class ClockGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	JButton startClock = null;
	JTextField interval = new JTextField("1000");

	public ClockGUI(String name, GUIService myService) {
		super(name, myService);
		display.add(getstartClockButton());
		display.add(new JLabel("  interval  "));
		display.add(interval);
		display.add(new JLabel("  ms  "));
	}

	public JButton getstartClockButton() {
		if (startClock == null) {
			startClock = new JButton("start clock");
			startClock.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (startClock.getText().compareTo("start clock") == 0) {
						startClock.setText("stop clock");
						myService.send(boundServiceName, "setInterval", Integer.parseInt(interval.getText()));
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
	public void attachGUI() {
	}

	@Override
	public void detachGUI() {
	}

}
