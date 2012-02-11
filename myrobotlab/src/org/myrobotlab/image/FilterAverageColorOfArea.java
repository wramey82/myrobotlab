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

package org.myrobotlab.image;

import java.awt.image.BufferedImage;

import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class FilterAverageColorOfArea extends OpenCVFilter {

	public FilterAverageColorOfArea(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		cfg.set("target.x", "120");
		cfg.set("target.y", "120");
		cfg.set("target.width", "40");
		cfg.set("target.height", "40");
		cfg.set("density", "5");
	}

	@Override
	public IplImage process(IplImage image) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		// TODO Auto-generated method stub
		return null;
	}

}
