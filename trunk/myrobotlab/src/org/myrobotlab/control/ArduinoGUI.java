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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.image.Util;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.GUI;

/**
 * Arduino Diecimila http://www.arduino.cc/en/Main/ArduinoBoardDiecimila Serial:
 * 0 (RX) and 1 (TX). Used to receive (RX) and transmit (TX) TTL serial data.
 * These pins are connected to the corresponding pins of the FTDI USB-to-TTL
 * Serial chip. External Interrupts: 2 and 3. These pins can be configured to
 * trigger an interrupt on a low value, a rising or falling edge, or a change in
 * value. See the attachInterrupt() function for details. PWM: 3, 5, 6, 9, 10,
 * and 11. Provide 8-bit PWM output with the analogWrite() function. SPI: 10
 * (SS), 11 (MOSI), 12 (MISO), 13 (SCK). These pins support SPI communication,
 * which, although provided by the underlying hardware, is not currently
 * included in the Arduino language. LED: 13. There is a built-in LED connected
 * to digital pin 13. When the pin is HIGH value, the LED is on, when the pin is
 * LOW, it's off.
 * 
 * TODO - log serial data window
 * 
 */
public class ArduinoGUI extends ServiceGUI implements ItemListener, ActionListener {

	private Arduino myArduino = null;

