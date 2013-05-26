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

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_video.BackgroundSubtractorMOG2;

public class OpenCVFilterBackgroundSubtractorMOG2 extends OpenCVFilter {

	private static final long serialVersionUID = 1L;
	
	public double learningRate = -1; // 0 trigger || -1 learn and fade

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterBackgroundSubtractorMOG2.class.getCanonicalName());

	transient BufferedImage frameBuffer = null;
	transient BackgroundSubtractorMOG2 mog;
	
	public void learn()
	{
		learningRate = -1;
	}
	
	public void search()
	{
		learningRate = 0;
	}
	
	public OpenCVFilterBackgroundSubtractorMOG2(VideoProcessor vp, String name, VideoSources source,  String sourceKey)  {
		super(vp, name, source, sourceKey);
	}
	
	@Override
	public BufferedImage display(IplImage image, OpenCVData data) {

		return foreground.getBufferedImage();
	}


	@Override
	public IplImage process(IplImage image, OpenCVData data) {
		mog.apply(image, foreground, learningRate); // 0 trigger || -1 learn and fade
		return foreground;
	}
	
	IplImage foreground;

	@Override
	public void imageChanged(IplImage image) {
		foreground =  IplImage.create(image.width(), image.height(),IPL_DEPTH_8U, 1);
		mog = new BackgroundSubtractorMOG2();
	}

}
