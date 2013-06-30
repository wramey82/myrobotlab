package org.myrobotlab.service.interfaces;

import java.net.Socket;
import java.util.HashSet;
import java.util.Properties;

import org.myrobotlab.webgui.NanoHTTPD.Response;

public interface HTTPProcessor {

	public Response serve(String uri, String method, Properties header, Properties parms, Socket socket);
	
	public HashSet<String> getURIs();
}
