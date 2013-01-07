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
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JLabel;

import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;

public class RemoteAdapterGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;

	CommunicationNodeList list = new CommunicationNodeList();

	public RemoteAdapterGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		gc.gridx = 0;
		gc.gridy = 0;
		display.add(new JLabel("number of connections : 5"), gc);
		gc.gridy = 1;
		display.add(new JLabel("last activity : arduino.digitaWrite"), gc);
		gc.gridy = 2;
		display.add(new JLabel("number of messages : 1388"), gc);
		gc.gridx = 0;
		++gc.gridy;
		// list.setPreferredSize(new Dimension(arg0, arg1))
		gc.gridwidth = 4;
		gc.fill = GridBagConstraints.HORIZONTAL;
		display.add(list, gc);
		/*
		 * list.model.add(0, (Object)new CommunicationNodeEntry(
		 * "0.0.0.0:6432 -> 192.168.0.5:6767 latency 32ms rx 30 tx 120 msg 5 UDP"
		 * , "3.gif")); list.model.add(0, (Object)new
		 * CommunicationNodeEntry("192.168.0.3:6767 disconnected ", "3.gif"));
		 * list.model.add(0, (Object)new CommunicationNodeEntry(
		 * "0.0.0.0:6432 -> 192.168.0.4:6767 latency 14ms rx 12 tx 430 msg 5 UDP"
		 * , "3.gif")); list.model.add(0, (Object)new CommunicationNodeEntry(
		 * "0.0.0.0:6432 -> 192.168.0.7:6767 latency 05ms rx 14 tx 742 msg 5 UDP"
		 * , "3.gif"));
		 */
		updateNodeList();

	}

	public void updateNodeList() {
		HashMap<URI, ServiceEnvironment> services = Runtime.getServiceEnvironments();
		log.info("service count " + Runtime.getRegistry().size());

		Iterator<URI> it = services.keySet().iterator();

		list.model.removeAllElements();

		while (it.hasNext()) {
			URI url = it.next();
			if (url != null) {
				list.model.add(0, (Object) new CommunicationNodeEntry(url.toString(), "3.gif"));
			}
		}

	}

	public void getState(RemoteAdapter data) {
		if (data != null) {
			updateNodeList();
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", RemoteAdapter.class);
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", RemoteAdapter.class);
	}

}
