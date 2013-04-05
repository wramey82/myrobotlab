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

import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvZero;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterAddMask extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterAddMask.class.getCanonicalName());
	public String sourceName;

	transient IplImage dst = null;
	transient IplImage negativeImage = null;

	public OpenCVFilterAddMask(VideoProcessor vp, String name, HashMap<String, IplImage> source, String sourceKey) {
		super(vp, name, source, sourceKey);
	}

	@Override
	public BufferedImage display(IplImage image, OpenCVData data) {

		return image.getBufferedImage(); // TODO - ran out of memory here
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		// cvAnd (src1, src2, dst, mask)
		// f'ing rocks ! -
		// http://www.neuroforge.co.uk/index.php/masking-colour-images
		if (sourceName != null) {
			IplImage src = sources.get(sourceName);
			if (src != null) {
				if (dst == null) {
					dst = src.clone();
				}
				cvZero(dst);
				cvCopy(src, dst, image);
			}
			return dst;
		} else {
			return image;
		}

	}

	@Override
	public void imageChanged(IplImage image) {
		dst = null;
	}

}
