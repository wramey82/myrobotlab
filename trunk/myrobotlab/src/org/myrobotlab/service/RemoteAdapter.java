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

package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.Communicator;

/***
 * 
 * @author GroG
 * 
 *         This is a service which allows foreign clients to connect. It
 *         maintains a list of currently connected clients. Most of the
 *         communication details are left up to a configurable Communicator and
 *         Serializer. At this point the RemoteAdapter has the ability to
 *         filter, block, or re-route any inbound messages.
 * 
 *         TODO - refactor the MRLClient comm devices back in TODO - optimize
 *         communication blocks - such that new objects don't need to be
 *         re-created on each message
 * 
 */

public class RemoteAdapter extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(RemoteAdapter.class.getCanonicalName());

	// types of listening threads - multiple could be managed
	// when correct interfaces and base classes are done
	transient TCPListener tcpListener = null;
	transient UDPListener udpListener = null;
	// transient UDPStringListener udpStringListener = null;

	// FIXME - all port & ip data needs to be only in the threads
	public int TCPPort = 6767;
	public int UDPPort = 6767;
	public int UDPStringPort = 6668;
	public String serverIP = "0.0.0.0";

	public RemoteAdapter(String n) {
		super(n, RemoteAdapter.class.getCanonicalName());
	}

	public RemoteAdapter(String n, String hostname) {
		super(n, RemoteAdapter.class.getCanonicalName(), hostname);
	}



	@Override
	public boolean isReady() {
		// TODO - selectively enable and/or check
		// TODO - check other threads
		if (tcpListener.serverSocket != null) {
			return tcpListener.serverSocket.isBound();
		}
		return false;
	}

	public int TCPMessages = 0;

	class TCPListener extends Thread {
		RemoteAdapter myService = null;
		transient ServerSocket serverSocket = null;
		ObjectOutputStream out;
		ObjectInputStream in;

		public TCPListener(String n, RemoteAdapter s) {
			super(n);
			myService = s;
		}

		public void shutdown() {
			if ((serverSocket != null) && (!serverSocket.isClosed())) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					logException(e);
				}
			}
			serverSocket = null;
		}

		public void run() {
			try {

				serverSocket = new ServerSocket(TCPPort, 10);

				log.info(getName() + " TCPListener listening on " + serverSocket.getLocalSocketAddress());

				while (isRunning()) {
					Socket clientSocket = serverSocket.accept();
					Communicator comm = (Communicator) cm.getComm();
					URI url = new URI("tcp://" + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
					log.info("new connection [" + url + "]");
					comm.addClient(url, clientSocket);
					broadcastState();
				}

				serverSocket.close();
			} catch (Exception e) {
				logException(e);
			}

		}
	}

	class UDPListener extends Thread {

		DatagramSocket socket = null;
		RemoteAdapter myService = null;

		public UDPListener(String n, RemoteAdapter s) {
			super(n);
			myService = s;
		}

		public void shutdown() {
			if ((socket != null) && (!socket.isClosed())) {
				socket.close();
			}
		}

		public void run() {

			try {
				socket = new DatagramSocket(UDPPort);

				log.info(getName() + " UDPListener listening on " + socket.getLocalAddress() + ":" + socket.getLocalPort());

				Communicator comm = (Communicator) cm.getComm();

				byte[] b = new byte[65535];
				ByteArrayInputStream b_in = new ByteArrayInputStream(b);
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				while (isRunning()) {
					socket.receive(dgram); // receives all datagrams
					// FIXME - do we need o re-create???
					ObjectInputStream o_in = new ObjectInputStream(b_in);
					try {
						Message msg = (Message) o_in.readObject();
						dgram.setLength(b.length); // must reset length field!
						b_in.reset();
						if ("registerServices".equals(msg.method)) {
							URI url = new URI("tcp://" + dgram.getAddress().getHostAddress() + ":" + dgram.getPort());
							comm.addClient(url, socket);
							invoke("registerServices", dgram.getAddress().getHostAddress(), dgram.getPort(), msg);
							// broadcastState();
							continue;
						}

						if (msg.getName().equals(getName())) {
							getInbox().add(msg);
						} else {
							getOutbox().add(msg);
						}

					} catch (ClassNotFoundException e) {
						logException(e);
						log.error("udp datagram dumping bad msg");
					}
					dgram.setLength(b.length); // must reset length field!
					b_in.reset(); // reset so next read is from start of byte[]
									// again
				} // while isRunning

			} catch (Exception e) {
				log.error("UDPListener could not listen");
				logException(e);
			}
		}
	}

	public ArrayList<URI> getClients() {
		// ArrayList<U>
		return null;
	}

	@Override
	public void startService() {
		// FIXME - block until isReady on the ServerSocket
		if (!isRunning()) {
			super.startService();
			/*
			 * FIXME No longer support auto-listening with TCP or UDPStrings
			 * this needs to refined such that you can explicitly set the
			 * listening type/protocol additionally there should be a explicit
			 * setting to do outbound communication or set outbound
			 * type/protocol based on a datatype mapping udpStringListener = new
			 * UDPStringListener(getName() + "_udpStringListener", this);
			 * udpStringListener.start();
			 */
			udpListener = new UDPListener(getName() + "_udpMsgListener", this);
			udpListener.start();
			tcpListener = new TCPListener(getName() + "_tcpMsgListener", this);
			tcpListener.start();
			// block until actually listening

		} else {
			log.warn("RemoteAdapter " + getName() + " is already started");
		}
	}

	@Override
	public void stopService() {

		super.stopService();

		if (tcpListener != null) {
			tcpListener.interrupt();
			tcpListener.shutdown();
			tcpListener = null;
		}
		if (udpListener != null) {
			udpListener.interrupt();
			udpListener.shutdown();
			udpListener = null;
		}

		if (thisThread != null) {
			thisThread.interrupt();
		}

		thisThread = null;

	}

	@Override
	public String getToolTip() {
		return "allows remote communication between applets, or remote instances of myrobotlab";
	}

	// FIXME - restart listening threads whenever set port is called -
	// otherwise its meaningless
	public void setUDPPort(int port) {
		UDPPort = port;
	}

	public void setTCPPort(int port) {
		TCPPort = port;
	}

	public void setUDPStringPort(int port) {
		UDPStringPort = port;
	}

	static public ArrayList<InetAddress> getLocalAddresses() {
		ArrayList<InetAddress> ret = new ArrayList<InetAddress>();
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					ret.add(inetAddress);
					/*
					 * if (!inetAddress.isLoopbackAddress() &&
					 * !inetAddress.isLinkLocalAddress()) {
					 * System.out.println(inetAddress.getHostAddress()); }
					 */
				}
			}
			;
		} catch (Exception e) {
			logException(e);
		}
		return ret;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Runtime.main(args);
		/*
		 * Runtime.createAndStart("remote0", "RemoteAdapter");
		 * Runtime.createAndStart("log0", "Log");
		 * Runtime.createAndStart("python0", "Python");
		 */
		
		Runtime.createAndStart("remote", "RemoteAdapter");
		Runtime.createAndStart("rgui", "GUIService");
		Runtime.createAndStart("controller", "Python");

	}
}
