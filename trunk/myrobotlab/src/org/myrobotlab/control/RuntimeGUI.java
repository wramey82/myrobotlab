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
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceInfo;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;

public class RuntimeGUI extends ServiceGUI {

	public final static Logger LOG = Logger.getLogger(RuntimeGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	BasicArrowButton addServiceButton = null;
	BasicArrowButton releaseServiceButton = null;
	private static final Color HIGHLIGHT_COLOR = new Color(0, 0xEE, 0x22);
	
	DefaultListModel possibleServicesModel = new DefaultListModel();
	DefaultListModel currentServicesModel = new DefaultListModel();

	JList possibleServices = new JList(possibleServicesModel);
	JList currentServices  = new JList(currentServicesModel);
	
	// TODO - widgetize the "possible services" list
	public RuntimeGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {

		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		getPossibleServices(null);
		
		getCurrentServices();

		//currentServices = new JList(namesAndClasses);
		currentServices.setCellRenderer(new ServiceRenderer());

		currentServices.setFixedCellWidth(200);
		possibleServices.setFixedCellWidth(200);
		
		GridBagConstraints inputgc = new GridBagConstraints();
		inputgc.anchor = GridBagConstraints.FIRST_LINE_START;

		JScrollPane currentServicesScrollPane = new JScrollPane(currentServices);
		JScrollPane possibleServicesScrollPane = new JScrollPane(possibleServices);

		currentServices.setVisibleRowCount(20);
		possibleServices.setVisibleRowCount(20);
		
		possibleServices.setCellRenderer(new ServiceRenderer());
		
		// make category filter buttons
		JPanel filters = new JPanel(new GridBagLayout());
		GridBagConstraints fgc = new GridBagConstraints();
		++fgc.gridy;
		fgc.fill = GridBagConstraints.HORIZONTAL;
		filters.add(new JLabel("filters"), fgc);		
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
				
		input.add(filters, inputgc);
		input.add(possibleServicesScrollPane, inputgc);
		input.add(getReleaseServiceButton(), inputgc);
		input.add(getAddServiceButton(), inputgc);
		input.add(currentServicesScrollPane, inputgc);

		TitledBorder title;
		title = BorderFactory.createTitledBorder("local services - current");
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

		//ServiceEntry[] namesAndClasses = new ServiceEntry[sortedMap.size()];
		int i = 0;
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = services.get(serviceName);
			String shortClassName = sw.service.getShortTypeName();
			if (sw.host != null && sw.host.accessURL != null)
			{
				//namesAndClasses[i] = serviceName + " - " + shortClassName + " - " + sw.host.accessURL.getHost() ;
				//namesAndClasses[i] = new ServiceEntry(serviceName, shortClassName);
				currentServicesModel.addElement(new ServiceEntry(serviceName, shortClassName));
			} else {
				//namesAndClasses[i] = serviceName + " - " + shortClassName;
				//namesAndClasses[i] = new ServiceEntry(serviceName, shortClassName);
				currentServicesModel.addElement(new ServiceEntry(serviceName, shortClassName));
			}
			++i;
		}
	}

	public JButton getAddServiceButton() {
		addServiceButton = new BasicArrowButton(BasicArrowButton.EAST);
		addServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JFrame frame = new JFrame();
				frame.setTitle("add new service");
				String name = JOptionPane.showInputDialog(frame,"new service name");
				if (name != null) {
					String newService = ((ServiceEntry) possibleServices.getSelectedValue()).toString();
					myService.send(boundServiceName, "createAndStart", name, newService);
					// TODO - this is asynchronous - the service will be created later
					// - Especially on a remote process
					// it would be nice to momentarily block on this call !!!
					// in the interim - do a pause to allow the Service to start & register
					// before updating the  GUI
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						Service.stackToString(e1);
					}
					myService.loadTabPanels();// FIXME - reload on Runtime register service event !
					ServiceEntry newServiceEntry = (ServiceEntry) possibleServices.getSelectedValue();
					newServiceEntry.name = name;
					currentServicesModel.addElement(newServiceEntry);// FIXME - reload on Runtime register service event !
				}

			}

		});

		return addServiceButton;
	}

	public JButton getReleaseServiceButton() {
		releaseServiceButton = new BasicArrowButton(BasicArrowButton.WEST);
		releaseServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ServiceEntry oldService = (ServiceEntry) currentServices.getSelectedValue();
				currentServicesModel.removeElement(oldService); // FIXME - callback from releaseService !!!!
				myService.send(boundServiceName, "releaseService", oldService.type);
				myService.loadTabPanels(); // FIXME - callback from releaseService !!!!
			}

		});

		return releaseServiceButton;
	}

	@Override
	public void attachGUI() {
		// FIXME - to be implemented when merged with Runtime
		//sendNotifyRequest("released", "released", String.class); 
		//sendNotifyRequest("publishState", "getState", Arduino.class);
		//myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		// FIXME - to be implemented when merged with Runtime
		//removeNotifyRequest("released", "released", String.class);
	}
	
	class ServiceEntry {
		public String name;
		public String type;
		public boolean loaded = false;
		ServiceEntry(String name, String type)
		{
			this.name = name;
			this.type = type;
		}
		
		public String toString()
		{
			return type;
		}
	}
	
	class ServiceRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public ServiceRenderer() {
		    setOpaque(true);
		    setIconTextGap(12);
		  }

		  public Component getListCellRendererComponent(JList list, Object value,
		      int index, boolean isSelected, boolean cellHasFocus) {
			  ServiceEntry entry = (ServiceEntry) value;
			  String title = "";
			  if (entry.type != null)
			  {
				  title = entry.type;
			  }
			  if (entry.name != null)
			  {
				  title = entry.name;
			  }
		
			  if (entry.name != null)
			  {
				  setText("<html><font color=#004400>" + title + "</font></html>");
			  } else {
				  setText("<html><font color=#BBBBBB>" + title + "</font></html>");
			  }
			
		    ImageIcon icon = 
		    		ServiceGUI.getScaledIcon(
		    		ServiceGUI.getImage((entry.type + ".png").toLowerCase(),"unknown.png"), 0.50); 
		    setIcon(icon);
		    
		    if (isSelected) {
		      setBackground(HIGHLIGHT_COLOR);
		      setForeground(Color.white);
		    } else {
		      setBackground(Color.white);
		      setForeground(Color.black);
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
			//ses[i] = new ServiceEntry(null, sscn[i]);
			possibleServicesModel.addElement(new ServiceEntry(null, sscn[i]));
		}
				
		//possibleServices = new JList(ses);
	}
	
	FilterListener filterListener = new FilterListener();
	
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