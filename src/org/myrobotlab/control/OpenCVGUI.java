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

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.VideoGUISource;

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

	JLabel inputFileLable = new JLabel("output1.avi");

	// input
	ButtonGroup group = new ButtonGroup();
	String[] cindex = { "0", "1", "2", "3", "4", "5" };
	DefaultListModel currentFilterListModel = new DefaultListModel();
	JComboBox cameraIndex = new JComboBox(cindex);

	JRadioButton camera = new JRadioButton("camera");
	JRadioButton file = new JRadioButton("file");
	JRadioButton nullInput = new JRadioButton("null", true);

	JButton inputFileButton = new JButton("open file");

	JPanel filterParameters = new JPanel();

	public OpenCVGUI(String name, GUIService myService) {
		super(name, myService);

		video = new VideoWidget(name, myService);

		camera.addActionListener(al);
		file.addActionListener(al);
		nullInput.addActionListener(al);

		String plist[] = { "And", "AverageColor", "Canny", "CreateHistogram",
				"ColorTrack", "Dilate", "Erode", "FGBG", "FaceDetect",
				"Fauvist", "FindContours", "FloodFill", "GoodFeaturesToTrack",
				"Gray", "HoughLines2",
				"HSV",
				"InRange",
				// "Laser Tracking", oops lost cause not checked in !
				"LKOpticalTrack", "MotionTemplate", "Mouse", "Not",
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

		group.add(nullInput);
		group.add(camera);
		group.add(file);

		gc.gridx = 0;
		gc.gridy = 0;
		input.add(nullInput, gc);
		++gc.gridx;
		input.add(camera, gc);
		++gc.gridx;
		input.add(cameraIndex, gc);
		
		
		cameraIndex.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent e) {
		    	  	OpenCVGUI.this.myService.send(boundServiceName, "setCameraIndex", cameraIndex.getSelectedIndex());
					//myService.send(boundServiceName, "setCameraIndex", cameraIndex.getSelectedIndex());
		        //t.setText("index: " + cameraIndex.getSelectedIndex() + "   "
		        //    + ((JComboBox) e.getSource()).getSelectedItem());
		      }
		    });
		
		
		
		gc.gridx = 0;
		++gc.gridy;
		input.add(file, gc);
		++gc.gridx;
		input.add(inputFileButton, gc);
		gc.gridx = 0;
		++gc.gridy;
		input.add(inputFileLable, gc);

		gc.gridx = 0;
		gc.gridy = 2;
		display.add(input, gc);
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
		gc.gridheight = 2;
		display.add(filterParameters, gc);

		setCurrentFilterMouseListener();
		// build filters end ------------------

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

				String newFilter = (String) possibleFilters.getSelectedValue();
				myService.send(boundServiceName, "addFilter", name, newFilter);
				// TODO - block on response - if (myService.send...)

				currentFilterListModel.addElement(name);

				String guiType = "org.myrobotlab.control.OpenCVFilter" + newFilter + "GUI";
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

		});

		return addFilterButton;
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

	private ActionListener al = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String useInput = ((JRadioButton) e.getSource()).getText();
			myService.send(boundServiceName, "setUseInput", useInput);
			
			if (useInput.compareTo("null") == 0)
			{
				myService.send(boundServiceName, "stopCapture");
			} else {
				myService.send(boundServiceName, "capture");
			}
		}
	};
	
	

	public void webCamDisplay(SerializableImage img) {
		video.webCamDisplay(img);
	}

	@Override
	public void attachGUI() {
		video.attachGUI();
	}

	@Override
	public void detachGUI() {
		video.detachGUI();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// LOG.debug(e);
		if (!e.getValueIsAdjusting()) {
			String filterName = (String) currentFilters.getSelectedValue();
			if (filterName != null) {
				myService
						.send(boundServiceName, "setDisplayFilter", filterName);
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
						System.out
								.println("Double-clicked on: " + o.toString());
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

}
