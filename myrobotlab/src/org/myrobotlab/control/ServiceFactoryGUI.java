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
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.ServiceFactory;
import org.myrobotlab.service.interfaces.GUI;

public class ServiceFactoryGUI extends ServiceGUI {

	public final static Logger LOG = Logger.getLogger(ServiceFactoryGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	JList possibleServices;
	JList currentServices;
	BasicArrowButton addServiceButton = null;
	BasicArrowButton removeServiceButton = null;
	
	// TODO - widgetize the "possible services" list
	public ServiceFactoryGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {

		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		String[] sscn = ServiceFactory.getServiceShortClassNames();
		ServiceEntry[] ses = new ServiceEntry[sscn.length];
		for (int i = 0; i < ses.length; ++i)
		{
			ses[i] = new ServiceEntry(sscn[i]);
		}
				
		possibleServices = new JList(ses);
		possibleServices.setCellRenderer(new ServiceRenderer());

		//HashMap<String, ServiceEntry> services = myService.getHostCFG().getServiceMap();
		HashMap<String, ServiceWrapper> services = RuntimeEnvironment.getRegistry();
		
		Map<String, ServiceWrapper> sortedMap = null;
		sortedMap = new TreeMap<String, ServiceWrapper>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		String[] namesAndClasses = new String[sortedMap.size()];
		int i = 0;
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = services.get(serviceName);
			String shortClassName = sw.service.serviceClass.substring(sw.service.serviceClass.lastIndexOf(".") + 1);
			if (sw.host != null && sw.host.accessURL != null)
			{
				namesAndClasses[i] = serviceName + " - " + shortClassName + " - " + sw.host.accessURL.getHost() ;
			} else {
				namesAndClasses[i] = serviceName + " - " + shortClassName;
			}
			++i;
		}

		currentServices = new JList(namesAndClasses);

		GridBagConstraints inputgc = new GridBagConstraints();
		inputgc.anchor = GridBagConstraints.FIRST_LINE_START;

		JScrollPane currentServicesScrollPane = new JScrollPane(currentServices);
		JScrollPane possibleServicesScrollPane = new JScrollPane(possibleServices);

		currentServices.setVisibleRowCount(20);
		possibleServices.setVisibleRowCount(20);
		input.add(possibleServicesScrollPane, inputgc);
		input.add(getRemoveServiceButton(), inputgc);
		input.add(getAddServiceButton(), inputgc);
		input.add(currentServicesScrollPane, inputgc);

		TitledBorder title;
		title = BorderFactory.createTitledBorder("local services - current");
		input.setBorder(title);

		display.add(input, gc);
		
		++gc.gridy;
		gc.gridx = 0;
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
					myService.loadTabPanels(); 
				}

			}

		});

		return addServiceButton;
	}

	public JButton getRemoveServiceButton() {
		removeServiceButton = new BasicArrowButton(BasicArrowButton.WEST);
		removeServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String oldService = (String) currentServices.getSelectedValue();
				myService.send(boundServiceName, "removeService", oldService);
				myService.loadTabPanels();
			}

		});

		return removeServiceButton;
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}
	
	class ServiceEntry {
		public String type;
		public boolean loaded = false;
		ServiceEntry(String type)
		{
			this.type = type;
		}
		
		public String toString()
		{
			return type;
		}
	}
	
	
	private static final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);
	
	class ServiceRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public ServiceRenderer() {
		    setOpaque(true);
		    setIconTextGap(12);
		  }

		  public Component getListCellRendererComponent(JList list, Object value,
		      int index, boolean isSelected, boolean cellHasFocus) {
			  ServiceEntry entry = (ServiceEntry) value;
		    setText(entry.type);
		    ImageIcon icon = 
		    		ServiceGUI.getScaledIcon(
		    		ServiceGUI.getImage((entry.type + ".png").toLowerCase(),"help.png"), 0.50); 
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
}