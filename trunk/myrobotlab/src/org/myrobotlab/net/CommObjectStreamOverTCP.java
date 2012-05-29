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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.interfaces.Communicator;

public class CommObjectStreamOverTCP extends Communicator implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * A Service needs a CommunicatorManager to communicate to another service.
	 * The manager can handle local (intra-process) communication - by using a
	 * Service reference. Remote (inter-process) communication requires a
	 * Communicator. There is a global (static) map of
	 * service names to endpoints - this is maintained here.
	 * 
	 * A thread is create for each new connection - its basically the "ear" Here
	 * is what basically happens 1. a request comes in for a foreign ip/port -
	 * msg.hostname:msg.servicePort != :0 2. the global clientList is checked -
	 * if found - the endpoint is used 3. if not found - a new connection &
	 * thread is created 4. messages coming from the remote endpoint are
	 * extracted with the serializer and put into the inbox for processing by
	 * the Service 5. outbound messages are sent directly from the Service
	 * (myService) to the endpoint
	 */

	public final static Logger LOG = Logger.getLogger(CommObjectStreamOverTCP.class.getCanonicalName());

	boolean isRunning = false;

	public static HashMap<URL, Remote> clientList = new HashMap<URL, Remote>();

	Service myService = null;

	InetSocketAddress remoteAddr;

	boolean isListening = false;
	
	public class Remote {
		public transient TCPThread tcp = null;
	}

	public class TCPThread extends Thread { // implements Runnable? {

		transient Socket socket = null;

		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;

		public TCPThread() {
			super(myService.getName() + "_TCPThread");
		}

		public TCPThread(Socket socket) {
			// socket comes in already connected
			this();
			try {
				this.socket = socket;
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new ObjectInputStream(socket.getInputStream());
				this.start(); // starting listener
			} catch (IOException e) {
				Service.logException(e);
				LOG.error("could not create streams from socket");
			}

		}
		
		synchronized public void send(final URL url, final Message msg) throws IOException {
			// FIXME - implement
		}
		
		// run / listen for more msgs
		@Override
		public void run() {

			IPAndPort ip = null;

			try {
				isRunning = true; // this is GLOBAL !

				if (socket != null) {
					ip = new IPAndPort(socket.getInetAddress().toString(),
							socket.getPort());
				}
				while (socket != null && isRunning) {

					Message msg = null;

					try {
						Object o = ois.readObject();
						msg = (Message) o;
					} catch (Exception e) {
						msg = null;
						Service.logException(e);
					}
					if (msg == null) {
						// TODO
						LOG.error(myService.getName()
								+ " null message - will continue to listen");
						LOG.error("disconnecting " + socket.getInetAddress()
								+ ":" + socket.getPort());
						socket.close();
						socket = null; // stream corrupted exception does not
										// recover TODO - remove from client
										// list - rem
					} else {
						if (msg.method.equals("registerServices")) {
							
							myService.registerServices(remoteAddr.getAddress().toString(), remoteAddr.getPort(), msg);
							continue;
						}
						myService.getInbox().add(msg);
					}
				}

				// closing connections TODO - why wouldn't you close the others?
				ois.close();
				oos.close();

			} catch (IOException e) {
				LOG.error("TCPThread threw");
				isRunning = false;
				socket = null;
				Service.logException(e);
			}

			// connection has been broken
			// myService.connectionBroken(remoteIP, remotePort);
			// myService.send("", "connectionBroken", remoteIP, remotePort);

			myService.invoke("connectionBroken", ip);
		}

		public Socket getSocket() {
			return socket;
		}

	} // TCP Thread

	public CommObjectStreamOverTCP(Service service) {
		this.myService = service;
	}

	// send udp or tcp or based on type
	@Override
	public void send(final URL url, final Message msg) {

		Remote phone = null;

		try {

			if (clientList.containsKey(url)) {
				phone = clientList.get(url);
			} else {
				phone = new Remote();
				phone.tcp = new TCPThread();
				clientList.put(url, phone);
			}

			if (phone.tcp == null) {
				phone.tcp = new TCPThread();
			}

			// TODO - implement different modes of communication based on config or config + datatype
			phone.tcp.send(url, msg);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Service.logException(e);
		}

	}


	// TODO - generate addClient event - so gui tabs can be refreshed or other
	// listeners notified

	public void addClient(Socket socket) {
		LOG.info("adding tcp client ");

		InetSocketAddress remoteAddr = (InetSocketAddress) socket
				.getRemoteSocketAddress();
		URL url;
		try {
			url = new URL("http://" + remoteAddr.getAddress().getHostAddress() + ":"
					+ remoteAddr.getPort());
		} catch (MalformedURLException e) {
			Service.stackToString(e);
			return;
		}
		Remote phone = new Remote();
		phone.tcp = new TCPThread(socket);
		clientList.put(url, phone);
	}

	
	// TODO refactor - use URL instead or address and port - Is the DatagramSocket used?
	public void addClient(Socket s, InetAddress address, int port) {
		

		//String key = address.getHostAddress() + ":" + port;
		try {
			URL url;
			url = new URL("http://" + address.getHostAddress() + ":" + port);
			Remote phone = new Remote();
			//phone.tcp = new TCPThread(s, address, port);
			phone.tcp = new TCPThread(s);//
			// phone.udp.start(); REMOTE ADAPTER ONLY ADDS THESE - if we already
			// have a UDP listener we dont want another
			if (!clientList.containsKey(url))
			{	LOG.debug("adding client " + url);
				clientList.put(url, phone);
			}

		} catch (MalformedURLException e) {
			LOG.error(Service.stackToString(e));
			return;
		}

	}

	@Override
	public void stopService() {
		// TODO Auto-generated method stub
		if (clientList != null) {
			for (int i = 0; i < clientList.size(); ++i) {
				Remote r = clientList.get(i);
				if (r != null) {
					r.tcp.interrupt();
				}
				r = null;
			}
		}
		clientList.clear();
		clientList = new HashMap<URL, Remote>();
		isRunning = false;
	}

	@Override
	public void disconnectAll() {
		Iterator<URL> sgi = clientList.keySet().iterator();
		while (sgi.hasNext()) {
			URL accessURL = sgi.next();
			Remote r = clientList.get(accessURL);
			try {
				if (r.tcp.socket != null)
					r.tcp.socket.close();
			} catch (IOException e) {
				Service.logException(e);
			}
		}

	}

	// FIXME - deprecate
	@Override
	public void setIsUDPListening(boolean set) {
		isListening = set;
	}

	@Override
	public void addClient(DatagramSocket s, InetAddress address, int port) {
		// TODO Auto-generated method stub
		
	}
	

}
