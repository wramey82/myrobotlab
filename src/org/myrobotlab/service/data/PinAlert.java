package org.myrobotlab.service.data;

public class PinAlert {

	public static final int BOUNDRY = 1;
	public static final int STATE_LOW = 2;
	public static final int STATE_HIGH = 3;

	public String name;
	public int min;
	public int max;
	public int type; // ONESHOT (only) | MEAN ?
	public int state;
	public PinData pinData = new PinData();
	public int targetPin;
	public int threshold; // use this

	public PinAlert()
	{
		
	}
	
	public PinAlert(String n, int min, int max, int type, int state,
			int targetPin) {
		this.name = n;
		this.min = min;
		this.max = max;
		this.type = type;
		this.state = state;
		this.targetPin = targetPin;
	}

}
