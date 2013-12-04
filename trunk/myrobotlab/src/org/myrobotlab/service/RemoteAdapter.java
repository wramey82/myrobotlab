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

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.Communicator;
import org.slf4j.Logger;

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

	public RemoteAdapter(String n) {
		super(n);
	}

	@Override
	public boolean isReady() {
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
		int listeningPort;


		public TCPListener(int listeningPort, RemoteAdapter s) {
			super(String.format("%s.tcp.%d",s.getName(), listeningPort));
			this.listeningPort = listeningPort;
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

				serverSocket = new ServerSocket(listeningPort, 10);

				log.info(getName() + " TCPListener listening on " + serverSocket.getLocalSocketAddress());
				myService.info(String.format("listening on %s tcp", serverSocket.getLocalSocketAddress()));

				while (isRunning()) {
					Socket clientSocket = serverSocket.accept();
					URI uri = new URI(String.format("mrl:%stcp://%s:%d", getName(), clientSocket.getInetAddress().getHostAddress(),clientSocket.getPort()));
					//myService.info(String.format("new connection %s", url.toString()));
					Communicator comm = (Communicator) cm.getComm(uri);
					comm.addClient(uri, clientSocket);
					//broadcastState(); don't broadcast unless requested to
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
		int listeningPort;

		public UDPListener(int listeningPort, RemoteAdapter s) {
			super(String.format("%s.usp.%d",s.getName(), listeningPort));
			this.listeningPort = listeningPort;
			myService = s;
		}

		public void shutdown() {
			if ((socket != null) && (!socket.isClosed())) {
				socket.close();
			}
		}

		// FIXME FIXME FIXME - large amount of changes to tcp - application
		// logic which handles the "Messaging" should be common to both
		// tcp & udp
		public void run() {

			try {
				socket = new DatagramSocket(listeningPort);

				log.info(getName() + " UDPListener listening on " + socket.getLocalAddress() + ":" + socket.getLocalPort());

				Communicator comm = (Communicator) cm.getComm(new URI(String.format("mrl:%s/udp://%s:%d", getName(),socket.getLocalAddress(),socket.getLocalPort())));

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
		super.startService();
		startListening(6767);
	}
	
	public void startListening(int listeningPort)
	{
		stopListening();
		udpListener = new UDPListener(listeningPort, this);
		udpListener.start();
		tcpListener = new TCPListener(listeningPort, this);
		tcpListener.start();
		
	}
	
	public void stopListening()
	{
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

	}

	@Override
	public void stopService() {
		stopListening();
		super.stopService();
	}

	@Override
	public String getDescription() {
		return "allows remote communication between applets, or remote instances of myrobotlab";
	}


	static public ArrayList<InetAddress> getLocalAddresses() {
		ArrayList<InetAddress> ret = new ArrayList<InetAddress>();
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					ret.add(inetAddress);
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
		LoggingFactory.getInstance().setLevel(Level.ERROR);

		Runtime.main(args);
		/*
		 * Runtime.createAndStart("remote0", "RemoteAdapter");
		 * Runtime.createAndStart("log0", "Log");
		 * Runtime.createAndStart("python0", "Python");
		 */
		
		Runtime.createAndStart("remote", "RemoteAdapter");
		Runtime.createAndStart("rgui", "GUIService");
		//Runtime.createAndStart("controller", "Python");

	}
}
