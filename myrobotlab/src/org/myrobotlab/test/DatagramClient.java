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

import java.applet.Applet;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class DatagramClient extends Applet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(DatagramClient.class
			.getCanonicalName());

	public static void main(String[] args) throws IOException {

	}

	public void start() {
		try {
			// get a datagram socket
			DatagramSocket socket;
			socket = new DatagramSocket();

			// send request
			byte[] buf = new byte[256];
			// InetAddress address = InetAddress.getByName(args[0]);
			InetAddress address = InetAddress.getByName("10.0.0.152");
			DatagramPacket packet = new DatagramPacket(buf, buf.length,
					address, 4445);
			LOG.info("sending " + buf);
			socket.send(packet);

			boolean isRunning = true;

			while (isRunning) {
				// get response
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				// display response
				String received = new String(packet.getData(), 0, packet
						.getLength());
				LOG.info("recv: " + received + "from "
						+ packet.getAddress().toString() + ":"
						+ packet.getPort());
			}

			socket.close();

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}