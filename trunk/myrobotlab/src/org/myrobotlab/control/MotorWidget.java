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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.interfaces.GUI;

// http://www.superrobotica.com/VisualSC2e.htm
//public class ServoWidget extends JPanel {
public class MotorWidget extends ServiceGUI {

	public final static Logger LOG = Logger.getLogger(MotorWidget.class
			.getCanonicalName());
	static final long serialVersionUID = 1L;

	JLabel dataLabel = null;

	DigitalButton onOffButton = null;
	JSlider slider = null;

	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	public MotorWidget(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		// this.setSize(453, 62);
		display.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.EAST;

		gc.gridx = 0;
		gc.gridy = 0;

		display.add(new JLabel(boundServiceName), gc);
		++gc.gridx;

		display.add(left, gc);
		++gc.gridx;

		display.add(right, gc);
		++gc.gridx;

		onOffButton = new DigitalButton(this);
		display.add(onOffButton, gc);
		++gc.gridx;

		display.add(getAnalogValue(), gc);
		onOffButton.setText("On");

		++gc.gridx;
		display.add(new JLabel(" "));
		++gc.gridx;
		dataLabel = new JLabel("90");
		display.add(dataLabel, gc);

	}

	private JSlider getAnalogValue() {
		if (slider == null) {
			slider = new JSlider(0, 180, 90);
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					dataLabel.setText("" + slider.getValue());

					if (myService != null) {
						myService.send(boundServiceName, "moveTo", slider
								.getValue());
					} else {
						LOG.error("can not send message myService is null");
					}
				}
			});

		}
		return slider;
	}

	public Service getOperator() {
		return myService;
	}

//	public void setService(GUIService myService) {
//		this.myService = myService;
//	}

	private class DigitalButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 1L;
		MotorWidget pin = null;

		public DigitalButton(MotorWidget pin) {
			super();
			this.pin = pin;
			setText("Off");
			addActionListener(this);
		}

		// TODO - bind unbind
		@Override
		public void actionPerformed(ActionEvent e) {
			if (getText().equals("On")) {
				setText("Off");
				IOData io = new IOData();
				// io.address.set(pin.getPinNumber()); io.value.set(0); TODO -
				// fix
				pin.getOperator().send(boundServiceName, "attach", io);
			} else {
				setText("On");
				IOData io = new IOData();
				// io.address.set(pin.getPinNumber()); io.value.set(1);
				pin.getOperator().send(boundServiceName, "detach", io);
			}
		}
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}

}