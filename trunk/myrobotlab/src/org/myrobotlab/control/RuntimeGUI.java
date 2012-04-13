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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.ServiceInfo;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Style;
import org.myrobotlab.service.interfaces.GUI;

public class RuntimeGUI extends ServiceGUI {

	public final static Logger LOG = Logger.getLogger(RuntimeGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	BasicArrowButton addServiceButton = null;
	BasicArrowButton releaseServiceButton = null;
	
	// FIXME - put in as method of Style
	private static final Color highlight = Color.decode("0x" + Style.highlight);
	private static final Color foreground = Color.decode("0x" + Style.listForeground);
	private static final Color background = Color.decode("0x" + Style.listBackground);
	private static final Color disabled = Color.decode("0x" + Style.disabled);
	
	HashMap<String, ServiceEntry> nameToServiceEntry = new HashMap<String, ServiceEntry>(); 
	
	DefaultListModel possibleServicesModel = new DefaultListModel();
	DefaultListModel currentServicesModel = new DefaultListModel();

	JList possibleServices = new JList(possibleServicesModel);
	JList currentServices  = new JList(currentServicesModel);
	
	PossibleServicesRenderer possibleServicesRenderer = new PossibleServicesRenderer();
	CurrentServicesRenderer currentServicesRenderer = new CurrentServicesRenderer();
	
	FilterListener filterListener = new FilterListener();
	
	// TODO - widgetize the "possible services" list
	public RuntimeGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		gc.gridx = 0;
		gc.gridy = 0;

		getPossibleServices(null);		
		getCurrentServices();

		currentServices.setCellRenderer(currentServicesRenderer);

		currentServices.setFixedCellWidth(200);
		possibleServices.setFixedCellWidth(200);
		
		GridBagConstraints inputgc = new GridBagConstraints();
//		inputgc.anchor = GridBagConstraints.FIRST_LINE_START;

		JScrollPane currentServicesScrollPane = new JScrollPane(currentServices);
		JScrollPane possibleServicesScrollPane = new JScrollPane(possibleServices);

		currentServices.setVisibleRowCount(20);
		possibleServices.setVisibleRowCount(20);
		
		possibleServices.setCellRenderer(possibleServicesRenderer);
		
		// make category filter buttons
		JPanel filters = new JPanel(new GridBagLayout());
		GridBagConstraints fgc = new GridBagConstraints();
		++fgc.gridy;
		fgc.fill = GridBagConstraints.HORIZONTAL;
		filters.add(new JLabel("category filters"), fgc);		
		++fgc.gridy;
		JButton nofilter = new JButton("all");
		nofilter.addActionListener(filterListener);
		filters.add(nofilter, fgc);
		++fgc.gridy;
		
		String[] cats = ServiceInfo.getUniqueCategoryNames();
		for (int j = 0; j < cats.length ; ++j)
		{
			JButton b = new JButton(cats[j]);
			b.addActionListener(filterListener);
			filters.add(b, fgc);
			++fgc.gridy;
		}
		
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());
		inputgc.anchor = GridBagConstraints.NORTH;
		inputgc.gridx = 0;
		inputgc.gridy = 1;
		input.add(filters, inputgc);
		++inputgc.gridx;
		inputgc.gridy = 0;
		input.add(new JLabel("possible services"), inputgc);
		inputgc.gridy = 1;
		input.add(possibleServicesScrollPane, inputgc);
		++inputgc.gridx;
		input.add(getReleaseServiceButton(), inputgc);
		++inputgc.gridx;
		input.add(getAddServiceButton(), inputgc);
		++inputgc.gridx;
		inputgc.gridy = 0;
		input.add(new JLabel("running services"), inputgc);
		inputgc.gridy = 1;
		input.add(currentServicesScrollPane, inputgc);

		TitledBorder title;
		title = BorderFactory.createTitledBorder("services");
		input.setBorder(title);

		display.add(input, gc);
		
