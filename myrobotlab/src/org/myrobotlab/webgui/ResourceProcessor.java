package org.myrobotlab.webgui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.java_websocket.util.Base64;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.security.BasicSecurity;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.WebGUI;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.myrobotlab.webgui.NanoHTTPD.Response;
import org.slf4j.Logger;

public class ResourceProcessor implements HTTPProcessor {

	public static String root = "root"; // get from WebGUI

	public HashSet<String> scannedDirectories = new HashSet<String>();

	public HashMap<String, String> searchAndReplace = new HashMap<String, String>();

	public final static Logger log = LoggerFactory.getLogger(NanoHTTPD.class.getCanonicalName());

	private WebGUI webgui;

	private boolean authorized = false;

	private boolean requiresSecurity = false;

	public ResourceProcessor(WebGUI webgui) {
		this.webgui = webgui;
		scan();
	}

	public void scan() {
		try {
			List<File> files = FindFile.find(root, null);
			for (int i = 0; i < files.size(); ++i) {
				File file = files.get(i);
				String t = file.getPath().replace('\\', '/');
				String uri = t.substring(root.length());
				log.info(String.format("overriding with uri [%s]", uri));
				scannedDirectories.add(uri);
			}
		} catch (FileNotFoundException e) {
			// Logging.logException(e);
			log.info("root directory not found - no overrides");
		}
	}

	@Override
	public Response serve(String uri, String method, Properties header, Properties parms, Socket socket) {
		return serveFile(uri, header, new File(root), true, socket, scannedDirectories.contains(uri));
	}

