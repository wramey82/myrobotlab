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

package org.myrobotlab.net;

import java.io.Serializable;
import java.net.URI;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class CommunicationManager implements Serializable, CommunicationInterface {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(CommunicationManager.class);
	Service myService = null;
	Outbox outbox = null;

	private Communicator comm = null;

	public CommunicationManager(Service myService) {
		// set local private references
		this.myService = myService;
		this.outbox = myService.getOutbox();

		String communicatorClass = "org.myrobotlab.net.CommObjectStreamOverTCP";
		log.info("instanciating a " + communicatorClass);
		Communicator c = (Communicator) Service.getNewInstance(communicatorClass, myService);

		outbox.setCommunicationManager(this);

		setComm(c);

	}

	/**
	 * getComm(uri) gets the local service responsible for sending the message remotely
	 * .send(uri, msg) uri is a key into that service's data to send the message where it
	 * needs to go
	 * 
	 */
	public void send(final URI uri, final Message msg) {
		getComm(uri).sendRemote(uri, msg);
	}

	public void send(final Message msg) {

		ServiceInterface sw = Runtime.getService(msg.getName());
		if (sw == null) {
			myService.error("Runtime.getService could not return %s.%s for sender %s ", msg.name, msg.method, msg.sender);
			return;
		}
		
		URI host = sw.getHost();
		if (host == null) {
			log.info(String.format("local %s.%s->%s/%s.%s(%s)", msg.sender, msg.sendingMethod, sw.getHost(), msg.name, msg.method, Encoder.getParameterSignature(msg.data)));
			sw.in(msg);
		} else {
			log.info(String.format("remote %s.%s->%s/%s.%s(%s)", msg.sender, msg.sendingMethod, sw.getHost(), msg.name, msg.method, Encoder.getParameterSignature(msg.data)));
			getComm(host).sendRemote(host, msg);
		}
	}

	public void setComm(Communicator comm) {
		this.comm = comm;
	}

	public Communicator getComm(URI uri) {
		if (uri.getScheme().equals(Encoder.SCHEME_MRL))
		{
			ServiceInterface sw = Runtime.getService(uri.getHost());
			Communicator c = (Communicator)sw;
			return c;
		}
		// FIXME remove - keeping only for deprecated RemoteAdapter	
		// should be ERROR if can not return with URI !!! - return null
		return comm;
	}

}
