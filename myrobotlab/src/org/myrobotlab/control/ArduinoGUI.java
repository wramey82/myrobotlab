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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.GUI;

import processing.app.Base;

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
public class ArduinoGUI extends ServiceGUI implements ItemListener, ActionListener, WindowListener {

	private Arduino myArduino = null;

	public ArduinoGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	/**
	 * component array - to access all components by name
	 */
	HashMap<String, Component> components = new HashMap<String, Component>();

	static final long serialVersionUID = 1L;

	JTabbedPane tabs = new JTabbedPane();
	
	/*
	 * ---------- Config begin   -------------------------
	 */
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
	/*
	 * ---------- Config end   -------------------------
	 */
	

	/*
	 * ---------- Oscope begin -------------------------
	 */
	SerializableImage sensorImage = null;
	Graphics g = null;
	VideoWidget oscope = null;
	/*
	 * ---------- Oscope end   -------------------------
	 */

	/*
	 * ---------- Editor begin -------------------------
	 */
	RSyntaxTextArea editor = new RSyntaxTextArea();
	RTextScrollPane editorScrollPane = null;
	//DigitalButton fullscreenButton = null;
	DigitalButton uploadButton = null;
	JPanel editorPanel = null;
	GridBagConstraints epgc = new GridBagConstraints();
	/*
	 * ---------- Editor begin -------------------------
	 */

	
	/**
	 * creates an array of graphical pin objects for a particular Arduino board
	 * @return
	 */
	public ArrayList<Pin> makePins() {
		ArrayList<Pin> pins = new ArrayList<Pin>();
		for (int i = 0; i < 20; ++i) {
			Pin p = null;
			String type = (String)types.getSelectedItem();
			if ("Duemilanove".equals(type))
			{
				if (i < 14)
				{
					if  (((i == 3) || (i == 5) || (i == 6) || (i == 9) || (i == 10) || (i == 11))) {
						p = new Pin(myService, boundServiceName, i, true, false);
					} else {
						p = new Pin(myService, boundServiceName, i, false, false);
					}
				} else {
					p = new Pin(myService, boundServiceName, i, false, true);
				}

			} else {
				LOG.error("dont know how to make pins for a " + type);
			}

			if (p != null)
			{
				// set up the listeners
				p.onOff.addActionListener(this);
				p.inOut.addActionListener(this);
				p.activeInActive.addActionListener(this);
				p.trace.addActionListener(this);
				//p.inOut2.addActionListener(this);
				
				pins.add(p);
			}
		}
		return pins;
	}

