package org.myrobotlab.inmoov;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

public class Head {

	public Servo neck;
	public Servo rothead;

	public void initialize(Arduino arduino) {
		neck = (Servo) Runtime.createAndStart("neck", "Servo");
		rothead = (Servo) Runtime.createAndStart("rothead", "Servo");

		arduino.servoAttach(neck.getName(), 12);
		arduino.servoAttach(rothead.getName(), 13);

		// initial position
		rest();

		// notify gui
		neck.broadcastState();
		rothead.broadcastState();
	}

	public void move(Integer neck, Integer rothead) {
		this.neck.moveTo(neck);
		this.rothead.moveTo(rothead);
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveHead(%d,%d)\n", inMoovServiceName, neck.getPosition(), rothead.getPosition());
	}

	public void setSpeed(Float neck2, Float rothead2) {
		this.neck.setSpeed(neck2);
		this.rothead.setSpeed(rothead2);
	}

	public void rest() {
		neck.moveTo(90);
		rothead.moveTo(90);
	}

	public void broadcastState() {
		neck.broadcastState();
		rothead.broadcastState();
	}
}
