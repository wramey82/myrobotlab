package org.myrobotlab.webgui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Properties;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.myrobotlab.webgui.NanoHTTPD.Response;

public class ResourceProcessor implements HTTPProcessor {

	@Override
	public Response serve(String uri, String method, Properties header, Properties parms, Socket socket) {
		return serveFile(uri, header);
	}

	@Override
	public HashSet<String> getURIs() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI,
	 * ignores all headers and HTTP parameters.
	 */
	public Response serveFile(String uri, Properties header) {
		// Make sure we won't die of an exception later
//		if (!homeDir.isDirectory())
//			return new Response(NanoHTTPD.HTTP_INTERNALERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERRROR: serveFile(): given homeDir is not a directory.");
		
		// FIXME - checked for custom scanned directory paths !!!
		// if path.contains /resource/scanned -> return NanoHTTPD.serveFile(custom/scanned);

		// Remove URL arguments
		//uri = uri.trim().replace(File.separatorChar, '/');
		if (uri.indexOf('?') >= 0)
			uri = uri.substring(0, uri.indexOf('?'));

		// Prohibit getting out of current directory
		if (uri.startsWith("..") || uri.endsWith("..") || uri.indexOf("../") >= 0)
			return new Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");

		InputStream fis = FileIO.class.getResourceAsStream(uri);
		//File f = new File("", uri);
		if (fis == null)
			return new Response(NanoHTTPD.HTTP_NOTFOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");

		try {
			// Get MIME type from file name extension, if possible
			String mime = null;
			int dot = uri.lastIndexOf('.');
			if (dot >= 0)
				mime = (String) NanoHTTPD.theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
			if (mime == null)
				mime = NanoHTTPD.MIME_DEFAULT_BINARY;

			// FIXME - support skipping again :P
			// Support (simple) skipping:
			// http://stackoverflow.com/questions/716680/difference-between-content-range-and-range-headers
			/*
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

			//FileInputStream fis = new FileInputStream(f);
			fis.skip(startFrom);
			// TODO - fix again
			r.addHeader("Content-length", "" + (f.length() - startFrom));
			r.addHeader("Content-range", "" + startFrom + "-" + (f.length() - 1) + "/" + f.length());
			r.addHeader("Content-length", "" + (f.length() - startFrom));
			*/
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = fis.read(data, 0, data.length)) != -1) {
			  buffer.write(data, 0, nRead);
			}

			buffer.flush();

			byte[] content =  buffer.toByteArray();
			
			Response r = new Response(NanoHTTPD.HTTP_OK, mime, new ByteArrayInputStream(content));
			
			r.addHeader("Content-length", "" + content.length);
			return r;
		} catch (IOException ioe) {
			return new Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
		}
	}


}