	public void init() {

		// ---------------- tabs begin ----------------------		
		Dimension size = new Dimension(640, 380);
		tabs.setTabPlacement(JTabbedPane.RIGHT);
		//tabs.setBackground(Color.decode("0x0a4b5e"));
		
		// ---------------- tabs begin ----------------------		
		// --------- configPanel begin ----------------------	
		JPanel main = new JPanel();
		main.setPreferredSize(size);
		JPanel configPanel = new JPanel(new GridBagLayout());
		GridBagConstraints cpgc = new GridBagConstraints();
		
		cpgc.anchor = GridBagConstraints.WEST;

		cpgc.gridx = 0;
		cpgc.gridy = 0;

		configPanel.add(new JLabel("type : "), cpgc);
		++cpgc.gridx;
		configPanel.add(types, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" pwm 5 6 : "), cpgc);
		++cpgc.gridx;
		PWMRate1.setSelectedIndex(2);
		configPanel.add(PWMRate1, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" pwm 9 10 : "), cpgc);
		++cpgc.gridx;
		PWMRate2.setSelectedIndex(2);
		configPanel.add(PWMRate2, cpgc);

		cpgc.gridx = 0;
		++cpgc.gridy;
		configPanel.add(new JLabel("port : "), cpgc);
		++cpgc.gridx;
		configPanel.add(ttyPort, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" serial rate : "), cpgc);
		++cpgc.gridx;
		configPanel.add(baudRate, cpgc);

		++cpgc.gridx;
		configPanel.add(new JLabel(" pwm 3 11 : "), cpgc);
		++cpgc.gridx;
		PWMRate3.setSelectedIndex(2);
		configPanel.add(PWMRate3, cpgc);

		++cpgc.gridy;
		cpgc.gridx = 0;

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

		main.add(configPanel);
		// --------- configPanel end ----------------------		
		// --------- pinPanel begin -----------------------	
		// FIXME - board type specific
		JLayeredPane imageMap = new JLayeredPane();
		imageMap.setPreferredSize(size);

		// set correct arduino image
		JLabel image = new JLabel();
		ImageIcon dPic = Util.getImageIcon("images/service/Arduino/arduino.duemilanove.200.pins.png");
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));
				
		// overlay pin buttons
		pinList = makePins();
		for (int i = 2; i < pinList.size(); ++i) {

			Pin p = pinList.get(i);
			if (i < 14)
			{	// digital pins -----------------
				int yOffSet = 0;
				if (i > 7) yOffSet = 18; // skip pin
				p.inOut.setBounds(406, 297 - 18 * i - yOffSet, 30, 15);			
				imageMap.add(p.inOut, new Integer(2));
				p.onOff.setBounds(436, 297 - 18 * i - yOffSet, 30, 15);			
				imageMap.add(p.onOff, new Integer(2));
				
				if (p.isPWM)
				{
					p.pwmSlider.setBounds(256, 297 - 18 * i - yOffSet, 90, 15);
					imageMap.add(p.pwmSlider, new Integer(2));
					p.data.setBounds(232, 297 - 18 * i - yOffSet, 32, 15);
					p.data.setForeground(Color.white);
					p.data.setBackground(Color.decode("0x0f7391"));
					p.data.setOpaque(true);
					imageMap.add(p.data, new Integer(2));	
				}
			} else {
				// analog pins -----------------
				p.activeInActive.setBounds(11, 208 - 18 * (14-i), 48, 15);			
				imageMap.add(p.activeInActive, new Integer(2));	
				p.data.setBounds(116, 205 - 18 * (14-i), 32, 18);
				p.data.setForeground(Color.white);
				p.data.setBackground(Color.decode("0x0f7391"));
				p.data.setOpaque(true);
				imageMap.add(p.data, new Integer(2));	
			}
		}

		// ------digital pins tab end ------------

		// ------oscope tab begin ----------------
		JPanel oscopePanel = new JPanel(new GridBagLayout());
		GridBagConstraints opgc = new GridBagConstraints();
		
		JPanel tracePanel = new JPanel(new GridBagLayout());

		opgc.fill = GridBagConstraints.HORIZONTAL;
		opgc.gridx = 0;
		opgc.gridy = 0;
		//opgc.anchor = GridBagConstraints.WEST;
		
		//Color gradient = new Color();
		int red = 0x00;
		int gre = 0x16;
		int blu = 0x16;
		
		for (int i = 0; i < pinList.size(); ++i) {
			Pin p = pinList.get(i);
			if (i < 14)
			{	// digital pins -----------------
				p.trace.setText("D " + (i));
				p.trace.onText = "D " + (i);
				p.trace.offText = "D " + (i);
			} else {
				// analog pins ------------------
				p.trace.setText("A " + (i - 14));
				p.trace.onText = "A " + (i - 14);
				p.trace.offText = "A " + (i - 14);
			}
			tracePanel.add(p.trace, opgc);
			p.trace.setBackground(new Color(red,gre,blu));
			p.trace.offBGColor = new Color(red,gre,blu);
			gre+=12;blu+=12;
			++opgc.gridy;
		}
		
		opgc.gridx = 0;
		opgc.gridy = 0;

		oscope = new VideoWidget(boundServiceName, myService, false);
		oscope.init();
		sensorImage = new SerializableImage(new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB), "output");
		g = sensorImage.getImage().getGraphics();
		oscope.displayFrame(sensorImage);
		
		oscopePanel.add(tracePanel, opgc);
		++opgc.gridx;
		oscopePanel.add(oscope.display, opgc);

		// ------oscope tab end ----------------
		// ------editor tab begin ----------------
		editorPanel = new JPanel(new GridBagLayout());
		epgc = new GridBagConstraints();
		epgc.gridx = 0;
		epgc.gridy = 0;
/*		
		fullscreenButton = new DigitalButton(this, 
				"fullscreen",  Color.decode("0x418dd9"), Color.white, 
				"leave fullscreen", Color.red, Color.white, 7);
		uploadButton = new DigitalButton(this, 
				"upload",  Color.decode("0x418dd9"), Color.white, 
				"upload", Color.red, Color.white, 7);
		
		fullscreenButton.addActionListener(this);
*/		
//		uploadButton.addActionListener(this);
		
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
		editorScrollPane = new RTextScrollPane(editor);
		editorScrollPane.setPreferredSize(new Dimension(size.width, size.height));

/*		
		resizer.registerComponent(editor);
		resizer.registerComponent(editorScrollPane);
		resizer.registerComponent(editorPanel);
		resizer.registerComponent(tabs);
*/		
		//xx
		//editorPanel.setPreferredSize(new Dimension(size.width, size.height));
		epgc.anchor = GridBagConstraints.WEST;
		epgc.gridx = 0;
		epgc.gridy = 0;
//		editorPanel.add(fullscreenButton,epgc);
		
//		++epgc.gridx;
//		editorPanel.add(uploadButton,epgc);
		
		// leave where scroll pain needs to be
