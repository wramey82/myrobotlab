/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */
package org.myrobotlab.fileLib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipException;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class FileIO {

	public final static Logger log = LoggerFactory.getLogger(FileIO.class.getCanonicalName());

	public final static String fileToString(File file) {
		String result = null;
		DataInputStream in = null;

		try {
			// File f = new File(filename);
			byte[] buffer = new byte[(int) file.length()];
			in = new DataInputStream(new FileInputStream(file));
			in.readFully(buffer);
			result = new String(buffer);
		} catch (IOException e) {
			log.error("could not open filename " + file.getName());
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) { /* ignore it */
			}
		}
		return result;
	}

	public final static String fileToString(String filename) {
		return fileToString(new File(filename));
	}

	public static void stringToFile(String filename, String data) {
		stringToFile(filename, data, null);
	}

	public static void stringToFile(String filename, String data, String encoding) {
		Writer out = null;
		try {
			if (encoding != null) {
				out = new OutputStreamWriter(new FileOutputStream(filename), encoding);
			} else {
				out = new OutputStreamWriter(new FileOutputStream(filename));
			}
			out.write(data);
		} catch (Exception e) {
			log.error(Logging.stackToString(e));
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				log.error(Logging.stackToString(e));
			}
		}
	}

	// TODO - NOT IMPLMENENTED
	public final static String getResourceBinary(String filename) {
		InputStream isr = FileIO.class.getResourceAsStream("/resource/" + filename);
		if (isr == null) {
			log.error("could not locate resource " + filename);
		}
		DataInputStream input = new DataInputStream(isr);
		String stringData = null;

		byte b[] = new byte[1024];
		try {
			while (true) {
				input.readFully(b);
				stringData = input.readUTF();
			}
		} catch (EOFException e) {
			// Do nothing if it is the end of file.
		} catch (Exception e) {
			Logging.logException(e);
		} finally {
			try {
				isr.close();
			} catch (IOException e) {
				/* ignore */
				return null;
			}
		}

		return stringData;
	}

	/**
	 * Safe method for trying to read the content of a resource file.
	 * 
	 * @param filename
	 * @return
	 */
	public final static String getResourceFile(String filename) {
		StringBuffer str = new StringBuffer();
		BufferedReader br = null;

		String davlikPrefix = "";
		if (Platform.isDavlik()) {
			davlikPrefix = "/assets";
		}

		try {
			InputStream is = FileIO.class.getResourceAsStream(String.format("%s/resource/%s", davlikPrefix, filename));

			if (is == null) {
				log.error(String.format("resource %s not found", filename));
				return null;
			}

			InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			String s;
			while ((s = br.readLine()) != null) {
				str.append(s);
				str.append("\n");
			}
		} catch (IOException e) {
			log.error(String.format("could not open filename %s/resource/%s", davlikPrefix, filename));
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}

		return str.toString();
	}

	public final static boolean writeBinary(String filename, Object toSave) {
		try {
			// use buffering
			OutputStream file = new FileOutputStream(filename);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(toSave);
				output.flush();
			} finally {
				output.close();
			}
		} catch (IOException e) {
			Logging.logException(e);
			return false;
		}
		return true;
	}

	public final static Object readBinary(String filename) {
		try {
			InputStream file = new FileInputStream(filename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				return (Object) input.readObject();
			} finally {
				input.close();
			}
		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}
	}

	static public String streamToString(InputStream is) {
		try {
			return new java.util.Scanner(is).useDelimiter("\\A").next();
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}

	static public String getResouceLocation() {
		URL url = File.class.getResource("/resource");

		// FIXME - DALVIK issue !
		if (url == null) {
			return null; // FIXME DALVIK issue
		} else {
			return url.toString();
		}
	}

	static public String getRootLocation() {
		URL url = File.class.getResource("/");
		return url.toString();
	}

	static public boolean inJar() {
		String location = getResouceLocation();
		if (location != null) {
			return getResouceLocation().startsWith("jar:");
		} else {
			return false;
		}
	}

	static public String getResourceJarPath() {

		if (!inJar()) {
			log.info("resource is not in jar");
			return null;
		}

		String full = getResouceLocation();
		String jarPath = full.substring(full.indexOf("jar:file:/") + 10, full.lastIndexOf("!"));
		return jarPath;
	}

	static public ArrayList<String> listInternalContents(String path) {
		if (!inJar()) {
			// get listing if in debug mode or classes are unzipped
			String rp = getRootLocation();
			String targetDir = rp.substring(rp.indexOf("file:/") + 6);
			String fullPath = targetDir + path;
			File dir = new File(fullPath);
			if (!dir.exists()) {
				log.error(String.format("%s does not exist", fullPath));
			}
			ArrayList<String> ret = new ArrayList<String>();
			String[] tmp = dir.list();
			for (int i = 0; i < tmp.length; ++i) {
				File dirCheck = new File(targetDir + path + "/" + tmp[i]);
				if (dirCheck.isDirectory()) {
					ret.add(tmp[i] + "/");
				} else {
					ret.add(tmp[i]);
				}
			}
			dir.list();
			return ret;
		} else {
			// unzip
			return null;
		}
	}

	static public ArrayList<String> listResourceContents(String path) {
		return listInternalContents("/resource" + path);
	}

	public static void main(String[] args) throws ZipException, IOException {

		LoggingFactory.getInstance().configure();
		// LoggingFactory.getInstance().setLevel(Level.INFO);
		LoggingFactory.getInstance().setLevel(Level.INFO);

		ArrayList<String> files = listInternalContents("resource/images");
		for (int i = 0; i < files.size(); ++i) {
			log.info(files.get(i));
		}

		files = Zip.listDirectoryContents("myrobotlab.jar", "resource/images");
		for (int i = 0; i < files.size(); ++i) {
			log.info(files.get(i));
		}

		log.info("done");

	}

	public static byte[] getBytes(InputStream is) {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}

		return buffer.toByteArray();

	}

	public static File[] getPackageContent(String packageName) throws IOException {
		ArrayList<File> list = new ArrayList<File>();
		Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(packageName);
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			File dir = new File(url.getFile());
			for (File f : dir.listFiles()) {
				list.add(f);
			}
		}
		return list.toArray(new File[] {});
	}

}
