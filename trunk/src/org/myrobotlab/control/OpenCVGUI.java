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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.OpenCVFilter;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.OpenCV.FilterWrapper;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.VideoGUISource;

import com.googlecode.javacv.FrameGrabber;

public class OpenCVGUI extends ServiceGUI implements ListSelectionListener,
		VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(OpenCVGUI.class.toString());

	JButton connectButton = null;
	JButton saveFeaturesButton = null;
	JButton compareFeaturesButton = null;
	JButton dumpFeatureDataButton = null;
	JButton getFeaturesButton = null;

	BasicArrowButton addFilterButton = null;
	BasicArrowButton removeFilterButton = null;

	JList possibleFilters;
	JList currentFilters;

	VideoWidget video = null;
	PhotoReelWidget templateDisplay = null;
	

	JLabel inputFileLable = new JLabel("output1.avi");

	// input
	ButtonGroup group = new ButtonGroup();
	DefaultListModel currentFilterListModel = new DefaultListModel();

	JComboBox kinectImageOrDepth = new JComboBox(new String[]{"image","depth","interleave"});
	JComboBox grabberTypeSelect = null;
	
	JButton capture = new JButton("capture");
	
	JPanel captureCfg = new JPanel();
	JRadioButton file = new JRadioButton("file");
	JButton inputFileButton = new JButton("open file");
	JLabel indexLabel = new JLabel("index");
	JComboBox cameraIndex = new JComboBox(new String[]{ "0", "1", "2", "3", "4", "5" });
	
	// garbage
	//JRadioButton kinect = new JRadioButton("kinect");
	//JRadioButton nullInput = new JRadioButton("null", true);
	//JRadioButton camera = new JRadioButton("camera");

	JPanel filterParameters = new JPanel();
	
	OpenCV myOpenCV;

	public OpenCVGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	public void init() {

		video = new VideoWidget(boundServiceName, myService);
		video.init();
		
		templateDisplay = new PhotoReelWidget(boundServiceName, myService);
		templateDisplay.init();

		//camera.addActionListener(captureListener);
		//kinect.addActionListener(captureListener);
		capture.addActionListener(captureListener);
		file.addActionListener(captureListener);
		//nullInput.addActionListener(captureListener);

		ArrayList<String> s = new ArrayList<String>();
		//s.add("default");
		for (int i = 0; i < FrameGrabber.list.size(); ++i)
		{
			String ss = FrameGrabber.list.get(i).getCanonicalName();
			s.add(ss.substring(ss.lastIndexOf(".") + 1));
		}
		grabberTypeSelect = new JComboBox(s.toArray());
		
		kinectImageOrDepth.addActionListener(kinectListener);
		
		String plist[] = { "And", "AverageColor", "Canny", "CreateHistogram",
				"ColorTrack", "Dilate", "Erode", "FGBG", "FaceDetect",
				"Fauvist", "FindContours", "FloodFill", "GoodFeaturesToTrack",
				"Gray", "HoughLines2",
				"HSV",
				"InRange","KinectDepth", "KinectInterleave",
				// "Laser Tracking", oops lost cause not checked in !
				"LKOpticalTrack", "MatchTemplate", "MotionTemplate", "Mouse", "Not",
				"PyramidDown", "PyramidUp", "RepetitiveAnd", "RepetitiveOr",
				"ResetImageROI", "SampleArray", "SampleImage", "SetImageROI",
				"Smooth", "Threshold" };

		possibleFilters = new JList(plist);
		possibleFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		possibleFilters.setSelectedIndex(0);
		possibleFilters.setVisibleRowCount(10);

		currentFilters = new JList(currentFilterListModel);
		currentFilters.setFixedCellWidth(100);
		currentFilters.addListSelectionListener(this);
		currentFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		currentFilters.setSize(140, 160);
		currentFilters.setVisibleRowCount(10);

		JScrollPane currentFiltersScrollPane = new JScrollPane(currentFilters);
		JScrollPane possibleFiltersScrollPane = new JScrollPane(possibleFilters);

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 2;
		display.add(video.display, gc);
		gc.gridheight = 1;

		// build input begin ------------------
		JPanel input = new JPanel(new GridBagLayout());

		TitledBorder title;
		title = BorderFactory.createTitledBorder("input");
		input.setBorder(title);

		//JPanel cpanel = new JPanel(new GridBagLayout());
		JPanel cpanel = new JPanel();
		cpanel.setBorder(BorderFactory.createEtchedBorder());
		
		//cpanel.add(camera);
		cpanel.add(cameraIndex);		
		//cpanel.add(kinect);
		//cpanel.add(kinectImageOrDepth);
		
		//group.add(nullInput);
		//group.add(camera);
		//group.add(kinect);
		group.add(file);

		gc.gridx = 0;
		gc.gridy = 0;
		//input.add(nullInput, gc);		
		//input.add(cpanel);
		
		grabberTypeSelect.addActionListener(grabberTypeListener);
		
		// capture panel
		cpanel = new JPanel();
		cpanel.setBorder(BorderFactory.createEtchedBorder());
		cpanel.add(capture);
		cpanel.add(grabberTypeSelect);
		
		// build configuration for the various captures 
		// non visible - when not applicable
		// disable when capturing
		captureCfg.setBorder(BorderFactory.createEtchedBorder());
		captureCfg.add(cameraIndex);
		captureCfg.add(kinectImageOrDepth);
		captureCfg.add(inputFileLable);
		
		cpanel.add(captureCfg);
		
		input.add(cpanel);

		
		cameraIndex.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent e) {
		    	  	OpenCVGUI.this.myService.send(boundServiceName, "setCameraIndex", cameraIndex.getSelectedIndex());
		      }
		    });
		
		
		
		gc.gridx = 0;
		++gc.gridy;

		/*
		cpanel = new JPanel();
		cpanel.setBorder(BorderFactory.createEtchedBorder());
		
		cpanel.add(file);
		cpanel.add(inputFileButton);
		cpanel.add(inputFileLable);
		
		
		input.add(file, gc);
		++gc.gridx;
		input.add(inputFileButton, gc);
		gc.gridx = 0;
		++gc.gridy;
		input.add(inputFileLable, gc);
		*/
		gc.gridwidth = 2;
		input.add(cpanel, gc);
		gc.gridwidth = 1;
		
		gc.gridx = 0;
		gc.gridy = 2;
		display.add(input, gc);
		
		gc.gridy = 3;
		display.add(templateDisplay.display, gc);
		
		// build input end ------------------

		// build filters begin ------------------
		JPanel filters = new JPanel();
		title = BorderFactory.createTitledBorder("filters: available - current");
		filters.setBorder(title);
		filters.add(possibleFiltersScrollPane);
		filters.add(getRemoveFilterButton());
		filters.add(getAddFilterButton());
		filters.add(currentFiltersScrollPane);

		gc.gridx = 1;
		gc.gridy = 0;
		display.add(filters, gc);

		title = BorderFactory.createTitledBorder("filter parameters");
		filterParameters.setBorder(title);
		filterParameters.setPreferredSize(new Dimension(340, 360));
		gc.gridx = 1;
		gc.gridy = 1;
		gc.gridheight = 3;
		display.add(filterParameters, gc);

		setCurrentFilterMouseListener();
		// build filters end ------------------

		
		// TODO - bury in framework?
        myOpenCV = (OpenCV)RuntimeEnvironment.getService(boundServiceName).service;

	}
	
	public void setFilterData (FilterWrapper filterData)
	{
		if (filters.containsKey(filterData.name))
		{
			OpenCVFilterGUI gui = filters.get(filterData.name);
			gui.setFilterData(filterData);
		} else {
			LOG.error(filterData.name + " does not contain a gui");
		}
	}
	
	

	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	// TODO - bind - and keep these lists in synch
	// private LinkedHashMap<String, OpenCVFilterGUI> currentFilters = new
	// LinkedHashMap<String, OpenCVFilterGUI>();
	// LinkedHashMap<String, OpenCVFilter> filters = new
	// LinkedHashMap<String,OpenCVFilter>();
	

	LinkedHashMap<String, OpenCVFilterGUI> filters = new LinkedHashMap<String, OpenCVFilterGUI>();

	public JButton getAddFilterButton() {
		addFilterButton = new BasicArrowButton(BasicArrowButton.EAST);
		addFilterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame frame = new JFrame();
				frame.setTitle("add new filter");
				String name = JOptionPane.showInputDialog(frame, "new filter name");

				String type = (String) possibleFilters.getSelectedValue();
				myService.send(boundServiceName, "addFilter", name, type);
				// TODO - block on response - if (myService.send...)

				addFilterToGUI(name, type);

			}

		});

		return addFilterButton;
	}
	
	public void addFilterToGUI (String name, String type)
	{
		currentFilterListModel.addElement(name);

		String guiType = "org.myrobotlab.control.OpenCVFilter" + type + "GUI";
		Object[] params = new Object[3];
		params[0] = name;
		params[1] = boundServiceName;
		params[2] = myService;
		OpenCVFilterGUI filter = (OpenCVFilterGUI) Service.getNewInstance(guiType, params); // TODO - Object[] parameters

		// filter.display(); // TODO - ServiceGUI enforce the ability to
		// do modalDisplay() on all??!
		if (filter != null)
		{
			filters.put(name, filter);
		}
		
	}

	public JButton getRemoveFilterButton() {
		removeFilterButton = new BasicArrowButton(BasicArrowButton.WEST);
		removeFilterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String name = (String) currentFilters.getSelectedValue();
				myService.send(boundServiceName, "removeFilter", name);
				// TODO - block on response
				currentFilterListModel.removeElement(name);
			}

		});

		return removeFilterButton;
	}

	private ActionListener kinectListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String mode = (String)kinectImageOrDepth.getSelectedItem();
			if ("depth".equals(mode))
			{
				//myOpenCV.kinectMode = "depth";
			} else {
				//myOpenCV.kinectMode = "";
			}
			//myService.send(boundServiceName, "stopCapture");			
			myService.send(boundServiceName, "setState", myOpenCV);
			//myService.send(boundServiceName, "capture");

		}
	};
	
	
	private ActionListener captureListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			
			myService.send(boundServiceName, "setState", myOpenCV);
			
			if (("capture".equals(capture.getText())))
			{
				myService.send(boundServiceName, "capture");
				capture.setText("stop");
				//captureCfg.disable();
				setChildrenEnabled(captureCfg, false);
			} else {
				myService.send(boundServiceName, "stopCapture");
				capture.setText("capture");
				setChildrenEnabled(captureCfg, true	);
			}
			
		}
	};

	
	private ActionListener grabberTypeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			
			// TODO - complete reset or just one fn?
			myOpenCV.grabberType = "com.googlecode.javacv." + (String)grabberTypeSelect.getSelectedItem(); 			
			myService.send(boundServiceName, "setState", myOpenCV); // TODO - make cleaner myBoundService.setState();
