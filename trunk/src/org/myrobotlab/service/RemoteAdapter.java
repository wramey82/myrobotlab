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
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceDirectoryUpdate;
import org.myrobotlab.service.interfaces.Communicator;

/***
 * 
 * @author GPerry
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
	transient static HashMap<String, ServerSocket> serverSockets = new HashMap<String, ServerSocket>();
	transient Thread tcpListener = null;
	transient Thread udpListener = null;
	transient Thread udpStringListener = null;
	InetAddress serverAddress = null;
	transient ServerSocket serverSocket = null; 
	
	public RemoteAdapter(String n) {
		super(n, RemoteAdapter.class.getCanonicalName());
	}

	
	public RemoteAdapter(String n, String hostname) {
		super(n, TestCatcher.class.getCanonicalName(), hostname);
	}

	@Override
	public void loadDefaultConfiguration() {
		cfg.set("servicePort", 6767);
		cfg.set("serverIP", "0.0.0.0"); // listen on all
	}

	@Override
	public boolean isReady() {
		if (serverSocket != null) {
			return serverSocket.isBound();
		}
		return false;
	}

	// TCPtcpListener to maintain connections - TODO - refactor TCPMessageListener
	class TCPtcpListener implements Runnable {
		public void run() {
			if (cfg.getInt("servicePort") > 0) {
				try {

					LOG.info(name + " tcp attempting to listen on servicePort "
							+ cfg.get("serverIP") + ":"
							+ cfg.getInt("servicePort"));
					serverSocket = new ServerSocket(cfg.getInt("servicePort"),
							0, serverAddress);
					serverSockets.put(host, serverSocket);

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
							+ cfg.getInt("servicePort") + "]");
					cfg.set("servicePort", 0);
				}
			} else {
				LOG.error("servicePort is <= 0 - terminating");
			}

		}
	}
	
	
	
	class UDPStringListener implements Runnable
	{
		DatagramSocket socket = null;
		String dst = "chess";
		String fn = "parseOSC";

		public void run() {

			LOG.info(name + " udp attempting to listen on servicePort "
					+ cfg.get("serverIP") + ":" + cfg.getInt("servicePort"));

			try {
				socket = new DatagramSocket(6668);

				byte[] b = new byte[65535];
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				while (isRunning()) {
					/*
					 * byte[] buf = new byte[65535]; DatagramPacket packet = new
					 * DatagramPacket(buf, buf.length); socket.receive(packet);
					 * LOG.info("recieved udp");
					 */

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

	class UDPMsgListener implements Runnable { // TODO refactor and derive

		DatagramSocket socket = null;

		public void run() {

			LOG.info(name + " udp attempting to listen on servicePort "
					+ cfg.get("serverIP") + ":" + cfg.getInt("servicePort"));

			try {
				socket = new DatagramSocket(cfg.getInt("servicePort"));
				Communicator comm = (Communicator) cm.getComm();

				byte[] b = new byte[65535];
				ByteArrayInputStream b_in = new ByteArrayInputStream(b);
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				comm.setIsUDPListening(true);

				while (isRunning()) {
					/*
					 * byte[] buf = new byte[65535]; DatagramPacket packet = new
					 * DatagramPacket(buf, buf.length); socket.receive(packet);
					 * LOG.info("recieved udp");
					 */

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
								registerServices(dgram.getAddress().getHostAddress(), dgram.getPort(), msg);
								// getting clients address and port
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
	
/*	
	public boolean preProcessHook(Message m)
	{
	}
*/	
	
/*
	@Override
	public void run() {
		thisThread = Thread.currentThread();
		isRunning = true;

		try {
			while (isRunning) {
				// TODO - should be config of Service to process anonymous and
				// relay named
				Message msg = getMsg();
				if (msg.name.length() == 0 || msg.name.compareTo(name) == 0) {
					// process anonymous
					LOG.info(name + " processing " + msg.method + " <-- "
							+ msg.sender + "/" + msg.sendingMethod);
					invoke(msg);
				} else {
					// relay
					out(msg);
				}
			}
		} catch (InterruptedException e) {
			LOG.info("service INTERRUPTED " + thisThread.getName());
			isRunning = false;
		} // sink it TODO - ALL invokes should return out message!!!
	}
*/
	
	@Override
	public void startService() {
		// TODO - block until isReady on the ServerSocket
		super.startService();
		tcpListener = new Thread(new TCPtcpListener(), name + "_tcpMsgListener");
		tcpListener.start();
		udpListener = new Thread(new UDPMsgListener(), name + "_udpMsgListener");
		udpListener.start();
		udpStringListener = new Thread(new UDPStringListener(), name + "_udpStringListener");
		udpStringListener.start();
	}

	@Override
	public void stopService() {
		ServerSocket serverSocket = serverSockets.get(getHost());

		try {
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		serverSocket = null;
		super.stopService();
		if (tcpListener != null)
		{
			tcpListener.interrupt();
			tcpListener = null;
		}

		if (thisThread != null) {
			thisThread.interrupt();
		}
		thisThread = null;

	}

	// TODO - should Service even have these ??? - should be an Interface !!
	@Override
	public synchronized void registerServices(ServiceDirectoryUpdate sdu) {
		LOG.error("ra registerServices here");	
/*		
		for (int i = 0; i < sdu.serviceEntryList_.size(); ++i) {
			hostcfg.setServiceEntry(sdu.serviceEntryList_.get(i));
		}

		sendServiceDirectoryUpdate(sdu.remoteHostname, sdu.remoteServicePort,
				sdu.hostname, sdu.servicePort);
*/				
	}

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
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		RemoteAdapter remote = new RemoteAdapter("remote");
		remote.startService();

		ChessGame chess = new ChessGame("chess");
		chess.startService();
				
		Invoker services = new Invoker("services");
		services.startService();
		
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}	
}
