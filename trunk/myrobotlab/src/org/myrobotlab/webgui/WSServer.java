package org.myrobotlab.webgui;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.http.Response;
import org.myrobotlab.net.http.ResponseException;
import org.myrobotlab.service.WebGUI;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.slf4j.Logger;
/**
 * WSServer - to be used as a general purpose HTTP server 
 * extends WebSocketServer for web socket support
 * clients which do not implement websockets are processed with
 * registered processors HTTP 1.1 support
 * 
 * @author GroG
 *
 */
public class WSServer extends WebSocketServer {

	public final static Logger log = LoggerFactory.getLogger(WSServer.class.getCanonicalName());

	private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
	private HashMap<String, HTTPProcessor> processors = new HashMap<String, HTTPProcessor>();
	HTTPProcessor defaultProcessor;

	public static final String HTTP_OK = "200 OK";
	public static final String HTTP_REDIRECT = "301 Moved Permanently";
	// public static final String HTTP_NOT_AUTHORIZED = "401 Not Authorized";
	public static final String HTTP_NOT_AUTHORIZED = "401 Access Denied";

	public static final String HTTP_FORBIDDEN = "403 Forbidden";
	public static final String HTTP_NOTFOUND = "404 Not Found";
	public static final String HTTP_BADREQUEST = "400 Bad Request";
	public static final String HTTP_INTERNALERROR = "500 Internal Server Error";
	public static final String HTTP_NOTIMPLEMENTED = "501 Not Implemented";

	/**
	 * Common mime types for dynamic content
	 */
	public static final String MIME_PLAINTEXT = "text/plain";
	public static final String MIME_HTML = "text/html";
	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

	private Inbox inbox;
	private WebGUI webgui;

	public WSServer(WebGUI webgui, int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
		this.webgui = webgui;

		processors.put("/services", new RESTProcessor());
		defaultProcessor = new ResourceProcessor(webgui);
		processors.put("/resource", defaultProcessor);// FIXME < wrong should be
														// root
		this.inbox = webgui.getInbox();
	}

	public WSServer(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		String clientkey = String.format("%s:%d", conn.getRemoteSocketAddress().getAddress().getHostAddress(), conn.getRemoteSocketAddress().getPort());
		log.info(String.format("onOpen %s", clientkey));
		log.info(String.format("onOpen %s", conn.getLocalSocketAddress().getHostName()));
		log.info(String.format("onOpen %s", conn.getRemoteSocketAddress().getHostName()));
		webgui.clients.put(clientkey, clientkey);
		// this.sendToAll( "new connection: " +
		// handshake.getResourceDescriptor() );
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		String clientkey = String.format("%s:%d", conn.getRemoteSocketAddress().getAddress().getHostAddress(), conn.getRemoteSocketAddress().getPort());
		webgui.clients.remove(clientkey);
		// this.sendToAll( conn + " has left the room!" );
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		// this.sendToAll( message );
		// System.out.println( conn.getLocalSocketAddress() + ": " + message );
		// System.out.println("[" + message + "]" );
		log.info("webgui <---to--- client {}", message);
		// Gson gson = new Gson();
		// Gson gson = new
		// GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").create();
		// Message msg = gson.fromJson(message, Message.class);

		Message msg = Encoder.gson.fromJson(message, Message.class);

		// log.info("{}",msg);
		// log.info("parsed message");
		// outbox.add(msg);
		inbox.add(msg);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
		if (conn != null) {
			// some errors like port binding failed may not be assignable to a
			// specific websocket
		}
	}

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	// FIXME - on release all - this throws an exception and doesn't complete -
	// but is it worth
	// the overhead of a try???
	public void sendToAll(String text) {
		Collection<WebSocket> con = connections();
		log.info("webgui ---to---> client ");
		synchronized (con) {
			for (WebSocket c : con) {
				c.send(text);
			}
		}
	}

	@Override
	public void onRawOpen(WebSocket conn, ByteBuffer d) {

		try {

			String s = new String(d.array());
			String sub = s.substring(0, d.limit());

			HashMap<String, String> parms = new HashMap<String, String>();
			HashMap<String, String> headers = new HashMap<String, String>();

			// Decode the header into parms and header java properties
			Map<String, String> pre = new HashMap<String, String>();

			// FIXME - return an object - pre ???
			decodeHeader(sub, pre, parms, headers);
			String uri = pre.get("uri");
			String method = pre.get("method");

			// ////////////// webserver //////////////////////////
			log.info(String.format("%s [%s]", method, uri));
			String[] keys = uri.split("/");
			String key = null;
			if (keys.length > 1) {
				key = String.format("/%s", keys[1]);
			}

			Response r = null;

			// needs routing to correct processor
			if (processors.containsKey(key)) {

				HTTPProcessor processor = processors.get(key);
				log.debug(String.format("uri hook - [%s] invoking %s", key, processor.getClass().getSimpleName()));
				r = processor.serve(uri, method, headers, parms);

			} else {
				r = defaultProcessor.serve(uri, method, headers, parms);
			}

			// ///////////// webserver ////////////////////////////

			// Response r = processor.serve(uri, method, headers, parms);

			// sendResponse(r.status, r.mimeType, r.header, r.data);

			// log.info(String.format("onRawOpen %s", sub));
			// String response = "HTTP/1.0 200 OK\r\n" +
			// "Content-Type: text/html; charset=utf-8\r\n" + "\r\n" +
			// "<html>\r\n" + "hello java_websocket !\r\n" + "</html>\r\n" +
			// "\r\n";
			// conn.send(bytes)
			// FIXME - inefficient and stoopid

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			r.send(out);
			out.flush();
			// out.close();
			// conn.send("hello");
			byte[] ba = out.toByteArray();
			log.info(String.format("sending %d bytes", ba.length));
			conn.send(ba);
			conn.close();
		} catch (Exception e) {
			// attempt a 500
			Logging.logException(e);
		}
	}

