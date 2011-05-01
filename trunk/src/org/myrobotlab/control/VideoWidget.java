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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.VideoGUISource;

public class VideoWidget extends ServiceGUI {

	private static final long serialVersionUID = 1L;
	JLabel screen = new JLabel();
	JLabel info = new JLabel("mouse x y");
	JLabel deltaTime = new JLabel("0");

	HashMap<String, JLabel> screens = new HashMap<String, JLabel>();
	ArrayList<VideoWidget> exports = new ArrayList<VideoWidget>();

	JButton attach = new JButton("attach");
	JComboBox localSource = null;

	public SerializableImage lastImage = null;
	public ImageIcon lastIcon = new ImageIcon();
	public ImageIcon myIcon = new ImageIcon();
	public VideoMouseListener vml = new VideoMouseListener();
	public String boundFilterName = "";
	
	public int lastImageWidth = 0;

	public class VideoMouseListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			info.setText("clicked " + e.getX() + "," + e.getY());
			// myService.send(boundServiceName, "invokeFilterMethod",
			// "samplePoint", boundFilterName, e);
			Object[] d = new Object[1];
			d[0] = e;
			myService.send(boundServiceName, "invokeFilterMethod",
					boundFilterName, "samplePoint", d); // TODO - overload and
														// hind boundServiceName
														// in ServiceGUI
			// 2DPoint p = new 2DPoint(e.getX(), e.getY());
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// info.setText("entered");
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// info.setText("exit");

		}

		@Override
		public void mousePressed(MouseEvent e) {
			// info.setText("pressed");
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// info.setText("release");
		}

	}

	public ArrayList<VideoWidget> getExports() {
		return exports;
	}

	public JComboBox getServices(JComboBox cb) {
		if (cb == null) {
			cb = new JComboBox();
		}

		HashMap<String, ServiceEntry> services = myService.getHostCFG()
				.getServiceMap();
		Map<String, ServiceEntry> sortedMap = null;
		sortedMap = new TreeMap<String, ServiceEntry>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		// String [] namesAndClasses = new String[sortedMap.size()];
		int i = 0;
		while (it.hasNext()) {
			String serviceName = it.next();
			cb.addItem(serviceName);
			// ServiceEntry se = services.get(serviceName);
			// String shortClassName =
			// se.serviceClass.substring(se.serviceClass.lastIndexOf(".") + 1);
			// namesAndClasses[i] = serviceName + " - " + shortClassName;
			++i;
		}

		return cb;
	}

	class AttachListener implements ActionListener {
		AttachListener() {
		}

		public void actionPerformed(ActionEvent e) {
			attachLocalGUI();
		}
	}

	public VideoWidget(String boundServiceName, GUIService myService) {
		super(boundServiceName, myService);

		setScreenIcon("mrl_logo.jpg");

		screen.addMouseListener(vml);
		myIcon.setImageObserver(screen); // WWOOAH - THIS MAY BE A BIG
											// OPTIMIZATION !

		TitledBorder title;
		title = BorderFactory.createTitledBorder(boundServiceName + " "
				+ boundFilterName + " video widget");
		display.setBorder(title);

		gc.gridx = 0;
		gc.gridy = 0;
		// gc.anchor = GridBagConstraints.BOTH;
		// display.add(new JLabel(boundServiceName), gc);
		// ++gc.gridy;
		localSource = getServices(null);
		display.add(localSource, gc);
		++gc.gridx;
		attach.addActionListener(new AttachListener());
		display.add(attach, gc);

		gc.gridx = 0;
		gc.gridwidth = 2;
		++gc.gridy;
		display.add(screen, gc);
		gc.gridwidth = 1;
		++gc.gridy;
		// display.add(getConnectButton(), gc);
		// ++gc.gridy;
		display.add(info, gc);
		++gc.gridy;
		display.add(deltaTime, gc);
	}


	public void webCamDisplay(String filterName, SerializableImage img) {
		if (!screens.containsKey(filterName)) {
			screens.put(filterName, new JLabel());
		}

		if (lastImage != null) {
			screen.setIcon(lastIcon);
		}
		boundFilterName = img.source;
		myIcon.setImage(img.getImage());
		screen.setIcon(myIcon);
		if (lastImage != null) {
			if (img.timestamp != null)
				deltaTime.setText(""
						+ (img.timestamp.getTime() - lastImage.timestamp.getTime()));
		}
		lastImage = img;
		lastIcon.setImage(img.getImage());

		if (exports.size() > 0) {
			for (int i = 0; i < exports.size(); ++i) {
				exports.get(i).webCamDisplay(filterName, img);
			}
		}
		
		// resize gui if necessary
		if (lastImageWidth != img.getImage().getWidth())
		{
			screen.invalidate();
			myService.frame.pack();
		}

		img = null;

	}

	public void webCamDisplay(BufferedImage img) {
		if (lastImage != null) {
			screen.setIcon(lastIcon);
		}
		myIcon.setImage(img);
		screen.setIcon(myIcon);
		screen.repaint();
		if (lastImage != null) {
			// if timestamp != null)
			// deltaTime.setText(""+ ( img.timestamp.getTime() -
			// lastImage.timestamp.getTime()));
		}

		lastIcon.setImage(img);

		if (exports.size() > 0) {
			for (int i = 0; i < exports.size(); ++i) {
				exports.get(i).webCamDisplay(img);
			}
		}

		// resize gui if necessary
		if (lastImageWidth != img.getWidth())
		{
			screen.invalidate();
			myService.frame.pack();
		}
		
		img = null;
		
	}

	public void webCamDisplay(SerializableImage img) {
		if (img.source != null && img.source.length() > 0) {
			webCamDisplay(img.source, img);
		} else {
			webCamDisplay("unknown", img);
		}
	}

	protected ImageIcon setScreenIcon(String path) {
		ImageIcon icon = null;
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			icon = new ImageIcon(imgURL);
			screen.setIcon(icon);
			return icon;
		} else {
			LOG.error("Couldn't find file: " + path);
			return null;
		}
	}

	@Override
	public void attachGUI() {
		sendNotifyRequest("sendImage", "webCamDisplay", SerializableImage.class);
	}

	/*
	 * attaching a widget capable of display - to relay the image to good for
	 * customizing displays
	 */
	public void attachLocalGUI() {
		VideoGUISource vgs = (VideoGUISource) myService.getServiceGUIMap().get(
				localSource.getSelectedItem());
		VideoWidget vw = vgs.getLocalDisplay();
		vw.getExports().add(this);
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("sendImage", "webCamDisplay", SerializableImage.class);
	}

}
