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

package org.myrobotlab.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import org.myrobotlab.comm.CommunicationManager;

/*
 * Outbox is a message based thread which sends messages based on notify lists and current
 * queue status.  It is only aware of the Service directory, notify lists, and operators.
 * It can (if possible) take a message and move it to the inbox of a local service, or
 * (if necessary) send it to a local operator.
 * 
 * It knows nothing about protocols, serialization methods, or communication methods.
 */

public class Outbox extends Thread {
	public final static Logger LOG = Logger.getLogger(Outbox.class.getCanonicalName());

	Service myService = null;
	LinkedList<Message> msgBox = new LinkedList<Message>();
	// TODO - these all have to be CFG'd
	boolean isRunning = false;
	boolean bufferOverrun = false;
	boolean blocking = false;
	int maxQueue = 10;

	public HashMap<String, ArrayList<NotifyEntry>> notifyList = new HashMap<String, ArrayList<NotifyEntry>>();
	CommunicationManager comm = null;

	public Outbox(Service myService) {
		super(myService.name + "_outbox");
		this.myService = myService;
	}

	public CommunicationManager getCommunicationManager() {
		return comm;
	}

	public void setCommunicationManager(CommunicationManager c) {
		this.comm = c;
	}

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			Message msg = null;
			synchronized (msgBox) {
				try {
					while (msgBox.size() == 0) {
						LOG.debug("outbox run WAITING " + this.getName());
						msgBox.wait(); // must own the lock
					}
				} catch (InterruptedException ex) {
					LOG.debug("outbox run INTERRUPTED " + this.getName());
					// msgBox.notifyAll();
					isRunning = false;
					continue;
				}
				msg = msgBox.removeLast();
				LOG.debug("removed from msgBox size now " + msgBox.size());
				msgBox.notifyAll();
			}

			// TODO - refer to ticket #80
			// all of this needs to be controlled by Service paramters
			// TODO - clean up - (name || hostname && serviceport) &&
			// outboxMsgHandling == RELAY
			if (msg.name.length() > 0
					&& myService.outboxMsgHandling.compareTo(Service.RELAY) == 0
					|| msg.msgType.compareTo("S") == 0) {
				LOG.info("configured to RELAY " + msg.name);
				comm.send(msg);

			}

			if (notifyList.size() != 0) {
				LOG.error(myService.name + " notify list size " + notifyList.size());
				LOG.info("notifying");
				ArrayList<NotifyEntry> subList = notifyList.get(msg.sendingMethod.toString()); // Get the value for
															// the sourceMethod
				if (subList == null) {
					LOG.info("no static route for " + msg.sender + "."
							+ msg.sendingMethod); // This will cause issues in
													// broadcasts
					continue;
				}

				for (int i = 0; i < subList.size(); ++i) {
					NotifyEntry ne = subList.get(i);
					msg.name = ne.name;
					msg.method = ne.inMethod_;
					if (i != 0) // TODO - this is crap refactor
					{
						// TODO - optimization do NOT make copy of message on
						// sending end -
						// ONLY make copy from recieving end - this will work
						// with and without Serialization
						msg = new Message(msg);
					}
					comm.send(msg);
				}
			} else {
				LOG.debug(msg.name + "/" + msg.method + "#"
						+ msg.getParameterSignature() + " notifyList is empty");
				continue;
			}

		} // while (isRunning)
	}

	// TODO - config to put message in block mode - with no buffer overrun
	// TODO - config to drop message without buffer overrun e.g. like UDP
	public void add(Message msg) {
		synchronized (msgBox) {
			while (blocking && msgBox.size() == maxQueue)
				// queue "full"
				try {
					LOG.debug("outbox enque msg WAITING " + this.getName());
					msgBox.wait(); // Limit the size
				} catch (InterruptedException ex) {
					LOG.debug("outbox add enque msg INTERRUPTED "
							+ this.getName());
				}

			// we warn if over 10 messages are in the queue - but we will still process them
			if (msgBox.size() > maxQueue) {
				bufferOverrun = true;
				LOG.warn(getName() + " outbox BUFFER OVERRUN size "
						+ msgBox.size());
			}
			msgBox.addFirst(msg);
			LOG.debug(this.getName() + " msgBox size " + msgBox.size()
					+ " msg [" + msg.method + "(" + msg.getParameterSignature()
					+ ")]");
			msgBox.notifyAll(); // must own the lock
		}
	}

	public int size() {
		return msgBox.size();
	}

}
