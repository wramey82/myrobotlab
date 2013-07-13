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
	// ------------- added pins and the defaults
	public int thumbPin=2;
	public int indexPin=3;
	public int majeurePin=4;
	public int ringFingerPin=5;
	public int pinkyPin=6;
	public int wristPin=7;

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
		if (wrist != null)this.wrist.moveTo(wrist);
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
	// ------------- added set pins
	public void setpins(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist){
		 thumbPin=thumb;
		 indexPin=index;
		 majeurePin=majeure;
		 ringFingerPin=ringFinger;
		 pinkyPin=pinky;
		 wristPin=wrist;
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
		// ------------- changed to used set pins
		arduino.servoAttach(thumb.getName(), thumbPin);
		arduino.servoAttach(index.getName(), indexPin);
		arduino.servoAttach(majeure.getName(), majeurePin);
		arduino.servoAttach(ringFinger.getName(), ringFingerPin);
		arduino.servoAttach(pinky.getName(), pinkyPin);
		arduino.servoAttach(wrist.getName(), wristPin);

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
