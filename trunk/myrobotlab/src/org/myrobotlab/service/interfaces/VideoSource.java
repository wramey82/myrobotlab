package org.myrobotlab.service.interfaces;

public interface VideoSource {
	
	public void add(VideoSink vs);
	public void remove(VideoSink vs);

}
