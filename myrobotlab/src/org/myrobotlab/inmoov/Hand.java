package org.myrobotlab.inmoov;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

public class Hand {
	private String side;

	public Servo thumb;
	public Servo index;
	public Servo majeure;
	public Servo ringFinger;
	public Servo pinky;
	public Servo wrist;

	public Hand() {
	}
	
	public void setInverted(boolean isInverted)
	{
		thumb.setInverted(isInverted);
		index.setInverted(isInverted);
		majeure.setInverted(isInverted);
		ringFinger.setInverted(isInverted);
		pinky.setInverted(isInverted);
		wrist.setInverted(isInverted);
	}

	public void moveTo(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
		this.thumb.moveTo(thumb);
		this.index.moveTo(index);
		this.majeure.moveTo(majeure);
		this.ringFinger.moveTo(ringFinger);
		this.pinky.moveTo(pinky);
		this.wrist.moveTo(wrist);
	}

	public void rest() {
		// initial positions
		setSpeed(1.0f,1.0f,1.0f,1.0f,1.0f,1.0f);
		
		thumb.moveTo(0);
		index.moveTo(0);
		majeure.moveTo(0);
		ringFinger.moveTo(0);
		pinky.moveTo(0);
		wrist.moveTo(90);
	}

	public void attach(Arduino arduino, String key) {
		// create finger and wrist servos
		side = key;
		thumb = (Servo) Runtime.createAndStart(String.format("thumb%s", key), "Servo");
		index = (Servo) Runtime.createAndStart(String.format("index%s", key), "Servo");
		majeure = (Servo) Runtime.createAndStart(String.format("majeure%s", key), "Servo");
		ringFinger = (Servo) Runtime.createAndStart(String.format("ringFinger%s", key), "Servo");
		pinky = (Servo) Runtime.createAndStart(String.format("pinky%s", key), "Servo");
		wrist = (Servo) Runtime.createAndStart(String.format("wrist%s", key), "Servo");

		Service.sleep(500);
		// attach the controller
		arduino.servoAttach(thumb.getName(), 2);
		arduino.servoAttach(index.getName(), 3);
		arduino.servoAttach(majeure.getName(), 4);
		arduino.servoAttach(ringFinger.getName(), 5);
		arduino.servoAttach(pinky.getName(), 6);
		arduino.servoAttach(wrist.getName(), 7);

		rest();

		broadcastState();
	}

	public void broadcastState() {
		// notify the gui
		thumb.broadcastState();
		index.broadcastState();
		majeure.broadcastState();
		ringFinger.broadcastState();
		pinky.broadcastState();
		wrist.broadcastState();
	}

	public void detach() {
		if (thumb != null) {
			thumb.detach();
		}
		if (index != null) {
			index.detach();
		}
		if (majeure != null) {
			majeure.detach();
		}
		if (ringFinger != null) {
			ringFinger.detach();
		}
		if (pinky != null) {
			pinky.detach();
		}
		if (wrist != null) {
			wrist.detach();
		}
	}

	public void release() {
		detach();

		if (thumb != null) {
			thumb.releaseService();
			thumb = null;
		}
		if (index != null) {
			index.releaseService();
			index = null;
		}
		if (majeure != null) {
			majeure.releaseService();
			majeure = null;
		}
		if (ringFinger != null) {
			ringFinger.releaseService();
			ringFinger = null;
		}
		if (pinky != null) {
			pinky.releaseService();
			pinky = null;
		}
		if (wrist != null) {
			wrist.releaseService();
			wrist = null;
		}
	}

	public void setSpeed(Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist) {
		this.thumb.setSpeed(thumb);
		this.index.setSpeed(index);
		this.majeure.setSpeed(majeure);
		this.ringFinger.setSpeed(ringFinger);
		this.pinky.setSpeed(pinky);
		this.wrist.setSpeed(wrist);
	}
	
	public boolean isValid()
	{
		thumb.moveTo(2);
		index.moveTo(2);
		majeure.moveTo(2);
		ringFinger.moveTo(2);
		pinky.moveTo(2);
		wrist.moveTo(92);	
		return true;
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveHand(\"%s\",%d,%d,%d,%d,%d,%d)\n", inMoovServiceName, side, thumb.getPosition(), index.getPosition(), majeure.getPosition(),
				ringFinger.getPosition(), pinky.getPosition(), wrist.getPosition());
	}
}
