package org.myrobotlab.service;

import java.awt.image.BufferedImage;
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
import org.myrobotlab.image.IPCameraFrameGrabber;
import org.myrobotlab.image.SerializableImage;

public class IPCamera extends Service {

	private static final long serialVersionUID = 1L;

	public URL url = null;

	public String host = "";
	public String user = "";
	public String password = "";
	
	private IPCameraFrameGrabber grabber = null;
	private Thread videoProcess = null;
	
	private boolean capturing = false;

	public final static Logger LOG = Logger.getLogger(IPCamera.class.getCanonicalName());

	public IPCamera(String n) {
		super(n, IPCamera.class.getCanonicalName());
	}

	public class VideoProcess implements Runnable {		
		@Override
		public void run() {
			capturing = true;
			while (capturing)
			{
				//publishFrame(host, grabber.grabBufferedImage());
				invoke("publishFrame", new Object[]{host, grabber.grabBufferedImage()});
			}
		}
	}
	
	public final static SerializableImage publishFrame(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		si.source = source;
		return si;
	}

	public boolean attach (String host, String user, String password)
	{
		this.host = host;
		this.user = user;
		this.password = password;
		
		grabber = new IPCameraFrameGrabber(host, user, password);
		
		return true;
	}
	
	public String getStatus() {
		StringBuffer ret = new StringBuffer();		
		try {
			
			url = new URL("http://" + host + "/get_status.cgi??user=" + user
					+ "&pwd=" + password);
			URLConnection yc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					yc.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null)
			{
				ret.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			logException(e);
		}
		return ret.toString();
	}

	public void capture() {
		if (videoProcess != null)
		{
			capturing = false;
			videoProcess = null;
		}
		videoProcess = new Thread(new VideoProcess(),name + "_videoProcess");
		videoProcess.start();
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

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

}
