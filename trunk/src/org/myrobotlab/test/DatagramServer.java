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

package org.myrobotlab.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class DatagramServer extends Thread {

	public final static Logger LOG = Logger.getLogger(DatagramServer.class
			.getCanonicalName());

	protected BufferedReader in = null;
	boolean isRunning = true;

	int serverPort = 4445;
	HashMap<String, AddressAndPort> clients = new HashMap<String, AddressAndPort>();

	DatagramSocket socket = new DatagramSocket(serverPort);

	public DatagramServer() throws IOException {
		super("DatagramServer");
	}

	class AddressAndPort {
		int port;
		InetAddress address;

		public AddressAndPort(InetAddress address, int port) {
			this.address = address;
			this.port = port;
		}

		public String toString() {
			return address + ":" + port;
		}
	}

	public class DatagramThread extends Thread {
		public void run() {
			try {

				LOG.info("starting DatagramThread processor ");
				byte[] videoFrame = new byte[65535];

				while (isRunning) {

					Iterator<String> it = clients.keySet().iterator();
					while (it.hasNext()) {
						String endpoint = it.next();
						AddressAndPort ap = clients.get(endpoint);

						videoFrame = new Date().toString().getBytes();

						// send the response to the client at "address" and
						// "port"
						DatagramPacket packet = new DatagramPacket(videoFrame,
								videoFrame.length, ap.address, ap.port);
						LOG.info("sending videoFrame to " + ap);
						socket.send(packet);
						Thread.sleep(1000); // / LAME
					}

					if (clients.size() == 0) {
						LOG.info("noone listening");
						Thread.sleep(1000); // / LAME
					}

				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// TODO - spin through all and close ?
			// socket.close();
		}

	}

	public void run() {

		try {

			// start the sender
			new DatagramThread().start();

			LOG.info("starting listener");

			while (isRunning) {
				// receive request
				byte[] buf = new byte[65535];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				AddressAndPort ap = new AddressAndPort(packet.getAddress(),
						packet.getPort());
				clients.put(ap.toString(), ap);

				LOG.info(ap.toString() + " has requested a video feed (TCP)");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws IOException {
		new DatagramServer().start();
	}

}
