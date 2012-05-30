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

// Jar packaging info - http://mindprod.com/jgloss/jar.html

import java.applet.Applet;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.ServiceDirectoryUpdate;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.service.GUIService;

public class PlayerAppletTest extends Applet {

	private static final long serialVersionUID = 1L;
	public GUIService gui = null;

	public final static Logger log = Logger.getLogger(PlayerAppletTest.class
			.getCanonicalName());

	public void init() {
		log.error("init");
		ConfigurationManager cfg = new ConfigurationManager();
		cfg.clear();

		//CommAsciiOverTCP.clientList = new HashMap<String, CommunicatorTCPRequestThread>(); // CRAP
	}

	public void start() {
		log.error("start");
		// resize(150,25);
		// Arduino arduino = new Arduino("arduino");
		// Player player = new Player("player01");
		// Servo tilt = new Servo("tilt");
		Random rand = new Random();
		gui = new GUIService("gui" + rand.nextInt());

		ServiceEntry se = new ServiceEntry();
		se.host = "127.0.0.1";
		// se.host = "10.0.0.152";
		se.name = "match01";
		se.servicePort = 6767;
		se.serviceClass = "org.myrobotlab.framework.Service";
		se.lastModified = new Date();

		Class[] params = new Class[1];
		params[0] = ServiceDirectoryUpdate.class;
		Class returnType = ServiceDirectoryUpdate.class;

		gui.getHostCFG().setServiceEntry(se);
		// gui.getHostCFG().setMethod("10.0.0.145", "match01",
		// "registerService", returnType, params);
		gui.getHostCFG().setMethod("70.59.157.45", "match01",
				"registerService", returnType, params);
		// gui.getHostCFG().set(name, value)
		// gui.getHostCFG().save("10.cfg.txt");

		/*
		 * ConfigurationManager hostcfg = gui.getHostCFG();
		 * gui.getCFG().setMethod(host, serviceName, methodName, returnType,
		 * parameterTypes);
		 */

		// tilt.attach("arduino", 6);

		// tilt.start();
		// arduino.start();
		// player.start();
		gui.startService();
		gui.display();

	}

	public void stop() {

		log.error("stop");
		gui.getOutbox().getCommunicationManager().getComm().stopService();
		gui.stopService();
		gui.getCFG().clear();
		gui = null;
	}

	public void destroy() {
		// clean up
		log.error("destroy");

	}

	public static void main(String[] args) throws InterruptedException {

		PlayerAppletTest p = new PlayerAppletTest();
		p.init();
		p.start();
	}

}
