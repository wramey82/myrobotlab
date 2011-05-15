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
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;

public class ArduinoGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	final String type = "Diecimila";
	JIntegerField rawReadMsgLength = new JIntegerField(4);

	ArrayList<Pin> pinList = null; 
	JComboBox types = new JComboBox(new String[] { "Duemilanove" }); 
	JComboBox ttyPort = new JComboBox(new String[] { "" }); 
	JComboBox serialRate = new JComboBox(new String[] { "300", "1200", "2400",
			"4800", "9600", "14400", "19200", "28800", "57600", "115200" }); 
	JComboBox PWMRate1 = new JComboBox(new String[] { "62", "250", "1000",
			"8000", "64000" }); // For pins 6 and 5 1kHz default
	JComboBox PWMRate2 = new JComboBox(new String[] { "31", "125", "500",
			"4000", "32000" }); // For pins 9, 10 500 hz default
	JComboBox PWMRate3 = new JComboBox(new String[] { "31", "125", "500",
			"4000", "32000" }); // For pins 3, 11 500 hz default

	/*
	 * Arduino Diecimila http://www.arduino.cc/en/Main/ArduinoBoardDiecimila
	 * Serial: 0 (RX) and 1 (TX). Used to receive (RX) and transmit (TX) TTL
	 * serial data. These pins are connected to the corresponding pins of the
	 * FTDI USB-to-TTL Serial chip. External Interrupts: 2 and 3. These pins can
	 * be configured to trigger an interrupt on a low value, a rising or falling
	 * edge, or a change in value. See the attachInterrupt() function for
	 * details. PWM: 3, 5, 6, 9, 10, and 11. Provide 8-bit PWM output with the
	 * analogWrite() function. SPI: 10 (SS), 11 (MOSI), 12 (MISO), 13 (SCK).
	 * These pins support SPI communication, which, although provided by the
	 * underlying hardware, is not currently included in the Arduino language.
	 * LED: 13. There is a built-in LED connected to digital pin 13. When the
	 * pin is HIGH value, the LED is on, when the pin is LOW, it's off.
	 * 
	 * 
	 * TODO - log serial data window
	 */

	public ArduinoGUI(String name, GUIService myService) {
		super(name, myService);

		initialize();
	}

	public ArrayList<Pin> makePins() {
		ArrayList<Pin> pins = new ArrayList<Pin>();
		for (int i = 0; i < 14; ++i) {
			Pin p = null;
			if (type.compareTo("Diecimila") == 0
					&& ((i == 3) || (i == 5) || (i == 6) || (i == 9)
							|| (i == 10) || (i == 11))) {
				p = new Pin(boundServiceName, i, true);
			} else {
				p = new Pin(boundServiceName, i, false);
			}
			p.setService(myService);
			pins.add(p);
		}
		return pins;
	}

	public void setPorts(ArrayList<String> p) {
		// ttyPort.removeAllItems();
		for (int i = 0; i < p.size(); ++i) {
			String n = p.get(i);
			LOG.info(n);
			ttyPort.addItem(n);
		}

	}

	private void initialize() {

		// build input begin ------------------
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.anchor = GridBagConstraints.EAST;
		gc.fill = GridBagConstraints.HORIZONTAL;

		gc.gridx = 0;
		gc.gridy = 0;
		
		input.add(new JLabel("type : "), gc);
		++gc.gridx;
		input.add(types, gc);

		++gc.gridx;
		input.add(new JLabel(" pwm 5 6 : "), gc);
		++gc.gridx;
		PWMRate1.setSelectedIndex(2);
		input.add(PWMRate1, gc);

		++gc.gridx;
		input.add(new JLabel(" pwm 9 10 : "), gc);
		++gc.gridx;
		PWMRate2.setSelectedIndex(2);
		input.add(PWMRate2, gc);

		gc.gridx = 0;
		++gc.gridy;
		input.add(new JLabel("port : "), gc);
		++gc.gridx;
		// ttyPort.setSelectedIndex(2); - TODO - get config first, poll system
		// second
		input.add(ttyPort, gc);

		++gc.gridx;
		input.add(new JLabel(" serial rate : "), gc);
		++gc.gridx;
		input.add(serialRate, gc);

		++gc.gridx;
		input.add(new JLabel(" pwm 3 11 : "), gc);
		++gc.gridx;
		PWMRate3.setSelectedIndex(2);
		input.add(PWMRate3, gc);

		++gc.gridy;
		gc.gridx = 0;
		pinList = makePins();

		gc.gridwidth = 8;
		for (int i = 0; i < pinList.size(); ++i) {
			++gc.gridy;
			input.add(pinList.get(i), gc);
		}

		
		
		Action rawReadMsgAction = new AbstractAction("CheckBox Label") {
		      /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			// called when the button is pressed
		      public void actionPerformed(ActionEvent evt) {
		        JCheckBox cb = (JCheckBox) evt.getSource();
		        // Determine status
		        boolean isSel = cb.isSelected();
		        if (isSel) {
			          myService.send(boundServiceName, "setRawReadMsg", true);
			          myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
		        } else {
				      myService.send(boundServiceName, "setRawReadMsg", false);
				      myService.send(boundServiceName, "setReadMsgLength", rawReadMsgLength.getInt());
		        }
		      }
		    };
		    

		  JCheckBox rawReadMessage = new JCheckBox(rawReadMsgAction);
		  rawReadMsgLength = new JIntegerField();
		  rawReadMsgLength.setInt(4);
		  rawReadMsgLength.setEnabled(false);

		  gc.gridx = 0;
		  gc.gridy = 0;

		  
		  JPanel msgPanel = new JPanel();
		  rawReadMessage.setText(" read raw ");
		  msgPanel.add(rawReadMessage);
		  msgPanel.add(new JLabel(" msg length "));
		  msgPanel.add(rawReadMsgLength);
		  
		  
		  
		  display.add(msgPanel, gc);
		  ++gc.gridy;
		  display.add(input, gc);
		  
		// TODO - set up routing of messages from service - to catch events TODO
		// attachGUI
		sendNotifyRequest("publishPin", "setDataLabel", PinData.class);

		PopupMenuListener PortPopUpListener = new PopupMenuListener() {

			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
			}

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				// myService.send(boundServiceName, "getPorts"); // TODO - needs
				// to block
				// ArrayList<String> p =
				// (ArrayList<String>)myService.sendBlocking(boundServiceName,
				// "getPorts", null);

				/*
				 * try { Thread.sleep(1000); } catch (InterruptedException e) {
				 * // TODO Auto-generated catch block e.printStackTrace(); }
				 */
				// TODO - lame use BlockingQueue & SendBlocking - with timeout
				// value - populate with error on timeout
				/*
				 * ttyPort.removeAllItems(); for (int i = 0; i < p.size();++i) {
				 * ttyPort.addItem(p.get(i)); }
				 */

			}

		};

		ActionListener PortActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				String newPort = (String) cb.getSelectedItem();
				if (newPort != null && newPort.length() > 0) {
					myService.send(boundServiceName, "setSerialPort", newPort);
				}

			}
		};

		ActionListener PWMRate1ActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				Integer newFrequency = Integer.parseInt((String) cb
						.getSelectedItem());
				IOData io = new IOData();
				io.address = Arduino.TCCR0B;
				io.value = newFrequency;
				myService.send(boundServiceName, "setPWMFrequency", io);

			}
		};

		ActionListener PWMRate2ActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				Integer newFrequency = Integer.parseInt((String) cb
						.getSelectedItem());
				IOData io = new IOData();
				io.address = Arduino.TCCR1B;
				io.value = newFrequency;
				myService.send(boundServiceName, "setPWMFrequency", io);

			}
		};

		ActionListener PWMRate3ActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				Integer newFrequency = Integer.parseInt((String) cb
						.getSelectedItem());
				IOData io = new IOData();
				io.address = Arduino.TCCR2B;
				io.value = newFrequency;
				myService.send(boundServiceName, "setPWMFrequency", io);

			}
		};

		PWMRate1.addActionListener(PWMRate1ActionListener);
		PWMRate2.addActionListener(PWMRate2ActionListener);
		PWMRate3.addActionListener(PWMRate3ActionListener);

		ttyPort.addPopupMenuListener(PortPopUpListener);
		ttyPort.addActionListener(PortActionListener);

	}

	public void setDataLabel(PinData p) {
		LOG.info("ArduinoGUI setDataLabel " + p);
		Pin pin = pinList.get(p.pin);
		pin.dataLabel.setText(new Integer(p.value).toString());
		Integer d = Integer.parseInt(pin.counter.getText());
		d++;
		pin.counter.setText((d).toString());
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

		// listen for the get ports on the Arduino
		sendNotifyRequest("getPorts", "setPorts", ArrayList.class);
		sendNotifyRequest("getCurrentPort", "setCurrentPort", String.class);

		myService.send(boundServiceName, "getPorts");
		myService.send(boundServiceName, "getCurrentPort");

	}

	public void setCurrentPort(String p) {
		if (p == null || p.length() == 0)
			return;

		boolean found = false;
		for (int i = 0; i < ttyPort.getItemCount(); ++i) {
			if (((String) ttyPort.getItemAt(i)).compareTo(p) == 0) {
				found = true;
				break;
			}
		}

		if (!found) {
			ttyPort.addItem(p);
		}

		ttyPort.setSelectedItem(p);
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("getPorts", "setPorts", ArrayList.class);
		removeNotifyRequest("getCurrentPort", "setCurrentPort", String.class);

	}

}
