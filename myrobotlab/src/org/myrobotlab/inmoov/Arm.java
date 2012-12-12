package org.myrobotlab.inmoov;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

public class Arm {
	private String side;
	// public Hand hand;
	public Servo bicep;
	public Servo rotate;
	public Servo shoulder;
	public Servo omoplate;

	public Arm()
	{
	}
	
	public void rest() {
		// initial position
		bicep.moveTo(0);
		rotate.moveTo(90);
		shoulder.moveTo(30);
		omoplate.moveTo(10);
	}

	public void initialize(Arduino arduino, String key) {
		//name = String.format("%sArm", key);
		side = key;
		bicep = (Servo) Runtime.createAndStart(String.format("bicep%s", key), "Servo");
		rotate = (Servo) Runtime.createAndStart(String.format("rotate%s", key), "Servo");
		shoulder = (Servo) Runtime.createAndStart(String.format("shoulder%s", key), "Servo");
		omoplate = (Servo) Runtime.createAndStart(String.format("omoplate%s", key), "Servo");

		// attach to controller
		arduino.servoAttach(bicep.getName(), 8);
		arduino.servoAttach(rotate.getName(), 9);
		arduino.servoAttach(shoulder.getName(), 10);
		arduino.servoAttach(omoplate.getName(), 11);

		// servo limits
		bicep.setPositionMax(90);
		omoplate.setPositionMax(80);
		omoplate.setPositionMin(10);
		rotate.setPositionMin(40);

		rest();
		
		broadcastState();

	}

	public void broadcastState() {
		// notify the gui
		bicep.broadcastState();
		rotate.broadcastState();
		shoulder.broadcastState();
		omoplate.broadcastState();
	}

	public void detach() {
		if (bicep != null) {
			bicep.detach();
		}
		if (rotate != null) {
			rotate.detach();
		}
		if (shoulder != null) {
			shoulder.detach();
		}
		if (omoplate != null) {
			omoplate.detach();
		}
	}

	public void release() {
		detach();
		if (bicep != null) {
			bicep.releaseService();
			bicep = null;
		}
		if (rotate != null) {
			rotate.releaseService();
			rotate = null;
		}
		if (shoulder != null) {
			shoulder.releaseService();
			shoulder = null;
		}
		if (omoplate != null) {
			omoplate.releaseService();
			omoplate = null;
		}

	}
	
	public void setSpeed(Float bicep, Float rotate, Float shoulder, Float omoplate)
	{
		this.bicep.setSpeed(bicep);
		this.rotate.setSpeed(rotate);
		this.shoulder.setSpeed(shoulder);
		this.omoplate.setSpeed(omoplate);
	}
	
	public String getScript(String inMoovServiceName)
	{
		return String.format("%s.moveArm(\"%s\",%d,%d,%d,%d)\n", inMoovServiceName, side, bicep.getPosition(), rotate.getPosition(), shoulder.getPosition(), omoplate.getPosition());
	}

	public void moveTo(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		this.bicep.moveTo(bicep);
		this.rotate.moveTo(rotate);
		this.shoulder.moveTo(shoulder);
		this.omoplate.moveTo(omoplate);
		
	}
}