	public ArduinoGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
		// NOT the way to do it since a call to getState over remote will
		// serialize a new state of Arduino and send it as a parameter
		// below would be stale data
		//myArduino = (Arduino) Runtime.getService(boundServiceName).service;
	}
	
	/**
	 * component array - to access all components by name
	 */
	HashMap<String, Component> components = new HashMap<String, Component>();

	static final long serialVersionUID = 1L;

	ArrayList<Pin> pinList = null;
	JComboBox types = new JComboBox(new String[] { "Duemilanove", "Mega" });
	JComboBox ttyPort = new JComboBox(new String[] { "" });
	JComboBox baudRate = new JComboBox(
			new Integer[] { 300, 1200, 2400, 4800, 9600, 14400, 19200, 28800, 57600, 115200 });
	/**
	 * for pins 6 and 5 1kHz default
	 */
	JComboBox PWMRate1 = new JComboBox(new String[] { "62", "250", "1000", "8000", "64000" });
	/**
	 * for pins 9 and 10 500 hz default
	 */
	JComboBox PWMRate2 = new JComboBox(new String[] { "31", "125", "500", "4000", "32000" });
	/**
	 * for pins 3 and 111 500 hz default
	 */
	JComboBox PWMRate3 = new JComboBox(new String[] { "31", "125", "500", "4000", "32000" });

	JIntegerField rawReadMsgLength = new JIntegerField(4);
	JCheckBox rawReadMessage = new JCheckBox();	
	
	/**
	 * creates an array of graphical pin objects for a particular Arduino board
	 * @return
	 */
	public ArrayList<Pin> makePins() {
		ArrayList<Pin> pins = new ArrayList<Pin>();
		for (int i = 0; i < 14; ++i) {
			Pin p = null;
			String type = (String)types.getSelectedItem();
			if ("Duemilanove".equals(type))
			{
				if  (((i == 3) || (i == 5) || (i == 6) || (i == 9) || (i == 10) || (i == 11))) {
					p = new Pin(myService, boundServiceName, i, true);
				} else {
					p = new Pin(myService, boundServiceName, i, false);
				}
				
				// set initial icon states of buttons
				p.inOut.setIcon(Util.getScaledIcon(Util.getImage("square_unknown.png"), 0.50));
				p.onOff.setIcon(Util.getScaledIcon(Util.getImage("grey.png"), 0.50));
				p.inOut.setActionCommand("");
				p.onOff.setActionCommand("OFF");
				
			} else {
				LOG.error("dont know how to make pins for a " + type);
			}

			if (p != null)
			{
				// set up the listeners
				p.onOff.addActionListener(this);
				p.inOut.addActionListener(this);
				
				pins.add(p);
			}
		}
		return pins;
	}

	public void init() {

		// typePanel begin ---------------------------------
		JPanel typePanel = new JPanel(new GridBagLayout());
		GridBagConstraints gc1 = new GridBagConstraints();

		gc1.anchor = GridBagConstraints.WEST;

		gc1.gridx = 0;
		gc1.gridy = 0;

		typePanel.add(new JLabel("type : "), gc1);
		++gc1.gridx;
		typePanel.add(types, gc1);

		++gc1.gridx;
		typePanel.add(new JLabel(" pwm 5 6 : "), gc1);
		++gc1.gridx;
		PWMRate1.setSelectedIndex(2);
		typePanel.add(PWMRate1, gc1);

		++gc1.gridx;
		typePanel.add(new JLabel(" pwm 9 10 : "), gc1);
		++gc1.gridx;
		PWMRate2.setSelectedIndex(2);
		typePanel.add(PWMRate2, gc1);

		gc1.gridx = 0;
		++gc1.gridy;
		typePanel.add(new JLabel("port : "), gc1);
		++gc1.gridx;
		typePanel.add(ttyPort, gc1);

		++gc1.gridx;
		typePanel.add(new JLabel(" serial rate : "), gc1);
		++gc1.gridx;
		typePanel.add(baudRate, gc1);

		++gc1.gridx;
		typePanel.add(new JLabel(" pwm 3 11 : "), gc1);
		++gc1.gridx;
		PWMRate3.setSelectedIndex(2);
		typePanel.add(PWMRate3, gc1);

		++gc1.gridy;
		gc1.gridx = 0;
		pinList = makePins();

		rawReadMessage.addItemListener(this);
		rawReadMsgLength = new JIntegerField();
		rawReadMsgLength.setInt(4);
		rawReadMsgLength.setEnabled(true);

		rawReadMessage.setText(" read raw ");

		gc1.gridx = 0;
		++gc1.gridy;
		typePanel.add(rawReadMessage, gc1);
		++gc1.gridx;
		typePanel.add(new JLabel(" msg length "), gc1);
		++gc1.gridx;
		typePanel.add(rawReadMsgLength, gc1);
		
		// TODO - want to deprecate
		rawReadMsgLength.setVisible(false);
		rawReadMessage.setVisible(false);

		// typePanel end ------------------------------------------
		display.add(typePanel, gc);
		++gc.gridy;

		// outPinPanel begin -----------------------------------------
		JPanel outPinPanel = new JPanel(new GridBagLayout());
		gc1.gridx = 0;
		gc1.gridy = 0;

		for (int i = 0; i < pinList.size(); ++i) {
			++gc1.gridy;
			outPinPanel.add(pinList.get(i), gc1);
		}
		display.add(outPinPanel, gc);
		// outPinPanel end -----------------------------------------

		ttyPort.setName	("ttyPort");
		types.setName	("types");
		baudRate.setName("baudRate");
		PWMRate1.setName("PWMRate1");
		PWMRate2.setName("PWMRate2");
		PWMRate3.setName("PWMRate3");

		PWMRate1.addActionListener(this);
		PWMRate2.addActionListener(this);
		PWMRate3.addActionListener(this);

		types.addActionListener(this);
		ttyPort.addActionListener(this);
		baudRate.addActionListener(this);

	}

	/**
	 * FIXME - needs to add a route on AttachGUI
	 * and publish from the Arduino service when applicable (when polling)
	 * 
	 * @param p - PinData from serial reads
	 */
	public void readData(PinData p) {
		LOG.info("ArduinoGUI setDataLabel " + p);
		Pin pin = pinList.get(p.pin);
		pin.analogData.setText(new Integer(p.value).toString());
		Integer d = Integer.parseInt(pin.counter.getText());
		d++;
		pin.counter.setText((d).toString());
	}

	public void getState(Arduino myArduino) {
		if (myArduino != null) {
			setPorts(myArduino.portNames);
			baudRate.removeActionListener(this);
			baudRate.setSelectedItem(myArduino.getBaudRate());
			baudRate.addActionListener(this);
		}

	}

	/**
	 * 
	 * FIXME - should be called "displayPorts" or "refreshSystemPorts"
	 * 
	 * setPorts is called by getState - which is called when the Arduino changes
	 * port state is NOT called by the GUI component
	 * 
	 * @param p
	 */
	public void setPorts(ArrayList<String> p) {
		// ttyPort.removeAllItems();

		// ttyPort.addItem(""); // the null port
		// ttyPort.removeAllItems();
		for (int i = 0; i < p.size(); ++i) {
			String n = p.get(i);
			LOG.info(n);
			ttyPort.addItem(n);
		}

		if (myArduino != null) {
			// remove and re-add the action listener
			// because we don't want a recursive event
			// when the Service changes the state
			ttyPort.removeActionListener(this);
			ttyPort.setSelectedItem(myArduino.getPortName());
			ttyPort.addActionListener(this);
		}

	}

	@Override
	public void attachGUI() {
		sendNotifyRequest("publishState", "getState", Arduino.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishState", "getState", Arduino.class);
	}

	@Override
	public void itemStateChanged(ItemEvent item) {
		{
			// called when the button is pressed
			JCheckBox cb = (JCheckBox) item.getSource();
			// Determine status
			boolean isSel = cb.isSelected();
			if (isSel) {
				myService.send(boundServiceName, "setRawReadMsg", true);
				myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
				rawReadMsgLength.setEnabled(false);
			} else {
				myService.send(boundServiceName, "setRawReadMsg", false);
				myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
				rawReadMsgLength.setEnabled(true);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		String cmd = e.getActionCommand(); 
		Component c = (Component) e.getSource();

		// buttons
		if (DigitalButton.class == o.getClass())
		{			
			DigitalButton b = (DigitalButton)o;
			IOData io = new IOData();
			io.address = ((Pin)b.parent).pinNumber;

			if (b.type == Pin.ONOFF)
			{
				if ("OFF".equals(cmd)){
					// now on
					io.value = Pin.HIGH;
					myService.send(boundServiceName, "digitalWrite", io);
					b.setIcon(b.onIcon);
					b.setActionCommand("ON");
				} else {
					// now off
					io.value = Pin.HIGH;
					myService.send(boundServiceName, "digitalWrite", io);
					b.setIcon(b.offIcon);						
					b.setActionCommand("OFF");
				}
				
			} else if (b.type == Pin.INPUTOUTPUT)
			{
				if ("OUTPUT".equals(cmd))
				{
					// is now input
					io.value = Pin.INPUT;
					myService.send(boundServiceName, "pinMode", io); 
					myService.send(boundServiceName, "digitalReadPollStart", io.address);
					b.setIcon(b.onIcon);						
					b.setActionCommand("INPUT");					
				} else {
					// is now output
					io.value = Pin.OUTPUT;
					myService.send(boundServiceName, "pinMode", io); 
					myService.send(boundServiceName, "digitalReadPollStop", io.address);
					b.setIcon(b.offIcon);						
					b.setActionCommand("OUTPUT");
				}
			}
			LOG.info("DigitalButton");
		}  		
		
		// ports & timers
		if (c == ttyPort)
		{
			JComboBox cb = (JComboBox)c;
			String newPort = (String) cb.getSelectedItem();
			myService.send(boundServiceName, "setPort", newPort);
		} else if (c == baudRate) {
			JComboBox cb = (JComboBox)c;
			Integer newBaud = (Integer) cb.getSelectedItem();
			myService.send(boundServiceName, "setBaud", newBaud);
		} else if (c == PWMRate1 || c == PWMRate2 || c == PWMRate3) {
			JComboBox cb = (JComboBox) e.getSource();
			Integer newFrequency = Integer.parseInt((String) cb.getSelectedItem());
			IOData io = new IOData();
			int timerAddr = (c == PWMRate1)?Arduino.TCCR0B:((c == PWMRate2)?Arduino.TCCR0B:Arduino.TCCR2B);
			io.address = timerAddr;
			io.value = newFrequency;
			myService.send(boundServiceName, "setPWMFrequency", io);
		} else if (c == types) {
			LOG.info("type change");
			JComboBox cb = (JComboBox) e.getSource();
			String newType = (String)cb.getSelectedItem();
			// ----------- TODO ---------------------
			// MEGA Type switching

		}
		
	}


}
