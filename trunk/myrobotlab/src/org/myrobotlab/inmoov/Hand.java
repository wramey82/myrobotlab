package org.myrobotlab.inmoov;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

public class Hand {
	private String side;

	private InMoov inmoov;
	
	public Servo thumb;
	public Servo index;
	public Servo majeure;
	public Servo ringFinger;
	public Servo pinky;
	public Servo wrist;

	public Hand() {
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
	/*
	public void setpins(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist){
		 thumbPin=thumb;
		 indexPin=index;
		 majeurePin=majeure;
		 ringFingerPin=ringFinger;
		 pinkyPin=pinky;
		 wristPin=wrist;
	}
	*/
	
	public Hand startHand(InMoov inmoov, String port, String key, int thumb, int index, int majeure, int ringFinger, int pinky, int wrist) {
		Arduino arduino = inmoov.getArduino(port);
		
		if (arduino == null || !arduino.isConnected())
		{
			inmoov.error("%s is invalid could not start %s hand", port, key);
			return null;
		}

		this.thumb = (Servo) Runtime.startReserved(String.format("thumb%s", key));
		this.index = (Servo) Runtime.startReserved(String.format("index%s", key));
		this.majeure = (Servo) Runtime.startReserved(String.format("majeure%s", key));
		this.ringFinger = (Servo) Runtime.startReserved(String.format("ringFinger%s", key));
		this.pinky = (Servo) Runtime.startReserved(String.format("pinky%s", key));
		this.wrist = (Servo) Runtime.startReserved(String.format("wrist%s", key));

		this.thumb.setPin(thumb);
		this.index.setPin(index);
		this.majeure.setPin(majeure);
		this.ringFinger.setPin(ringFinger);
		this.pinky.setPin(pinky); 
		this.wrist.setPin(wrist);

		
		// attach the controller
		// ------------- changed to used set pins
		arduino.servoAttach(this.thumb.getName());
		arduino.servoAttach(this.index.getName());
		arduino.servoAttach(this.majeure.getName());
		arduino.servoAttach(this.ringFinger.getName());
		arduino.servoAttach(this.pinky.getName());
		arduino.servoAttach(this.wrist.getName());

		rest();

		broadcastState();
		return this;
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
