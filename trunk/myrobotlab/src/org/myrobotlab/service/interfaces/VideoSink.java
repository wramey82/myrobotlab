package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;

public abstract class VideoSink extends Service {

	private static final long serialVersionUID = 1L;

	public VideoSink(String name) {
		super(name, VideoSink.class.getCanonicalName());
	}

	public boolean attach(VideoSource vs) {
		subscribe("publishDisplay", vs.getName(), "publishDisplay", SerializableImage.class);
		return true;
	}

	public boolean detach(VideoSource vs) {
		unsubscribe("publishDisplay", vs.getName(), "publishDisplay", SerializableImage.class);
		return true;
	}
	
	public abstract void publishDisplay(SerializableImage img);
}
