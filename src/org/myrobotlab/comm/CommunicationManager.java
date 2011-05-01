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

package org.myrobotlab.comm;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;

public class CommunicationManager {

	public final static Logger LOG = Logger
			.getLogger(CommunicationManager.class.toString());
	Service myService = null;
	Outbox outbox = null;
	ConfigurationManager hostcfg = null;

	private Communicator comm = null;

	public CommunicationManager(Service myService) {
		// set local private references
		this.myService = myService;
		this.outbox = myService.getOutbox();
		this.hostcfg = myService.getHostCFG(); // Communication is at a host
												// level

		// get hostcfg - get a communicator
		String communicatorClass = hostcfg.get("Communicator");
		LOG.info("instanciating a " + communicatorClass);
		Communicator c = (Communicator) Service.getNewInstance(
				communicatorClass, myService);

		// setting the outbox's communcation manager
		outbox.setCommunicationManager(this);

		setComm(c);

	}

	public void send(final Message msg) {
		Service local = (Service) hostcfg.getLocalServiceHandle(msg.name);

		if (local != null) {
			LOG.info("sending local");
			Message m = new Message(msg); // TODO UNECESSARY - BUT TOO SCARED TO
											// REMOVE !!!!
			local.in(m);
		} else {
			LOG.info("sending " + msg.method + " remote");
			getComm().send(msg);
		}
	}

	void sendLocal(Message msg) {
		Object s = hostcfg.getLocalServiceHandle(msg.name);
		if (s != null) {
			// TODO - OPTIMIZATON - copy only on recieving !!!!
			Message m = new Message(msg);
			// address msg appropriately relative to this notify entry

			LOG.info("sending local [" + msg.sender + "." + msg.sendingMethod
					+ "->" + msg.name + "." + msg.method + "#"
					+ msg.getParameterSignature() + "]");
			((Service) s).in(m);

		} else {
			LOG.error("did not find valid recipient " + msg.name);
		}
	}

	public void setComm(Communicator comm) {
		this.comm = comm;
	}

	public Communicator getComm() {
		return comm;
	}

}
