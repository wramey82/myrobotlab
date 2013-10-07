package org.myrobotlab.webgui;

import java.io.File;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.NanoHTTPD;
import org.myrobotlab.service.WebGUI;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 * 
 */
public class WebServer extends NanoHTTPD implements HTTPProcessor {

	public final static Logger log = LoggerFactory.getLogger(WebServer.class);

	private HashMap<String, HTTPProcessor> processors = new HashMap<String, HTTPProcessor>();

	private WebGUI webgui;

	public String theme = "vader";

	HTTPProcessor defaultProcessor;

	public WebServer(WebGUI webgui, int port) {
		super(port);

		this.webgui = webgui;
		processors.put("/services", new RESTProcessor());
		defaultProcessor = new ResourceProcessor(this.webgui);
		processors.put("/resource", defaultProcessor);// FIXME < wrong should be
														// root
	}

	@Override
	public HashSet<String> getURIs() {
		// TODO Auto-generated method stub
		return null;
	}

	// TODO - caching ?
	public Response serve(String uri, String method, Properties header, Properties parms, Socket socket) {
		log.info(String.format("%s [%s]", method, uri));
		String[] keys = uri.split("/");
		String key = null;

		// needs routing to correct processor
		if (keys.length > 1) {
			key = String.format("/%s", keys[1]);
			if (processors.containsKey(key)) {
				HTTPProcessor processor = processors.get(key);
				log.debug(String.format("uri hook - [%s] invoking %s", key, processor.getClass().getSimpleName()));
				return processor.serve(uri, method, header, parms, socket);
			}
		}

		log.info("handling with default processor");
		return defaultProcessor.serve(uri, method, header, parms, socket);
	}

}
