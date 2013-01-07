/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of Mini Wegb Server / SimpleWebServer.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: ServerSideScriptEngine.java,v 1.4 2004/02/01 13:37:35 pjm2 Exp $

Graciously lifted from http://www.jibble.org/  
... and mercilessly hacked...

 */

package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jibble.simplewebserver.SimpleWebServer;
import org.myrobotlab.framework.Service;

public class WebServer extends Service {

	private static final long serialVersionUID = 1L;

	SimpleWebServer webServer = null;

	private HashMap<String, String> handler = new HashMap<String, String>();

	public final static Logger log = Logger.getLogger(WebServer.class.getCanonicalName());

	public WebServer(String n) {
		super(n, WebServer.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public void startWebServer() {
		startWebServer("./", 80);
	}

	public void startWebServer(String root, int port) {
		log.info("starting web server " + root + " port " + port);
		// webServer = new SimpleWebServer(arg0, arg1);
		try {
			webServer = new SimpleWebServer(new File(root), port, this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setHandler(String name, String extention) {
		handler.put(extention, name);
	}

	public String getHandlerName(String extension) {
		return handler.get(extension);
	}

	public void removeHandler(String name) {
		handler.remove(name);
	}

	public boolean containsKey(String key) {
		return handler.containsKey(key);
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		WebServer web = new WebServer("web");
		web.startService();
		web.startWebServer();
		web.setHandler("python", ".py");

		/*
		 * SoccerGame game = new SoccerGame("soccergame"); game.startService();
		 */
		Arduino arduino = new Arduino("arduino");
		arduino.startService();

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */

		/*
		 * Object[] data = new Object[2]; data[0]="name"; data[1]="password";
		 * Message msg = web.createMessage("sessionMgr", "logon", data); String
		 * s = gson.toJson(msg); log.info(s);
		 * 
		 * 
		 * 
		 * 
		 * Message={ msgID:'', timeStamp:'', name:'', sender:'',
		 * sendingMethod:'', historyList:[], status:'', msgType:'', method:'',
		 * data:[]}
		 * 
		 * 
		 * var txt =
		 * '{"msg":{"msgID":"20120429135553578","timeStamp":"20120429135553578","name":"sessionMgr","sender":"web","sendingMethod":"","historyList":[],"status":"","msgType":"","method":"logon","data":["name","password"]}}';
		 * 
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */

	}

}