/*			
			if (("capture".equals(capture.getText())))
			{
				myService.send(boundServiceName, "capture");
				capture.setText("stop");
				//captureCfg.disable();
				setChildrenEnabled(captureCfg, false);
			} else {
				myService.send(boundServiceName, "stopCapture");
				capture.setText("capture");
				setChildrenEnabled(captureCfg, true	);
			}
*/			
		}
	};
	
	private void setChildrenEnabled(Container container, boolean enabled) {
		for (int i=0; i<container.getComponentCount(); i++) {
		Component comp = container.getComponent(i);
		comp.setEnabled(enabled);
		if (comp instanceof Container)
		setChildrenEnabled((Container) comp, enabled);
		}
	}

	public void webCamDisplay(SerializableImage img) {
		video.webCamDisplay(img);
	}

	public void publishTemplate(SerializableImage img) {
		templateDisplay.publishTemplate(img);
	}
	
	@Override
	public void attachGUI() {
		// TODO - bury in GUI Framework?
		sendNotifyRequest("publishState", "getState", OpenCV.class);
		myService.send(boundServiceName, "publishState");

		video.attachGUI();
		templateDisplay.attachGUI();
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishState", "getState", OpenCV.class);
		
		video.detachGUI();
		templateDisplay.detachGUI();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// LOG.debug(e);
		if (!e.getValueIsAdjusting()) {
			String filterName = (String) currentFilters.getSelectedValue();
			if (filterName != null) {
				myService.send(boundServiceName, "setDisplayFilter", filterName);
				OpenCVFilterGUI f = filters.get(filterName);
				if (f != null) {
					filterParameters.removeAll();
					filterParameters.add(f.getDisplay());
					filterParameters.repaint();
					filterParameters.validate();
				} else {
					filterParameters.removeAll();
					filterParameters.add(new JLabel("no parameters available"));
					filterParameters.repaint();
					filterParameters.validate();
				}
			} else {
				// TODO - send message to OpenCV - that no filter should be sent
				// to publish
				filterParameters.removeAll();
				filterParameters.add(new JLabel("no filter selected"));
				filterParameters.repaint();
				filterParameters.validate();
			}
			// TODO - if filterName = null - it has been "un"selected ctrl-click

		}
	}

	// MouseListener mouseListener = new MouseAdapter() {
	public void setCurrentFilterMouseListener() {
		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent mouseEvent) {
				JList theList = (JList) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2) {
					int index = theList.locationToIndex(mouseEvent.getPoint());
					if (index >= 0) {
						Object o = theList.getModel().getElementAt(index);
						System.out.println("Double-clicked on: " + o.toString());
					}
				}
			}
		};

		currentFilters.addMouseListener(mouseListener);
	}

	// jlist.addMouseListener(mouseListener);

	@Override
	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return video;
	}
	
	final static String prefix = "OpenCVFilter";
	public void getState(OpenCV opencv)
	{
		if (opencv != null)
		{
			filters.clear();

			Iterator<String> itr = opencv.getFilters().keySet().iterator();

			while (itr.hasNext()) {
				String name;
				try {
					name = itr.next();
					OpenCVFilter f = opencv.getFilters().get(name);
					String type = f.getClass().getSimpleName();
					type = type.substring(prefix.length());
					addFilterToGUI(name, type);

				} catch (Exception e) {
					Service.logException(e);
					break;
				}
				
			}
		}
		
		/*
		if ("null".equals(opencv.useInput))
		{
			nullInput.setSelected(true);
		} else if ("camera".equals(opencv.useInput))
		{
			camera.setSelected(true);
		} else if ("file".equals(opencv.useInput))
		{
			file.setSelected(true);
		}
		*/
		
		cameraIndex.setSelectedIndex(opencv.cameraIndex);
				
	}
	

}