		++gc.gridy;
		gc.gridx = 0;
	}
	
	public void getCurrentServices ()
	{
		HashMap<String, ServiceWrapper> services = Runtime.getRegistry();

		Map<String, ServiceWrapper> sortedMap = null;
		sortedMap = new TreeMap<String, ServiceWrapper>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = services.get(serviceName);
			String shortClassName = sw.service.getShortTypeName();
			if (sw.host != null && sw.host.accessURL != null)
			{
				// FIXME
				//namesAndClasses[i] = serviceName + " - " + shortClassName + " - " + sw.host.accessURL.getHost() ;
				//namesAndClasses[i] = new ServiceEntry(serviceName, shortClassName);
				ServiceEntry se = new ServiceEntry(serviceName, shortClassName, true);
				currentServicesModel.addElement(se);
				nameToServiceEntry.put(serviceName, se);
			} else {
				//namesAndClasses[i] = serviceName + " - " + shortClassName;
				//namesAndClasses[i] = new ServiceEntry(serviceName, shortClassName);
				ServiceEntry se = new ServiceEntry(serviceName, shortClassName, false);
				currentServicesModel.addElement(se);
				nameToServiceEntry.put(serviceName, se);
			}
		}
	}

	public JButton getAddServiceButton() {
		addServiceButton = new BasicArrowButton(BasicArrowButton.EAST);
		addServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				String newService = ((ServiceEntry) possibleServices.getSelectedValue()).toString();
				
				
				if (ServiceInfo.hasUnfulfilledDependencies("org.myrobotlab.service." + newService))
				{
					// dependencies needed !!!
					String msg = "<html>This Service has dependencies which are not yet loaded,<br>" +
					"do you wish to download them now?";
					
					int result = JOptionPane.showConfirmDialog((Component) null, msg,
					        "alert", JOptionPane.OK_CANCEL_OPTION);
					if (result == JOptionPane.CANCEL_OPTION)
					{
						return;
					}
					
					// FIXME - FIXME - FIXME 
					// Test & Develop appropriately - ie - remove all lib references to the repo
					// use the libraries directory !! - fill with a Ivy call !!! - you need to 
					// work with ivy standalone to do so ...
					// you will need to restart in order to use the newly downloaded Service
					// would you like to restart now ?
					// -- additionally have a download all Service dependencies to get it out of the way
					// -- additionally a check for updates
					// -- additionally a Service report (versions like drupal)
					// -- additionally force a different configuration (like dev - bleeding edge)
					// finish all "released" Services 
					
					
				}
				
				JFrame frame = new JFrame();
				frame.setTitle("add new service");
				String name = JOptionPane.showInputDialog(frame,"new service name");
				
				if (name != null) {					
					myService.send(boundServiceName, "createAndStart", name, newService);
					// FYI - this is an asynchronous request - to handle call back
					// you must register for a "registered" event on the local or remote Runtime
				}

			}

		});

		return addServiceButton;
	}
	
	public ServiceWrapper registered (ServiceWrapper sw)
	{
		String typeName;
		if (sw.service == null)
		{
			typeName = "unknown";
		} else {
			typeName = sw.service.getShortTypeName();
		}
		ServiceEntry newServiceEntry = new ServiceEntry(sw.name, 
				typeName,
				(sw.host != null && sw.host.accessURL != null));
		currentServicesModel.addElement(newServiceEntry);
		nameToServiceEntry.put(sw.name, newServiceEntry);
		//myService.loadTabPanels();
		myService.addTab(sw.name);
		return sw;
	}

	public ServiceWrapper released (ServiceWrapper sw)
	{
		// FIXME - bug if index is moved before call back is processed
		//ServiceEntry selected = (ServiceEntry) currentServices.getSelectedValue();
		//ServiceEntry newServiceEntry = new ServiceEntry(newServiceName, selected.type);
		myService.removeTab(sw.name);// FIXME will bust when service == null
		if (nameToServiceEntry.containsKey(sw.name))
		{
			currentServicesModel.removeElement(nameToServiceEntry.get(sw.name));
		} else {
			LOG.error(sw.name + " released event - but could not find in currentServiceModel");
		}
		//myService.loadTabPanels();
		return sw;
	}
	
	public JButton getReleaseServiceButton() {
		releaseServiceButton = new BasicArrowButton(BasicArrowButton.WEST);
		releaseServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ServiceEntry oldService = (ServiceEntry) currentServices.getSelectedValue();
				//currentServicesModel.removeElement(oldService); // FIXME - callback from releaseService !!!!
				myService.send(boundServiceName, "releaseService", oldService.name);
				//myService.loadTabPanels(); // FIXME - callback from releaseService !!!!
			}

		});

		return releaseServiceButton;
	}

	@Override
	public void attachGUI() {
		sendNotifyRequest("registered", "registered", ServiceWrapper.class);
		sendNotifyRequest("released", "released", ServiceWrapper.class);
		sendNotifyRequest("failedDependency", "failedDependency", String.class);
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("registered", "registered", ServiceWrapper.class);
		removeNotifyRequest("released", "released", ServiceWrapper.class);
		removeNotifyRequest("failedDependency", "failedDependency", String.class);
	}
	
	public void failedDependency (String dep)
	{
		JOptionPane.showMessageDialog(null, "<html>Unable to load Service...<br>" + dep + "</html>", "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	class ServiceEntry {
		public String name;
		public String type;
		public boolean loaded = false;
		public boolean isRemote = false;
		ServiceEntry(String name, String type, boolean isRemote)
		{
			this.name = name;
			this.type = type;
			this.isRemote = isRemote;
		}
		
		public String toString()
		{
			return type;
		}
	}

	class CurrentServicesRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public CurrentServicesRenderer() {
			setOpaque(true);
			setIconTextGap(12);
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			ServiceEntry entry = (ServiceEntry) value;

			setText("<html><font color=#" + Style.base + ">" + entry.name
					+ "</font></html>");

			ImageIcon icon = Util.getScaledIcon(Util.getImage(
					(entry.type + ".png").toLowerCase(), "unknown.png"), 0.50);
			setIcon(icon);

			if (isSelected) {
				setBackground(highlight);
				setForeground(background);
			} else {
				setBackground(background);
				setForeground(foreground);
			}

			return this;
		}
	}
	
	class PossibleServicesRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public PossibleServicesRenderer() {
			setOpaque(true);
			setIconTextGap(12);
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			ServiceEntry entry = (ServiceEntry) value;

			boolean available = !(ServiceInfo.hasUnfulfilledDependencies("org.myrobotlab.service." + entry.type)); 

			setText(entry.type);
			
			ImageIcon icon = Util.getScaledIcon(Util.getImage(
					(entry.type + ".png").toLowerCase(), "unknown.png"), 0.50);
			setIcon(icon);

			if (isSelected && available) {
				setBackground(highlight);
				setForeground(background);
			} else if (!isSelected && available) {
				setBackground(background);
				setForeground(foreground);
			} else if (isSelected && !available) {
				setBackground(highlight);
				setForeground(foreground);
			} else if (!isSelected && !available) {
				setBackground(disabled);
				setForeground(background);
			}

			return this;
		}
	}

	public void getPossibleServices(String filter)
	{
		possibleServicesModel.clear();
		String[] sscn = Runtime.getServiceShortClassNames(filter);
		ServiceEntry[] ses = new ServiceEntry[sscn.length];
		for (int i = 0; i < ses.length; ++i)
		{
			possibleServicesModel.addElement(new ServiceEntry(null, sscn[i], false));
		}

	}
		
	class FilterListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent cmd) {
			LOG.info(cmd.getActionCommand());
			if ("all".equals(cmd.getActionCommand()))
			{
				getPossibleServices(null);
			} else {
				getPossibleServices(cmd.getActionCommand());
			}
		}
		
	}
}