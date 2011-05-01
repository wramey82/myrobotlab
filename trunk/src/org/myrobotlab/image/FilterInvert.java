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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.googlecode.javacv.jna.cxcore.IplImage;

import org.myrobotlab.framework.ConfigurationManager;

public class FilterInvert {

	ConfigurationManager cfg;
	Rectangle target = new Rectangle();

	public FilterInvert(String CFGRoot, String name) {
		// super(CFGRoot, name);
	}

	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public void loadDefaultConfiguration() {

		cfg.set("target.x", "0");
		cfg.set("target.y", "0");
		cfg.set("target.width", "320");
		cfg.set("target.height", "240");

		cfg.set("step.x", "1");
		cfg.set("step.y", "1");

	}

	public Object process(BufferedImage output, BufferedImage image) {
		// TODO Auto-generated method stub

		target.x = cfg.getInt("target.x");
		target.y = cfg.getInt("target.y");
		target.width = cfg.getInt("target.width");
		target.height = cfg.getInt("target.height");
		int stepx = cfg.getInt("step.x");
		int stepy = cfg.getInt("step.y");

		for (int x = target.x; x < target.x + target.width; x += stepx) {
			for (int y = target.y; y < target.y + target.height; y += stepy) {
				// WritableRaster raster = image.getRaster();
				output.setRGB(x, y, ~image.getRGB(x, y));
			}

		}

		return null;
	}

	public IplImage process(IplImage image, Object[] data) {
		// TODO Auto-generated method stub
		return null;
	}

	public void init() {
		// TODO Auto-generated method stub

	}

	public BufferedImage display(IplImage image, Object[] data) {
		// TODO Auto-generated method stub
		return null;
	}

}
