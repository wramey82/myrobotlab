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

import static com.googlecode.javacv.jna.cxcore.*;
import static com.googlecode.javacv.jna.cv.*;
import static com.googlecode.javacv.jna.highgui.*;
import static com.googlecode.javacv.jna.cvaux.*;

import org.apache.log4j.Logger;

import com.googlecode.javacv.jna.cv;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterTemplateMatch extends OpenCVFilter {

	public final static Logger LOG = Logger
			.getLogger(OpenCVFilterTemplateMatch.class.getCanonicalName());

	IplImage buffer = null;
	BufferedImage frameBuffer = null;
	IplImage templ = null;

	int i = 0;

	public OpenCVFilterTemplateMatch(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		return image.getBufferedImage();
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// Read in the template to be used for matching:
		templ = cvLoadImage("template.jpg");
	}

	@Override
	public IplImage process(IplImage image) {
		// cvMatchTemplate(iamge, arg1, arg2, arg3);

		// Read in the source image to be searched
		IplImage src = image;

		// Allocate Output Images:
		int iwidth = src.width - templ.width + 1;
		int iheight = src.height - templ.height + 1;
		for (i = 0; i < 6; ++i) {
			// ftmp[i]= cvCreateImage( cvSize( iwidth, iheight ), 32, 1 );
		}

		// CV_TM_CCOEFF_NORMED
		//cv.cvMatchTemplate(arg0, arg1, arg2, arg3);
		
		// Do the matching of the template with the image
		for (i = 0; i < 6; ++i) {
			// cvMatchTemplate( src, templ, ftmp[i], i );
			// cvNormalize( ftmp[i], ftmp[i], 1, 0, CV_MINMAX );
		}

		return image;

	}

}
