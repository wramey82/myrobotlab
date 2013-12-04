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

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;

import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.ServoController;

public class ServoGUI extends ServiceGUI implements ActionListener, MouseListener {

	public final static Logger log = LoggerFactory.getLogger(ServoGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	JLabel boundPos = null;

	JButton attachButton = new JButton("attach");
	JButton updateLimitsButton = new JButton("update limits");

	JSlider slider = new JSlider(0, 180, 90);

	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	JComboBox controller = new JComboBox();
	JComboBox pin = new JComboBox();

	DefaultComboBoxModel controllerModel = new DefaultComboBoxModel();
	DefaultComboBoxModel pinModel = new DefaultComboBoxModel();

	JTextField posMin = new JTextField("0");
	JTextField posMax = new JTextField("180");

	Servo myServo = null;

	SliderListener sliderListener = new SliderListener();

	private class SliderListener implements ChangeListener {
		public void stateChanged(javax.swing.event.ChangeEvent e) {

			boundPos.setText(String.format("%d",slider.getValue()));

			if (myService != null) {
				myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
			} else {
				log.error("can not send message myService is null");
			}
		}
	}


	public ServoGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
		myServo = (Servo) Runtime.getService(boundServiceName);
	}

	public void init() {

		// build input begin ------------------
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		// row 1
		gc.gridx = 0;
		gc.gridy = 0;

		input.add(slider, gc);
		slider.addChangeListener(sliderListener);

		gc.gridwidth = 2;
		gc.gridx = 1;
		++gc.gridy;
		input.add(left, gc);
		++gc.gridx;

		input.add(right, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;

		JPanel control = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		control.add(attachButton, gc);
		++gc.gridx;

		control.add(controller, gc);

		++gc.gridx;
		control.add(new JLabel("pin"), gc); 
		
		++gc.gridx;
		control.add(pin, gc);

		display.add(control);
		display.add(input);

		gc.gridx = 0;
		++gc.gridy;

		JPanel limits = new JPanel();
		limits.add(updateLimitsButton);
		limits.add(new JLabel("min "));
		limits.add(posMin);
		limits.add(new JLabel(" max "));
		limits.add(posMax);

		limits.add(new JLabel(" "));
		boundPos = new JLabel("90");

		limits.add(boundPos);
		
		display.add(limits, gc);

		left.addActionListener(this);
		right.addActionListener(this);
		controller.addActionListener(this);
		attachButton.addActionListener(this);
		pin.addActionListener(this);

		// http://stackoverflow.com/questions/6205433/jcombobox-focus-and-mouse-click-events-not-working
		// jComboBox1.getEditor().getEditorComponent().addMouseListener(...);
		// have to add mouse listener to the MetalComboButton embedded in the
		// JComboBox
		Component[] comps = controller.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].addMouseListener(this); // JComboBox composite listener -
												// have to get all the sub
												// components
			/*
			 * comps[i].addMouseListener(new MouseAdapter() { public void
			 * mouseClicked(MouseEvent me) { System.out.println("clicked"); }
			 * });
			 */
		}
		// controller.getEditor().getEditorComponent().addMouseListener(this);
		controller.setModel(controllerModel);
		pin.setModel(pinModel);

		refreshControllers();
	}

	public void refreshControllers() {
		// FIXME - would newing? a new DefaultComboBoxModel be better?
		controllerModel.removeAllElements();
		// FIXME - get Local services relative to the servo
		controllerModel.addElement("");
		Vector<String> v = Runtime.getServicesFromInterface(ServoController.class.getCanonicalName());
		for (int i = 0; i < v.size(); ++i) {
			controllerModel.addElement(v.get(i));
		}
		controller.invalidate();
		// if isAttached() - select the correct one
	}

	public void getState(final Servo servo) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				controller.setSelectedItem(servo.getControllerName());
				pin.setSelectedItem(servo.getPin());
				if (servo.isAttached()) {
					attachButton.setText("detach");
				} else {
					attachButton.setText("attach");
				}

				if (servo.getPosition() == null) {
					boundPos.setText("");
				} else {
					boundPos.setText(servo.getPosition().toString());
					slider.removeChangeListener(sliderListener);
					slider.setValue(servo.getPosition());
					slider.addChangeListener(sliderListener);
				}
				posMin.setText(servo.getMin().toString());
				posMax.setText(servo.getMax().toString());
			}
		});
	}

	public void attachGUI() {
		subscribe("publishState", "getState", Servo.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Servo.class);
	}

	// GUI's action processing section
	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == controller) {
			String controllerName = (String) controller.getSelectedItem();
			log.info(String.format("controller event %s", controllerName));
			if (controllerName != null && controllerName.length() > 0) {

				@SuppressWarnings("unchecked")
				ArrayList<Pin> pinList = (ArrayList<Pin>) myService.sendBlocking(controllerName, "getPinList");
				log.info("{}", pinList.size());

				pinModel.removeAllElements();
				// FIXME - get Local services relative to the servo
				pinModel.addElement("");

				for (int i = 0; i < pinList.size(); ++i) {
					pinModel.addElement(pinList.get(i).pin);
				}

				pin.invalidate();

			}
		}

		if (o == attachButton) {
			if (attachButton.getText().equals("attach")) {
				myService.send(controller.getSelectedItem().toString(), "servoAttach", boundServiceName, pin.getSelectedItem());
				attachButton.setText("detach");
			} else {
				myService.send(controller.getSelectedItem().toString(), "servoDetach", boundServiceName);
				attachButton.setText("attach");
			}
			return;
		}

		if (o == updateLimitsButton) {
			myService.send(boundServiceName, "setMin", Integer.parseInt(posMin.getText()));
			myService.send(boundServiceName, "setMax", Integer.parseInt(posMax.getText()));
			return;
		}

		if (o == right) {
			slider.setValue(slider.getValue() + 1);
			return;
		}

		if (o == left) {
			slider.setValue(slider.getValue() - 1);
			return;
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("clicked");

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("entered");

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("exited");

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("controller pressed");
		refreshControllers();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("released");
	}

}