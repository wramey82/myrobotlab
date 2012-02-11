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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.IPCamera;
import org.myrobotlab.service.interfaces.GUI;

public class IPCameraGUI extends ServiceGUI implements ListSelectionListener {

	public final static Logger LOG = Logger.getLogger(IPCameraGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	VideoWidget video0;
	Keyboard keyboard;
	
	IPCamera myIPCamera;

	JButton connect;
	JButton capture;
	JLabel connected;
	JLabel notConnected;
	
	JPanel info;
	
	DirectionWidget direction = new DirectionWidget();
	DirectionEventListener del = new DirectionEventListener();

	public class DirectionEventListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ae) {
			LOG.info(ae);
			if ("n".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_UP);
			} else if ("ne".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_UP);
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_LEFT);
			} else if ("e".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_LEFT);
			} else if ("se".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_DOWN);
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_LEFT);
			} else if ("s".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_DOWN);
			} else if ("sw".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_DOWN);
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_RIGHT);
			} else if ("w".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_RIGHT);
			} else if ("nw".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_UP);
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_RIGHT);
			} else if ("stop".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "move", IPCamera.FOSCAM_MOVE_CENTER);
			} else if ("connect".equals(ae.getActionCommand())) {
				myService.send(boundServiceName, "connect", host.getText(), user.getText(), password.getText());
			} else if ("capture".equals(ae.getActionCommand())) {
				JButton b = (JButton)ae.getSource();
				if ("stop capture".equals(b.getText()))
				{	b.setText("capture");
					myService.send(boundServiceName, "stopCapture");					
				} else {
					b.setText("stop capture");
					myService.send(boundServiceName, "capture");
				}
			}
		}

	}

	public IPCameraGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
		myIPCamera = (IPCamera)RuntimeEnvironment.getService(boundServiceName).service;
		direction.setDirectionListener(del);
	}

	JTextField host = new JTextField("myrobotlab.dyndns.org:1060", 8);
	JTextField user = new JTextField("admin", 8);
	JPasswordField password = new JPasswordField("zardoz7", 8);
	
	public void init() {

		video0 = new VideoWidget(boundServiceName, myService, false);
		video0.init();
		gc.gridheight = 8;
		gc.gridx = 0;
		gc.gridy = 0;

		display.add(video0.display, gc);
		gc.gridheight = 1;
		
		gc.gridx = 1;
		display.add(new JLabel("ip"), gc);
		++gc.gridx;
		display.add(host, gc);
		++gc.gridy;

		gc.gridx = 1;
		display.add(new JLabel("user"), gc);
		++gc.gridx;
		display.add(user, gc);
		++gc.gridy;
		
		gc.gridx = 1;
		display.add(new JLabel("password  "), gc);
		++gc.gridx;
		display.add(password, gc);
		++gc.gridy;

		gc.gridx = 1;
		connect = new JButton("connect");
		connect.addActionListener(del);
		display.add(connect, gc);
		++gc.gridx;
		connected = new JLabel(new ImageIcon(IPCameraGUI.class.getResource("/resource/bullet_ball_glass_green.png")));
		notConnected = new JLabel(new ImageIcon(IPCameraGUI.class.getResource("/resource/bullet_ball_glass_grey.png")));
		display.add(notConnected, gc);
		display.add(connected, gc);
		connected.setVisible(false);
		
		++gc.gridy;
		
		gc.gridx = 1;
		++gc.gridy;
		gc.gridwidth = 2;
		display.add(direction, gc);

		info = new JPanel();
		
		capture = new JButton("capture");
		capture.setActionCommand("capture");
		capture.addActionListener(del);
		++gc.gridy;
		display.add(capture, gc);
		++gc.gridy;
		display.add(info, gc);
	}

	/**
	 * TODO - make Keyboard Widget
	 * 
	 * @author greg
	 * 
	 */
	public class Keyboard implements KeyListener {

		SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss:SSS");

		public void keyPressed(KeyEvent keyEvent) {
			myService.send(boundServiceName, "keyPressed",
					keyEvent.getKeyCode());
		}

		public void keyReleased(KeyEvent keyEvent) {
			// LOG.error("Released" + keyEvent);
		}

		public void keyTyped(KeyEvent keyEvent) {
			// LOG.error("Typed" + keyEvent);
		}

	};

	public void isConnected(Boolean b)
	{
		if (b)
		{
			connected.setVisible(true);
			notConnected.setVisible(false);
		} else {
			connected.setVisible(false);
			notConnected.setVisible(true);
		}
		
	}
	
	/**
	 * The return of IPCamera.getStatus which is used to test connectivity
	 * @param s
	 */
	public void getStatus(String s)
	{
		info.removeAll();
		info.add(new JLabel(s));
	}
	
	public void displayFrame(SerializableImage img) {

		video0.displayFrame(img);
	}

	public void attachGUI() {
		video0.attachGUI();
		sendNotifyRequest("connect", "isConnected", Boolean.class);
		sendNotifyRequest("getStatus", "getStatus", String.class);
	}

	@Override
	public void detachGUI() {
		video0.detachGUI();
		removeNotifyRequest("connect", "isConnected", Boolean.class);
		removeNotifyRequest("getStatus", "getStatus", String.class);
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * Returns an ImageIcon, or null if the path was invalid. TODO - move to
	 * FileIO.Util
	 */
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

}