	// ///////////////////// FROM NANOHTTPD ///////////////////////

	/**
	 * Decodes the sent headers and loads the data into Key/value pairs
	 */
	public void decodeHeader(String in, Map<String, String> pre, Map<String, String> parms, Map<String, String> headers) throws ResponseException {

		int pos0 = in.indexOf("\r");
		String inLine = in.substring(0, pos0);

		StringTokenizer st = new StringTokenizer(inLine);
		if (!st.hasMoreTokens()) {
			throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
		}

		pre.put("method", st.nextToken());

		if (!st.hasMoreTokens()) {
			throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
		}

		String uri = st.nextToken();

		// Decode parameters from the URI
		int qmi = uri.indexOf('?');
		if (qmi >= 0) {
			decodeParms(uri.substring(qmi + 1), parms);
			uri = decodePercent(uri.substring(0, qmi));
		} else {
			//uri = decodePercent(uri);
		}

		// If there's another token, it's protocol version,
		// followed by HTTP headers. Ignore version but parse headers.
		// NOTE: this now forces header names lowercase since they are
		// case insensitive and vary by client.
		++pos0;
		if (st.hasMoreTokens()) {

			String str = in.substring(pos0 + 1);

			StringTokenizer st2 = new StringTokenizer(str, "\r\n");

			while (st2.hasMoreElements()) {
				String line = (String) st2.nextElement();
				int p = line.indexOf(':');
				if (p >= 0)
					headers.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
			}

		}

		pre.put("uri", uri);

	}

	/**
	 * Find byte index separating header from body. It must be the last byte of
	 * the first two sequential new lines.
	 */
	private int findHeaderEnd(final byte[] buf, int rlen) {
		int splitbyte = 0;
		while (splitbyte + 3 < rlen) {
			if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
				return splitbyte + 4;
			}
			splitbyte++;
		}
		return 0;
	}

	/**
	 * Find the byte positions where multipart boundaries start.
	 */
	private int[] getBoundaryPositions(ByteBuffer b, byte[] boundary) {
		int matchcount = 0;
		int matchbyte = -1;
		List<Integer> matchbytes = new ArrayList<Integer>();
		for (int i = 0; i < b.limit(); i++) {
			if (b.get(i) == boundary[matchcount]) {
				if (matchcount == 0)
					matchbyte = i;
				matchcount++;
				if (matchcount == boundary.length) {
					matchbytes.add(matchbyte);
					matchcount = 0;
					matchbyte = -1;
				}
			} else {
				i -= matchcount;
				matchcount = 0;
				matchbyte = -1;
			}
		}
		int[] ret = new int[matchbytes.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = matchbytes.get(i);
		}
		return ret;
	}

	/**
	 * Decodes parameters in percent-encoded URI-format ( e.g.
	 * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Map.
	 * NOTE: this doesn't support multiple identical keys due to the simplicity
	 * of Map.
	 */
	private void decodeParms(String parms, Map<String, String> p) {
		if (parms == null) {
			p.put(QUERY_STRING_PARAMETER, "");
			return;
		}

		p.put(QUERY_STRING_PARAMETER, parms);
		StringTokenizer st = new StringTokenizer(parms, "&");
		while (st.hasMoreTokens()) {
			String e = st.nextToken();
			int sep = e.indexOf('=');
			if (sep >= 0) {
				p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
			} else {
				p.put(decodePercent(e).trim(), "");
			}
		}
	}

	/**
	 * Decode percent encoded <code>String</code> values.
	 * 
	 * @param str
	 *            the percent encoded <code>String</code>
	 * @return expanded form of the input, for example "foo%20bar" becomes
	 *         "foo bar"
	 */
	protected String decodePercent(String str) {
		String decoded = null;
		try {
			decoded = URLDecoder.decode(str, "UTF8");
		} catch (UnsupportedEncodingException ignored) {
		}
		return decoded;
	}

}
