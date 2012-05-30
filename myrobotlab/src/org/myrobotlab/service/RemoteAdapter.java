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
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.interfaces.MRLMSGReciever;

/***
 * 
 * @author Gro-G
 * 
 *         This is a service which allows foreign clients to connect. It
 *         maintains a list of currently connected clients. Most of the
 *         communication details are left up to a configurable Communicator and
 *         Serializer. At this point the RemoteAdapter has the ability to
 *         filter, block, or re-route any inbound messages.
 *         
 *         TODO - refactor the MRLClient comm devices back in
 *         TODO - optimize communication blocks - such that new objects don't need to be
 *         re-created on each message
 * 
 */

public class RemoteAdapter extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(RemoteAdapter.class.getCanonicalName());

	// types of listening threads - multiple could be managed
	// when correct interfaces and base classes are done
	transient TCPListener tcpListener = null;
	transient UDPListener udpListener = null;
	transient UDPStringListener udpStringListener = null;

	MRLMSGReciever mrlMsgReciever = null;
		
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
		ObjectOutputStream out;
		ObjectInputStream in;

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
			serverSocket = null;
		}
				
		public void run() {
			if (TCPPort > 0) {
				try {

					serverSocket = new ServerSocket(TCPPort, 10);
					
					log.info(getName() + " TCPListener listening on "
							+ serverSocket.getLocalSocketAddress());
					
					Socket clientSocket = serverSocket.accept();
					Communicator comm = (Communicator) cm.getComm();
					log.info("new connection [" + clientSocket.getRemoteSocketAddress() + "]");
					
					out = new ObjectOutputStream(clientSocket.getOutputStream());
					out.flush();
					in = new ObjectInputStream(clientSocket.getInputStream());					
					
					while (isRunning()) {

						Message msg = (Message)in.readObject();
						
						if ("registerServices".equals(msg.method)) 
						{
							comm.addClient(clientSocket);
							invoke("registerServices", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort(), msg);
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

					serverSocket.close();
				} catch (IOException e) {
					log.error("Could not listen on requested ["
							+ TCPPort + "]");
					TCPPort = 0;
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				log.error("servicePort is <= 0 - terminating");
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
				socket = new DatagramSocket(UDPStringPort);
				
				log.info(getName() + " UDPStringListener listening on "
						+ socket.getLocalAddress() + ":" + socket.getLocalPort());
				
				byte[] b = new byte[65535];
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				while (isRunning()) {

					socket.receive(dgram); // blocks
					//String data = new String(b);
					String data = new String(dgram.getData(), 0, dgram.getLength());
					dgram.setLength(b.length); // must reset length field!

					log.debug("udp data [" + data + "]");
					send(dst, fn, data);
					// create a string message and send it
				}

			} catch (Exception e) {
				log.error("UDPStringListener could not listen");
				Service.logException(e);
			}
		}
	}

	class UDPListener extends Thread { 

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
				socket = new DatagramSocket(UDPPort);

				log.info(getName() + " UDPListener listening on "
						+ socket.getLocalAddress() + ":" + socket.getLocalPort());

				Communicator comm = (Communicator) cm.getComm();

				byte[] b = new byte[65535];
				ByteArrayInputStream b_in = new ByteArrayInputStream(b);
				DatagramPacket dgram = new DatagramPacket(b, b.length);

				comm.setIsUDPListening(true);

				while (isRunning()) {
					socket.receive(dgram); // receives all datagrams
					ObjectInputStream o_in = new ObjectInputStream(b_in); // FIXME - not well optimized - should be cached (same on the receiver)
					try {
						Message msg = (Message) o_in.readObject();
						dgram.setLength(b.length); // must reset length field!
						b_in.reset(); // reset so next read is from start of byte[] again


						if ("registerServices".equals(msg.method)) 
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
				Service.logException(e);
			}
		}
	}
		
	@Override
	public void startService() {
		// FIXME - block until isReady on the ServerSocket
		if (!isRunning())
		{
			super.startService();
			/* FIXME No longer support auto-listening with TCP or UDPStrings
			 * this needs to refined such that you can explicitly set the listening type/protocol
			 * additionally there should be a explicit setting to do outbound communication
			 * or set outbound type/protocol based on a datatype mapping
			udpStringListener = new UDPStringListener(getName() + "_udpStringListener", this);
			udpStringListener.start();
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
	
	/**
	 * Registers message routing to flow from a remote MRL instance to a
	 * MRLMSGReciever
	 * 
	 * @param host - remote MRL's host name or ip
	 * @param port - remote MRL's listening port
	 * @param client - the message receiver which will be getting messages
	 */
	public void registerForMsgs(String host, int port, MRLMSGReciever client)
	{
		mrlMsgReciever = client;
		
		// FIXME FIXME FIXME - 
		// when the SDU is de-serialize it will explode for all the unknown Service types
		// work-around would be a new serviceDirectoryUpdate which has no type info ???
		// registering with remote instance of MRL
		sendServiceDirectoryUpdate(null, null, null, host, port, null);
		
		// FIXME - very cheesy vs. blocking
	}
	
	/**
	 * Subscribes to a remote MRL service method.  When the method is called on the remote system an event
	 * message with return data is sent.  It is necessary to registerForServices before subscribing.
	 * 
	 * @param outMethod - the name of the remote method to hook/subscribe to
	 * @param serviceName - service name of the remote service
	 * @param inMethod - inMethod can be used as an identifier
	 * @param paramTypes
	 */
	public void subscribe(String outMethod, String serviceName, String inMethod, Class<?> ... paramTypes) {
		NotifyEntry ne = new NotifyEntry(outMethod, this.getName(), inMethod, paramTypes);
		send(serviceName, "notify", ne);
	}
	
	/**
	 * Registers remote service to a host and port. Used in client API to register
	 * a remote MRL instance's service.  After this is done messages can be sent
	 * to the service using basic "Service.send" command. This method is typically
	 * only used if messages are ONLY to be sent to MRL and not received.  If they
	 * are to be sent AND received the preferred registration is registerForMSGs, 
	 * which allows messages to be sent and received.
	 * 
	 * @param host - remote MRL's host name or ip
	 * @param port - remote MRL's listening port
	 * @param serviceName - name of remote service
	 * @return
	 */
	public boolean register(String host, int port, String serviceName)
	{
		try {
			url = new URL("http://" + host + ":" + port);
		} catch (MalformedURLException e) {
			Service.logException(e);
			return false;
		}

		// FIXME - "more info" should win update - logic in Runtime which allows
		// more info regarding a Service to update the registry.  Services which
		// share the same domain could be hammered by this registration
		// since we only have a url & port name - if they are in the same process
		// you could nullify the service pointer in the ServiceWrapper
		ServiceWrapper sw = Runtime.getService(serviceName);
		if (sw != null)
		{
			log.warn("request to register " + serviceName + " which is already registered");
		} else {
			// FIXME - asking for a legitimate registry from running system
			// this will be more complex in that it requires a call-back
			// additionally - it seems overkill for just sending messages
			sendServiceDirectoryUpdate(null, null, null, host, port, null);
		}
		
		return true;
		
	}

	/******************* Client API End ************************************/	
	
	public void setUDPPort (int port)
	{		
		UDPPort = port;
	}

	public void setTCPPort (int port)
	{
		TCPPort = port;
	}

	public void setUDPStringPort (int port)
	{
		UDPStringPort = port;
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		
		// modes of operation
		// command line - single command lind which sends a message 
		// to mrl - this does not require the service to start
		// just sends a quick message into a running mrl instance
		// includes -file - to send a file as the message (possibly a serialized binary object)
		
		// simple send command 
		
		// full service
		Logging logger = new Logging("logger");
		logger.startService();
				
		RemoteAdapter client = new RemoteAdapter("remote");
		client.startService();
		
		TestCatcher catcher = new TestCatcher("catcher");
		catcher.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}		
}
