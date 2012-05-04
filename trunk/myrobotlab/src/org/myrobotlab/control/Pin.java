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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.data.IOData;

public class Pin extends JPanel {

	public final static Logger LOG = Logger.getLogger(Pin.class.getCanonicalName());
	static final long serialVersionUID = 1L;
	public final String boundServiceName;
	public final int pinNumber;

	public DigitalButton inOut = null;
	public DigitalButton onOff = null;
	public JSlider analogSlider = null;

	JLabel pinLabel = null;
	public JLabel analogData = null;
	
	boolean isAnalog = false;
	public final Service myService;
	JComboBox analogDigital = null;
	JLabel counter = null;
	
	// types of DigitalButtons
	public final static int ONOFF = 0;
	public final static int INPUTOUTPUT = 1;
	
	// values 
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;
	public static final int OUTPUT = 0x1;
	public static final int INPUT = 0x0;

	public Pin(Service myService, String boundServiceName, int pinNumber, boolean isAnalog) {
		super();
		this.boundServiceName = boundServiceName;
		this.isAnalog = isAnalog;
		this.pinNumber = pinNumber;
		this.myService = myService;
	
		this.setLayout(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		//gc.weightx = 1.0;
		//gc.weighty = 1.0;
		//gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.WEST;

		pinLabel = new JLabel("pin " + pinNumber);
		pinLabel.setPreferredSize(new Dimension(40,13));

		gc.gridx = 0;
		gc.gridy = 0;

		add(pinLabel, gc);
		++gc.gridx;
		
		inOut = new DigitalButton(this, 
				Util.getScaledIcon(Util.getImage("square_out.png"), 0.50),
				Util.getScaledIcon(Util.getImage("square_in.png"), 0.50),
				INPUTOUTPUT);
		
		gc.anchor = GridBagConstraints.EAST;
		add(inOut, gc);
		++gc.gridx;

		onOff = new DigitalButton(this, 
				Util.getScaledIcon(Util.getImage("grey.png"), 0.50),
				Util.getScaledIcon(Util.getImage("green.png"), 0.50),
				ONOFF);
		
		add(onOff, gc);
		
		if (isAnalog) {
			/*
			++gc.gridx;
			analogDigital = new AnalogDigital(this);
			this.add(analogDigital, gc);
			*/
			++gc.gridy;
			gc.gridwidth = 5;
			gc.gridx = 0;

			this.add(getAnalogSlider(), gc);

			//onOff.setText("On");
		}

		gc.gridwidth = 1;
		++gc.gridx;
		add(new JLabel(" "));
		
		if (isAnalog)
		{
			++gc.gridx;
			analogData = new JLabel("0");
			add(analogData, gc);
		}
		//++gc.gridx;
		//counter = new JLabel("0");
		//add(counter, gc);
		
	}

	private JSlider getAnalogSlider() {
		if (analogSlider == null) {
			analogSlider = new JSlider(0, 255, 0);
			analogSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					analogData.setText("" + analogSlider.getValue());
					IOData io = new IOData();
					io.address = pinNumber;
					io.value = analogSlider.getValue();
					if (myService != null) {
						myService.send(boundServiceName, "analogWrite", io);
					} else {
						LOG.error("can not send message myService is null");
					}
				}
			});

		}
		return analogSlider;
	}


	// FIXME - generalize & remove from inner definition
	/*
	private static class DigitalButton extends JButton implements
			ActionListener {
		private static final long serialVersionUID = 1L;
		Pin pin = null;

		public DigitalButton(Pin pin) {
			super();
			
			// image button properties
			setOpaque(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			
			this.pin = pin;
			setIcon(Util.getScaledIcon(Util.getImage("grey.png"), 0.50));
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("On")) {
				//setText("Off");
				//setIcon(Util.getScaledIcon(Util.getImage("help.png"), 0.50));
				setIcon(Util.getScaledIcon(Util.getImage("grey.png"), 0.50));
				IOData io = new IOData();
				io.address = pin.getPinNumber();
				io.value = 0;
				pin.getService().send(pin.getServiceName(), "digitalWrite", io);
			} else {
				//setText("On");
				setIcon(Util.getScaledIcon(Util.getImage("green.png"), 0.50));
				IOData io = new IOData();
				io.address = pin.getPinNumber();
				io.value = 1;
				pin.getService().send(pin.getServiceName(), "digitalWrite", io);
			}
		}
	}
	*/

	/*
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
				io.address = pin.pinNumber;
				io.value = OUTPUT;
				pin.myService.send(pin.boundServiceName, "pinMode", io); 
				pin.myService.send(pin.boundServiceName, "digitalReadPollStop", pin.pinNumber);
			} else {
				LOG.info("INPUT");
				IOData io = new IOData();
				io.address = pin.pinNumber;
				io.value = INPUT;
				pin.myService.send(pin.boundServiceName, "pinMode", io);
				pin.myService.send(pin.boundServiceName, "digitalReadPollStart", pin.pinNumber);
			}
		}
	}
*/
}