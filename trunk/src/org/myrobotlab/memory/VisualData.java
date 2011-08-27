package org.myrobotlab.memory;

import java.io.Serializable;

import org.myrobotlab.image.SerializableImage;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class VisualData implements Serializable{

	private static final long serialVersionUID = 1L;
	public int ID = 0;
	public transient IplImage image = null; // TODO - make Java only object - remove
	public SerializableImage bi = null;
	public String imageFilePath = null;
}
