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

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.interfaces.GUI;

public class VideoWidget extends ServiceGUI {


	// is this global?
//	HashMap<String, JLabel> screens = new HashMap<String, JLabel>();
	HashMap<String, VideoDisplayPanel> displays = new HashMap<String, VideoDisplayPanel>();
	ArrayList<VideoWidget> exports = new ArrayList<VideoWidget>();
	// JComboBox sources = new JComboBox();
	
	// TODO - too big for inner class
	public class VideoDisplayPanel
	{
		VideoWidget parent;
		String boundFilterName;
 
		JPanel myDisplay = new JPanel();
		JComboBox sources = new JComboBox();
		JLabel screen = new JLabel();
		JLabel mouseInfo = new JLabel("mouse x y");
		JLabel resolutionInfo = new JLabel("width x height");
		JLabel deltaTime = new JLabel("0");

		JButton attach = new JButton("attach");
		JButton fork = new JButton("fork");
		JComboBox localSources = null;
		JLabel sourceNameLabel = new JLabel("");

		public SerializableImage lastImage = null;
		public ImageIcon lastIcon = new ImageIcon();
		public ImageIcon myIcon = new ImageIcon();
		public VideoMouseListener vml = new VideoMouseListener();
		
		public int lastImageWidth = 0;
		
		public class VideoMouseListener implements MouseListener {

			@Override
			public void mouseClicked(MouseEvent e) {
				mouseInfo.setText("clicked " + e.getX() + "," + e.getY());
				Object[] d = new Object[1];
				d[0] = e;
				myService.send(boundServiceName, "invokeFilterMethod", sourceNameLabel.getText(), "samplePoint", d); 
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// mouseInfo.setText("entered");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// mouseInfo.setText("exit");

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// mouseInfo.setText("pressed");
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// mouseInfo.setText("release");
			}

		}

		VideoDisplayPanel(String boundFilterName, VideoWidget p)
		{
			this.parent = p;
			this.boundFilterName = boundFilterName;			
			
			myDisplay.setLayout(new GridBagLayout());

			ImageIcon icon = FileIO.getResourceIcon("mrl_logo.jpg");
			if (icon != null)
			{
				screen.setIcon(icon);	
			}

			GridBagConstraints gc = new GridBagConstraints();

			screen.addMouseListener(vml);
			myIcon.setImageObserver(screen); // Good(necessary) Optimization

			TitledBorder title;
			title = BorderFactory.createTitledBorder(boundServiceName + " " + boundFilterName + " video widget");
			myDisplay.setBorder(title);

			gc.gridx = 0;
			gc.gridy = 0;
			// gc.anchor = GridBagConstraints.BOTH;
			// myDisplay.add(new JLabel(boundServiceName), gc);
			// ++gc.gridy;
			localSources = getServices(null);
			myDisplay.add(localSources, gc);

			++gc.gridx;
			fork.addActionListener(new ButtonListener());
			myDisplay.add(fork, gc);
			
			++gc.gridx;
			myDisplay.add(sources, gc);
			
			++gc.gridx;
			attach.addActionListener(new ButtonListener());
			myDisplay.add(attach, gc);

			
			gc.gridx = 0;
			gc.gridwidth = 5;
			++gc.gridy;
			myDisplay.add(screen, gc);
			gc.gridwidth = 1;
			++gc.gridy;
			// myDisplay.add(getConnectButton(), gc);
			// ++gc.gridy;
			myDisplay.add(mouseInfo, gc);
			++gc.gridx;
			myDisplay.add(resolutionInfo, gc);
			++gc.gridy;
			myDisplay.add(deltaTime, gc);
			++gc.gridy;
			gc.gridwidth = 5;
			myDisplay.add(sourceNameLabel, gc);		
			
		}
		
