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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.MotorController;

public class MotorGUI extends ServiceGUI implements ActionListener, ChangeListener {

	// controller
	JButton attachButton = null;
	JComboBox controller = new JComboBox();
	JComboBox powerPin = new JComboBox();
	JComboBox directionPin = new JComboBox();
	MotorController myMotorController = null;
	JLabel powerPinLabel = new JLabel("power pin");
	JLabel directionPinLabel = new JLabel("direction pin");
	JCheckBox invert = new JCheckBox("invert");
	
	// power
	private FloatJSlider power = null;
	private JLabel powerValue = new JLabel("0");
	ImageButton stopButton;
	
	// position
	private JLabel posValue = new JLabel("0");
	

	// TODO - make MotorPanel - for 1 motor - for shared embedded widget
	// TODO - stop sign button for panic stop
	// TODO - tighten up interfaces
	// TODO - DIRECT calls ! - motor & controller HAVE to be on the same
	// computer

	public class FloatJSlider extends JSlider {

		private static final long serialVersionUID = 1L;
		final int scale;

		public FloatJSlider(int min, int max, int value, int scale) {
			super(min, max, value);
			this.scale = scale;
			
			
			Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
			labelTable.put( new Integer( min ), new JLabel(String.format("%.2f", (float)min/scale)));
			labelTable.put( new Integer( min/2 ), new JLabel(String.format("%.2f", (float)min/scale/2)));
			labelTable.put( new Integer( value ), new JLabel(String.format("%.2f", (float)value/scale)));
			labelTable.put( new Integer( max/2 ), new JLabel(String.format("%.2f", (float)max/scale/2)));
			labelTable.put( new Integer( max ), new JLabel(String.format("%.2f", (float)max/scale)));
			setLabelTable(labelTable);
			setPaintTrack(false);
		}

		public float getScaledValue() {
			return ((float) super.getValue()) / this.scale;
		}
	}

	public MotorGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {

		// controllerPanel begin ------------------
		JPanel controllerPanel = new JPanel();

		controllerPanel.setBorder(BorderFactory.createTitledBorder("controller"));

		Vector<String> v = Runtime.getServicesFromInterface(MotorController.class.getCanonicalName());
		v.add(0, "");
		controller = new JComboBox(v);
		controllerPanel.add(controller);

		powerPinLabel.setEnabled(false);
		powerPin.setEnabled(false);
		controllerPanel.add(powerPinLabel);
		controllerPanel.add(powerPin);

		directionPinLabel.setEnabled(false);
		directionPin.setEnabled(false);
		controllerPanel.add(directionPinLabel);
		controllerPanel.add(directionPin);
		controllerPanel.add(invert);
		// controllerPanel end ------------------

		// powerPanel begin ------------------
		JPanel powerPanel = new JPanel();
		powerPanel.setBorder(BorderFactory.createTitledBorder("power"));

		stopButton = new ImageButton("Motor", "stop", this);
		powerPanel.add(stopButton);
		power = new FloatJSlider(-100, 100, 0, 100);
		// Turn on labels at major tick marks.
		power.setMajorTickSpacing(25);
		// power.setMinorTickSpacing(10);
		power.setPaintTicks(true);
		power.setPaintLabels(true);
		powerPanel.add(power);
		powerPanel.add(powerValue);
		// powerPanel end ------------------

		// positionPanel begin ------------------
		JPanel positionPanel = new JPanel();
		positionPanel.setBorder(BorderFactory.createTitledBorder("position"));
		// positionPanel end ------------------

		gc.gridx = 0;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.BOTH;

		display.add(controllerPanel, gc);
		++gc.gridy;
		display.add(powerPanel, gc);
		++gc.gridy;
		display.add(positionPanel, gc);

		controller.addActionListener(this);
		power.addChangeListener(this);
		

	}

	public void incrementPosition(Integer pos) {
		posValue.setText("" + pos);
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub
		subscribe("incrementPosition", "incrementPosition", Integer.class);
	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub
		unsubscribe("incrementPosition", "incrementPosition", Integer.class);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == controller) {

			String newController = (String) controller.getSelectedItem();

			if (newController != null && newController.length() > 0) {
				// myService.send(boundServiceName, "setPort", newPort);
				myMotorController = (MotorController) Runtime.getService(newController).service;
				// TODO - lame - data is not mutable - should be an appropriate
				// method
				// clear then add
				powerPin.removeAllItems();
				directionPin.removeAllItems();

				powerPin.addItem("");
				directionPin.addItem("");

				/*
				 * Vector<Integer> v = myMotorController.getOutputPins();
				 * 
				 * for (int i = 0; i < v.size(); ++i) {
				 * powerPin.addItem(""+v.get(i));
				 * directionPin.addItem(""+v.get(i)); }
				 */

				powerPin.setEnabled(true);
				directionPin.setEnabled(true);
				powerPinLabel.setEnabled(true);
				directionPinLabel.setEnabled(true);
			} else {
				// TODO detach
				powerPin.removeAllItems();
				directionPin.removeAllItems();

				powerPin.setEnabled(false);
				directionPin.setEnabled(false);
				powerPinLabel.setEnabled(false);
				directionPinLabel.setEnabled(false);
			}
		} else if (source == stopButton)
		{
			power.setValue(0);
		}
	}

	@Override
	public void stateChanged(ChangeEvent ce) {
		Object source = ce.getSource();
		if (power == source) {
			// powerValue.setText(power.getValue() + "%");
			powerValue.setText(String.format("%.2f", power.getScaledValue()));
		}
	}

}
