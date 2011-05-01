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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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

public class CommAsciiOverTCP extends Communicator {

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

	public final static Logger LOG = Logger.getLogger(CommAsciiOverTCP.class
			.getCanonicalName());

	boolean isRunning = false;

	public static HashMap<String, CommunicatorTCPRequestThread> clientList = new HashMap<String, CommunicatorTCPRequestThread>();

	ConfigurationManager cfg = null;
	Service myService = null;
	static String stopString = null;
	static int counter;

	InetSocketAddress remoteAddr;

	// A READER THREAD - sits and waits for data - generates messages - needs to
	// use a serializer
	public class CommunicatorTCPRequestThread extends Thread { // implements
																// Runnable? {

		// network
		Socket socket = null;

		// ascii serialization
		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;
		OutputStreamWriter osw = null;
		InputStreamReader isr = null;

		// binary serialization
		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;

		String remoteIP;
		int remotePort;

		public CommunicatorTCPRequestThread(Socket socket) throws IOException {
			super(myService.name + "_CommunicatorTCPRequestThread_"
					+ socket.getRemoteSocketAddress());
			this.socket = socket;
			remoteAddr = (InetSocketAddress) socket.getRemoteSocketAddress();
			remoteIP = remoteAddr.getAddress().getHostAddress();
			remotePort = remoteAddr.getPort();

			stopString = "</XML>";

			++counter;

			bis = new BufferedInputStream(socket.getInputStream());
			isr = new InputStreamReader(bis, "US-ASCII");
			bos = new BufferedOutputStream(socket.getOutputStream());
			osw = new OutputStreamWriter(bos, "US-ASCII");

		}

		// TODO - re-evaluate - does it really need to be synchronized?
		synchronized public void send(StringBuffer sb) throws IOException {
			osw.write(sb.toString());
			osw.flush();
		}

		synchronized public void send(Message msg) throws IOException {
			try {
				oos.writeObject(msg);
				LOG.info("send " + msg.getParameterSignature());
				oos.flush();
				oos.reset();
			} catch (NotSerializableException e) {
				LOG.error("could not serialize [" + e.getMessage() + "]");
			}
		}

		// run / listen
		@Override
		public void run() {
			try {
				isRunning = true; // this is GLOBAL !

				char end = 0;
				end = stopString.charAt(stopString.length() - 1);

				while (socket != null && isRunning) {

					StringBuffer sb = new StringBuffer();
					int c;
					int cnt = 0;
					Message msg = null;

					// read message
					while ((c = isr.read()) != -1 && isRunning) {
						sb.append((char) c);
						if (sb.charAt(cnt) == end) {
							if (sb.length() > stopString.length()
									&& sb.substring(
											cnt - stopString.length() + 1)
											.compareTo(stopString) == 0) {
								break; // read a complete message
							}
						}
						++cnt;
					}

					if (msg == null) {
						// TODO
						LOG.error(myService.name
								+ " null message - will continue to listen");
						LOG.error("disconnecting " + remoteIP + ":"
								+ remotePort);
						socket.close();
						socket = null; // stream corrupted exception does not
										// recover
					} else {
						// This has actual socket data vs what is "put" in
						if (msg.method.compareTo("registerServices") == 0) {
							ServiceDirectoryUpdate sdu = (ServiceDirectoryUpdate) msg.data[0]; // TODO
																								// -
																								// not
																								// really
																								// good
							InetSocketAddress remoteAddr = (InetSocketAddress) socket
									.getRemoteSocketAddress();
							InetSocketAddress localAddr = (InetSocketAddress) socket
									.getLocalSocketAddress();

							sdu.hostname = localAddr.getAddress()
									.getHostAddress();
							sdu.servicePort = localAddr.getPort();
							sdu.remoteHostname = remoteIP;
							sdu.remoteServicePort = remotePort;

						}
						myService.getInbox().add(msg);
					}
				}

				// closing connections TODO - why wouldn't you close the others?
				isr.close();
				bis.close();

			} catch (IOException e) {
				LOG.error("CommunicatorTCPRequestThread threw");
				isRunning = false;
				socket = null;
				e.printStackTrace();
			}

			// connection has been broken
			// myService.connectionBroken(remoteIP, remotePort);
			// myService.send("", "connectionBroken", remoteIP, remotePort);

			myService.invoke("connectionBroken", new IPAndPort(remoteIP,
					remotePort));
		}

