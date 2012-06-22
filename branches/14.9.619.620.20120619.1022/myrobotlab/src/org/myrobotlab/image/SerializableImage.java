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
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class SerializableImage implements Serializable {

	private static final long serialVersionUID = 1L;
	private BufferedImage image = null;
	public String source = "";
	public Date timestamp = new Date();

	public SerializableImage() {
		super();
	}

	public SerializableImage(BufferedImage im) {
		this();
		setImage(im);
	}

	public SerializableImage(BufferedImage im, String source) {
		this();
		this.source = source; 
		setImage(im);
	}
	
	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage img) {
		this.image = img;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		ImageIO.write(getImage(), "jpg", new MemoryCacheImageOutputStream(out));
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		setImage(ImageIO.read(new MemoryCacheImageInputStream(in)));
	}
}