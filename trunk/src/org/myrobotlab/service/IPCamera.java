package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class IPCamera extends Service {

	private static final long serialVersionUID = 1L;
	
	public URL url = null;
	
	public String user = "";
	public String password = "";
	public String host = "";

	public final static Logger LOG = Logger.getLogger(IPCamera.class.getCanonicalName());

	public IPCamera(String n) {
		super(n, IPCamera.class.getCanonicalName());
	}
	
	
	public boolean getStatus ()
	{
		try {
			url = new URL("http://" + host + "/get_status.cgi??user=" +user+ "&pwd=" + password);
	        URLConnection yc = url.openConnection();
	        BufferedReader in = new BufferedReader(
	                                new InputStreamReader(
	                                yc.getInputStream()));
	        String inputLine;

	        while ((inputLine = in.readLine()) != null) 
	            System.out.println(inputLine);
	        in.close();
		} catch (Exception e) {
			logException(e);
			return false;
		}
		return true;
	}
	
	public boolean getVideo ()
	{
		try {
			url = new URL("http://" + host + "/videostream.cgi??user=" +user+ "&pwd=" + password);
	        URLConnection connection = url.openConnection();
	        	        
	        InputStream input = connection.getInputStream();
            byte[] buffer = new byte[4096];
            int n = - 1;

            String file = "out.jpg";
            OutputStream output = new FileOutputStream( file );
            while ( (n = input.read(buffer)) != -1)
            {
                    if (n > 0)
                    {
                            output.write(buffer, 0, n);
                    }
            }
            output.close();		} catch (Exception e) {
			logException(e);
			return false;
		}
		return true;
		
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		IPCamera foscam = new IPCamera("foscam");
		
		foscam.startService();
		foscam.getVideo();
		foscam.getStatus();
				
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
