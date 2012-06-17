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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class FileIO {

	public final static Logger log = Logger.getLogger(FileIO.class.getCanonicalName());

	public final static String fileToString(File file) {
		String result = null;
		DataInputStream in = null;

		try {
			//File f = new File(filename);
			byte[] buffer = new byte[(int) file.length()];
			in = new DataInputStream(new FileInputStream(file));
			in.readFully(buffer);
			result = new String(buffer);
		} catch (IOException e) {
			log.error("could not open filename " + file.getName());
		} finally {
			try {
				if (in != null)
				{
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

	public static void stringToFile(String filename, String data)
	{
		stringToFile(filename, data, null);
	}
	
	public static void stringToFile(String filename, String data, String encoding) {
		Writer out = null;
		try {
			if (encoding != null)
			{
				out = new OutputStreamWriter(new FileOutputStream(filename), encoding);
			} else {
				out = new OutputStreamWriter(new FileOutputStream(filename));				
			}
			out.write(data);
		} catch (Exception e) {
			log.error(Service.stackToString(e));
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				log.error(Service.stackToString(e));
			}
		}
	}

	// TODO - NOT IMPLMENENTED 
	public final static String getResourceBinary(String filename) {
		InputStream isr = FileIO.class.getResourceAsStream("/resource/" + filename);
		if (isr == null)
		{
			log.error("could not locate resource " + filename);
		}
	    DataInputStream input = new DataInputStream (isr);
	    String stringData = null;

	    byte b[] = new byte[1024];
	    try
	    {
	      while (true)
	      {
	    	input.readFully(b);
	        stringData = input.readUTF();
	      }
	    }
	    catch (EOFException e)
	    {
	      // Do nothing if it is the end of file.
	    }
	    catch (Exception e)
	    {
	    	Service.logException(e);
	    }
	    finally
	    {
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
		try {
			InputStream is = FileIO.class.getResourceAsStream(String.format("/resource/%1$s", filename));
			
			if (is == null)
			{
				log.error(String.format("resource %1$s not found", filename));
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
			log.error(String.format("could not open filename /resource/%1$s", filename));
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {}
			}
		}

		return str.toString();
	}

	
	public final static boolean writeBinary (String filename, Object toSave)
	{
		   try{
			      //use buffering
			      OutputStream file = new FileOutputStream(filename);
			      OutputStream buffer = new BufferedOutputStream( file );
			      ObjectOutput output = new ObjectOutputStream( buffer );
			      try{
			        output.writeObject(toSave);
			      }
			      finally{
			        output.close();
			      }
			    }  
			    catch(IOException e){
			    	Service.logException(e);
			    	return false;
			    }
			    return true;
	}
	
	
	public final static Object readBinary (String filename)
	{
	    try{
	      InputStream file = new FileInputStream(filename);
	      InputStream buffer = new BufferedInputStream( file );
	      ObjectInput input = new ObjectInputStream ( buffer );
	      try{
	        return (Object)input.readObject();
	      }
	      finally{
	        input.close();
	      }
	    }
	    catch(Exception e){
	    	Service.logException(e);
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
	
	static public void unzip(String zipFile, String newPath) throws ZipException, IOException 
	{
	    System.out.println(zipFile);
	    int BUFFER = 2048;
	    File file = new File(zipFile);

	    ZipFile zip = new ZipFile(file);
	    //String newPath = zipFile.substring(0, zipFile.length() - 4);

	    new File(newPath).mkdir();
	    Enumeration zipFileEntries = zip.entries();

	    // Process each entry
	    while (zipFileEntries.hasMoreElements())
	    {
	        // grab a zip file entry
	        ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
	        String currentEntry = entry.getName();
	        File destFile = new File(newPath, currentEntry);
	        //destFile = new File(newPath, destFile.getName());
	        File destinationParent = destFile.getParentFile();

	        // create the parent directory structure if needed
	        destinationParent.mkdirs();

	        if (!entry.isDirectory())
	        {
	            BufferedInputStream is = new BufferedInputStream(zip
	            .getInputStream(entry));
	            int currentByte;
	            // establish buffer for writing file
	            byte data[] = new byte[BUFFER];

	            // write the current file to disk
	            FileOutputStream fos = new FileOutputStream(destFile);
	            BufferedOutputStream dest = new BufferedOutputStream(fos,
	            BUFFER);

	            // read and write until last byte is encountered
	            while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
	                dest.write(data, 0, currentByte);
	            }
	            dest.flush();
	            dest.close();
	            is.close();
	        }
	        else{
	            destFile.mkdirs();
	        }
	        if (currentEntry.endsWith(".zip"))
	        {
	            // found a zip file, try to open
	            unzip(destFile.getAbsolutePath(), "./");
	        }
	    }
	}
		
}



