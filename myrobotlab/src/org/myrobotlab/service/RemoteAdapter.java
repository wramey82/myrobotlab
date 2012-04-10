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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.interfaces.MRLMSGReciever;

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

	MRLMSGReciever mrlMsgReciever = null;
	
	InetAddress serverAddress = null;
	
	// FIXME - all port & ip data needs to be only in the threads
	public int servicePort = 6767;
	public String serverIP = "0.0.0.0";
	
	public RemoteAdapter(String n) {
		super(n, RemoteAdapter.class.getCanonicalName());
	}

	
	public RemoteAdapter(String n, String hostname) {
		super(n, RemoteAdapter.class.getCanonicalName(), hostname);
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
					logException(e);
				}
			}
		}
				
		public void run() {
			if (servicePort > 0) {
				try {

					serverSocket = new ServerSocket(servicePort, 0, serverAddress);
					
					LOG.info(getName() + " TCPListener listening on "
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
				
				LOG.info(getName() + " UDPStringListener listening on "
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

			} catch (Exception e) {
				LOG.error("UDPStringListener could not listen");
				Service.logException(e);
			}
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

				LOG.info(getName() + " UDPListener listening on "
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
								invoke("registerServices", dgram.getAddress().getHostAddress(), dgram.getPort(), msg);
								continue;
							}

							// client API
							if (mrlMsgReciever != null)
							{
								mrlMsgReciever.receive(msg);
								continue;
							}
							
							if (msg.getName().equals(getName()))
							{
								getInbox().add(msg);
							} else {
								getOutbox().add(msg);
							}
						}

					} catch (ClassNotFoundException e) {
						logException(e);						
						LOG.error("udp datagram dumping bad msg");
					}
					dgram.setLength(b.length); // must reset length field!
					b_in.reset(); // reset so next read is from start of byte[]
									// again
				} // while isRunning

			} catch (Exception e) {
				LOG.error("UDPListener could not listen");
				Service.logException(e);
			}
		}
	}
		
	@Override
	public void startService() {
		// TODO - block until isReady on the ServerSocket
		if (!isRunning())
		{
			super.startService();
			tcpListener = new TCPListener(getName() + "_tcpMsgListener", this);
			tcpListener.start();
			udpListener = new UDPListener(getName() + "_udpMsgListener", this);
			udpListener.start();
			udpStringListener = new UDPStringListener(getName() + "_udpStringListener", this);
			udpStringListener.start();
		} else {
			LOG.warn("RemoteAdapter " + getName() + " is already started");
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

	public void disconnectAll() {
		cm.getComm().disconnectAll();
	}

	@Override
	public String getToolTip() {
		return "allows remote communication between applets, or remote instances of myrobotlab";
	}
	
	static public String help() {
		return "java -jar MRLClient.jar -host [localhost] -port [6767] -service [myService] -method [doIt] -data \"data1\" \"data2\" \"data3\"... \n"
				+ "host: the name or ip of the instance of MyRobotLab which the message should be sent."
				+ "port: the port number which the foreign MyRobotLab is listening to."
				+ "service: the Service the message is to be sent."
				+ "method: the method to be invoked on the Service"
				+ "data: the method's parameters."
				;
		
	}
	
	/******************* Client API Begin **********************************/
	
	
	public void register (String host, int port, MRLMSGReciever client)
	{
		sendServiceDirectoryUpdate(null, null, null, host, port, null);
	}
	
	public boolean sendUDPMSG (String host, int port, String serviceName, String method, Object ... data)
	{
		Message msg = new Message();
		msg.sender = this.getName();
		msg.method = method;
		msg.name = serviceName;
		msg.data = data;
		
		// send it
		try {

			DatagramSocket socket = new DatagramSocket();
			ByteArrayOutputStream b_out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(b_out);

			oos.writeObject(msg);
			oos.flush();
			byte[] b = b_out.toByteArray();

			DatagramPacket packet = new DatagramPacket(b, b.length,
					InetAddress.getByName(host), port);

			socket.send(packet);
			oos.reset();

		} catch (Exception e) {
			LOG.error("threw [" + e.getMessage() + "]");
			return false;
		}

		return true;
		
	}
	
	// use public void Service.send(String name, String method, Object... data)
	/******************* Client API End ************************************/	

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);
				
		String clientServiceName = cmdline.getSafeArgument("-name", 0, "client");
		
		// modes of operation
		// command line - single command lind which sends a message 
		// to mrl - this does not require the service to start
		// just sends a quick message into a running mrl instance
		// includes -file - to send a file as the message (possibly a serialized binary object)
		
		// simple send command 
		
		// full service
				
		RemoteAdapter client = new RemoteAdapter(clientServiceName);
		client.startService();
	}		
}
