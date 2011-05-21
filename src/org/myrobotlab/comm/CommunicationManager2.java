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

import java.io.Serializable;
import java.net.URL;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Communicator;

public class CommunicationManager2  implements Serializable, CommunicationInterface{

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(CommunicationManager2.class.toString());
	Service myService = null;
	Outbox outbox = null;

	private Communicator comm = null;

	public CommunicationManager2(Service myService) {
		// set local private references
		this.myService = myService;
		this.outbox = myService.getOutbox();

		String communicatorClass ="org.myrobotlab.comm.CommObjectStreamOverTCPUDP2";
		LOG.info("instanciating a " + communicatorClass);
		Communicator c = (Communicator) Service.getNewInstance(communicatorClass, myService);

		outbox.setCommunicationManager(this);

		setComm(c);

	}

	public void send(final Message msg) {
		
		ServiceWrapper sw = RuntimeEnvironment.getService(myService.url, msg.name);
		if (sw.host.accessURL != null && !sw.host.accessURL.equals(myService.url))
		{
			LOG.info("sending " + msg.method + " remote");
			getComm().send(sw.host.accessURL, msg);			
		} else {
			LOG.info("sending local");
			Message m = new Message(msg); // TODO UNECESSARY - BUT TOO SCARED TO REMOVE !!
			sw.get().in(m);			
		}

		/*
		if (!sw.isRemote()) {
			LOG.info("sending local");
			Message m = new Message(msg); // TODO UNECESSARY - BUT TOO SCARED TO REMOVE !!
			sw.get().in(m);
		} else {
			LOG.info("sending " + msg.method + " remote");
			getComm().send(msg);
		}
		*/
	}

	public void setComm(Communicator comm) {
		this.comm = comm;
	}

	public Communicator getComm() {
		return comm;
	}

}
