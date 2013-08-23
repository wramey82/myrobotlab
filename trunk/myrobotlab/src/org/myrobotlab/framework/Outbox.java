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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;

import org.myrobotlab.service.interfaces.CommunicationInterface;

/*
 * Outbox is a message based thread which sends messages based on addListener lists and current
 * queue status.  It is only aware of the Service directory, addListener lists, and operators.
 * It can (if possible) take a message and move it to the inbox of a local service, or
 * (if necessary) send it to a local operator.
 * 
 * It knows nothing about protocols, serialization methods, or communication methods.
 */

public class Outbox implements Runnable, Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Outbox.class.getCanonicalName());

	Service myService = null;
	LinkedList<Message> msgBox = new LinkedList<Message>();
	// TODO - these all have to be CFG'd
	boolean isRunning = false;
	boolean bufferOverrun = false;
	boolean blocking = false;
	int maxQueue = 1024;
	int initialThreadCount = 1;
	transient ArrayList<Thread> outboxThreadPool = new ArrayList<Thread>();

	public HashMap<String, ArrayList<MRLListener>> notifyList = new HashMap<String, ArrayList<MRLListener>>();
	CommunicationInterface comm = null;

	public Outbox(Service myService) {
		this.myService = myService;
	}

	public CommunicationInterface getCommunicationManager() {
		return comm;
	}

	public void setCommunicationManager(CommunicationInterface c) {
		this.comm = c;
	}

	public void start() {
		for (int i = outboxThreadPool.size(); i < initialThreadCount; ++i) {
			Thread t = new Thread(this, myService.getName() + "_outbox_" + i);
			outboxThreadPool.add(t);
			t.start();
		}
	}

	public void stop() {
		isRunning = false;
		for (int i = 0; i < outboxThreadPool.size(); ++i) {
			Thread t = outboxThreadPool.get(i);
			t.interrupt();
			outboxThreadPool.remove(i);
			t = null;
		}
	}

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			Message msg = null;
			synchronized (msgBox) {
				try {
					while (msgBox.size() == 0) {
						// log.debug("outbox run WAITING ");
						msgBox.wait(); // must own the lock
					}	
				} catch (InterruptedException ex) {
					log.debug("outbox run INTERRUPTED ");
					// msgBox.notifyAll();
					isRunning = false;
					continue;
				}
				msg = msgBox.removeLast();
				//chase network bugs 
				//log.error(String.format("%s.outbox.run(msg) %s.%s -- %s.%s ", myService.getName(), msg.sender, msg.sendingMethod, msg.name, msg.method));
				// log.debug(String.format("removed from msgBox size now %d", msgBox.size()));
				msgBox.notifyAll();
			}

			// TODO - refer to ticket #80
			// all of this needs to be controlled by Service paramters
			// TODO - clean up - (name || hostname && serviceport) &&
			// outboxMsgHandling == RELAY
			if (msg.getName().length() > 0 && myService.outboxMsgHandling.compareTo(Service.RELAY) == 0 || "S".equals(msg.msgType)) {
				log.debug("{} configured to RELAY ", msg.getName());
				comm.send(msg);

			}

			if (notifyList.size() != 0) {

				// key is now sendingMethod.destName.methodName - parameterType
				// are
				// left out until invoke time
				ArrayList<MRLListener> subList = notifyList.get(msg.sendingMethod); // Get
																					// the
																					// value
																					// for
				// the sourceMethod
				if (subList == null) {
					log.debug("no static route for " + msg.sender + "." + msg.sendingMethod);
					// This will cause issues in broadcasts
					continue;
				}

				for (int i = 0; i < subList.size(); ++i) {
					MRLListener listener = subList.get(i);
					msg.name = listener.name;
					msg.method = listener.inMethod;
					if (i != 0) // TODO - this is crap refactor
					{
						// TODO - optimization do NOT make copy of message on
						// sending end -
						// ONLY make copy from recieving end - this will work
						// with and without Serialization
						msg = new Message(msg);
					}
					
					//chase network bugs 
					//log.error(String.format("%s.outbox.com.send(msg) %s.%s --> %s.%s ", myService.getName(), msg.sender, msg.sendingMethod, msg.name, msg.method));

					comm.send(msg);
				}
			} else {
				log.debug(msg.getName() + "/" + msg.method + "#" + msg.getParameterSignature() + " notifyList is empty");
				continue;
			}

		} // while (isRunning)
	}

	// TODO - config to put message in block mode - with no buffer overrun
	// TODO - config to drop message without buffer overrun e.g. like UDP
	public void add(Message msg) {
		//chase network bugs 
		//log.error(String.format("%s.outbox.add(msg) %s.%s --> %s.%s", myService.getName(), msg.sender, msg.sendingMethod, msg.name, msg.method));
		synchronized (msgBox) {
			while (blocking && msgBox.size() == maxQueue)
				// queue "full"
				try {
					// log.debug("outbox enque msg WAITING ");
					msgBox.wait(); // Limit the size
				} catch (InterruptedException ex) {
					log.debug("outbox add enque msg INTERRUPTED ");
				}

			// we warn if over 10 messages are in the queue - but we will still
			// process them
			if (msgBox.size() > maxQueue) {
				bufferOverrun = true;
				log.warn(" outbox BUFFER OVERRUN size " + msgBox.size());
			}
			msgBox.addFirst(msg);

			if (log.isDebugEnabled()) {
				log.debug(String.format("msg [%s.%s (%s)]", msg.name, msg.method, msg.getParameterSignature()));
			}
			msgBox.notifyAll(); // must own the lock
		}
	}

	public int size() {
		return msgBox.size();
	}

}
