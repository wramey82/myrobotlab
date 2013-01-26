package org.myrobotlab.service.data;


public class Point2Df {

	public long timestamp;

	public float x;
	public float y;
	public float value;

	public Point2Df() {
	}

	public Point2Df(float x, float y) {
		timestamp = System.currentTimeMillis();
		this.x = x;
		this.y = y;
	}

	public Point2Df(float x, float y, float value) {
		timestamp = System.currentTimeMillis();
		this.x = x;
		this.y = y;
		this.value = value;
	}

	public Point2Df(float x, float y, int value) {
		timestamp = System.currentTimeMillis();
		this.x = x;
		this.y = y;
		this.value = value;
	}
	
	public String toString() {
		return String.format("(%f,%f)", x, y);
	}

}
