package org.myrobotlab.framework;

import java.net.URI;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.python.antlr.PythonParser.decorator_return;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *  handles all encoding and decoding of MRL messages or api(s)
 *  assumed context - services can add an assumed context as a prefix
 *  /api/returnEncoding/inputEncoding/service/method/param1/param2/ ...
 *  
 *  xmpp for example assumes (/api/String/Gson)/service/method/param1/param2/ ...
 */
public class Encoder {

	public final static Logger log = LoggerFactory.getLogger(Encoder.class);
	public final transient static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();
	
	/*
	public Message decodePath(String pathInfo) {
		
	}
	*/
	public final static String API_REST_PREFIX = "/api";
	
	public static Message decodeURI(URI uri){
		String authority = uri.getAuthority(); // gperry:blahblah@localhost:7777
		String host = uri.getHost(); // localhost
		int port = uri.getPort(); // 7777
		String pathInfo = uri.getPath();
		String query = uri.getQuery(); // /api/string/gson/runtime/getUptime
		String scheme = uri.getScheme(); // http
		String userInfo = uri.getUserInfo(); // gperry:blahblah
				
		Message msg = decodePathInfo(uri.getPath());
		
		return msg;
	}
	
	public static Message decodePathInfo(String pathInfo){
		if (pathInfo == null)
		{
			log.error("pathInfo is null");
			return null;
		}
		
		if (pathInfo.startsWith(API_REST_PREFIX)){
			log.error(String.format("pathInfo [%s] needs to start with [%s]", pathInfo, API_REST_PREFIX));
			return null;
		}
		
		int p0 = API_REST_PREFIX.length() + 1; // "/api/"
		int p1 = pathInfo.indexOf("/", p0);
		
		String responseEncoding = pathInfo.substring(p0, p1);
		
		return null;
		
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		
		try {

		String url = "http://gperry:blahblah@localhost:7777/api/string/gson/runtime/getUptime";
		URI uri = new URI(url);
		
		Message msg = decodeURI(uri);
		
		decodePathInfo("/api");
		decodePathInfo(null);
		decodePathInfo("  /api/  ");
		
		// REST rest = new REST();
	} catch (Exception e) {
		Logging.logException(e);
	}
		
	}
	
}
