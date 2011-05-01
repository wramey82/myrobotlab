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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;

// TODO - rename ColorUtils

public class Utils {

	/*
	 * Integer.toHexString( color.getRGB() & 0x00ffffff ) public String
	 * printPixelARGB(int pixel) { int alpha = (pixel >> 24) & 0xff; int red =
	 * (pixel >> 16) & 0xff; int green = (pixel >> 8) & 0xff; int blue = (pixel)
	 * & 0xff; System.out.println("argb: " + alpha + ", " + red + ", " + green +
	 * ", " + blue); }
	 */

	// static HashMap <int,>
	// array [r][g][b]
	// TODO - fix arrggh head hurts
	final static String[][][] colorNameCube = {
			{ { "black", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "navy", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "xxx", "xxx", "xxx" } },

			{ { "maroon", "xxx", "xxx" }, { "green", "xxx", "xxx" },
					{ "blue", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "xxx", "gray", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "xxx", "xxx", "xxx" } },

			{ { "red", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "lime", "y0", "z0" }, { "xxx", "xxx", "xxx" },
					{ "xxx", "xxx", "xxx" }, { "x0", "y0", "z0" },
					{ "xxx", "xxx", "xxx" }, { "xxx", "xxx", "xxx" },
					{ "x0", "y0", "white" } } };

	static public String getColorString(Color c) {
		// TODO - static this
		String[][][] colorDictionary = new String[3][3][3];

		colorDictionary[0][0][0] = "black";
		colorDictionary[1][1][1] = "gray";
		colorDictionary[2][2][2] = "white";

		colorDictionary[2][0][0] = "red";
		colorDictionary[0][2][0] = "lime";
		colorDictionary[0][0][2] = "blue";

		colorDictionary[0][2][2] = "aqua";
		colorDictionary[2][0][2] = "fushia";
		colorDictionary[2][2][0] = "yellow";

		colorDictionary[1][0][0] = "maroon";
		colorDictionary[0][1][0] = "green";
		colorDictionary[0][0][1] = "navy";

		colorDictionary[0][1][1] = "teal";
		colorDictionary[1][0][1] = "purple";
		colorDictionary[1][1][0] = "olive";

		colorDictionary[2][1][1] = "pink";
		colorDictionary[1][2][1] = "auquamarine";
		colorDictionary[1][1][2] = "sky blue";

		colorDictionary[1][2][2] = "pale blue";
		colorDictionary[2][1][2] = "plum";
		colorDictionary[2][2][1] = "apricot";

		colorDictionary[0][1][2] = "bondi blue";
		colorDictionary[1][0][2] = "amethyst";
		colorDictionary[1][2][0] = "brown";

		colorDictionary[2][1][0] = "persimmon";
		colorDictionary[2][0][1] = "rose";
		colorDictionary[0][2][1] = "persian green";

		// colorDictionary [1][2][0] = "lawn green";
		// colorDictionary [2][1][1] = "salmon";

		String ret = "";
		int red = c.getRed();
		int green = c.getGreen();
		int blue = c.getBlue();

		// 63 < divisor < 85
		red = red / 64 - 1;
		green = green / 64 - 1;
		blue = blue / 64 - 1;

		if (red < 1)
			red = 0;
		if (green < 1)
			green = 0;
		if (blue < 1)
			blue = 0;

		ret = colorDictionary[red][green][blue];

		return ret;
	}

	public Color getColor(String colorName) {
		try {
			// Find the field and value of colorName
			Field field = Class.forName("java.awt.Color").getField(colorName);
			return (Color) field.get(null);
		} catch (Exception e) {
			return null;
		}
	}

	final public static int[] RGB2HSV(Color c, int hsv[]) {
		return RGB2HSV(c.getRed(), c.getGreen(), c.getBlue(), hsv);
	}
	
	// TODO - depricate
	// When programming in Java, use the RGBtoHSB and HSBtoRGB  functions from the java.awt.Color class.
	// http://cs.haifa.ac.il/hagit/courses/ist/Lectures/Demos/ColorApplet2/t_convert.html#RGB to HSV & HSV to RGB
	final public static int[] RGB2HSV(int r, int g, int b, int hsv[]) {
			
			int min;    //Min. value of RGB
			int max;    //Max. value of RGB
			int delMax; //Delta RGB value
			
			if (r > g) { min = g; max = r; }
			else { min = r; max = g; }
			if (b > max) max = b;
			if (b < min) min = b;
									
			delMax = max - min;
		 
			float H = 0, S;
			float V = max;
			   
			if ( delMax == 0 ) { H = 0; S = 0; }
			else {                                   
				S = delMax/255f;
				if ( r == max ) 
					H = (      (g - b)/(float)delMax)*60;
				else if ( g == max ) 
					H = ( 2 +  (b - r)/(float)delMax)*60;
				else if ( b == max ) 
					H = ( 4 +  (r - g)/(float)delMax)*60;   
			}
									 
			hsv[0] = (int)(H);
			hsv[1] = (int)(S*100);
			hsv[2] = (int)(V*100);
			return hsv;
		}	

	public final static void saveBufferedImage(BufferedImage newImg,
			String filename) {
		saveBufferedImage(newImg, filename, null);
	}

	public final static void saveBufferedImage(BufferedImage newImg,
			String filename, String format) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(newImg, "jpg", baos);
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(baos.toByteArray());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Produces a copy of the supplied image
	 * 
	 * @param image
	 *            The original image
	 * @return The new BufferedImage
	 */
	public static BufferedImage copyImage(BufferedImage image) {
		return scaledImage(image, image.getWidth(), image.getHeight());
	}

	/**
	 * Produces a resized image that is of the given dimensions
	 * 
	 * @param image
	 *            The original image
	 * @param width
	 *            The desired width
	 * @param height
	 *            The desired height
	 * @return The new BufferedImage
	 */
	public static BufferedImage scaledImage(BufferedImage image, int width,
			int height) {
		BufferedImage newImage = createCompatibleImage(width, height);
		Graphics graphics = newImage.createGraphics();

		graphics.drawImage(image, 0, 0, width, height, null);

		graphics.dispose();
		return newImage;
	}

	/**
	 * Creates an image compatible with the current display
	 * 
	 * @return A BufferedImage with the appropriate color model
	 */
	public static BufferedImage createCompatibleImage(int width, int height) {
		GraphicsConfiguration configuration = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();
		return configuration.createCompatibleImage(width, height,
				Transparency.TRANSLUCENT);
	}

}
