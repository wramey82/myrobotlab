package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.webgui.WSServer;
import org.myrobotlab.webgui.WebServer;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

public class WebGUI extends Service {

	// import javax.xml.transform.Transformer;

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WebGUI.class.getCanonicalName());

	private Integer httpPort = 7777;
	private Integer wsPort = 7778;

	public transient WebServer ws;
	public transient WSServer wss;

	boolean spawnBrowserOnStartUp = false;
	
	public WebGUI(String n) {
		super(n, WebGUI.class.getCanonicalName());
	}

	public Integer getPort() {
		return httpPort;
	}

	public boolean startWebServer(Integer port) {
		try {
			if (port.equals(httpPort) && ws != null)
			{
				warn("web server already running on port %d", port);
				return true;
			}
			
			this.httpPort = port;
			if (ws != null) {
				ws.stop();
			}

			ws = new WebServer(port);
			ws.start();

			return true;

		} catch (IOException e) {
			error(e.getMessage());
		}
		return false;
	}

	public boolean startWebSocketServer(Integer port) {
		try {
			
			if (port.equals(wsPort) && wss != null)
			{
				warn("web socket server already running on port %d", port);
				return true;
			}
			
			this.wsPort = port;

			if (wss != null) {
				wss.stop();
			}

			wss = new WSServer(port, getOutbox());
			wss.start();
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}

		return false;
	}

	public boolean start() {

		boolean result = true;
		result &= startWebServer(httpPort);
		result &= startWebSocketServer(wsPort);
		if (spawnBrowserOnStartUp)
		{
			BareBonesBrowserLaunch.openURL(String.format("http://localhost:%d/services", httpPort));
		}
		return result;
	}

	@Override
	public String getToolTip() {
		return "The new web enabled GUI 2.0 !";
	}

	public void startService() {
		super.startService();
		start();
	}

	@Override
	public void stopService() {
		try {
			super.stopService();
			if (ws != null) {
				ws.stop();
			}
			if (wss != null) {
				wss.stop();
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public boolean preProcessHook(Message m) {
		// FIXME - problem with collisions of this service's methods
		// and dialog methods ?!?!?
		
		// if the method name is == to a method in the GUIService
		if (methodSet.contains(m.method)) {
			// process the message like a regular service
			return true;
		}
		
		// otherwise send the message to the dialog with the senders name
		sendToAll(m);
		return false;
	}

	// FIXME - take out of RESTProcessor - normalize
	public String toJson(Message msg) {
		try {
			ByteArrayOutputStream out = null;
			//Gson gson = new Gson(); // FIXME - threadsafe? singleton?
			//Gson gson = new GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").create();
			
			Gson gson = new GsonBuilder()
			   .setDateFormat(DateFormat.FULL, DateFormat.FULL).create();
			out = new ByteArrayOutputStream(); // FIXME - threadsafe? singleton?
			JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8")); // FIXME - threadsafe? singleton?
			//writer.setIndent("  "); // TODO config driven - very cool !

			//writer.beginArray();
			gson.toJson(msg, Message.class, writer);
			// for (Message message : messages) {
			// gson.toJson(message, Message.class, writer);
			// }
			//writer.endArray();
			writer.close();
			return new String(out.toByteArray());
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	public void sendToAll(Message msg) {
		wss.sendToAll(toJson(msg));
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		// REST rest = new REST();
		// Runtime.createAndStart("arduino", "Arduino");
		Clock clock = (Clock)Runtime.createAndStart("clock", "Clock");
		//clock.startClock();
		WebGUI webgui = (WebGUI) Runtime.createAndStart("webgui", "WebGUI");

		webgui.subscribe("pulse", "clock", "pulse", String.class);
		
		/*
		Message msg = webgui.createMessage("webgui", "publishPin", new Object[] { new Pin(12, Pin.DIGITAL_VALUE, 1, "arduino") });
		webgui.sendToAll(msg);
		*/

		// FileIO.stringToFile("services.html", rest.getServices());

		// Runtime.releaseAll();
		// Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
