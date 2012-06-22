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
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.myrobotlab.service.Runtime;

public class VideoWidget extends ServiceGUI {

	HashMap<String, VideoDisplayPanel> displays = new HashMap<String, VideoDisplayPanel>();
	ArrayList<VideoWidget> exports = new ArrayList<VideoWidget>();
	boolean allowFork = false;
	JComboBox localSources = null;
	

	public VideoWidget(final String boundFilterName, final GUI myService, boolean allowFork)
	{
		this(boundFilterName, myService);
		this.allowFork = allowFork;
	}

	public VideoWidget(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	

	public ArrayList<VideoWidget> getExports() {
		return exports;
	}

	
	
	public JComboBox getServices(JComboBox cb) {
		if (cb == null) {
			cb = new JComboBox();
		}
				
		//Runtime.getServicesFromInterface(interfaceName);
		ServiceEnvironment se = Runtime.getLocalServices();
		Map<String, ServiceWrapper> sortedMap = new TreeMap<String, ServiceWrapper>(se.serviceDirectory);
		Iterator<String> it = sortedMap.keySet().iterator();
		
		// String [] namesAndClasses = new String[sortedMap.size()];
		while (it.hasNext()) {
			String serviceName = it.next();
			cb.addItem(serviceName);
		}

		return cb;
	}



	@Override
	public void attachGUI() {
		sendNotifyRequest("publishFrame", "displayFrame", SerializableImage.class);
	}

	
	public void attachGUI(String srcMethod, String dstMethod, Class<?> c) {
		sendNotifyRequest(srcMethod, dstMethod, c);
	}
	
	
	/*
	 * attaching a widget capable of display - to relay the image to good for
	 * customizing displays
	 */
	public void attachLocalGUI() {
		VideoGUISource vgs = (VideoGUISource) myService.getServiceGUIMap().get(localSources.getSelectedItem());
		VideoWidget vw = vgs.getLocalDisplay();
		vw.getExports().add(this);
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishFrame", "displayFrame", SerializableImage.class);
	}

	//VideoDisplayPanel vid = new VideoDisplayPanel("output"); 

	int videoDisplayXPos = 0;
	int videoDisplayYPos = 0;

	@Override
	public void init() {
		init(null);
	}
	
	public void init(ImageIcon icon)
	{
		TitledBorder title;
		title = BorderFactory.createTitledBorder(boundServiceName + " " + " video widget");
		display.setBorder(title);
		
		addVideoDisplayPanel("output"); 
	}
	
	public VideoDisplayPanel addVideoDisplayPanel(String source)
	{
		return addVideoDisplayPanel(source, null);
	}

	public VideoDisplayPanel addVideoDisplayPanel(String source, ImageIcon icon)
	{
		// FIXME FIXME FIXME - should be FlowLayout No?
		
		if (videoDisplayXPos%2 == 0)
		{
			videoDisplayXPos = 0;
			++videoDisplayYPos;
		}
		
		gc.gridx = videoDisplayXPos;
		gc.gridy = videoDisplayYPos;

		VideoDisplayPanel vp = new VideoDisplayPanel(source, this,myService, boundServiceName);
		
		// add it to the map of displays
		displays.put(source, vp);		
		
		// add it to the display
		display.add(vp.myDisplay, gc);
		
		++videoDisplayXPos;		
		display.invalidate();
		myService.pack();
		
		return vp;
	}
	
	public void removeVideoDisplayPanel(String source)
	{
		if (!displays.containsKey(source))
		{
			log.error("cannot remove VideoDisplayPanel " + source);
			return;
		}
		
		VideoDisplayPanel vdp = displays.remove(source);
		display.remove(vdp.myDisplay);		
		//--videoDisplayXPos;		
		display.invalidate();
		myService.pack();
	}
	
	public void removeAllVideoDisplayPanels ()
	{
		Iterator<String> itr = displays.keySet().iterator();
		while (itr.hasNext()) {
			String n = itr.next();
			log.error("removing " + n);
			//removeVideoDisplayPanel(n);
			VideoDisplayPanel vdp = displays.get(n);
			display.remove(vdp.myDisplay);
		}
		displays.clear();
		videoDisplayXPos = 0;
		videoDisplayYPos = 0;
	}
	
	/* 
	 * displayFrame(BufferedImage img) is for handling non-serializable images
	 * from the Graphics Service - its an optimization kludge
	 * another optimization is to create a single SerializableImage as a container
	 * and replace only the new BufferedImage 
	 */
	SerializableImage container = new SerializableImage();
	public void displayFrame(BufferedImage img) {
		container.setImage(img);
		container.source = "output";
		displays.get("output").displayFrame(container);
	}
	
	// multiplex images if desired
	public void displayFrame(SerializableImage img) {

		if (displays.containsKey(img.source))
		{
			displays.get(img.source).displayFrame(img);
		} else {
			displays.get("output").displayFrame(img); // catchall
		}

	}
}
