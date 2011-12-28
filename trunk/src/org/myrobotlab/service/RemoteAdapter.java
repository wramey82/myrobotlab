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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceDirectoryUpdate;
import org.myrobotlab.service.interfaces.Communicator;

/***
 * 
 * @author grog
 * 
 *         This is a service which allows foreign clients to connect. It
 *         maintains a list of currently connected clients. Most of the
 *         communication details are left up to a configurable Communicator and
 *         Serializer. At this point the RemoteAdapter has the ability to
 *         filter, block, or re-route any inbound messages.
 * 
 */

public class RemoteAdapter extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(RemoteAdapter.class.getCanonicalName());

	// types of listening threads - multiple could be managed
	// when correct interfaces and base classes are done
	transient TCPListener tcpListener = null;
	transient UDPListener udpListener = null;
	transient UDPStringListener udpStringListener = null;

	InetAddress serverAddress = null;
	
	// FIXME - all port & ip data needs to be only in the threads
	public int servicePort = 6767;
	public String serverIP = "0.0.0.0";
	
	public RemoteAdapter(String n) {
		super(n, RemoteAdapter.class.getCanonicalName());
	}

	
	public RemoteAdapter(String n, String hostname) {
		super(n, TestCatcher.class.getCanonicalName(), hostname);
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	@Override
	public boolean isReady() {
		// TODO - selectively enable and/or check
		// TODO - check other threads
		if (tcpListener.serverSocket != null)
		{
			return tcpListener.serverSocket.isBound();
		}
		return false;
	}

	class TCPListener extends Thread {
		RemoteAdapter myService = null;
		transient ServerSocket serverSocket = null; 

		public TCPListener (String n, RemoteAdapter s)
		{
			super(n);
			myService = s;
		}

		public void shutdown()
		{
			if ((serverSocket != null) && (!serverSocket.isClosed()))
			{
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
				
		public void run() {
			if (servicePort > 0) {
				try {

					serverSocket = new ServerSocket(servicePort,0, serverAddress);
					
					LOG.info(name + " TCPListener listening on "
							+ serverSocket.getLocalSocketAddress());

					while (isRunning()) {
						Socket clientSocket = serverSocket.accept();
						Communicator comm = (Communicator) cm.getComm();
						LOG.info("new connection ["
								+ clientSocket.getRemoteSocketAddress() + "]");
						comm.addClient(clientSocket);
						// starting new thread to read/listen on inbox
						// TODO - threadpool IOCompletionPorts
						// CommunicatorTCP nt = new CommunicatorTCP(this); //
						// TODO - finish this cause it probably won't work
						// nt.start();

						// you can't add to the client list until AFTER you
						// receive your first message - preferably a SDU
						/*
						 * IF YOU MANAGE WITH THE KEY OF REMOTE ADDRESS YOU CAN
						 * !
						 */
						/*
					*/

					}

					serverSocket.close();
				} catch (IOException e) {
					LOG.error("Could not listen on requested ["
							+ servicePort + "]");
					servicePort = 0;
				}
			} else {
				LOG.error("servicePort is <= 0 - terminating");
			}

		}
	}
	
	
	
	class UDPStringListener extends Thread
	{
		DatagramSocket socket = null;
		String dst = "chess";
		String fn = "parseOSC";

		RemoteAdapter myService = null;
		public UDPStringListener (String n, RemoteAdapter s)
		{
			super(n);
			myService = s;
		}
		
		public void shutdown()
		{
			if ((socket != null) && (!socket.isClosed()))
			{
				socket.close();
			}
		}
		public void run() {


			try {
				socket = new DatagramSocket(6668);
				
				LOG.info(name + " UDPStringListener listening on "
						+ socket.getLocalAddress() + ":" + socket.getLocalPort());
				
				byte[] b = new byte[65535];
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				while (isRunning()) {

					socket.receive(dgram); // blocks
					//String data = new String(b);
					String data = new String(dgram.getData(), 0, dgram.getLength());
					dgram.setLength(b.length); // must reset length field!

					LOG.debug("udp data [" + data + "]");
					send(dst, fn, data);
					// create a string message and send it
				}

			} catch (SocketException e) {
				e.printStackTrace();
				LOG.error("could not listen");
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/*
			 * while (isRunning) { socket.receive(p); }
			 */
		}
	}

	class UDPListener extends Thread { // TODO refactor and derive

		DatagramSocket socket = null;
		RemoteAdapter myService = null;
		public UDPListener (String n, RemoteAdapter s)
		{
			super(n);
			myService = s;
		}

		public void shutdown()
		{
			if ((socket != null) && (!socket.isClosed()))
			{
				socket.close();
			}
		}
		
		public void run() {

			try {
				socket = new DatagramSocket(servicePort);

				LOG.info(name + " UDPListener listening on "
						+ socket.getLocalAddress() + ":" + socket.getLocalPort());

				Communicator comm = (Communicator) cm.getComm();

				byte[] b = new byte[65535];
				ByteArrayInputStream b_in = new ByteArrayInputStream(b);
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				comm.setIsUDPListening(true);

				while (isRunning()) {
					socket.receive(dgram); // blocks
					ObjectInputStream o_in = new ObjectInputStream(b_in);
					try {
						Message msg = (Message) o_in.readObject();
						dgram.setLength(b.length); // must reset length field!
						b_in.reset(); // reset so next read is from start of byte[] again

						if (msg == null) {
							LOG.error("UDP null message");
						} else {

							if (msg.method.compareTo("registerServices") == 0) 
							{
								comm.addClient(socket, dgram.getAddress(), dgram.getPort());
								//registerServices(dgram.getAddress().getHostAddress(), dgram.getPort(), msg);
								invoke("registerServices", dgram.getAddress().getHostAddress(), dgram.getPort(), msg);
								// FIXME - no retObject on invoke - how to generate the event ???
								// getting clients address and port
								//getOutbox().add(msg);
								//msg.name=myService.name;
								//getInbox().add(msg);
								
								continue;
							}

							
							if (msg.name.equals(name))
							{
								getInbox().add(msg);
							} else {
								getOutbox().add(msg);
							}
						}

					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						LOG.error("udp datagram dumping bad msg");
					}
					dgram.setLength(b.length); // must reset length field!
					b_in.reset(); // reset so next read is from start of byte[]
									// again
				}

			} catch (SocketException e) {
				e.printStackTrace();
				LOG.error("could not listen");
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/*
			 * while (isRunning) { socket.receive(p); }
			 */
		}
	}
		
	@Override
	public void startService() {
		// TODO - block until isReady on the ServerSocket
		if (!isRunning())
		{
			super.startService();
			tcpListener = new TCPListener(name + "_tcpMsgListener", this);
			tcpListener.start();
			udpListener = new UDPListener(name + "_udpMsgListener", this);
			udpListener.start();
			udpStringListener = new UDPStringListener(name + "_udpStringListener", this);
			udpStringListener.start();
		} else {
			LOG.warn("RemoteAdapter " + name + " is already started");
		}
	}

	@Override
	public void stopService() {
		
		super.stopService();
		
		if (tcpListener != null)
		{
			tcpListener.interrupt();
			tcpListener.shutdown();
			tcpListener = null;
		}
		if (udpListener != null)
		{
			udpListener.interrupt();
			udpListener.shutdown();
			udpListener = null;
		}
		if (udpStringListener != null)
		{
			udpStringListener.interrupt();
			udpStringListener.shutdown();
			udpStringListener = null;			
		}

		if (thisThread != null) {
			thisThread.interrupt();
		}
				
		thisThread = null;

	}
/*
	// TODO - should Service even have these ??? - should be an Interface !!
	@Override
	public synchronized void registerServices(ServiceDirectoryUpdate sdu) {
		LOG.error("ra registerServices here");	
		
		for (int i = 0; i < sdu.serviceEntryList_.size(); ++i) {
			hostcfg.setServiceEntry(sdu.serviceEntryList_.get(i));
		}

		sendServiceDirectoryUpdate(sdu.remoteHostname, sdu.remoteServicePort,
				sdu.hostname, sdu.servicePort);
			
	}
*/
	// TODO - should Service even have these ??? - should be an Interface !!
	// @Override
	public void sendServiceDirectoryUpdate(String remoteHost, int remotePort,
			String localHost, int localPort) {
		LOG.info(name + " sendServiceDirectoryUpdate from " + localHost + ":"
				+ localPort + " --> " + remoteHost + ":" + remotePort);
		ServiceDirectoryUpdate sdu = new ServiceDirectoryUpdate();

		/*
		StringBuffer sb = new StringBuffer();
		sb.append("http://");
		sb.append(dgram.getAddress().toString());
		sb.append(":");
		sb.append(dgram.getPort());
		
		sdu.remoteURL = new URL(sb.toString());
		
		sb = new StringBuffer();
		sb.append("http://");
		sb.append(localAddr.getAddress().getHostAddress());
		sb.append(":");
		sb.append(localAddr.getPort());
		
		sdu.url = new URL(sb.toString());
		
		LOG.error("remoteadapter - sdu local url " + sdu.url + " remote " + sdu.remoteURL);
		*/
		
		Message msg = createMessage("", "registerServices", sdu);
		msg.msgType = "S"; // Service / System / Process level message - a
							// message which can be processed by any service
							// regardless of name
		out(msg);
	}

	public void disconnectAll() {
		cm.getComm().disconnectAll();
	}

	@Override
	public String getToolTip() {
		return "allows remote communication between applets, or remote instances of myrobotlab";
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		RemoteAdapter remote = new RemoteAdapter("remote");
		remote.startService();
				
		ServiceFactory services = new ServiceFactory("services");
		services.startService();
/*		
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
*/
		Logging log = new Logging("log");
		log.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}	
}
