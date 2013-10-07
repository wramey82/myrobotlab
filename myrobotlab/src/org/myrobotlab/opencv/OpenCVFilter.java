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
	public boolean publishImage = false;
	//public boolean publishIplImage = false;
	
	int width;
	int height;
	int channels;
	int frameIndex;
	
	transient CvSize imageSize;

	public String sourceKey;

	transient VideoSources sources = new VideoSources();

	VideoProcessor vp;
	
	public OpenCVFilter()
	{
		this.name = this.getClass().getSimpleName().substring("OpenCVFilter".length());
	}
	
	public OpenCVFilter(String name)
	{
		this.name = name;
	}
	
	// TODO - refactor this back to single name constructor - the addFilter's new responsiblity it to 
	// check to see if inputkeys and other items are valid
	public OpenCVFilter(String filterName, String sourceKey) {	
		this.name = filterName;
		this.sourceKey = sourceKey;
	}

	public abstract IplImage process(IplImage image, OpenCVData data);
	public abstract BufferedImage display(IplImage image, OpenCVData data);
	public abstract void imageChanged(IplImage image);

	public VideoProcessor getVideoProcessor()
	{
		return vp;
	}
	
	public VideoSources getSources()
	{
		return sources;
	}
	
	public OpenCVFilter setState(OpenCVFilter other)
	{
		return (OpenCVFilter) Service.copyShallowFrom(this, other);
	}

	public IplImage preProcess(int frameIndex, IplImage frame, OpenCVData data) {
		data.setFilterName(String.format("%s.%s", vp.boundServiceName, this.name));
		this.frameIndex = frameIndex;
		
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
	
	/*
	
	public String publishFilterState(String state)
	{
		return publishFilterState(this.name, state, (Object[])null);
	}
	
	public String publishFilterState(String state, Object...data)
	{
		vp.getOpencv().invoke("publishFilterState", this.name, state, data);
		return state;
	}
	*/
	
	public void broadcastFilterState(){
		FilterWrapper fw = new FilterWrapper(this.name, this);
		vp.getOpencv().invoke("publishFilterState", fw);
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

	public IplImage prostProcess(IplImage image, OpenCVData data) {
		data.setWidth(image.width());
		data.setHeight(image.height());
		return image;
	}

}
