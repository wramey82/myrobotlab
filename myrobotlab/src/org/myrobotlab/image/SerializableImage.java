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
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class SerializableImage implements Serializable {

	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	private String source;
	private long timestamp;

	@Element
	private String id;

	/*
	public SerializableImage() {
	}

	public SerializableImage(BufferedImage im) {
		this.image = im;
	}
*/
	public SerializableImage(BufferedImage image, String source) {
		this.source = source;
		this.image = image;
		this.timestamp = System.currentTimeMillis();
		this.id = String.format("%s.%d", this.source, this.timestamp);
	}
	
	public BufferedImage getImage()
	{
		return image;
	}
	
	public int getHeight()
	{
		return image.getHeight();
	}
	
	public int getWidth()
	{
		return image.getWidth();
	}
	
	public String getSource()
	{
		return source;
	}
	
	public long getTimestamp()
	{
		return timestamp;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		ImageIO.write(image, "jpg", new MemoryCacheImageOutputStream(out));
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		image = (ImageIO.read(new MemoryCacheImageInputStream(in)));
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public void setImage(BufferedImage image) {
		this.image = image;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
}