//		epgc.gridy = 1;
//		epgc.gridx = 0;
//		epgc.gridwidth = 10;
		
		//editorPanel.add(editorScrollPane,epgc);
		
		Base.main(myService.getFrame());
		Base.handleActivated(Base.editor);
		editorPanel.add(Base.editor,epgc);
		
		// ------editor tab end ----------------
		
		tabs.addTab("pins", imageMap);
		tabs.addTab("config", main);
		tabs.addTab("oscope", oscopePanel);
		tabs.addTab("editor", editorPanel);
		display.add(tabs);
		
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
		pin.data.setText(new Integer(p.value).toString());
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
		sendNotifyRequest("publishPin", "publishPin", PinData.class); // TODO - FIXME - sendNotifyRequest should handle single in/out method
		sendNotifyRequest("publishState", "getState", Arduino.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishPin", "publishPin", PinData.class); // TODO - FIXME - sendNotifyRequest should handle single in/out method
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
/*			
			if (fullscreenButton == c)
			{
				if ("fullscreen".equals(cmd))
				{
					JFrame full = new JFrame();
					// icon
					URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
					Toolkit kit = Toolkit.getDefaultToolkit();
					Image img = kit.createImage(url);
					full.setIconImage(img);					
					
					full.setExtendedState(JFrame.MAXIMIZED_BOTH);
					
					editorPanel.remove(editorScrollPane);
					full.getContentPane().add(editorScrollPane);
					//full.pack();
					full.setVisible(true);
					full.addWindowListener(this);
					myService.pack();
					return;
				} 
			}
*/			
			if (uploadButton == c)
			{
				uploadButton.toggle();
				return;
			}
			
			IOData io = new IOData();
			Pin pin = null;
			
			if (b.parent != null)
			{
				io.address = ((Pin)b.parent).pinNumber; // TODO - optimize
				 pin = ((Pin)b.parent);
			}

			if (b.type == Pin.TYPE_ONOFF)
			{
				if ("off".equals(cmd)){
					// now on
					io.value = Pin.HIGH;
					myService.send(boundServiceName, "digitalWrite", io);
					b.toggle();
				} else {
					// now off
					io.value = Pin.LOW;
					myService.send(boundServiceName, "digitalWrite", io);
					b.toggle();
				}
				
			} else if (b.type == Pin.TYPE_INOUT)
			{
				if ("OUTPUT".equals(cmd))
				{
					// is now input
					io.value = Pin.INPUT;
					myService.send(boundServiceName, "pinMode", io); 
					myService.send(boundServiceName, "digitalReadPollStart", io.address);
					b.toggle();
				} else {
					// is now output
					io.value = Pin.OUTPUT;
					myService.send(boundServiceName, "pinMode", io); 
					myService.send(boundServiceName, "digitalReadPollStop", io.address);
					b.toggle();
				}
			} else if (b.type == Pin.TYPE_ACTIVEINACTIVE)
			{
				if ("active".equals(cmd))
				{
					// now inactive
					myService.send(boundServiceName, "analogReadPollingStop", io.address);
					b.toggle();
				} else {
					// now active
					myService.send(boundServiceName, "analogReadPollingStart", io.address);
					b.toggle();
				}
			} else if (b.type == Pin.TYPE_TRACE)
			{
				if (b.isOn)
				{
					// now off

					// if pin is analog && off - switch it on
					if (pin.isAnalog)
					{
						if (pin.activeInActive.isOn)
						{
							myService.send(boundServiceName, "analogReadPollingStop", io.address);
							pin.activeInActive.setOff();
						}
						
					}

					b.toggle();
				} else {
					
					// if pin is digital and on - turn off
					if (pin.onOff.isOn && !pin.isAnalog)
					{
						io.value = Pin.LOW;
						myService.send(boundServiceName, "digitalWrite", io);
						pin.onOff.toggle();		
					}
					
					// if pin is digital - make sure pinmode is input
					if (!pin.inOut.isOn && !pin.isAnalog)
					{
						io.value = Pin.INPUT;
						myService.send(boundServiceName, "pinMode", io); 
						myService.send(boundServiceName, "digitalReadPollStart", io.address);
						pin.inOut.toggle();
					}
					
					// if pin is analog && off - switch it on
					if (pin.isAnalog)
					{
						if (!pin.activeInActive.isOn)
						{
							myService.send(boundServiceName, "analogReadPollingStart", io.address);
							pin.activeInActive.setOn();
						}
						
					}
					
					//myService.send(boundServiceName, "analogReadPollingStart", io.address);
					b.toggle();
				}
			} else {
				LOG.error("unknown pin type " + b.type);
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
	class TraceData {
		Color color = null;
		String label;
		String controllerName;
		int pin;
		int data[] = new int[DATA_WIDTH];
		int index = 0;
		int total = 0;
		int max = 0;
		int min = 1024; // TODO - user input on min/max
		int sum = 0;
		int mean = 0;
	}

	int DATA_WIDTH = 480; // TODO - sync with size
	int DATA_HEIGHT = 380;
	HashMap<Integer, TraceData> traceData = new HashMap<Integer, TraceData>();
	int clearX = 0;

	public void publishPin(PinData pin)
	{
		if (!traceData.containsKey(pin.pin))
		{ 
			TraceData td = new TraceData();
			//td.color = Color.decode("0x0f7391");
			td.color = pinList.get(pin.pin).trace.offBGColor;
			traceData.put(pin.pin, td);
		} 
		
		TraceData t = traceData.get(pin.pin);
		t.index++;
		t.data[t.index] = pin.value;
		++t.total;
		t.sum += pin.value;
		t.mean = t.sum/t.total;

		g.setColor(t.color);
		// g.drawRect(20, t.pin * 15 + 5, 200, 15);
		g.drawLine(t.index, DATA_HEIGHT - t.data[t.index - 1] / 2, t.index,
				DATA_HEIGHT - pin.value / 2);

		// computer min max and mean
		// if different then blank & post to screen
		if (pin.value > t.max)
			t.max = pin.value;
		if (pin.value < t.min)
			t.min = pin.value;

		if (t.index < DATA_WIDTH - 1) {
			clearX = t.index + 1;
			// g.drawLine(clearX, DATA_HEIGHT - t.data[t.index-1]/2, clearX,
			// DATA_HEIGHT - t.data[clearX]/2);
		} else {
			t.index = 0;
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, DATA_WIDTH, DATA_HEIGHT);
			g.setColor(Color.GRAY);
			g.drawLine(0, DATA_HEIGHT - 25, DATA_WIDTH - 1,
					DATA_HEIGHT - 25);
			g.drawString("50", 10, DATA_HEIGHT - 25);
			g.drawLine(0, DATA_HEIGHT - 50, DATA_WIDTH - 1,
					DATA_HEIGHT - 50);
			g.drawString("100", 10, DATA_HEIGHT - 50);
			g.drawLine(0, DATA_HEIGHT - 100, DATA_WIDTH - 1,
					DATA_HEIGHT - 100);
			g.drawString("200", 10, DATA_HEIGHT - 100);
			g.drawLine(0, DATA_HEIGHT - 200, DATA_WIDTH - 1,
					DATA_HEIGHT - 200);
			g.drawString("400", 10, DATA_HEIGHT - 200);
			g.drawLine(0, DATA_HEIGHT - 300, DATA_WIDTH - 1,
					DATA_HEIGHT - 300);
			g.drawString("600", 10, DATA_HEIGHT - 300);
			g.drawLine(0, DATA_HEIGHT - 400, DATA_WIDTH - 1,
					DATA_HEIGHT - 400);
			g.drawString("800", 10, DATA_HEIGHT - 400);

			g.setColor(Color.BLACK);
			g.fillRect(20, t.pin * 15 + 5, 200, 15);
			g.setColor(t.color);
			g.drawString(" min " + t.min + " max " + t.max + " mean "
					+ t.mean + " total " + t.total + " sum " + t.sum, 20, t.pin * 15 + 20);

		}
		
		oscope.displayFrame(sensorImage);
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		editorPanel.add(editorScrollPane, epgc);
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	  /**
	   * Spew the contents of a String object out to a file.
	   */
	  static public void saveFile(String str, File file) throws IOException {
	    File temp = File.createTempFile(file.getName(), null, file.getParentFile());
	    //PApplet.saveStrings(temp, new String[] { str }); FIXME
	    if (file.exists()) {
	      boolean result = file.delete();
	      if (!result) {
	        throw new IOException("Could not remove old version of " +
	                              file.getAbsolutePath());
	    }
	  }
	    boolean result = temp.renameTo(file);
	    if (!result) {
	      throw new IOException("Could not replace " +
	                            file.getAbsolutePath());
	    }
	  }
	
	  /**
	   * Get the number of lines in a file by counting the number of newline
	   * characters inside a String (and adding 1).
	   */
	  static public int countLines(String what) {
	    int count = 1;
	    for (char c : what.toCharArray()) {
	      if (c == '\n') count++;
	    }
	    return count;
	  }
	
}


/*
rawReadMessage.addItemListener(this);
rawReadMsgLength = new JIntegerField();
rawReadMsgLength.setInt(4);
rawReadMsgLength.setEnabled(true);

rawReadMessage.setText(" read raw ");

cpgc.gridx = 0;
++cpgc.gridy;
configPanel.add(rawReadMessage, cpgc);
++cpgc.gridx;
configPanel.add(new JLabel(" msg length "), cpgc);
++cpgc.gridx;
configPanel.add(rawReadMsgLength, cpgc);

// TODO - want to deprecate
rawReadMsgLength.setVisible(false);
rawReadMessage.setVisible(false);
*/
