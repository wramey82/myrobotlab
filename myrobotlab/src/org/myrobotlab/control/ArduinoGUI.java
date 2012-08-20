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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.myrobotlab.arduino.compiler.Preferences2;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.GUI;

/**
 * Arduino Diecimila http://www.arduino.cc/en/Main/ArduinoBoardDiecimila Serial:
 * 0 (RX) and 1 (TX). Used to receive (RX) and transmit (TX) TTL serial data.
 * These pins are connected to the corresponding pins of the FTDI USB-to-TTL
 * Serial chip.attachGUI External Interrupts: 2 and 3. These pins can be
 * configured to trigger an interrupt on a low value, a rising or falling edge,
 * or a change in value. See the attachInterrupt() function for details. PWM: 3,
 * 5, 6, 9, 10, and 11. Provide 8-bit PWM output with the analogWrite()
 * function. SPI: 10 (SS), 11 (MOSI), 12 (MISO), 13 (SCK). These pins support
 * SPI communication, which, although provided by the underlying hardware, is
 * not currently included in the Arduino language. LED: 13. There is a built-in
 * LED connected to digital pin 13. When the pin is HIGH value, the LED is on,
 * when the pin is LOW, it's off.
 * 
 * TODO - log serial data window
 * 
 */
public class ArduinoGUI extends ServiceGUI implements ItemListener,
		ActionListener {

	public ArduinoGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	/**
	 * component array - to access all components by name
	 */
	public Arduino myArduino;
	HashMap<String, Component> components = new HashMap<String, Component>();

	static final long serialVersionUID = 1L;

	JTabbedPane tabs = new JTabbedPane();

	/*
	 * ---------- Pins begin -------------------------
	 */

	JLayeredPane imageMap;
	/*
	 * ---------- Pins end -------------------------
	 */

	/*
	 * ---------- Config begin -------------------------
	 */
	ArrayList<Pin> pinList = null;
	JComboBox serialDevice = new JComboBox(new String[] { "" });
	JComboBox baudRate = new JComboBox(new Integer[] { 300, 1200, 2400, 4800,
			9600, 14400, 19200, 28800, 57600, 115200 }); // TODO REMOVE
	/**
	 * for pins 6 and 5 1kHz default
	 */
	JComboBox PWMRate1 = new JComboBox(new String[] { "62", "250", "1000",
			"8000", "64000" });
	/**
	 * for pins 9 and 10 500 hz default
	 */
	JComboBox PWMRate2 = new JComboBox(new String[] { "31", "125", "500",
			"4000", "32000" });
	/**
	 * for pins 3 and 111 500 hz default
	 */
	JComboBox PWMRate3 = new JComboBox(new String[] { "31", "125", "500",
			"4000", "32000" });

	JIntegerField rawReadMsgLength = new JIntegerField(4);
	JCheckBox rawReadMessage = new JCheckBox();
	/*
	 * ---------- Config end -------------------------
	 */

	/*
	 * ---------- Oscope begin -------------------------
	 */
	SerializableImage sensorImage = null;
	Graphics g = null;
	VideoWidget oscope = null;
	JPanel oscopePanel = null;
	/*
	 * ---------- Oscope end -------------------------
	 */

	/*
	 * ---------- Editor begin -------------------------
	 */
	//Base arduinoIDE;
	DigitalButton uploadButton = null;
	JPanel editorPanel = null;
	GridBagConstraints epgc = new GridBagConstraints();
	Dimension size = new Dimension(620, 442);

	/*
	 * ---------- Editor begin -------------------------
	 */

	public void init() {

		// ---------------- tabs begin ----------------------
		tabs.setTabPlacement(JTabbedPane.RIGHT);

		// ---------------- tabs begin ----------------------
		// --------- configPanel begin ----------------------
		// TODO - deprecate completely
		JPanel configPanel = new JPanel(new GridBagLayout());
		GridBagConstraints cpgc = new GridBagConstraints();

		cpgc.anchor = GridBagConstraints.WEST;

		cpgc.gridx = 0;
		cpgc.gridy = 0;

		configPanel.add(new JLabel("type : "), cpgc);
		++cpgc.gridx;

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
		configPanel.add(serialDevice, cpgc);

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

		serialDevice.setName("serialDevice");
		// boardType.setName("boardType");
		baudRate.setName("baudRate");
		PWMRate1.setName("PWMRate1");
		PWMRate2.setName("PWMRate2");
		PWMRate3.setName("PWMRate3");

		PWMRate1.addActionListener(this);
		PWMRate2.addActionListener(this);
		PWMRate3.addActionListener(this);

		// boardType.addActionListener(this);
		serialDevice.addActionListener(this);
		baudRate.addActionListener(this);

		// --------- configPanel end ----------------------
		// --------- pinPanel begin -----------------------
		//arduinoIDE = Base.getBase(this, myService.getFrame());
		//arduinoIDE.handleActivated(arduinoIDE.editor);

		//Map<String, String> boardPreferences = Base.getBoardPreferences();

		getPinPanel(); // FIXME - need preferences ?!?!
		// ------digital pins tab end ------------

		// ------oscope tab begin ----------------
		getOscopePanel();

		// ------oscope tab end ----------------
		// ------editor 2 ! begin ---------------
		getEditorPanel();
		// ------editor 2 ! end ---------------
		// ------editor tab begin ----------------
		// TODO - rebuild like others
		editorPanel = new JPanel(new GridBagLayout());
		epgc = new GridBagConstraints();
		epgc.gridx = 0;
		epgc.gridy = 0;
		epgc.anchor = GridBagConstraints.WEST;
		epgc.gridx = 0;
		epgc.gridy = 0;
		// ------editor tab end ----------------

		JFrame top = myService.getFrame();
		/*
		 * tabs.addTab("pins", imageMap);
		 * tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top,
		 * tabs, imageMap, boundServiceName, "pins"));
		 */

		tabs.addTab("config", configPanel);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top,
				tabs, configPanel, boundServiceName, "config"));

		/*
		 * tabs.addTab("oscope", oscopePanel);
		 * tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top,
		 * tabs, oscopePanel, boundServiceName, "oscope"));
		 */
