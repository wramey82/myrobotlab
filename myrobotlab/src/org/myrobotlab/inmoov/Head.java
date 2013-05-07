package org.myrobotlab.inmoov;

import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Tracking;

public class Head {

	/*
	public Servo neck;
	public Servo rothead;
	public Arduino headArduino;
	public Tracking tracking;
	*/
	private InMoov inmoov;
	public boolean allowMove = true;

	public void initialize(InMoov inmoov) {
		this.inmoov = inmoov;
		inmoov.neck = (Servo) Runtime.createAndStart("neck", "Servo");
		inmoov.rothead = (Servo) Runtime.createAndStart("rothead", "Servo");

		inmoov.arduinoHead.servoAttach(inmoov.neck.getName(), 12);
		inmoov.arduinoHead.servoAttach(inmoov.rothead.getName(), 13);

		// initial position
		rest();

		// notify gui
		inmoov.neck.broadcastState();
		inmoov.rothead.broadcastState();
	
		inmoov.tracking = (Tracking) Runtime.create("tracking", "Tracking");
		// FIXME - make better
		inmoov.tracking.xName = "rothead";
		inmoov.tracking.yName = "neck";
		inmoov.tracking.opencvName = "eye";
		
		inmoov.tracking.startService();
		
		
	}

	public void move(Integer neck, Integer rothead) {
		if (!allowMove)
		{
			return;
		}
		inmoov.neck.moveTo(neck);
		inmoov.rothead.moveTo(rothead);
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveHead(%d,%d)\n", inMoovServiceName, inmoov.neck.getPosition(), inmoov.rothead.getPosition());
	}

	public void setSpeed(Float neck2, Float rothead2) {
		inmoov.neck.setSpeed(neck2);
		inmoov.rothead.setSpeed(rothead2);
	}

	public void rest() {
		inmoov.neck.moveTo(90);
		inmoov.rothead.moveTo(90);
	}

	public void broadcastState() {
		inmoov.neck.broadcastState();
		inmoov.rothead.broadcastState();
	}
}
