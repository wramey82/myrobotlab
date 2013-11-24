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
 * handles all encoding and decoding of MRL messages or api(s) assumed context -
 * services can add an assumed context as a prefix
 * /api/returnEncoding/inputEncoding/service/method/param1/param2/ ...
 * 
 * xmpp for example assumes (/api/string/gson)/service/method/param1/param2/ ...
 * 
 *  scheme        = alpha *( alpha | digit | "+" | "-" | "." )
 *  
 *  http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes
 */
public class Encoder {

	public final static Logger log = LoggerFactory.getLogger(Encoder.class);
	public final transient static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();

	/*
	 * public Message decodePath(String pathInfo) {
	 * 
	 * }
	 */
	public final static String API_REST_PREFIX = "/api";

	public static Message decodeURI(URI uri) {
		log.info(String.format("authority %s",uri.getAuthority())); // gperry:blahblah@localhost:7777
		log.info(String.format("     host %s",uri.getHost())); // localhost
		log.info(String.format("     port %d",uri.getPort())); // 7777
		log.info(String.format("     path %s",uri.getPath()));
		log.info(String.format("    query %s",uri.getQuery())); // /api/string/gson/runtime/getUptime
		log.info(String.format("   scheme %s",uri.getScheme())); // http
		log.info(String.format(" userInfo %s",uri.getUserInfo())); // gperry:blahblah

		Message msg = decodePathInfo(uri.getPath());

		return msg;
	}

	// TODO optimization of HashSet combinations of supported encoding instead of parsing...
	// e.g. HashMap<String> supportedEncoding.containsKey(
	public static Message decodePathInfo(String pathInfo) {
		
		if (pathInfo == null) {
			log.error("pathInfo is null");
			return null;
		}

		if (!pathInfo.startsWith(API_REST_PREFIX)) {
			log.error(String.format("pathInfo [%s] needs to start with [%s]", pathInfo, API_REST_PREFIX));
			return null;
		}

		int p0 = API_REST_PREFIX.length() + 1; // "/api/"
		int p1 = pathInfo.indexOf("/", p0);

		String responseEncoding = pathInfo.substring(p0, p1);

		p0 = p1 + 1;
		p1 = pathInfo.indexOf("/", p0);

		String inputEncoding = pathInfo.substring(p0, p1);

		p0 = p1 + 1;
		p1 = pathInfo.indexOf("/", p0);

		String serviceName = pathInfo.substring(p0, p1);

		p0 = p1 + 1;
		p1 = pathInfo.indexOf("/", p0);

		String method = null;
		String[] params = null;

		if (p1 != -1) {
			// there are parameters
			method = pathInfo.substring(p0, p1);
			params = pathInfo.substring(++p1).split("/");

			// param conversion via inputEncoding
		} else {
			method = pathInfo.substring(p0, p1);
		}
		
		// FIXME INVOKING VS PUTTING A MESSAGE ON THE BUS
		Message msg = new Message();
		msg.name = serviceName;
		msg.method = method;

		return msg;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			String url = "http://gperry:blahblah@localhost:7777/api/string/gson/runtime/getUptime";
			url = "mrl://remote/tcp://blah.com";
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
