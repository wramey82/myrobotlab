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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.IOData;

public class Pin extends JPanel {

	public final static Logger LOG = Logger.getLogger(Pin.class
			.getCanonicalName());
	static final long serialVersionUID = 1L;
	String name = "";
	Integer pinNumber = new Integer(0);
	JLabel pinLabel = null;
	DigitalButton onOffButton = null;
	JSlider analogSlider = null;
	JLabel dataLabel = null;
	boolean isAnalog = false;
	Service myService = null;
	JComboBox analogDigital = null;
	JComboBox inputOutput = null;
	JLabel counter = null;

	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;
	public static final int OUTPUT = 0x1;
	public static final int INPUT = 0x0;

	public enum PinType {
		ANALOG, DIGITAL
	};

	public Pin(String name, int pinNumber, boolean isAnalog) {
		super();
		this.name = name;
		this.isAnalog = isAnalog;
		this.pinNumber = pinNumber;
		initialize();
	}

	public Pin() {
		this("", 0, false);
	}

	private void initialize() {
		// this.setSize(453, 62);
		this.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.EAST;

		pinLabel = new JLabel("pin " + pinNumber);

		gc.gridx = 0;
		gc.gridy = 0;

		this.add(pinLabel, gc);
		++gc.gridx;

		inputOutput = new InputOutput(this);
		this.add(inputOutput, gc);
		++gc.gridx;

		onOffButton = new DigitalButton(this);
		this.add(onOffButton, gc);
		if (isAnalog) {
			++gc.gridx;
			analogDigital = new AnalogDigital(this);
			this.add(analogDigital, gc);
			++gc.gridy;
			gc.gridx = 0;

			this.add(getAnalogValue(), gc);

			onOffButton.setText("On");
		}

		++gc.gridx;
		this.add(new JLabel(" "));
		++gc.gridx;
		dataLabel = new JLabel("0");
		this.add(dataLabel, gc);
		++gc.gridx;
		counter = new JLabel("0");
		this.add(counter, gc);

	}

	private JSlider getAnalogValue() {
		if (analogSlider == null) {
			analogSlider = new JSlider(0, 255, 0);
			analogSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					dataLabel.setText("" + analogSlider.getValue());
					IOData io = new IOData();
					io.address = pinNumber;
					io.value = analogSlider.getValue();
					if (myService != null) {
						myService.send(name, "analogWrite", io);
					} else {
						LOG.error("can not send message myService is null");
					}
				}
			});

		}
		return analogSlider;
	}

	public String getServiceName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getPinNumber() {
		return pinNumber;
	}

	public void setPinNumber(Integer pinNumber) {
		this.pinNumber = pinNumber;
	}

	public Service getService() {
		return myService;
	}

	public void setService(Service myService) {
		this.myService = myService;
	}

	private static class DigitalButton extends JButton implements
			ActionListener {
		private static final long serialVersionUID = 1L;
		Pin pin = null;

		public DigitalButton(Pin pin) {
			super();
			this.pin = pin;
			setText("Off");
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (getText().equals("On")) {
				setText("Off");
				IOData io = new IOData();
				io.address = pin.getPinNumber();
				io.value = 0;
				pin.getService().send(pin.getServiceName(), "digitalWrite", io);
			} else {
				setText("On");
				IOData io = new IOData();
				io.address = pin.getPinNumber();
				io.value = 1;
				pin.getService().send(pin.getServiceName(), "digitalWrite", io);
			}
		}
	}

	private static class AnalogDigital extends JComboBox implements
			ActionListener {
		private static final long serialVersionUID = 1L;
		Pin pin = null;

		public AnalogDigital(Pin pin) {
			super(new String[] { "analog", "digital" });
			this.pin = pin;
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (getSelectedIndex() == 1) {
				pin.analogSlider.setVisible(false);
				pin.dataLabel.setVisible(false);
				pin.onOffButton.setText("Off");
			} else {
				pin.onOffButton.setText("On");
				pin.analogSlider.setVisible(true);
				pin.dataLabel.setVisible(true);
			}
		}
	}

	private static class InputOutput extends JComboBox implements
			ActionListener {
		private static final long serialVersionUID = 1L;
		Pin pin = null;

		public InputOutput(Pin pin) {
			super(new String[] { "output", "input" });
			this.pin = pin;
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (getSelectedIndex() == 0) {
				LOG.info("OUTPUT");
				IOData io = new IOData();
				io.address = pin.getPinNumber();
				io.value = OUTPUT;
				pin.myService.send(pin.name, "pinMode", io); // TODO - default
																// arduino is ??
																// OUTPUT??
				pin.myService.send(pin.name, "digitalReadPollStop", pin
						.getPinNumber());
			} else {
				LOG.info("INPUT");
				IOData io = new IOData();
				io.address = pin.getPinNumber();
				io.value = INPUT;
				pin.myService.send(pin.name, "pinMode", io);
				pin.myService.send(pin.name, "digitalReadPollStart", pin
						.getPinNumber());
			}
		}
	}

}