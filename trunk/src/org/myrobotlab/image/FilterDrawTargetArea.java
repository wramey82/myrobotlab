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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.googlecode.javacv.jna.cxcore.IplImage;

import org.myrobotlab.framework.ConfigurationManager;

public class FilterDrawTargetArea {

	ConfigurationManager cfg;
	Rectangle target = null;
	PointReference[][] grid = null;
	ArrayList<Group> groupList = new ArrayList<Group>();
	int stepx = 0;
	int stepy = 0;
	int xTotal = 0;
	int yTotal = 0;

	public FilterDrawTargetArea(String CFGRoot, String name) {
		// super(CFGRoot, name);
	}

	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public void loadDefaultConfiguration() {
		this.cfg = cfg;
		// TODO Auto-generated method stub
		cfg.set("target.x", "120");
		cfg.set("target.y", "120");
		cfg.set("target.width", "40");
		cfg.set("target.height", "40");
		cfg.set("target.color", "40");

		cfg.set("step.x", "1");
		cfg.set("step.y", "1");

		target = new Rectangle(cfg.getInt("target.x"), cfg.getInt("target.x"),
				cfg.getInt("target.width"), cfg.getInt("target.height"));
		stepx = cfg.getInt("step.x");
		stepy = cfg.getInt("step.y");
		xTotal = target.width / stepx;
		yTotal = target.height / stepy;
		grid = new PointReference[xTotal][yTotal];

		// initialization
		for (int x = 0; x < xTotal; ++x) {
			for (int y = 0; y < yTotal; ++y) {
				grid[x][y] = new PointReference();
			}
		}

	}

	public class Group {
		public int number; // externally keyed or put into list
		public Color avgColor = null;
		// ArrayList Map or 2D Array of PointReferences or Points?
	}

	public class PointReference {
		public Group group = null;
	}

	public Object process(BufferedImage output, BufferedImage image) {
		// TODO pre-allocate in init or constructor (init?)?

		Graphics2D g = output.createGraphics();
		g.setColor(new Color(cfg.getInt("target.color")));
		g.fillRect(cfg.getInt("target.x"), cfg.getInt("target.y"), cfg
				.getInt("target.width"), cfg.getInt("target.height"));
		return null;
	}

	public void init() {
		// TODO Auto-generated method stub

	}

	public BufferedImage display(IplImage image, Object[] data) {
		// TODO Auto-generated method stub
		return null;
	}

	public IplImage process(IplImage image, Object[] data) {
		// TODO Auto-generated method stub
		return null;
	}

}
