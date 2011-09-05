package org.myrobotlab.memory;

import java.io.Serializable;
import java.util.Date;

import org.myrobotlab.image.SerializableImage;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VisualData implements Serializable{

	private static final long serialVersionUID = 1L;
	public int ID = 0;
	public Date timestamp = new Date();
	public SerializableImage cameraFrame = null;
	public SerializableImage template = null;
	public transient IplImage cvTemplate = null; // TODO - make Java only object - remove
	public transient IplImage cvCameraFrame = null; // TODO - make Java only object - remove
	public transient IplImage cvGrayFrame = null; // TODO - make Java only object - remove
	public String imageFilePath = null;
}
