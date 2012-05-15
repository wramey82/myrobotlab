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

//import javax.swing.ImageIcon;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class FileIO {

	public final static Logger LOG = Logger.getLogger(FileIO.class.getCanonicalName());

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
			LOG.error("could not open filename " + file.getName());
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
			LOG.error(Service.stackToString(e));
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				LOG.error(Service.stackToString(e));
			}
		}
	}

	// TODO - NOT IMPLMENENTED 
	public final static String getResourceBinary(String filename) {
		InputStream isr = FileIO.class.getResourceAsStream("/resource/" + filename);
		if (isr == null)
		{
			LOG.error("could not locate resource " + filename);
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
	
	
	public final static String getResourceFile(String filename) {
		StringBuffer str = null;
		
		try {

			InputStream is = FileIO.class.getResourceAsStream("/resource/" + filename);
			
			if (is == null)
			{
				LOG.error("resource " + filename + " not found");
				return null;
			}
				
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			String s;
			str = new StringBuffer();
			while ((s = br.readLine()) != null) {
				str.append(s);
				str.append("\n");
			}
		} catch (IOException e) {
			LOG.error("could not open filename /resource/" + filename);
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
		
}



