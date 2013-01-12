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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;
import org.myrobotlab.control.widget.PhotoReelWidget;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.VideoGUISource;

import com.googlecode.javacv.FrameGrabber;

public class OpenCVGUI extends ServiceGUI implements ListSelectionListener, VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(OpenCVGUI.class.toString());
	public String prefixPath = "com.googlecode.javacv.";

	JButton connectButton = null;
	JButton saveFeaturesButton = null;
	JButton compareFeaturesButton = null;
	JButton dumpFeatureDataButton = null;
	JButton getFeaturesButton = null;

	BasicArrowButton addFilterButton = null;
	BasicArrowButton removeFilterButton = null;

	JList possibleFilters;
	JList currentFilters;

	VideoWidget video0 = null;

	PhotoReelWidget templateDisplay = null;

	JButton capture = new JButton("capture");

	// input
	// capture config
	JPanel captureCfg = new JPanel();
	JRadioButton fileRadio = new JRadioButton();
	JRadioButton cameraRadio = new JRadioButton();
	JTextField inputFile = new JTextField("");
	JLabel inputFileLable = new JLabel("file");
	JLabel cameraIndexLable = new JLabel("camera");
	JLabel modeLabel = new JLabel("mode");
	JButton inputFileButton = new JButton("open file");

	JComboBox IPCameraType = new JComboBox(new String[] { "foscam FI8918W" });

	ButtonGroup groupRadio = new ButtonGroup();
	DefaultListModel currentFilterListModel = new DefaultListModel();

	JComboBox kinectImageOrDepth = new JComboBox(new String[] { "image", "depth", "interleave" });
	JComboBox grabberTypeSelect = null;

	JComboBox cameraIndex = new JComboBox(new Integer[] { 0, 1, 2, 3, 4, 5 });

	JPanel filterParameters = new JPanel();

	LinkedHashMap<String, OpenCVFilterGUI> filters = new LinkedHashMap<String, OpenCVFilterGUI>();

	OpenCV myOpenCV;

	public OpenCVGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {

		video0 = new VideoWidget(boundServiceName, myService, false);
		video0.init();

		// TODO - refactor and make invisible unless filter warrants it
		templateDisplay = new PhotoReelWidget(boundServiceName, myService);
		templateDisplay.init();

		capture.addActionListener(captureListener);

		ArrayList<String> frameGrabberList = new ArrayList<String>();
		for (int i = 0; i < FrameGrabber.list.size(); ++i) {
			String ss = FrameGrabber.list.get(i);
			String fg = ss.substring(ss.lastIndexOf(".") + 1);
			// filter out the two I've never seen
			if (!"DC1394".equals(fg) && !"FlyCapture".equals(fg)) {
				frameGrabberList.add(fg);
			}
		}

		frameGrabberList.add("IPCamera");
		frameGrabberList.add("Image Stream Source"); // service which implements
														// ImageStreamSource

		grabberTypeSelect = new JComboBox(frameGrabberList.toArray());

		kinectImageOrDepth.addActionListener(kinectListener);

		String plist[] = { "And", "AverageColor", "Canny", "CreateHistogram", "ColorTrack", "Dilate", "Erode", "FGBG", "FaceDetect", "Fauvist", "FindContours", "FloodFill",
				"FloorFinder", "GoodFeaturesToTrack", "Gray", "HoughLines2", "HSV", "InRange", "KinectDepth", "KinectDepthMask", "KinectInterleave", "LKOpticalTrack", "Mask",
				"MatchTemplate", "MotionTemplate", "Mouse", "Not", "PyramidDown", "PyramidUp", "RepetitiveAnd", "RepetitiveOr", "ResetImageROI", "SampleArray", "SampleImage",
				"SetImageROI", "Smooth", "Threshold" };

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
		JPanel videoPanel = new JPanel();
		videoPanel.add(video0.display);
		gc.gridheight = 2;
		display.add(videoPanel, gc);
		// display.add(video0.display, gc);
		gc.gridheight = 1;

		// build input begin ------------------
		JPanel input = new JPanel(new GridBagLayout());

		TitledBorder title;
		title = BorderFactory.createTitledBorder("input");
		input.setBorder(title);

		groupRadio.add(cameraRadio);
		groupRadio.add(fileRadio);

		gc.gridx = 0;
		gc.gridy = 0;

		grabberTypeSelect.addActionListener(grabberTypeListener);

		// capture panel
		JPanel cpanel = new JPanel();
		cpanel.setBorder(BorderFactory.createEtchedBorder());
		cpanel.add(capture);
		cpanel.add(grabberTypeSelect);
		// build configuration for the various captures
		// non visible - when not applicable
		// disable when capturing
		captureCfg.setBorder(BorderFactory.createEtchedBorder());

		captureCfg.add(cameraRadio);
		captureCfg.add(cameraIndexLable);
		captureCfg.add(cameraIndex);
		captureCfg.add(modeLabel);
		captureCfg.add(kinectImageOrDepth);
		captureCfg.add(fileRadio);
		captureCfg.add(inputFileLable);
		captureCfg.add(inputFile);

		captureCfg.add(IPCameraType);

		input.add(cpanel, gc);
		++gc.gridy;
		input.add(captureCfg, gc);

		/*
		 * OpenCV members are set on the zombie
		 * cameraIndex.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent e) {
		 * OpenCVGUI.this.myService.send(boundServiceName, "setCameraIndex",
		 * cameraIndex.getSelectedIndex()); } });
		 */
		gc.gridx = 0;
		++gc.gridy;

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
		myOpenCV = (OpenCV) Runtime.getServiceWrapper(boundServiceName).service;

		// TODO - remove action listener?
		grabberTypeSelect.setSelectedItem("OpenCV");

	}

	public void setFilterState(FilterWrapper filterData) {
		if (filters.containsKey(filterData.name)) {
			OpenCVFilterGUI gui = filters.get(filterData.name);
			gui.getFilterState(filterData);
		} else {
			log.error(filterData.name + " does not contain a gui");
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

				// addFilterToGUI(name, type);

			}

		});

		return addFilterButton;
	}

	public OpenCVFilterGUI addFilterToGUI(String name, OpenCVFilter f) {
		
		String type = f.getClass().getSimpleName();
		type = type.substring(prefix.length());
		
		currentFilterListModel.addElement(name);

		String guiType = "org.myrobotlab.control.OpenCVFilter" + type + "GUI";
		OpenCVFilterGUI filtergui = null;
		try {
			filtergui = (OpenCVFilterGUI) Service.getNewInstance(guiType, name, boundServiceName, myService);
			filtergui.initFilterState(f); // set the bound filter
		} catch (Exception e)
		{
			log.info(String.format("filter %s does not have a gui defined", type));
		}
		
		filters.put(name, filtergui);
		return filtergui;
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
			String mode = (String) kinectImageOrDepth.getSelectedItem();
			if ("depth".equals(mode)) {
				myOpenCV.format = "depth";
			} else {
				myOpenCV.format = "image";
			}
			// myService.send(boundServiceName, "stopCapture");
			// myService.send(boundServiceName, "setState", myOpenCV);
			// myService.send(boundServiceName, "capture");

		}
	};

	private ActionListener captureListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {

			// TODO - setState only done in Capture !!!!!
			// TODO - setting all of OpenCV's actual variables should ONLY be
			// done here
			// otherwise invalid states may occur while a capture is running
			// the model is to set all the data of the gui just before the
			// capture
			// request is sent
			if ("IPCamera".equals((String) grabberTypeSelect.getSelectedItem())) {
				prefixPath = "org.myrobotlab.image.";
				myOpenCV.inputSource = OpenCV.INPUT_SOURCE_NETWORK;
			} else {
				prefixPath = "com.googlecode.javacv.";
			}

			myOpenCV.grabberType = prefixPath + (String) grabberTypeSelect.getSelectedItem() + "FrameGrabber";

			if (fileRadio.isSelected()) {
				myOpenCV.inputSource = OpenCV.INPUT_SOURCE_FILE;
				myOpenCV.inputFile = inputFile.getText();
			} else if (cameraRadio.isSelected()) {
				myOpenCV.inputSource = OpenCV.INPUT_SOURCE_CAMERA;
				myOpenCV.cameraIndex = (Integer) cameraIndex.getSelectedItem();
			} else {
				log.error("input source is " + myOpenCV.inputSource);
			}

			myService.send(boundServiceName, "setState", myOpenCV);

			if (("capture".equals(capture.getText()))) {
				myService.send(boundServiceName, "capture");
				capture.setText("stop");
				// captureCfg.disable();
				setChildrenEnabled(captureCfg, false);
			} else {
				myService.send(boundServiceName, "stopCapture");
				capture.setText("capture");
				setChildrenEnabled(captureCfg, true);
			}

		}
	};

	/**
	 * GUI defaults for grabber types
	 */
	private ActionListener grabberTypeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {

			String type = (String) grabberTypeSelect.getSelectedItem();
			if ("OpenKinect".equals(type)) {
				cameraRadio.setSelected(true);
				cameraIndexLable.setVisible(true);
				cameraIndex.setVisible(true);
				modeLabel.setVisible(true);
				kinectImageOrDepth.setVisible(true);
				inputFileLable.setVisible(false);
				inputFile.setVisible(false);
				fileRadio.setVisible(false);

				IPCameraType.setVisible(false);
			}

			if ("OpenCV".equals(type) || "VideoInput".equals(type) || "FFmpeg".equals(type)) {
				// cameraRadio.setSelected(true);
				kinectImageOrDepth.setSelectedItem("image");
				// myOpenCV.format = "image";
				cameraIndexLable.setVisible(true);
				cameraIndex.setVisible(true);
				modeLabel.setVisible(false);
				kinectImageOrDepth.setVisible(false);
				inputFileLable.setVisible(true);
				inputFile.setVisible(true);

				fileRadio.setVisible(true);
				cameraRadio.setVisible(true);

				IPCameraType.setVisible(false);
			}

			if ("IPCamera".equals(type)) {
				// cameraRadio.setSelected(true);
				// kinectImageOrDepth.setSelectedItem("image");
				// myOpenCV.format = "image";
				cameraIndexLable.setVisible(false);
				cameraIndex.setVisible(false);
				modeLabel.setVisible(false);
				kinectImageOrDepth.setVisible(false);
				inputFileLable.setVisible(true);
				inputFile.setVisible(true);
				fileRadio.setSelected(true);

				fileRadio.setVisible(false);
				cameraRadio.setVisible(false);

				IPCameraType.setVisible(true);
			}

		}
	};

	// TODO - put in util class
	private void setChildrenEnabled(Container container, boolean enabled) {
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component comp = container.getComponent(i);
			comp.setEnabled(enabled);
			if (comp instanceof Container)
				setChildrenEnabled((Container) comp, enabled);
		}
	}

	public void displayFrame(SerializableImage img) {
		video0.displayFrame(img);
	}

	public void publishTemplate(SerializableImage img) {
		templateDisplay.publishTemplate(img);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// log.debug(e);
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
		return video0; // else return video1
	}

	/*
	 * getState is an interface function which allow the interface of the GUI
	 * Bound service to update graphical portions of the GUI based on data
	 * changes.
	 * 
	 * The entire service is sent and it is this functions responsibility to
	 * update all of the gui components based on data elements and/or method of
	 * the service.
	 * 
	 * getState get's its Service directly if the gui is operating "in process".
	 * If the gui is operating "out of process" a serialized (zombie) process is
	 * sent to provide the updated state information. Typically "publishState"
	 * is the function which provides the event for getState.
	 */
	final static String prefix = "OpenCVFilter";

	public void getState(final OpenCV opencv) {
		//SwingUtilities.invokeLater(new Runnable() {
		//	public void run() {

		if (opencv != null) {
			filters.clear();

			Iterator<OpenCVFilter> itr = opencv.getFiltersCopy().iterator();

			currentFilterListModel.clear();
			while (itr.hasNext()) {
				String name;
				try {
					OpenCVFilter f = itr.next();
					name = f.name;
	
					OpenCVFilterGUI guifilter = addFilterToGUI(name, f);
					// set the state of the filter gui - first one is free :)
					if (guifilter != null){
						guifilter.getFilterState(new FilterWrapper(name, f));
					}

				} catch (Exception e) {
					Service.logException(e);
					break;
				}

			}

			currentFilters.repaint();

			for (int i = 0; i < grabberTypeSelect.getItemCount(); ++i) {
				String currentObject = prefixPath + (String) grabberTypeSelect.getItemAt(i);
				if (currentObject.equals(opencv.grabberType)) {
					grabberTypeSelect.setSelectedIndex(i);
					break;
				}

			}

			if (opencv.capturing) {
				capture.setText("stop"); // will be a bug if changed to jpg
			} else {
				capture.setText("capture");
			}

			inputFile.setText(opencv.inputFile);

		} else {
			log.error("getState for " + myService.getName() + " was called on " + boundServiceName + " with null reference to state info");
		}

		cameraIndex.setSelectedIndex(opencv.cameraIndex);
		
//			}
//		});
			
	}

	@Override
	public void attachGUI() {
		// TODO - bury in GUI Framework?
		subscribe("publishState", "getState", OpenCV.class);
		myService.send(boundServiceName, "publishState");

		video0.attachGUI(); // default attachment
		templateDisplay.attachGUI(); // default attachment
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", OpenCV.class);

		video0.detachGUI();
		templateDisplay.detachGUI();
	}

}