		public Socket getSocket() {
			return socket;
		}
	}

	public CommAsciiOverTCP(Service service) {
		this.myService = service;
		cfg = new ConfigurationManager(service.getHost());
	}

	@Override
	public void send(final Message msg) {
		Socket socket = null;
		CommunicatorTCPRequestThread phone = null;

		// TODO - determine the precedence

		String key;
		// if (msg.name.length() > 0)
		ServiceEntry se = null;
		try {
			// cfg.save("pre.remote.txt");
			se = cfg.getServiceEntry(msg.name);

		} catch (ConfigurationManager.CFGError e) {
			LOG.error("error could not find Service Entry in " + myService.name
					+ " for " + msg.name + "/" + msg.method);
		}
		if (se == null) {
			LOG.error("could not find " + msg.name + " service entry is null");
			return;
		}
		key = se.host + ":" + se.servicePort;
		LOG.info("looking for name [" + msg.name + "] @ [" + key
				+ "] in clientList");

		if (se.servicePort == 0 && !clientList.containsKey(key)) {
			LOG.error(clientList);
			LOG.error("could not find socket clientList [" + key
					+ "] and servicePort = 0 - disconnect?");
			LOG.error("removing notifyEntry from " + msg.sender + "/"
					+ msg.sendingMethod);

			NotifyEntry ne = new NotifyEntry();
			ne.name = msg.name;
			ne.outMethod_ = msg.sendingMethod;
			myService.removeNotify(ne);

			// removeNotifyRequest(msg); TODO depricate no need to send message
			// - same service - sending message gets sending to myself logic
			// what to do??

			return;
		}

		if (clientList.containsKey(key)) {
			phone = clientList.get(key);
			socket = phone.getSocket();
		}

		if (socket == null) {

			try {
				// new connection
				LOG.info(clientList);
				LOG.info("new socket " + msg.name + "@" + se.host + ":"
						+ se.servicePort + " for " + msg.name);
				try {
					socket = new Socket(se.host, se.servicePort);
				} catch (ConnectException e) {
					LOG
							.error("could not connect - removing notifier - TODO remote from client list");
					NotifyEntry ne = new NotifyEntry();
					ne.name = msg.name;
					ne.outMethod_ = msg.sendingMethod;
					myService.removeNotify(ne);
				}

				if (socket != null) {
					phone = new CommunicatorTCPRequestThread(socket);
					phone.start();
					LOG.info("adding " + key + " to clientList ");
					clientList.put(key, phone);
				} else {
					LOG.error("null socket");
				}

			} catch (UnknownHostException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		StringBuffer sb = null;

		if (socket != null) // sb != null && TODO - NEEDS TO BE TIGHTED UP !!
		{
			try {
				// send ascii
				LOG.info("sending " + socket.getLocalAddress() + ":"
						+ socket.getLocalPort() + "->"
						+ socket.getInetAddress() + ":" + socket.getPort()
						+ " data size [" + sb.length() + "]");
				phone.send(sb);
			} catch (IOException e) {
				clientList.remove(key);
				e.printStackTrace();
			}
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

	public void addClient(Socket socket) {
		// TODO - don't parse
		try {
			InetSocketAddress remoteAddr = (InetSocketAddress) socket
					.getRemoteSocketAddress();
			String key = remoteAddr.getAddress().getHostAddress() + ":"
					+ remoteAddr.getPort();
			CommunicatorTCPRequestThread phone;
			phone = new CommunicatorTCPRequestThread(socket);
			phone.start();
			clientList.put(key, phone);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stopService() {
		// TODO Auto-generated method stub
		if (clientList != null) {
			for (int i = 0; i < clientList.size(); ++i) {
				CommunicatorTCPRequestThread t = clientList.get(i);
				if (t != null) {
					t.interrupt();
				}
				t = null;
			}

		}
		clientList.clear();
		clientList = new HashMap<String, CommunicatorTCPRequestThread>();
		isRunning = false;
	}

	@Override
	public void disconnectAll() {
		Iterator<String> sgi = clientList.keySet().iterator();
		while (sgi.hasNext()) {
			String serviceName = sgi.next();
			CommunicatorTCPRequestThread sg = clientList.get(serviceName);
			try {
				if (sg.socket != null)
					sg.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void setIsUDPListening(boolean set) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addClient(DatagramSocket s, InetAddress address, int port) {
		// TODO Auto-generated method stub

	}

}
