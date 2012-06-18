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
 * 
 * */

package org.myrobotlab.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.Runtime;

public class CommObjectStreamOverTCP extends Communicator implements Serializable {

	public final static Logger log = Logger.getLogger(CommObjectStreamOverTCP.class.getCanonicalName());
	private static final long serialVersionUID = 1L;
	boolean isRunning = false;
	static public transient HashMap<URL, TCPThread> clientList = new HashMap<URL, TCPThread>();
	Service myService = null;

	public class TCPThread extends Thread {

		URL url;
		transient Socket socket = null;

		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		
		public TCPThread(URL url, Socket socket) throws UnknownHostException, IOException {
				super ("tcp " + url);
				this.url = url;
				if (socket == null)
				{
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
						//clientList.remove(url);
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
							// FIXME - the only reason this is pulled off the comm line here
							// is initial registerServices do not usually come with a "name"
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

		public void send(URL url2, Message msg) {
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
	public void send(final URL url, final Message msg) {

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

	public synchronized void addClient(URL url, Object commData) {
		if (!clientList.containsKey(url)) {
			log.debug("adding client " + url);
			try {
				TCPThread tcp = new TCPThread(url, (Socket)commData);
				clientList.put(url, tcp);
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
		clientList = new HashMap<URL, TCPThread>();
		isRunning = false;
	}

}
