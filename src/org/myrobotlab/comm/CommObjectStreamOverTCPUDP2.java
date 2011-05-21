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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceDirectoryUpdate;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.interfaces.Communicator;

public class CommObjectStreamOverTCPUDP2 extends Communicator implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * A Service needs a CommunicatorManager to communicate to another service.
	 * The manager can handle local (intra-process) communication - by using a
	 * Service reference. Remote (inter-process) communication requires a
	 * Communicator Interface In this implementation CommunicatorTCP sets up a
	 * duplex channel with XML over tcpip - there is a global (static) map of
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

	public final static Logger LOG = Logger
			.getLogger(CommObjectStreamOverTCPUDP2.class.getCanonicalName());

	boolean isRunning = false;

	public static HashMap<URL, Remote> clientList = new HashMap<URL, Remote>();

	ConfigurationManager cfg = null;
	Service myService = null;

	InetSocketAddress remoteAddr;

	static boolean isUDPListening = false;

	public class Remote {
		public transient TCPThread tcp = null;
		public transient UDPThread udp = null;
	}

	public class UDPThread extends Thread { // implements Runnable? {
		DatagramSocket socket = null;
		InetAddress address = null;
		int port = -1;

		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;

		byte[] b = new byte[65535];
		ByteArrayInputStream b_in = new ByteArrayInputStream(b);
		DatagramPacket dgram = new DatagramPacket(b, b.length);

		public UDPThread() {

		}

		public UDPThread(DatagramSocket s, InetAddress address, int port) {
			this.socket = s;
			this.address = address;
			this.port = port;
		}

		synchronized public void send(final URL url, final Message msg) throws IOException {

			String host = url.getHost();
			int port = url.getPort();
			
			LOG.info("sending udp msg to " + host + ":" +port + "/" + msg.name);

			if (socket == null) {
				socket = new DatagramSocket();
				if (!isUDPListening) {
					this.start();
				}
			}

			if (address == null) {
				address = InetAddress.getByName(host);
			}

			try {

				ByteArrayOutputStream b_out = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(b_out);

				oos.writeObject(msg);
				oos.flush();
				byte[] b = b_out.toByteArray();

				LOG.info("send " + msg.getParameterSignature());

				if (b.length > 65535) {
					LOG.error("udp datagram can not exceed 65535 msg size is "
							+ b.length + " !");
				}

				DatagramPacket packet = new DatagramPacket(b, b.length,
						address, port);
				socket.send(packet);

				oos.reset();

			} catch (NotSerializableException e) {
				LOG.error("could not serialize [" + e.getMessage() + "]");
			}
		}

		// "client" run / listen on a socket
		@Override
		public void run() {
			try {
				isRunning = true; // this is GLOBAL !

				while (socket != null && isRunning) {

					Message msg = null;

					try {
						socket.receive(dgram); // blocks
						// read message
						// if (ois == null)
						{
							ois = new ObjectInputStream(b_in);
						}

						Object o = ois.readObject();

						dgram.setLength(b.length); // must reset length field!
						b_in.reset(); // reset so next read is from start of
										// byte[] again
						msg = (Message) o;

						// PUTTING IN ACTUAL IP DATA (OF THE CONNECTION) TO THE
						// SDU Begin ---
						if (msg.method.compareTo("registerServices") == 0) // TODO
																			// RemoteAdapter
																			// needs
																			// to
																			// handle
																			// this
																			// for
																			// UDP
						{

							ServiceDirectoryUpdate sdu = (ServiceDirectoryUpdate) msg.data[0]; // TODO
																								// -
																								// not
																								// really
																								// good
							InetSocketAddress localAddr = (InetSocketAddress) socket
									.getLocalSocketAddress();

							sdu.hostname = localAddr.getAddress()
									.getHostAddress();
							sdu.servicePort = localAddr.getPort();
							sdu.remoteHostname = dgram.getAddress().toString();
							sdu.remoteServicePort = dgram.getPort();

							if (sdu.remoteHostname.startsWith("/")) {
								sdu.remoteHostname = sdu.remoteHostname
										.substring(1);
							}

							LOG.error(msg.name + " SDU " + sdu.remoteHostname
									+ ":" + sdu.remoteServicePort);
						}
						// PUTTING IN ACTUAL IP DATA (OF THE CONNECTION) TO THE
						// SDU End ---

					} catch (StreamCorruptedException e) {
						msg = null;
						e.printStackTrace();
					} catch (ClassCastException e) {
						msg = null;
						e.printStackTrace();
					} catch (IOException e) {
						msg = null;
						e.printStackTrace();
					}

					if (msg == null) {
						LOG
								.error(myService.name
										+ " UDP Datagram corrupt from "
										+ socket.getInetAddress() + ":"
										+ socket.getPort()
										+ " - dumping null message ");
					} else {
						myService.getInbox().add(msg);
					}
				}

				// closing connections TODO - why wouldn't you close the others?
				ois.close();
				oos.close();

			} catch (IOException e) {
				LOG.error("UDPThread threw");
				isRunning = false;
				socket = null;
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}// run

		public DatagramSocket getSocket() {
			return socket;
		}

	} // UDPThread

	// A READER THREAD - sits and waits for data - generates messages - needs to
	// use a serializer
	public class TCPThread extends Thread { // implements Runnable? {

		Socket socket = null;

		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;

		public TCPThread() {
			super(myService.name + "_TCPThread");
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
				e.printStackTrace();
				LOG.error("could not create streams from socket");
			}

		}

		synchronized public void send(Message msg) throws IOException {

			LOG.info("sending tcp msg to " + msg.name);
			if (socket == null || socket.isClosed()) {
				if (!connect(msg)) {
					String key = getKey(msg);
					LOG.error("could not connect to " + msg.name
							+ " removing key " + key);
					clientList.remove(key);
				}
			}

			try {
				oos.writeObject(msg);
				LOG.info("send " + msg.getParameterSignature());
				oos.flush();
				oos.reset();

			} catch (NotSerializableException e) {
				LOG.error("could not serialize [" + e.getMessage() + "]");
			}
		}

		public boolean connect(Message msg) {
			try {

				ServiceEntry se = null;
				se = cfg.getServiceEntry(msg.name);

				LOG.info("connecting to " + se.host + ":" + se.servicePort);

				/*
				 * InetAddress addr = InetAddress.getByName(host); SocketAddress
				 * sockaddr = new InetSocketAddress(addr, port);
				 * 
				 * Socket sock = new Socket();
				 * 
				 * int timeoutMs = 2000; // 2 seconds sock.connect(sockaddr,
				 * timeoutMs);
				 */

				socket = new Socket(se.host, se.servicePort);
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new ObjectInputStream(socket.getInputStream());
				this.start(); // start listening
				return true;

			} catch (Exception e) {
				e.printStackTrace();
			}

			return false;
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
						e.printStackTrace();
					}
					if (msg == null) {
						// TODO
						LOG.error(myService.name
								+ " null message - will continue to listen");
						LOG.error("disconnecting " + socket.getInetAddress()
								+ ":" + socket.getPort());
						socket.close();
						socket = null; // stream corrupted exception does not
										// recover TODO - remove from client
										// list - rem
					} else {
						// putting actual socket data into the SDU - restricts
						// on spoofing
						if (msg.method.compareTo("registerServices") == 0) {

							ServiceDirectoryUpdate sdu = (ServiceDirectoryUpdate) msg.data[0]; // TODO
																								// -
																								// not
																								// really
																								// good
							InetSocketAddress localAddr = (InetSocketAddress) socket
									.getLocalSocketAddress();

							sdu.hostname = localAddr.getAddress()
									.getHostAddress();
							sdu.servicePort = localAddr.getPort();
							sdu.remoteHostname = socket.getInetAddress()
									.toString();

							if (sdu.remoteHostname.startsWith("/")) { // removing
																		// "host"
																		// part
								sdu.remoteHostname = sdu.remoteHostname
										.substring(1);
							}

							sdu.remoteServicePort = socket.getPort();

							LOG.error(myService.name + " SDU "
									+ sdu.remoteHostname + ":"
									+ sdu.remoteServicePort);
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
				e.printStackTrace();
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

	public CommObjectStreamOverTCPUDP2(Service service) {
		this.myService = service;
		cfg = new ConfigurationManager(service.getHost());
	}

	public String getKey(Message msg) {
		StringBuffer key = new StringBuffer();

		ServiceEntry se = null;

		se = cfg.getServiceEntry(msg.name);

		if (se == null) {
			LOG.error("could not find " + msg.name + " service entry is null");
			return null;
		}

		key.append(se.host);
		key.append(":");
		key.append(se.servicePort);

		return key.toString();

	}

	// send udp or tcp or based on type
	@Override
	public void send(final URL url, final Message msg) {

		Remote phone = null;

		//String key;

		// Look up Service Entery info begin ----------
		try {

			//key = getKey(msg);

			if (clientList.containsKey(url)) {
				phone = clientList.get(url);
			} else {
				phone = new Remote();
				phone.tcp = new TCPThread();
				phone.udp = new UDPThread();
				clientList.put(url, phone);
			}

			if (phone.udp == null) {
				phone.udp = new UDPThread();
			}

			// if (msg.data.length > 0 &&
			// msg.data.getClass().getCanonicalName().compareTo("org.myrobotlab.image.SerializableImage")
			// == 0)
			// {
			phone.udp.send(url, msg);
			// } else {
			// phone.tcp.send(msg); // TODO - store se.host in CFG as a
			// InetAddress
			// }

		} catch (ConfigurationManager.CFGError e) {
			LOG.error("error could not find Service Entry in " + myService.name
					+ " for " + msg.name + "/" + msg.method);
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void removeNotifyRequest(Message msg) {
		NotifyEntry ne = new NotifyEntry();
		ne.name = msg.name;
		ne.outMethod_ = msg.sendingMethod;
		Message m = new Message();
		m.sender = myService.name; // TODO NEVER RE-USE A MESSAGE FOOL !
		m.sendingMethod = "send";
		m.method = "removeNotify";
		m.setData(ne);
		myService.out(m);
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

	public void addClient(DatagramSocket s, InetAddress address, int port) {
		LOG.info("adding udp client ");

		//String key = address.getHostAddress() + ":" + port;
		URL url;
		try {
			url = new URL("http://" + address.getHostAddress() + ":" + port);
		} catch (MalformedURLException e) {
			Service.stackToString(e);
			return;
		}
		Remote phone = new Remote();
		phone.udp = new UDPThread(s, address, port);
		// phone.udp.start(); REMOTE ADAPTER ONLY ADDS THESE - if we already
		// have a UDP listener we dont want another
		clientList.put(url, phone);

	}

	@Override
	public void stopService() {
		// TODO Auto-generated method stub
		if (clientList != null) {
			for (int i = 0; i < clientList.size(); ++i) {
				Remote r = clientList.get(i);
				if (r != null) {
					r.tcp.interrupt();
					r.udp.interrupt();
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
				e.printStackTrace();
			}
		}

	}

	@Override
	public void setIsUDPListening(boolean set) {
		isUDPListening = set;
	}

}
