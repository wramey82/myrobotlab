/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


public abstract class OpenCVFilter implements Serializable {

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilter.class.toString());

	private static final long serialVersionUID = 1L;
	final public String name;

	public boolean publish = false;
	public boolean publishOpenCVObjects = false;
	public boolean useFloatValues = true;
	
	public boolean publishDisplay = false;
	public boolean publishData = true;
	public boolean publishImage = true;
	public boolean publishIplImage = false;
	
	int width;
	int height;
	int channels;
	CvSize imageSize;

	public String sourceKey;
	HashMap<String, IplImage> sources;
	VideoProcessor vp;
	
	public OpenCVFilter(VideoProcessor vp, String filterName, HashMap<String, IplImage> sources, String sourceKey) {
		this.name = filterName;
		this.vp = vp;
		this.sources = sources;
		this.sourceKey = sourceKey;
	}

	public abstract IplImage process(IplImage image, OpenCVData data);
	public abstract BufferedImage display(IplImage image);
	public abstract void imageChanged(IplImage image);

	public VideoProcessor getVideoProcessor()
	{
		return vp;
	}
	
	public HashMap<String, IplImage> getSources()
	{
		return sources;
	}
	
	public OpenCVFilter setState(OpenCVFilter other)
	{
		return (OpenCVFilter) Service.copyShallowFrom(this, other);
	}

	public IplImage preProcess(IplImage frame, OpenCVData data) {
		data.setFilterName(this.name);
		if (frame.width() != width || frame.nChannels() != channels)
		{
			width = frame.width();
			channels = frame.nChannels();
			height = frame.height();
			imageSize = cvGetSize(frame);
			imageChanged(frame);
		}
		return frame;
	}
	
	
	public void invoke(String method, Object...params)
	{
		vp.getOpencv().invoke(method, params);
	}
	
	
	public ArrayList<String> getPossibleSources()
	{
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(name);
		return ret;
	}
	
	public void release()
	{
	}

}
