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

	//public URL url = null;

	public String host = "";
	public String user = "";
	public String password = "";

	private IPCameraFrameGrabber grabber = null;
	private Thread videoProcess = null;

	private boolean capturing = false;

	public final static Logger LOG = Logger.getLogger(IPCamera.class.getCanonicalName());

	public final static int FOSCAM_MOVE_UP 						= 0;
	public final static int FOSCAM_MOVE_STOP_UP 				= 1;
	public final static int FOSCAM_MOVE_DOWN 					= 2;
	public final static int FOSCAM_MOVE_STOP_DOWN 				= 3;
	public final static int FOSCAM_MOVE_LEFT 					= 4;
	public final static int FOSCAM_MOVE_STOP_LEFT 				= 5;
	public final static int FOSCAM_MOVE_RIGHT 					= 6;
	public final static int FOSCAM_MOVE_STOP_RIGHT 				= 7;
	public final static int FOSCAM_MOVE_CENTER 					= 25;
	public final static int FOSCAM_MOVE_VERTICLE_PATROL 		= 26;
	public final static int FOSCAM_MOVE_STOP_VERTICLE_PATROL 	= 27;
	public final static int FOSCAM_MOVE_HORIZONTAL_PATROL 		= 28;
	public final static int FOSCAM_MOVE_STOP_HORIZONTAL_PATROL 	= 29;
	public final static int FOSCAM_MOVE_IO_OUTPUT_HIGH 			= 94;
	public final static int FOSCAM_MOVE_IO_OUTPUT_LOW 			= 95;	
	
	public IPCamera(String n) {
		super(n, IPCamera.class.getCanonicalName());
	}

	public class VideoProcess implements Runnable {
		@Override
		public void run() {
			try {
				grabber.start();
				capturing = true;
				while (capturing) {
					BufferedImage bi = grabber.grabBufferedImage();
					if (bi != null){
						invoke("publishFrame", new Object[] { host,  bi });
					}
				}
			} catch (Exception e) {

				logException(e);
			}
		}
	}

	public final static SerializableImage publishFrame(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		si.source = source;
		return si;
	}

	public boolean attach(String host, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;

		grabber = new IPCameraFrameGrabber(host, user, password);

		return true;
	}

	
	public String move(int param)
	{
		StringBuffer ret = new StringBuffer();
		try {

			URL url = new URL("http://" + host + "/decoder_control.cgi?command="+param+"user=" + user+ "&pwd=" + password);
			URLConnection con = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				ret.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			logException(e);
		}
		return ret.toString();		
	}
	public final static int FOSCAM_ALARM_MOTION_ARMED_DISABLED 			= 0;
	public final static int FOSCAM_ALARM_MOTION_ARMED_ENABLED 			= 1	;
	public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_HIGH 		= 0;
	public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_MEDIUM 		= 1;
	public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_LOW 		= 2;
	public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_ULTRALOW	= 3;
	public final static int FOSCAM_ALARM_INPUT_ARMED_DISABLED 		= 0;
	public final static int FOSCAM_ALARM_INPUT_ARMED_ENABLED	 	= 1;
	public final static int FOSCAM_ALARM_MAIL_DISABLED	 	= 0;
	public final static int FOSCAM_ALARM_MAIL_ENABLED	 	= 1;

	public String setAlarm(int armed, int sensitivity, int inputArmed, int ioLinkage, int mail, int uploadInterval)
	{
		StringBuffer ret = new StringBuffer();
		try {

			URL url = new URL("http://" + host + "/set_alarm.cgi?motion_armed="+armed+
					"user=" + user+ "&pwd=" + password);
			URLConnection con = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				ret.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			logException(e);
		}
		return ret.toString();		
	}
		
	public String getStatus() {
		StringBuffer ret = new StringBuffer();
		try {

			URL url = new URL("http://" + host + "/get_status.cgi?user=" + user+ "&pwd=" + password);
			URLConnection con = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				ret.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			logException(e);
		}
		return ret.toString();
	}

	public void capture() {
		if (videoProcess != null) {
			capturing = false;
			videoProcess = null;
		}
		videoProcess = new Thread(new VideoProcess(), name + "_videoProcess");
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

		//foscam.attach("192.168.0.59", "", "");
		//foscam.capture();

		foscam.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		
	}

}