	// FIXME - deprecate
	@Override
	public HashSet<String> getURIs() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI,
	 * ignores all headers and HTTP parameters.
	 */
	// FIXME - normalize further
	public Response serveFile(String uri, Properties header, File homeDir, boolean allowDirectoryListing, Socket socket, boolean isCustomFile) {

		if (requiresSecurity && !authorized) {
			try {
				if (header.containsKey("authorization")) {
					String up = header.getProperty("authorization");
					int pos = up.indexOf("Basic ");
					if (pos != -1) {
						up = up.substring(pos + 6);
					}
					// FIXME - depends on commons !!!!
					String usernameAndPassword = new String(Base64.decode(up));
					String username = usernameAndPassword.substring(0, usernameAndPassword.lastIndexOf(":"));
					String password = usernameAndPassword.substring(usernameAndPassword.lastIndexOf(":") + 1);
					String token = BasicSecurity.authenticate(username, password);

					if (token != null) {
						authorized = true;
					} else {
						throw new Exception(String.format("no token for user %s", username));
					}
					log.info(usernameAndPassword);
				} else {
					throw new Exception("no authorization in header");
				}

			} catch (Exception e) {
				Logging.logException(e);
				Response r = new Response();
				r.status = NanoHTTPD.HTTP_NOT_AUTHORIZED;
				r.addHeader("WWW-Authenticate", "Basic realm=\"MyRobotLab\"");
				return r;
			}
		}

		// Remove URL arguments
		uri = uri.trim().replace(File.separatorChar, '/');
		if (uri.indexOf('?') >= 0) {
			uri = uri.substring(0, uri.indexOf('?'));
		}

		// Prohibit getting out of current directory
		if (uri.startsWith("..") || uri.endsWith("..") || uri.indexOf("../") >= 0) {
			return new Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");
		}

		File f;
		if (!isCustomFile) {
			// ---- begin resource ----------------

			InputStream fis = FileIO.class.getResourceAsStream(uri);
			if (fis == null)
				return new Response(NanoHTTPD.HTTP_NOTFOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");

			try {
				// Get MIME type from file name extension, if possible
				String mime = null;
				int dot = uri.lastIndexOf('.');
				if (dot >= 0) {
					mime = (String) NanoHTTPD.theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
				}
				if (mime == null) {
					// mime = NanoHTTPD.MIME_DEFAULT_BINARY;
					mime = NanoHTTPD.MIME_PLAINTEXT;
				}

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = fis.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				fis.close();
				buffer.flush();

				byte[] content = null;

				if ("/resource/WebGUI/myrobotlab.html".equals(uri)) {
					content = filter(new String(buffer.toByteArray()), header);
				} else {
					content = buffer.toByteArray();
				}

				Response r = new Response(NanoHTTPD.HTTP_OK, mime, new ByteArrayInputStream(content));

				r.addHeader("Content-length", "" + content.length);
				return r;
			} catch (IOException ioe) {
				return new Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
			}
			// ---- end resource -------------------
		} else {
			// ---- begin file -----------------------
			f = new File(homeDir, uri);
			if (!f.exists())
				return new Response(NanoHTTPD.HTTP_NOTFOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");

			// List the directory, if necessary
			if (f.isDirectory()) {
				// Browsers get confused without '/' after the
				// directory, send a redirect.
				if (!uri.endsWith("/")) {
					uri += "/";
					Response r = new Response(NanoHTTPD.HTTP_REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
					r.addHeader("Location", uri);
					return r;
				}

				// First try index.html and index.htm
				if (new File(f, "index.html").exists())
					f = new File(homeDir, uri + "/index.html");
				else if (new File(f, "index.htm").exists())
					f = new File(homeDir, uri + "/index.htm");

				// No index file, list the directory
				else if (allowDirectoryListing) {
					String[] files = f.list();
					String msg = "<html><body><h1>Directory " + uri + "</h1><br/>";

					if (uri.length() > 1) {
						String u = uri.substring(0, uri.length() - 1);
						int slash = u.lastIndexOf('/');
						if (slash >= 0 && slash < u.length())
							msg += "<b><a href=\"" + uri.substring(0, slash + 1) + "\">..</a></b><br/>";
					}

					for (int i = 0; i < files.length; ++i) {
						File curFile = new File(f, files[i]);
						boolean dir = curFile.isDirectory();
						if (dir) {
							msg += "<b>";
							files[i] += "/";
						}

						msg += "<a href=\"" + NanoHTTPD.encodeUri(uri + files[i]) + "\">" + files[i] + "</a>";

						// Show file size
						if (curFile.isFile()) {
							long len = curFile.length();
							msg += " &nbsp;<font size=2>(";
							if (len < 1024)
								msg += curFile.length() + " bytes";
							else if (len < 1024 * 1024)
								msg += curFile.length() / 1024 + "." + (curFile.length() % 1024 / 10 % 100) + " KB";
							else
								msg += curFile.length() / (1024 * 1024) + "." + curFile.length() % (1024 * 1024) / 10 % 100 + " MB";

							msg += ")</font>";
						}
						msg += "<br/>";
						if (dir)
							msg += "</b>";
					}
					return new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML, msg);
				} else {
					return new Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: No directory listing.");
				}
			}

			// ----- end file ------------------------
		}
		// }
		try {
			// Get MIME type from file name extension, if possible
			String mime = null;
			// int dot = f.getCanonicalPath().lastIndexOf('.');
			int dot = uri.lastIndexOf('.');
			if (dot >= 0) {
				mime = (String) NanoHTTPD.theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
			}
			if (mime == null) {
				mime = NanoHTTPD.MIME_PLAINTEXT;
			}

			// Support (simple) skipping:
			long startFrom = 0;
			String range = header.getProperty("Range");
			if (range != null) {
				if (range.startsWith("bytes=")) {
					range = range.substring("bytes=".length());
					int minus = range.indexOf('-');
					if (minus > 0)
						range = range.substring(0, minus);
					try {
						startFrom = Long.parseLong(range);
					} catch (NumberFormatException nfe) {
					}
				}
			}

			InputStream fis;
			if (!isCustomFile) {
				fis = FileIO.class.getResourceAsStream(uri);
				if (fis == null) {
					return new Response(NanoHTTPD.HTTP_NOTFOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
				}

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = fis.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				fis.close();
				buffer.flush();

				byte[] content = null;
				// FIXME - this is not normalized - because the code around it
				// is
				// not normalized
				if ("/resource/WebGUI/myrobotlab.html".equals(uri)) {

					content = filter(new String(buffer.toByteArray()), header);
				} else {
					content = buffer.toByteArray();
				}

				Response r = new Response(NanoHTTPD.HTTP_OK, mime, new ByteArrayInputStream(content));

				r.addHeader("Content-length", "" + content.length);
				return r;
			} else {
				fis = new FileInputStream(f);
				// fis.skip(startFrom); // TODO support skip in the future
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = fis.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				fis.close();
				buffer.flush();

				byte[] content = null;
				// FIXME - this is not normalized - because the code around it
				// is
				// not normalized
				if ("/resource/WebGUI/myrobotlab.html".equals(uri)) {

					content = filter(new String(buffer.toByteArray()), header);
				} else {
					content = buffer.toByteArray();
				}

				Response r = new Response(NanoHTTPD.HTTP_OK, mime, new ByteArrayInputStream(content));
				r.addHeader("Content-length", "" + content.length);
				// r.addHeader("Content-length", "" + (f.length() - startFrom));
				// r.addHeader("Content-range", "" + startFrom + "-" +
				// (f.length() - 1) + "/" + f.length());
				return r;
			}

		} catch (IOException ioe) {
			return new Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
		}
	}

	public byte[] filter(String filter, Properties header) {
		String inHost = header.getProperty("host");
		String hostIP;
		int pos0 = inHost.lastIndexOf(":");
		if (pos0 > 0) {
			hostIP = inHost.substring(0, pos0);
		} else {
			hostIP = inHost;
		}
		log.info("preforming the following substitutions for myrobotlab.html");
		// log.info("from client @ {}", socket.getRemoteSocketAddress());
		log.info("<%=getHostAddress%> --> {}", hostIP);
		filter = filter.replace("<%=getHostAddress%>", hostIP);
		log.info("<%=wsPort%> --> {}", webgui.wsPort);
		filter = filter.replace("<%=wsPort%>", webgui.wsPort.toString());
		log.info("<%=runtimeName%> --> {}", Runtime.getInstance().getName());
		filter = filter.replace("<%=runtimeName%>", Runtime.getInstance().getName());
		log.info("<%=webguiName%> --> {}", webgui.getName());
		filter = filter.replace("<%=webguiName%>", webgui.getName());
		log.info("<%=httpPort%> --> {}", webgui.httpPort.toString());
		filter = filter.replace("<%=httpPort%>", webgui.httpPort.toString());
		// filter.replace(, newChar);
		return filter.getBytes();

	}

}
