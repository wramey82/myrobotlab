package org.myrobotlab.service;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.MjpegServer;
import org.myrobotlab.service.interfaces.VideoSink;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Refeences of cool code snippets etc :
 *         
 *         http://www.java2s.com/Code/Java/Network-Protocol/
 *         AsimpletinynicelyembeddableHTTP10serverinJava.htm
 *         
 *         and most importantly Wireshark !!!  cuz it ROCKS for getting the truth !!!
 *         
 *         
 * 
 */


public class VideoStreamer extends VideoSink {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(VideoStreamer.class.getCanonicalName());

	public int listeningPort = 9090;
	transient private MjpegServer server;
	public boolean mergeSteams = true;

	public VideoStreamer(String name) {
		super(name);
	}
	
	public void startService()
	{
		super.startService();
		start();
	}
	

	
	public void setPort(int port)
	{
		listeningPort = port;
	}
	
	public void start()
	{
		start(listeningPort);
	}

	public void start(int port) {
		stop();
		listeningPort = port;
		try {
			server = new MjpegServer(listeningPort);
			server.start();
		} catch (IOException e) {
			Logging.logException(e);
		}
	}

	public void stop() {
		if (server != null)
		{
			server.stop();
		}
		server = null;
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override
	public void stopService() {
		super.stopService();
		stop();
	}

	@Override
	public void releaseService() {
		super.releaseService();
	}

	public void publishDisplay(SerializableImage img) {
		
		if (mergeSteams)
		{
			img.setSource("output");
		}
		
		if (!server.videoFeeds.containsKey(img.getSource())) {
			server.videoFeeds.put(img.getSource(), (BlockingQueue<SerializableImage>) new LinkedBlockingQueue<SerializableImage>());
		}

		BlockingQueue<SerializableImage> buffer = server.videoFeeds.get(img.getSource());
		// if its backed up over 10 frames we are dumping it
		if (buffer.size() < 10) {
			buffer.add(img);
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		VideoStreamer streamer = (VideoStreamer)Runtime.createAndStart("streamer", "VideoStreamer");
		OpenCV opencv = (OpenCV) Runtime.createAndStart("opencv", "OpenCV");

		//streamer.start();
		streamer.attach(opencv);

		opencv.addFilter("pyramidDown", "PyramidDown");
		opencv.capture();

		Runtime.createAndStart("gui", "GUIService");

	}

}
