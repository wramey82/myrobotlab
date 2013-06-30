package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.slf4j.Logger;

public class WebGUI extends Service {

	// import javax.xml.transform.Transformer;

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WebGUI.class.getCanonicalName());

	private Integer port = 7777;

	public org.myrobotlab.webgui.WebServer ws;

	public WebGUI(String n) {
		super(n, WebGUI.class.getCanonicalName());
	}

	public Integer getPort() {
		return port;
	}

	public boolean setPort(Integer port) {
		this.port = port;

		if (ws != null) {
			ws.stop();
			ws = new org.myrobotlab.webgui.WebServer(port);
		} else {
			ws = new org.myrobotlab.webgui.WebServer(port);
		}

		return true;
	}

	public boolean start() {
		if (ws != null) {
			setPort(port);
		}
		try {
			ws.start();
			BareBonesBrowserLaunch.openURL(String.format("http://localhost:%d/services", port));
			return true;
		} catch (IOException e) {
			error(e.getMessage());
		}

		return false;
	}

	public void display() {
		start();
	}

	@Override
	public String getToolTip() {
		return "The new web enabled GUI 2.0 !";
	}

	public void startService() {
		super.startService();
		try {
			setPort(port);
			ws.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stopService() {
		super.stopService();
		if (ws != null)
		{
			ws.stop();
		}
	}

	@Override
	public void releaseService() {
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		//REST rest = new REST();
		Runtime.createAndStart("arduino", "Arduino");
		Runtime.createAndStart("webgui", "WebGUI");

		//FileIO.stringToFile("services.html", rest.getServices());

		//Runtime.releaseAll();
		// Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
