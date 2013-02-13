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
import java.util.HashMap;

import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;

import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


public abstract class OpenCVFilter implements Serializable {

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilter.class.toString());

	private static final long serialVersionUID = 1L;
	protected ConfigurationManager cfg = null; // TODO - remove
	final public String name;
	OpenCV myService = null; // FIXME - How does this work on remote ?!?!? - is a zombie method .send called ???
	HashMap<String, Object> storage = null;
	
	public boolean publish = false;
	public boolean publishOpenCVObjects = false;
	public boolean useFloatValues = true;
	
	int width;
	int height;
	int channels;
	CvSize imageSize;

	final static public String INPUT_IMAGE_NAME = "inputImageName";
	final static public String OUTPUT_IMAGE_NAME = "outputImageName";
	final static public String USE_INPUT_IMAGE_NAME = "useInputImageName";
	final static public String USE_OUTPUT_IMAGE_NAME = "useOutputImageName";
	final static public String ROI_NAME = "roiName";
	final static public String USE_ROI = "useROI";

	static HashMap<String, Object> globalData = new HashMap<String, Object>();

	public static final String FILTER_CFG_ROOT = "displayFilter/filter/";
	public final String FILTER_INSTANCE_CFG_ROOT;

	public OpenCVFilter(OpenCV service, String filterName) {
		this.name = filterName;
		this.myService = service;
		this.storage = myService.storage;
		this.FILTER_INSTANCE_CFG_ROOT = myService.getCFG().getServiceRoot() + "/" + FILTER_CFG_ROOT + name;
		cfg = new ConfigurationManager(FILTER_INSTANCE_CFG_ROOT);
		cfg.set(INPUT_IMAGE_NAME, "output");
		cfg.set(OUTPUT_IMAGE_NAME, "output");
		cfg.set(USE_INPUT_IMAGE_NAME, false);
		cfg.set(USE_OUTPUT_IMAGE_NAME, false);
		cfg.set(ROI_NAME, "roi");
		cfg.set(USE_ROI, false);
	}

	// storage accessors begin
	public IplImage getIplImage(String name) {
		if (storage.containsKey(name)) {
			return (IplImage) storage.get(name);
		}
		log.error("request for " + name + " IplImage in storage - not found");
		return null;
	}

	// storage accessors end

	public abstract IplImage process(IplImage image);

	public abstract BufferedImage display(IplImage image, Object[] data);
	
	// daBomb
	public OpenCVFilter setState(OpenCVFilter other)
	{
		return (OpenCVFilter) Service.copyShallowFrom(this, other);
	}

	public IplImage preProcess(IplImage frame) {
		// TODO size or re-init based on change of channel or size - lastWidth lastHeight
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
	
	
	public abstract void imageChanged(IplImage frame);

	
	/**
	 *  release memory or other resources in filter - this method is called
	 *  when the filter is removed
	 */
	public void release()
	{
		
	}
	// FIXME - dispose of filter removal

}
