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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import org.myrobotlab.framework.Message;

public abstract class Communicator {

	// TODO static public Communicator getCommunicator (String name, String
	// classType);??
	public abstract void send(final Message msg); // TODO - manage throws

	public abstract void stopService();

	public abstract void disconnectAll();

	public abstract void addClient(Socket socket);

	public abstract void addClient(DatagramSocket s, InetAddress address,
			int port);

	public abstract void setIsUDPListening(boolean set);

	/*
	 * public abstract OutputStream getOutputStream(); public abstract
	 * InputStream getInputStream();
	 */

}
