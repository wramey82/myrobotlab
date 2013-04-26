package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.MemoryChangeListener;
import org.myrobotlab.memory.Node;
import org.slf4j.Logger;


public class VideoStreamer extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(VideoStreamer.class.getCanonicalName());

	public VideoStreamer(String n) {
		super(n, VideoStreamer.class.getCanonicalName());	
	}

	public static class VideoWebClient extends Thread
	{
		
	}
	
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}
	
	// add a stream - interfaces with a servic
	// which has publishDisplay
	public void addVideoStream(String name)
	{
		// subscribe to publishDisplay
		subscribe("publishDisplay", name, "publishDisplay", SerializableImage.class);
	}
	
	public void publishDisplay(SerializableImage img)
	{
		// add to hashed - blocking queue?
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		VideoStreamer template = new VideoStreamer("template");
		template.startService();
		
		Runtime.createAndStart("gui", "GUIService");

	}


}
