package org.myrobotlab.opencv;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

import org.apache.log4j.Logger;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


public class ImageFileFrameGrabber extends FrameGrabber {

	public final static Logger log = Logger.getLogger(ImageFileFrameGrabber.class.getCanonicalName());
	
	private IplImage image;

	public ImageFileFrameGrabber(String filename) {
			image = cvLoadImage(filename);
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void trigger() throws Exception {
	}

	@Override
	public IplImage grab() {
		return image;
	}
	
	@Override
	public void release() throws Exception {
	}

}
