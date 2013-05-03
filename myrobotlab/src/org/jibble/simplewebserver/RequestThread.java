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

 */

// TODO support HTTP 1.1
// TODO token & session
// TODO - parse into name-value map

package org.jibble.simplewebserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.RoutingEntry;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.WebServer;
import org.slf4j.Logger;

import com.google.gson.Gson;

/**
 * Copyright Paul Mutton http://www.jibble.org/
 * 
 */
public class RequestThread extends Thread {

	public final static Logger log = LoggerFactory.getLogger(RequestThread.class.getCanonicalName());

	WebServer myService;
	private File _rootDir;
	private Socket _socket;
	Gson gson = new Gson();

	public class Session {
		String login;
		String host;
		SocketAddress remoteAddress;
		Date lastActivity;
	}

	private HashMap<String, Session> sessions = new HashMap<String, Session>();

	public RequestThread(Socket socket, File rootDir, WebServer s) {
		myService = s;
		_socket = socket;
		_rootDir = rootDir;
	}

	private static void sendHeader(BufferedOutputStream out, int code, String contentType, long contentLength, long lastModified) throws IOException {
		out.write(("HTTP/1.0 " + code + " OK\r\n" + "Date: " + new Date().toString()
				+ "\r\n" // TODO - 1.1 give me keepalive
				+ "Server: JibbleWebServer/1.0\r\n" + "Content-Type: " + contentType + "\r\n" + "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n"
				+ ((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") + "Last-modified: " + new Date(lastModified).toString() + "\r\n" + "\r\n").getBytes());
	}

	private static void sendError(BufferedOutputStream out, int code, String message) throws IOException {
		message = message + "<hr>" + SimpleWebServer.VERSION;
		sendHeader(out, code, "text/html", message.length(), System.currentTimeMillis());
		out.write(message.getBytes());
		out.flush();
		out.close();
	}

	// properties
	boolean manageLogin = true;
	String loginType = "URL";

	// communication
	BufferedReader in = null;
	BufferedOutputStream out = null;

	// return message array
	LinkedList<Message> messages = new LinkedList<Message>();

	public void addMsg(String method, Object... data) {
		Message msg = myService.createMessage("chrome", method, data);
		msg.historyList = new ArrayList<RoutingEntry>();
		messages.add(msg);
	}

	public void sendJSONMsg(String method, Object... data) {
		try {
			Message msg = myService.createMessage("chrome", method, data);
			msg.historyList = new ArrayList<RoutingEntry>();

			// if we are responsible for security
			// then wrap all messages into a security
			// envelope
			if (manageLogin && messages.size() > 0) {
				int newSize = msg.data.length + messages.size();
				Object[] msgArray = new Object[newSize];
				for (int i = 0; i < newSize; ++i) {
					if (i < data.length) {
						msgArray[i] = data[i];
					} else {
						msgArray[i] = messages.get(i - (msg.data.length));
					}
				}
				msg.data = msgArray;
			}

			String json = gson.toJson(msg);
			sendHeader(out, 200, "text/json", json.getBytes().length, (new Date()).getTime());
			out.write(json.getBytes());
			out.flush();
		} catch (IOException e) {
			Logging.logException(e);
		}
	}

	public void run() {
		InputStream reader = null;
		try {
			_socket.setSoTimeout(30000);
			in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
			out = new BufferedOutputStream(_socket.getOutputStream());

			String request = in.readLine();
			if (request == null || !request.startsWith("GET ") || !(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
				// Invalid request type (no "GET")
				sendError(out, 500, "Invalid Method.");
				return;
			}
			
			log.info("request {}", request);

			int queryStrPos = request.indexOf('?');

			String path = null;
			String queryString = null;
			if (queryStrPos > 0) {
				path = request.substring(4, queryStrPos);
				queryString = request.substring(queryStrPos + 1, request.length() - 9);
			} else {
				path = request.substring(4, request.length() - 9);
			}

			log.info("queryString {}", queryString);

			Message msg = null;
			if (queryString != null) {
				log.info(_socket.getInetAddress() + " " + _socket.getRemoteSocketAddress());
				String msgstr = queryString.substring(4);
				msgstr = URLDecoder.decode(msgstr, "UTF-8");
				msg = gson.fromJson(msgstr, Message.class);

				if (manageLogin && "login".equals(msg.method)) {
					String err = authenticate((String) msg.data[0], (String) msg.data[1], loginType);
					if (err != null) {
						addMsg("error", err);
						sendJSONMsg("msg", ""); // LOOK - no token sent !
					} else {
						String token = "329rfuj0f89923urj39tugj49tu0g8u8ur80";

						addMsg("setContext", "select.players");
						addMsg("authenticated", token);
						sendJSONMsg("msg", token);
					}
				}

				log.info("msg {}",msg);
				msg.historyList = new ArrayList<RoutingEntry>();
				myService.getOutbox().add(msg);
				String json = gson.toJson(msg);

				sendHeader(out, 200, "text/json", json.getBytes().length, (new Date()).getTime());// file.lastModified()

				out.write(json.getBytes());

				out.flush();
				// out.close();
			}

			log.info("_rootDir {}", _rootDir);

			File file = new File(_rootDir, URLDecoder.decode(path, "UTF-8")).getCanonicalFile();

			log.info("file {}", file);

			if (file.isDirectory()) {
				// Check to see if there is an index file in the directory.
				File indexFile = new File(file, "index.html");
				if (indexFile.exists() && !indexFile.isDirectory()) {
					file = indexFile;
				}
			}

			if (!file.toString().startsWith(_rootDir.toString())) {
				// Uh-oh, it looks like some lamer is trying to take a peek
				// outside of our web root directory.
				sendError(out, 403, "Permission Denied.");
			} else if (!file.exists()) {
				// The file was not found.
				sendError(out, 404, "File Not Found.");
			} else if (file.isDirectory()) {
				// print directory listing
				if (!path.endsWith("/")) {
					path = path + "/";
				}
				File[] files = file.listFiles();
				sendHeader(out, 200, "text/html", -1, System.currentTimeMillis());
				String title = "Index of " + path;
				out.write(("<html><head><title>" + title + "</title></head><body><h3>Index of " + path + "</h3><p>\n").getBytes());
				for (int i = 0; i < files.length; i++) {
					file = files[i];
					String filename = file.getName();
					String description = "";
					if (file.isDirectory()) {
						description = "&lt;DIR&gt;";
					}
					out.write(("<a href=\"" + path + filename + "\">" + filename + "</a> " + description + "<br>\n").getBytes());
				}
				out.write(("</p><hr><p>" + SimpleWebServer.VERSION + "</p></body><html>").getBytes());
			} else {

				String ext = SimpleWebServer.getExtension(file);
				String contentType = (String) SimpleWebServer.MIME_TYPES.get(ext);
				if (contentType == null) {
					contentType = "application/octet-stream";
				}

				// handler based on extension type Python -> .py
				if (myService.containsKey(ext)) {
					// handler interface WebHandler
					// load file
					// file to string
					// get registered handler
					// cache file -
					// execute script

					// invoke the gson parser
					if (".json".equals(ext)) {

						/*
						 * Object[] data = new Object[2]; data[0]="name";
						 * data[1]="password"; Message msg =
						 * web.createMessage("sessionMgr", "logon", data);
						 * String s = gson.toJson(msg); //log.info(s);
						 */

						String json = "";
						// Message msg = gson.fromJson(json, Message.class);
						myService.send("player01", "callBack");

					}

					String script = FileIO.fileToString(file.toString());
					// TODO - make interface
					myService.send(myService.getHandlerName(ext), "exec", script);

				}

				reader = new BufferedInputStream(new FileInputStream(file));

				sendHeader(out, 200, contentType, file.length(), file.lastModified());

				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = reader.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
				reader.close();
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			Logging.logException(e);
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception anye) {
					// Do nothing.
				}
			}
		}
	}

	public String authenticate(String loginName, String loginPass, String type) {
		String err = null;
		try {

			// drupal
			if ("URL".equals(type)) {
				log.info("logon for " + loginName);
				// url get page - get token
				HTTPRequest loginPage;
				loginPage = new HTTPRequest("http://myrobotlab.org/authenticate.php?loginName=" + loginName + "&loginPass=" + loginPass + "");
				String response = loginPage.getString();
				if (!loginPage.hasError() && response.indexOf("authenticated") != -1) {
					sessions.put(loginName, new Session());
				} else {
					return "incorrect login or password :" + loginPage.getError();
				}
				// login us login ticket api
			} else {
				err = "authentication type " + type + " not avaialble";
			}
			// file
			// sql
			// open id

		} catch (IOException e) {
			Logging.logException(e);
			err = e.getMessage();
		}

		return err;
	}

}