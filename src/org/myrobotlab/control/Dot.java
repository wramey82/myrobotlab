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

package org.myrobotlab.control;

import java.awt.Color;

public class Dot {
	int angle, dist, age;

	public Dot(int newAngle, int newDist) {
		angle = newAngle;
		dist = newDist;
		age = 0;
	}

	public Color getShade() {
		return new Color(0, 255 - 3 * age, 0);
	}

	public boolean isOld() {
		age++;
		if (age > 80)
			return true;
		else
			return false;
	}

	public int getAngle() {
		return angle;
	}

	public int getDist() {
		return dist;
	}
}