		public void displayFrame(SerializableImage img) {
			
			/* got new frame - check if a screen exists for it
			 * or if i'm in single screen mode
			 * 
			 * img.source is the name of the bound filter
			 */
			 
			if (!displays.containsKey(img.source)) {
//				screens.put(img.source, new JLabel());
				parent.addVideoDisplayPanel(img.source);// dynamically spawn a display if a new source is found
				getSources();
			}

			if (lastImage != null) {
				screen.setIcon(lastIcon);
			}
			
			if (!sourceNameLabel.getText().equals(img.source))
			{
				sourceNameLabel.setText(img.source);
			}
			
			myIcon.setImage(img.getImage());
			screen.setIcon(myIcon);
			// screen.repaint(); - was in other function - performance hit remove if works in GraphicServiceGUI
			if (lastImage != null) {
				if (img.timestamp != null)
					deltaTime.setText(""
							+ (img.timestamp.getTime() - lastImage.timestamp.getTime()));
			}
			lastImage = img;
			lastIcon.setImage(img.getImage());

			if (exports.size() > 0) {
				for (int i = 0; i < exports.size(); ++i) {
//					exports.get(i).displayFrame(filterName, img); FIXME
				}
			}
			
			// resize gui if necessary
			if (lastImageWidth != img.getImage().getWidth())
			{
				screen.invalidate();
				myService.pack();
				lastImageWidth = img.getImage().getWidth();
				resolutionInfo.setText(" " + lastImageWidth + " x " + img.getImage().getHeight());
			}

			img = null;

		}

		public void getSources()
		{

			Map<String, VideoDisplayPanel> sortedMap = new TreeMap<String, VideoDisplayPanel>(displays);
			Iterator<String> it = sortedMap.keySet().iterator();

			sources.removeAllItems();
			
			// String [] namesAndClasses = new String[sortedMap.size()];
			int i = 0;
			while (it.hasNext()) {
				String serviceName = it.next();
				sources.addItem(serviceName);
				++i;
			}
		}

		class ButtonListener implements ActionListener {
			ButtonListener() {
			}

			public void actionPerformed(ActionEvent e) {
				String id = ((JButton)e.getSource()).getText(); 
				if (id.equals("attach"))
				{
					attachLocalGUI();
				} else if (id.equals("fork")) {
					String filter = (String)sources.getSelectedItem();
					parent.addVideoDisplayPanel(filter);
					myService.send(boundServiceName, "fork", filter); 

				} else {
					LOG.error("unhandled button event - " + id);
				}
			}
		}
		
		
	} // VideoDisplayPanel


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

		// FIXME - cfg deprecated !!!!
		HashMap<String, ServiceEntry> services = myService.getHostCFG().getServiceMap();
		Map<String, ServiceEntry> sortedMap = new TreeMap<String, ServiceEntry>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		// String [] namesAndClasses = new String[sortedMap.size()];
		int i = 0;
		while (it.hasNext()) {
			String serviceName = it.next();
			cb.addItem(serviceName);
			++i;
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
/* FIXME FIXME FIXME		
		VideoGUISource vgs = (VideoGUISource) myService.getServiceGUIMap().get(localSources.getSelectedItem());
		VideoWidget vw = vgs.getLocalDisplay();
		vw.getExports().add(this);
*/		
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
		// TODO Auto-generated method stub
		TitledBorder title;
		title = BorderFactory.createTitledBorder(boundServiceName + " " + " video widget");
		display.setBorder(title);
		
		addVideoDisplayPanel("output"); // create initial display "output"
//		screens.put("output", new JLabel("output"));
		//addVideoDisplayPanel("shoe"); // create initial display "output"
		//gc.gridx = 40;
		//display.add(new JLabel("blah blah blah"), gc);
	}

	public void addVideoDisplayPanel(String source)
	{
		
		if (videoDisplayXPos%2 == 0)
		{
			videoDisplayXPos = 0;
			++videoDisplayYPos;
		}
		
		gc.gridx = videoDisplayXPos;
		gc.gridy = videoDisplayYPos;

		VideoDisplayPanel vp = new VideoDisplayPanel(source, this);
		
		// add it to the map of displays
		displays.put(source, vp);		
		
		// add it to the display
		display.add(vp.myDisplay, gc);
		
		++videoDisplayXPos;		
		display.invalidate();
		myService.pack();
	}
	
	public void removeVideoDisplayPanel(String source)
	{
		if (!displays.containsKey(source))
		{
			LOG.error("cannot remove VideoDisplayPanel " + source);
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
			LOG.error("removing " + n);
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