/*
		tabs.addTab("editor", arduinoIDE.editor);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(top,
				tabs, arduinoIDE.editor, boundServiceName, "editor"));
*/
		display.add(tabs);

		// arduinoIDE editor status setup FIXME - lame
		//arduinoIDE.editor.status.setup();

		tabs.setSelectedIndex(0);
	}

	public JLayeredPane getPinPanel() {
		//Map<String, String> boardPreferences = Base.getBoardPreferences();
		//String boardName = boardPreferences.get("name");

		//if (boardName.contains("Mega")) {
			return getMegaPanel();
		//} else {
		//	return getDuemilanovePanel();
		//}

	}

	public void readData(PinData p) {
		log.info("ArduinoGUI setDataLabel " + p);
		Pin pin = pinList.get(p.pin);
		pin.data.setText(Integer.valueOf(p.value).toString());
		Integer d = Integer.parseInt(pin.counter.getText());
		d++;
		pin.counter.setText((d).toString());
	}

	class SerialMenuListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) e.getSource();
			myService.send(boundServiceName, "setSerialDevice", checkbox
					.getText(), 57600, 8, 1, 0);
			// setSerialDevice(String name, int rate, int databits, int
			// stopbits, int parity)
			// selectSerialPort(((JCheckBoxMenuItem) e.getSource()).getText());
			// base.onBoardOrPortChange();
		}
	}

	SerialMenuListener serialMenuListener = new SerialMenuListener();

	public void getState(Arduino data) {
		if (data != null) {
			myArduino = data; // FIXME - super updates registry state ?

			if (serialDevice != null) {
				serialDevice.removeActionListener(this);
				setPorts(myArduino.getSerialDeviceNames());
				serialDevice.setSelectedItem(myArduino.getPortName());
				serialDevice.addActionListener(this);
			}

			if (baudRate != null && serialDevice != null) {
				baudRate.removeActionListener(this);
				// baudRate.setSelectedItem(myArduino.getSerialDevice().getBaudRate());
				// // FIXME = just myArduino.getBaudRate()
				baudRate.addActionListener(this);
			}

			// TODO - iterate through others and unselect them
			// FIXME - do we need to remove action listeners?
			/*
			if (myArduino.getPortName() != null) {
				log.info(arduinoIDE.editor.serialCheckBoxMenuItems.get(
						myArduino.getPortName()).isSelected());
				arduinoIDE.editor.serialCheckBoxMenuItems.get(
						myArduino.getPortName()).setSelected(true);
				arduinoIDE.editor.serialCheckBoxMenuItems.get(
						myArduino.getPortName()).doClick();
			}
			*/

			// update panels based on state change
			getPinPanel();
			getOscopePanel();

			// editor2Panel.ser
			// createSerialDeviceMenu(m)
			for (int i = 0; i < myArduino.portNames.size(); ++i) {
				JCheckBoxMenuItem device = new JCheckBoxMenuItem(
						myArduino.portNames.get(i));
				device.addActionListener(serialMenuListener);
				editor.serialDeviceMenu.add(device);
				// rbMenuItem = new JCheckBoxMenuItem(curr_port,
				// curr_port.equals(Preferences2.get("serial.port")));
				// rbMenuItem.addActionListener(serialMenuListener);
			}
			
			String statusString = Preferences2.get("board") + " " + Preferences2.get("serial.port");
			editor.setStatus(statusString);

		}

	}

	/*
	public void openSketchInGUI(String path) {
		arduinoIDE.handleOpenReplace(path);
	}
	*/

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
		serialDevice.removeAllItems();
		serialDevice.addItem("");// null port
		for (int i = 0; i < p.size(); ++i) {
			String n = p.get(i);
			log.debug(n);
			serialDevice.addItem(n);
		}

	}

	/*
	public void uploadSketchFromGUI(Boolean b) {
		// arduinoIDE.editor.handleUpload(b);
		arduinoIDE.editor.handleBlockingUpload(b);
	}
	*/

	@Override
	public void attachGUI() {
		subscribe("publishPin", "publishPin", PinData.class);
		subscribe("publishState", "getState", Arduino.class);
		subscribe("openSketchInGUI", "openSketchInGUI", String.class);
		subscribe("uploadSketchFromGUI", "uploadSketchFromGUI", Boolean.class);

		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishPin", "publishPin", PinData.class);
		unsubscribe("publishState", "getState", Arduino.class);
		unsubscribe("openSketchInGUI", "openSketchInGUI", String.class);
		unsubscribe("uploadSketchFromGUI", "uploadSketchFromGUI", Boolean.class);
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
				myService.send(boundServiceName, "setReadMsgLength",
						rawReadMsgLength.getInt());
				rawReadMsgLength.setEnabled(false);
			} else {
				myService.send(boundServiceName, "setRawReadMsg", false);
				myService.send(boundServiceName, "setReadMsgLength",
						rawReadMsgLength.getInt());
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
		if (DigitalButton.class == o.getClass()) {
			DigitalButton b = (DigitalButton) o;

			if (uploadButton == c) {
				uploadButton.toggle();
				return;
			}

			IOData io = new IOData();
			Pin pin = null;

			if (b.parent != null) {
				io.address = ((Pin) b.parent).pinNumber;
				pin = ((Pin) b.parent);
			}

			if (b.type == Pin.TYPE_ONOFF) {
				if ("off".equals(cmd)) {
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

			} else if (b.type == Pin.TYPE_INOUT) {
				if ("out".equals(cmd)) {
					// is now input
					io.value = Pin.INPUT;
					myService.send(boundServiceName, "pinMode", io);
					myService.send(boundServiceName, "digitalReadPollStart",
							io.address);
					b.toggle();
				} else if ("in".equals(cmd)) {
					// is now output
					io.value = Pin.OUTPUT;
					myService.send(boundServiceName, "pinMode", io);
					myService.send(boundServiceName, "digitalReadPollStop",
							io.address);
					b.toggle();
				} else {
					log.error(String.format("unknown digital pin cmd %s", cmd));
				}
			} else if (b.type == Pin.TYPE_TRACE
					|| b.type == Pin.TYPE_ACTIVEINACTIVE) {

				// digital pin
				if (!pin.isAnalog) {
					if (!pin.inOut.isOn) { // pin is off turn it on
						io.value = Pin.INPUT;
						myService.send(boundServiceName, "pinMode", io);
						myService.send(boundServiceName,
								"digitalReadPollStart", io.address);
						pin.inOut.setOn(); // in
						b.setOn();
					} else {
						io.value = Pin.OUTPUT;
						myService.send(boundServiceName, "pinMode", io);
						myService.send(boundServiceName, "digitalReadPollStop",
								io.address);
						pin.inOut.setOff();// out
						b.setOff();
					}
				} else {
					io.value = Pin.INPUT;
					myService.send(boundServiceName, "pinMode", io);
					// analog pin
					if (pin.activeInActive.isOn) {
						myService.send(boundServiceName,
								"analogReadPollingStop", io.address);
						pin.activeInActive.setOff();
						pin.trace.setOff();
						b.setOff();
					} else {
						myService.send(boundServiceName,
								"analogReadPollingStart", io.address);
						pin.activeInActive.setOn();
						pin.trace.setOn();
						b.setOn();
					}
				}

			} else {
				log.error("unknown pin type " + b.type);
			}

			log.info("DigitalButton");
		}

		// ports & timers
		if (c == serialDevice) {
			JComboBox cb = (JComboBox) c;
			String newPort = (String) cb.getSelectedItem();
			myService.send(boundServiceName, "setPort", newPort);
		} else if (c == baudRate) {
			JComboBox cb = (JComboBox) c;
			Integer newBaud = (Integer) cb.getSelectedItem();
			myService.send(boundServiceName, "setBaud", newBaud);
		} else if (c == PWMRate1 || c == PWMRate2 || c == PWMRate3) {
			JComboBox cb = (JComboBox) e.getSource();
			Integer newFrequency = Integer.parseInt((String) cb
					.getSelectedItem());
			IOData io = new IOData();
			int timerAddr = (c == PWMRate1) ? Arduino.TCCR0B
					: ((c == PWMRate2) ? Arduino.TCCR0B : Arduino.TCCR2B);
			io.address = timerAddr;
			io.value = newFrequency;
			myService.send(boundServiceName, "setPWMFrequency", io);
		}
		/*
		 * else if (c == boardType) { log.info("type change"); JComboBox cb =
		 * (JComboBox) e.getSource(); String newType = (String)
		 * cb.getSelectedItem(); getPinPanel(); // ----------- TODO
		 * --------------------- // MEGA Type switching
		 * 
		 * }
		 */

	}

	public void closeSerialDevice() {
		myService.send(boundServiceName, "closeSerialDevice");
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
		int traceStart = 0;
	}

	int DATA_WIDTH = size.width;
	int DATA_HEIGHT = size.height;
	HashMap<Integer, TraceData> traceData = new HashMap<Integer, TraceData>();
	int clearX = 0;
	int lastTraceXPos = 0;

	public void publishPin(PinData pin) {
		if (!traceData.containsKey(pin.pin)) {
			TraceData td = new TraceData();
			float gradient = 1.0f / pinList.size();
			Color color = new Color(Color.HSBtoRGB((pin.pin * (gradient)),
					0.8f, 0.7f));
			td.color = color;
			traceData.put(pin.pin, td);
			td.index = lastTraceXPos;
		}

		TraceData t = traceData.get(pin.pin);
		t.index++;
		lastTraceXPos = t.index;
		t.data[t.index] = pin.value;
		++t.total;
		t.sum += pin.value;
		t.mean = t.sum / t.total;

		g.setColor(t.color);
		if (pin.type == PinData.DIGITAL_VALUE) {
			int yoffset = pin.pin * 15 + 35;
			int quantum = -10;
			g.drawLine(t.index, t.data[t.index - 1] * quantum + yoffset,
					t.index, pin.value * quantum + yoffset);
		} else if (pin.type == PinData.ANALOG_VALUE) {
			g.drawLine(t.index, DATA_HEIGHT - t.data[t.index - 1] / 2, t.index,
					DATA_HEIGHT - pin.value / 2);
		} else {
			log.error("dont know how to display pin data method");
		}

		// computer min max and mean
		// if different then blank & post to screen
		if (pin.value > t.max)
			t.max = pin.value;
		if (pin.value < t.min)
			t.min = pin.value;

		if (t.index < DATA_WIDTH - 1) {
			clearX = t.index + 1;
		} else {
			// TODO - when hit marks all startTracePos - cause the screen is
			// blank - must iterate through all
			t.index = 0;

			clearScreen();
			drawGrid();

			g.setColor(Color.BLACK);
			g.fillRect(20, t.pin * 15 + 5, 200, 15);
			g.setColor(t.color);

			g.drawString(String.format("min %d max %d mean %d ", t.min, t.max,
					t.mean), 20, t.pin * 15 + 20);

			t.total = 0;
			t.sum = 0;

		}

		oscope.displayFrame(sensorImage);
	}

	public void clearScreen() // TODO - static - put in oscope/image package
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, DATA_WIDTH, DATA_HEIGHT); // TODO - ratio - to expand
													// or reduce view
	}

	public void drawGrid() // TODO - static & put in oscope/image package
	{
		g.setColor(Color.DARK_GRAY);
		g.drawLine(0, DATA_HEIGHT - 25, DATA_WIDTH - 1, DATA_HEIGHT - 25);
		g.drawString("50", 10, DATA_HEIGHT - 25);
		g.drawLine(0, DATA_HEIGHT - 50, DATA_WIDTH - 1, DATA_HEIGHT - 50);
		g.drawString("100", 10, DATA_HEIGHT - 50);
		g.drawLine(0, DATA_HEIGHT - 100, DATA_WIDTH - 1, DATA_HEIGHT - 100);
		g.drawString("200", 10, DATA_HEIGHT - 100);
		g.drawLine(0, DATA_HEIGHT - 200, DATA_WIDTH - 1, DATA_HEIGHT - 200);
		g.drawString("400", 10, DATA_HEIGHT - 200);
		g.drawLine(0, DATA_HEIGHT - 300, DATA_WIDTH - 1, DATA_HEIGHT - 300);
		g.drawString("600", 10, DATA_HEIGHT - 300);
		g.drawLine(0, DATA_HEIGHT - 400, DATA_WIDTH - 1, DATA_HEIGHT - 400);
		g.drawString("800", 10, DATA_HEIGHT - 400);

	}

	/**
	 * Spew the contents of a String object out to a file.
	 */
	static public void saveFile(String str, File file) throws IOException {
		File temp = File.createTempFile(file.getName(), null,
				file.getParentFile());
		// PApplet.saveStrings(temp, new String[] { str }); FIXME
		if (file.exists()) {
			boolean result = file.delete();
			if (!result) {
				throw new IOException("Could not remove old version of "
						+ file.getAbsolutePath());
			}
		}
		boolean result = temp.renameTo(file);
		if (!result) {
			throw new IOException("Could not replace " + file.getAbsolutePath());
		}
	}

	/**
	 * Get the number of lines in a file by counting the number of newline
	 * characters inside a String (and adding 1).
	 */
	static public int countLines(String what) {
		int count = 1;
		for (char c : what.toCharArray()) {
			if (c == '\n')
				count++;
		}
		return count;
	}

	public JLayeredPane getMegaPanel() {

		if (imageMap != null) {
			tabs.remove(imageMap);
		}

		pinList = new ArrayList<Pin>();
		imageMap = new JLayeredPane();
		imageMap.setPreferredSize(size);

		// set correct arduino image
		JLabel image = new JLabel();

		ImageIcon dPic = Util
				.getImageIcon("images/service/Arduino/mega.200.pins.png");
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));

		for (int i = 0; i < 70; ++i) {

			Pin p = null;

			if (i > 1 && i < 14) { // pwm pins -----------------
				p = new Pin(myService, boundServiceName, i, true, false, true);
				int xOffSet = 0;
				if (i > 7)
					xOffSet = 18; // skip pin
				p.inOut.setBounds(252 - 18 * i - xOffSet, 30, 15, 30);
				imageMap.add(p.inOut, new Integer(2));
				p.onOff.setBounds(252 - 18 * i - xOffSet, 0, 15, 30);
				// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
				imageMap.add(p.onOff, new Integer(2));

				if (p.isPWM) {
					p.pwmSlider.setBounds(252 - 18 * i - xOffSet, 75, 15, 90);
					imageMap.add(p.pwmSlider, new Integer(2));
					p.data.setBounds(252 - 18 * i - xOffSet, 180, 32, 15);
					p.data.setForeground(Color.white);
					p.data.setBackground(Color.decode("0x0f7391"));
					p.data.setOpaque(true);
					imageMap.add(p.data, new Integer(2));
				}
			} else if (i < 54 && i > 21) {
				// digital pin racks
				p = new Pin(myService, boundServiceName, i, false, false, false);

				if (i != 23 && i != 25 && i != 27 && i != 29) {
					if ((i % 2 == 0)) {
						// first rack of digital pins
						p.inOut.setBounds(472, 55 + 9 * (i - 21), 30, 15);
						imageMap.add(p.inOut, new Integer(2));
						p.onOff.setBounds(502, 55 + 9 * (i - 21), 30, 15);
						// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
						imageMap.add(p.onOff, new Integer(2));
					} else {
						// second rack of digital pins
						p.inOut.setBounds(567, 45 + 9 * (i - 21), 30, 15);
						imageMap.add(p.inOut, new Integer(2));
						p.onOff.setBounds(597, 45 + 9 * (i - 21), 30, 15);
						// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
						imageMap.add(p.onOff, new Integer(2));
					}
				}

			} else if (i > 53) {
				p = new Pin(myService, boundServiceName, i, false, true, true);
				// analog pins -----------------
				int xOffSet = 0;
				if (i > 61)
					xOffSet = 18; // skip pin
				p.activeInActive.setBounds(128 + 18 * (i - 52) + xOffSet, 392,
						15, 48);
				imageMap.add(p.activeInActive, new Integer(2));
				/*
				 * bag data at the moment - go look at the Oscope
				 * p.data.setBounds(208 + 18 * (i - 52) + xOffSet, 260, 32, 18);
				 * p.data.setForeground(Color.white);
				 * p.data.setBackground(Color.decode("0x0f7391"));
				 * p.data.setOpaque(true); imageMap.add(p.data, new Integer(2));
				 */
			} else {
				p = new Pin(myService, boundServiceName, i, false, false, false);
			}

			// set up the listeners
			p.onOff.addActionListener(this);
			p.inOut.addActionListener(this);
			p.activeInActive.addActionListener(this);
			p.trace.addActionListener(this);
			// p.inOut2.addActionListener(this);

			pinList.add(p);

		}

		JFrame top = myService.getFrame();
		tabs.insertTab("pins", null, imageMap, "pin panel", 0);
		tabs.setTabComponentAt(0, new TabControl(top, tabs, imageMap,
				boundServiceName, "pins"));

		return imageMap;
	}

	// public

	public JLayeredPane getDuemilanovePanel() {

		if (imageMap != null) {
			tabs.remove(imageMap);
		}

		imageMap = new JLayeredPane();
		imageMap.setPreferredSize(size);
		pinList = new ArrayList<Pin>();

		// set correct arduino image
		JLabel image = new JLabel();

		ImageIcon dPic = Util
				.getImageIcon("images/service/Arduino/arduino.duemilanove.200.pins.png");
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));

		for (int i = 0; i < 20; ++i) {

			Pin p = null;
			if (i < 14) {
				if (((i == 3) || (i == 5) || (i == 6) || (i == 9) || (i == 10) || (i == 11))) {
					p = new Pin(myService, boundServiceName, i, true, false,
							false);
				} else {
					p = new Pin(myService, boundServiceName, i, false, false,
							false);
				}
			} else {
				p = new Pin(myService, boundServiceName, i, false, true, false);
			}

			// set up the listeners
			p.onOff.addActionListener(this);
			p.inOut.addActionListener(this);
			p.activeInActive.addActionListener(this);
			p.trace.addActionListener(this);
			// p.inOut2.addActionListener(this);

			pinList.add(p);

			if (i < 2) {
				continue;
			}
			if (i < 14) { // digital pins -----------------
				int yOffSet = 0;
				if (i > 7)
					yOffSet = 18; // skip pin
				p.inOut.setBounds(406, 297 - 18 * i - yOffSet, 30, 15);
				imageMap.add(p.inOut, new Integer(2));
				p.onOff.setBounds(436, 297 - 18 * i - yOffSet, 30, 15);
				// p.onOff.getLabel().setUI(new VerticalLabelUI(true));
				imageMap.add(p.onOff, new Integer(2));

				if (p.isPWM) {
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
				p.activeInActive.setBounds(11, 208 - 18 * (14 - i), 48, 15);
				imageMap.add(p.activeInActive, new Integer(2));
				p.data.setBounds(116, 205 - 18 * (14 - i), 32, 18);
				p.data.setForeground(Color.white);
				p.data.setBackground(Color.decode("0x0f7391"));
				p.data.setOpaque(true);
				imageMap.add(p.data, new Integer(2));
			}
		}

		JFrame top = myService.getFrame();
		tabs.insertTab("pins", null, imageMap, "pin panel", 0);
		tabs.setTabComponentAt(0, new TabControl(top, tabs, imageMap,
				boundServiceName, "pins"));
		return imageMap;
	}

	public JPanel getOscopePanel() {
		if (oscopePanel != null) {
			tabs.remove(oscopePanel);
		}

		oscopePanel = new JPanel(new GridBagLayout());
		GridBagConstraints opgc = new GridBagConstraints();

		JPanel tracePanel = new JPanel(new GridBagLayout());

		opgc.fill = GridBagConstraints.HORIZONTAL;
		opgc.gridx = 0;
		opgc.gridy = 0;
		float gradient = 1.0f / pinList.size();

		// pinList.size() mega 60 deuo 20
		for (int i = 0; i < pinList.size(); ++i) {
			Pin p = pinList.get(i);
			if (!p.isAnalog) { // digital pins -----------------
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
			Color hsv = new Color(Color.HSBtoRGB((i * (gradient)), 0.8f, 0.7f));
			p.trace.setBackground(hsv);
			p.trace.offBGColor = hsv;
			++opgc.gridy;
			if (opgc.gridy % 20 == 0) {
				opgc.gridy = 0;
				++opgc.gridx;
			}
		}

		opgc.gridx = 0;
		opgc.gridy = 0;

		oscope = new VideoWidget(boundServiceName, myService, false);
		oscope.init();
		sensorImage = new SerializableImage(new BufferedImage(size.width,
				size.height, BufferedImage.TYPE_INT_RGB), "output");
		g = sensorImage.getImage().getGraphics();
		oscope.displayFrame(sensorImage);

		oscopePanel.add(tracePanel, opgc);
		++opgc.gridx;
		oscopePanel.add(oscope.display, opgc);

		JFrame top = myService.getFrame();
		tabs.insertTab("oscope", null, oscopePanel, "oscope panel", 0);
		tabs.setTabComponentAt(0, new TabControl(top, tabs, oscopePanel,
				boundServiceName, "oscope"));
		myService.getFrame().pack();
		return oscopePanel;
	}

	JPanel editor2Panel = null;
	EditorArduino editor = null;

	public JPanel getEditorPanel() {
		if (editor2Panel != null) {
			tabs.remove(editor2Panel);
		}

		editor2Panel = new JPanel(new GridBagLayout());
		GridBagConstraints opgc = new GridBagConstraints();

		editor = new EditorArduino(boundServiceName, myService);
		editor.init();
		editor2Panel.add(editor.getDisplay());

		JFrame top = myService.getFrame();
		tabs.insertTab("editor2", null, editor2Panel, "editor 2", 0);
		tabs.setTabComponentAt(0, new TabControl(top, tabs, editor2Panel,
				boundServiceName, "editor2"));
		myService.getFrame().pack();
		return editor2Panel;
	}

	public void createSerialDeviceMenu(JMenu m) {
		for (int i = 0; i < myArduino.portNames.size(); ++i) {
			// m.add(a)
		}
	}

}
