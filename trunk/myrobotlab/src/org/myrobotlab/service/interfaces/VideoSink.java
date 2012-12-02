package org.myrobotlab.service.interfaces;

import org.myrobotlab.image.SerializableImage;

public interface VideoSink {
	
	public void add(SerializableImage si);

}
