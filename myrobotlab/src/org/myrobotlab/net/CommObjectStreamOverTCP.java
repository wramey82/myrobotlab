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
 * References :
 * 		http://www.javaspecialists.eu/archive/Issue088.html - details of ObjectOutputStream.reset()
 * 		http://zerioh.tripod.com/ressources/sockets.html - example of Object serialization
 * 		http://www.cafeaulait.org/slides/sd2003west/sockets/Java_Socket_Programming.html nice simple resource
 * 		http://stackoverflow.com/questions/1480236/does-a-tcp-socket-connection-have-a-keep-alive
 * 
 * TCP can detect if a endpoint is "closed" - it also has the capability of using SO_KEEPALIVE
 * which will detect a broken connection - but the details are left up to the operating system (with
 * interval up to 2 hours!)
 * 	I believe a small interval keepalive with very small data-packet would be beneficial for both TCP & UDP
 *  Communicators
 *  
 *  A dead heartbeat would mean removal of all references of the dead system from the running system
 * 
 * */

package org.myrobotlab.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.Communicator;

public class CommObjectStreamOverTCP extends Communicator implements Serializable {

	public final static Logger log = LoggerFactory.getLogger(CommObjectStreamOverTCP.class.getCanonicalName());
	private static final long serialVersionUID = 1L;
	boolean isRunning = false;
	static public transient HashMap<URI, TCPThread> clientList = new HashMap<URI, TCPThread>();
	Service myService = null;

	public class TCPThread extends Thread {

		URI url;
		transient Socket socket = null;

		ObjectInputStream in = null;
		ObjectOutputStream out = null;

		public TCPThread(URI url, Socket socket) throws UnknownHostException, IOException {
			super("tcp " + url);
			this.url = url;
			if (socket == null) {
				socket = new Socket(url.getHost(), url.getPort());
			}
			this.socket = socket;
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();// some flush before using :)
			in = new ObjectInputStream(socket.getInputStream());
			this.start(); // starting listener
		}

		// run / listen for more msgs
		@Override
		public void run() {
			try {
				isRunning = true;

				while (socket != null && isRunning) {

					Message msg = null;

					try {
						Object o = in.readObject();
						msg = (Message) o;
					} catch (Exception e) {
						Service.logException(e);
						msg = null;
						Service.logException(e);
						log.error(url + " connection failure - shutting down");
						log.error("removing url from registry");
						Runtime.release(url);
						log.error("removing client from clientList");
						clientList.remove(url);
						log.error("shutting down thread");
						isRunning = false;
						log.error("attempting to close streams");
						in.close();
						out.close();
						log.error("attempting to close socket");
						socket.close();
					}
					if (msg == null) {
						log.error("msg deserialized to null");
					} else {
						if (msg.method.equals("registerServices")) {
							// FIXME - the only reason this is pulled off the
							// comm line here
							// is initial registerServices do not usually come
							// with a "name"
							myService.registerServices(socket.getInetAddress().getHostAddress(), socket.getPort(), msg);
							continue;
						}
						myService.getInbox().add(msg);
					}
				}

				// closing connections TODO - why wouldn't you close the others?
				in.close();
				out.close();

			} catch (IOException e) {
				log.error("TCPThread threw");
				isRunning = false;
				socket = null;
				Service.logException(e);
			}

			// connection has been broken
			// myService.invoke("connectionBroken", url); FIXME
		}

		public Socket getSocket() {
			return socket;
		}

		public synchronized void send(URI url2, Message msg) { // FIX'd !!! you
																// had to
																// synchronize !
			try {
				out.writeObject(msg);
				out.flush();

			} catch (Exception e) {
				Service.logException(e);
			}
		}

	} // TCP Thread

	public CommObjectStreamOverTCP(Service service) {
		this.myService = service;
	}

	// send tcp
	@Override
	public void send(final URI url, final Message msg) {

		TCPThread phone = null;
		if (clientList.containsKey(url)) {
			phone = clientList.get(url);
		} else {
			log.info("could not find url in client list attempting new connection ");
			try {
				phone = new TCPThread(url, null);
				clientList.put(url, phone);
			} catch (Exception e) {
				Service.logException(e);
				log.error("could not connect to " + url);
				return;
			}
		}

		phone.send(url, msg);
	}

	HashMap<URI, Heart> heartbeatList = new HashMap<URI, Heart>();
	boolean useHeartbeat = false;

	public synchronized void addClient(URI url, Object commData) {
		if (!clientList.containsKey(url)) {
			log.debug("adding client " + url);
			try {
				TCPThread tcp = new TCPThread(url, (Socket) commData);
				clientList.put(url, tcp);

				if (useHeartbeat) {
					Heart heart = new Heart(url, this);
					heart.start();
					heartbeatList.put(url, heart);
				}

			} catch (Exception e) {
				Service.logException(e);
				log.error("could not connect to " + url);
			}
		}
	}

	@Override
	public void stopService() {
		// TODO Auto-generated method stub
		if (clientList != null) {
			for (int i = 0; i < clientList.size(); ++i) {
				TCPThread r = clientList.get(i);
				if (r != null) {
					r.interrupt();
				}
				r = null;
			}
		}
		clientList.clear();
		clientList = new HashMap<URI, TCPThread>();
		isRunning = false;
	}

	boolean heartbeatRunning = false;

	class Heart extends Thread {
		URI url;
		CommObjectStreamOverTCP comm;
		boolean isRunning = false;
		int heartbeatIntervalMilliSeconds = 1000;

		Heart(URI url, CommObjectStreamOverTCP comm) {
			this.url = url;
			this.comm = comm;
		}

		@Override
		public void run() {
			try {
				while (isRunning) {
					Thread.sleep(heartbeatIntervalMilliSeconds);
					Message msg = new Message();
					msg.method = "echoHeartbeat";
					Heartbeat heartbeat = new Heartbeat();
					heartbeat.sender = myService.getName();
					msg.data = new Object[] { new Heartbeat() };
					// comm.send(name, msg);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void startHeartbeat() {
		if (!heartbeatRunning) {

		}
	}

	@Override
	public void stopHeartbeat() {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<URI> getClients() {
		ArrayList<URI> ret = new ArrayList<URI>();
		for (URI key : clientList.keySet()) {
			ret.add(key);
		}

		return ret;
	}

}
