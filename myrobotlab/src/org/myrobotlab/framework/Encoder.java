package org.myrobotlab.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
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
 * scheme = alpha *( alpha | digit | "+" | "-" | "." )
 * 
 * http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes
 */
public class Encoder {

	public final static Logger log = LoggerFactory.getLogger(Encoder.class);

	public final static String SCHEME_MRL = "mrl";
	public final static String SCHEME_BASE64 = "base64";

	public final transient static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();

	/*
	 * public Message decodePath(String pathInfo) {
	 * 
	 * }
	 */
	public final static String API_REST_PREFIX = "/api";

	public static Message decodeURI(URI uri) {
		log.info(String.format("authority %s", uri.getAuthority())); // gperry:blahblah@localhost:7777
		log.info(String.format("     host %s", uri.getHost())); // localhost
		log.info(String.format("     port %d", uri.getPort())); // 7777
		log.info(String.format("     path %s", uri.getPath()));
		log.info(String.format("    query %s", uri.getQuery())); // /api/string/gson/runtime/getUptime
		log.info(String.format("   scheme %s", uri.getScheme())); // http
		log.info(String.format(" userInfo %s", uri.getUserInfo())); // gperry:blahblah

		Message msg = decodePathInfo(uri.getPath());

		return msg;
	}

	// TODO optimization of HashSet combinations of supported encoding instead
	// of parsing...
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

	// TODO
	// public static Object encode(Object, encoding) - dispatches appropriately

	public static String msgToGson(Message msg) {
		return gson.toJson(msg, Message.class);
	}

	public static Message gsonToMsg(String gsonData) {
		return (Message) gson.fromJson(gsonData, Message.class);
	} 
	
	public static final Message base64ToMsg(String base64) {
		String data = base64;
		if (base64.startsWith(String.format("%s://",SCHEME_BASE64))){
			data = base64.substring(SCHEME_BASE64.length()+3);
		}
		final ByteArrayInputStream dataStream = new ByteArrayInputStream(Base64.decodeBase64(data));
		try {
			final ObjectInputStream objectStream = new ObjectInputStream(dataStream);
			Message msg = (Message) objectStream.readObject();
			return msg;
		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}
	}

	public static final String msgToBase64(Message msg) {
		final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
		try {
			final ObjectOutputStream objectStream = new ObjectOutputStream(dataStream);
			objectStream.writeObject(msg);
			objectStream.close();
			dataStream.close();
			String base64 = String.format("%s://%s",SCHEME_BASE64, new String(Base64.encodeBase64(dataStream.toByteArray())));
			return base64;
		} catch (Exception e) {
			log.error(String.format("couldnt seralize %s", msg));
			Logging.logException(e);
			return null;
		}
	}
	
	static final public String getParameterSignature(Object[] data) {
		if (data == null) {
			//return "null";
			return "";
		}

		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < data.length; ++i) {
			if (data[i] != null) {
				Class<?> c = data[i].getClass(); // not all data types are safe
													// toString() e.g.
													// SerializableImage
				if (c == String.class || c == Integer.class || c == Boolean.class || c == Float.class || c == MRLListener.class) {
					ret.append(data[i].toString());
				} else {
					String type = data[i].getClass().getCanonicalName();
					String shortTypeName = type.substring(type.lastIndexOf(".") + 1);
					ret.append(shortTypeName);
				}

				if (data.length != i + 1) {
					ret.append(",");
				}
			} else {
				ret.append("null");
			}

		}
		return ret.toString();

	}


	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			
			String user = null;
			String group = null;
			
			HashMap<String,String> userGroup = new HashMap<String,String>();
			userGroup.put(String.format("%s", group), "ALLOW");
			userGroup.put(String.format("%s.%s", user, group), "ALLOW");
			
			String x = userGroup.get("null.null");

			String url = "http://gperry:blahblah@localhost:7777/api/string/gson/runtime/getUptime";
			log.info(url.substring(5));
			url = "mrl://remote/tcp://blah.com";
			URI uri = new URI(url);
			
			log.info(uri.getHost());
			log.info(uri.getScheme());
			log.info(uri.getPath());

			Message msg = decodeURI(uri);

			decodePathInfo("/api");
			decodePathInfo(null);
			decodePathInfo("  /api/  ");

			// REST rest = new REST();
		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	public static String type(String type) {
		int pos0 = type.indexOf(".");
		if (pos0 > 0){
			return type;
		}
		return String.format("org.myrobotlab.service.%s",type);
	}

}
