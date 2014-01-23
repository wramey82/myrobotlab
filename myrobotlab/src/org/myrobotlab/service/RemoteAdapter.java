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
import java.util.HashMap;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.CommData;
import org.myrobotlab.net.CommObjectStreamOverTCP;
import org.myrobotlab.net.TCPThread2;
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
 */

public class RemoteAdapter extends Service implements Communicator {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(RemoteAdapter.class);

	// types of listening threads - multiple could be managed
	// when correct interfaces and base classes are done
	transient TCPListener tcpListener = null;
	transient UDPListener udpListener = null;

	private Integer udpPort = 6767;
	private Integer tcpPort = 6767;
	
	// FIXME static map of clients is within this object - 
	// simplify - remove all data maps and put in this service 
	//CommObjectStreamOverTCP tcp;

	transient HashMap<URI, TCPThread2> clientList = new HashMap<URI, TCPThread2>();
	
	public RemoteAdapter(String n) {
		super(n);
		
		//tcp = new CommObjectStreamOverTCP(this);
	}

	@Override
	public boolean isReady() {
		if (tcpListener.serverSocket != null) {
			return tcpListener.serverSocket.isBound();
		}
		return false;
	}

	class TCPListener extends Thread {
		RemoteAdapter myService = null;
		transient ServerSocket serverSocket = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		int listeningPort;

		public TCPListener(int listeningPort, RemoteAdapter s) {
			super(String.format("%s.tcp.%d", s.getName(), listeningPort));
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
					// FIXME - on contact register the "environment" regardless if a service registers !!!
					Socket clientSocket = serverSocket.accept(); // FIXME ENCODER SHOULD BE DOING THIS
					String clientKey = String.format("tcp://%s:%d",clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
					//String newHostEntryKey = String.format("mrl://%s/%s", myService.getName(), clientKey);
					//info(String.format("connection from %s", newHostEntryKey));
					URI uri = new URI(clientKey);
					
					clientList.put(uri, new TCPThread2(myService, uri, clientSocket));
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
			super(String.format("%s.usp.%d", s.getName(), listeningPort));
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

				log.info(String.format("%s listening on udp %s:%d", getName(), socket.getLocalAddress(), socket.getLocalPort()));

				Communicator comm = (Communicator) cm.getComm(new URI(String.format("mrl:%s/udp://%s:%d", getName(), socket.getLocalAddress(), socket.getLocalPort())));

				byte[] b = new byte[65535]; // max udp size
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

					} catch (Exception e) {
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

	// FIXME - remove or change to be more general
	public HashMap<URI, CommData> getClients() {
		// ArrayList<U>
		return null;
	}

	@Override
	public void startService() {
		super.startService();
		startListening();
	}

	public void startListening() {
		startListening(udpPort, tcpPort);
	}

	public void startListening(int udpPort, int tcpPort) {
		startUDP(udpPort);
		startTCP(tcpPort);
	}

	public void startUDP(int port) {
		stopUDP();
		udpPort = port;
		udpListener = new UDPListener(udpPort, this);
		udpListener.start();

	}

	public void startTCP(int port) {
		stopTCP();
		tcpPort = port;
		tcpListener = new TCPListener(tcpPort, this);
		tcpListener.start();

	}

	public void stopUDP() {
		if (udpListener != null) {
			udpListener.interrupt();
			udpListener.shutdown();
			udpListener = null;
		}
	}

	public void stopTCP() {
		if (tcpListener != null) {
			tcpListener.interrupt();
			tcpListener.shutdown();
			tcpListener = null;
		}
	}

	public void stopListening() {
		stopUDP();
		stopTCP();
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
	
	

	//public transient HashMap<URI, TCPThread> clientList = new HashMap<URI, TCPThread>();

	
	// FIXME - make URI keyed map - check for map - if not there
	// make new TCP/UDPThread
	// the datastore - is a uri client --to--> data store which is used to connect & communicate
	// to endpoints
	// endpoint is expected to be of the following format
	// mrl://(name)/tcp://host:port/
	@Override
	public void sendRemote(URI uri, Message msg) {
		log.info(String.format("host %s", uri.getHost()));
		log.info(String.format("pathInfo %s", uri.getPath()));
		log.info(String.format("getRawPath %s", uri.getRawPath()));
		log.info(String.format("getQuery %s", uri.getQuery()));
		log.info(String.format("sendRemote %s %s.%s", uri, msg.name, msg.method));
		TCPThread2 t = null;
		if (clientList.containsKey(uri))
		{
			t = clientList.get(uri);
		} else {
			try {
				t = new TCPThread2(this, uri, null);
				//clientList.put(new URI(String.format("mrl://%s/%s", getName(), uri.toString())), t);
				clientList.put(uri, t);
			} catch(Exception e){
				Logging.logException(e);
			}
		}
		
		if (t == null)
		{
			log.info("here");
		}
		
		t.send(msg);
	}

	// FIXME - remote
	@Override
	public void addClient(URI uri, Object commData) {
		// TODO Auto-generated method stub
		log.info("add client");
	}
	
	public Integer getUdpPort() {
		return udpPort;
	}

	public void setUDPPort(Integer udpPort) {
		this.udpPort = udpPort;
	}

	public Integer getTcpPort() {
		return tcpPort;
	}

	public void setTCPPort(Integer tcpPort) {
		this.tcpPort = tcpPort;
	}


	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.ERROR);

		try {

			int i = 2;
			Runtime.main(new String[] { "-runtimeName", String.format("r%d", i) });
			RemoteAdapter remote = (RemoteAdapter) Runtime.createAndStart(String.format("remote%d", i), "RemoteAdapter");
			Runtime.createAndStart(String.format("clock%d", i), "Clock");
			Runtime.createAndStart(String.format("gui%d", i), "GUIService");

			// FIXME - sholdn't this be sendRemote ??? or at least
			// in an interface
			// remote.sendRemote(uri, msg);
			// xmpp1.sendMessage("xmpp 2", "robot02 02");
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
