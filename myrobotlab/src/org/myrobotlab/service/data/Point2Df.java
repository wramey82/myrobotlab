package org.myrobotlab.service.data;

import java.util.Date;

public class Point2Df {

	public long timestamp;

	public float x;
	public float y;

	public Point2Df() {
	}

	public Point2Df(float x, float y) {
		timestamp = System.currentTimeMillis();
		this.x = x;
		this.y = y;
	}

	public String toString() {
		return String.format("(%f,%f)", x, y);
	}

}
