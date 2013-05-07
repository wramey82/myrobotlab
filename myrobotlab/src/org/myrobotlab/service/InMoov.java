package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.inmoov.Arm;
import org.myrobotlab.inmoov.Hand;
import org.myrobotlab.inmoov.Head;
import org.myrobotlab.service.interfaces.ServiceInterface;

public class InMoov extends Service {

	// TODO - normalize bi-lateral code parts

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoov.class.getCanonicalName());

	String bodyPartContext = null;

	public static final String left = "left";
	public static final String right = "right";
	public static final String both = "both";

	Head head;

	// head
	public Sphinx ear;
	public Speech mouth;
	public OpenCV eye;
	public Python python;
	public Tracking tracking;
	public Arduino arduinoHead;
	
	public Servo rothead;
	public Servo neck;
	
	// left side
	public Arduino arduinoLeft;
	public Hand handLeft;
	public Arm armLeft;
	
	// right side
	public Arduino arduinoRight;
	public Hand handRight;
	public Arm armRight;
	
	public InMoov(String n) {
		super(n, InMoov.class.getCanonicalName());
	}
	
	public void createAndStartSubServices()
	{
		ear = (Sphinx) Runtime.createAndStart("ear", "Sphinx");
		mouth = (Speech) Runtime.createAndStart("mouth", "Speech");
		eye = (OpenCV) Runtime.createAndStart("eye", "OpenCV");
		python = (Python) Runtime.createAndStart("python", "Python");
		
	}

	// ----------- normalization begin ---------------------

	public Arduino getArduino(String key) {
		if (key.equals(left))
		{
			return arduinoLeft;
		} else if (key.equals(right))
		{
			return arduinoRight;
		} 
		setError("getArduino ({}) not found");
		return null;
	}
	
	public void setArduino(String key, Arduino arduino) {
		if (key.equals(left))
		{
			arduinoLeft = arduino;
		} else if (key.equals(right))
		{
			arduinoRight = arduino;
		} 
		log.error("setArduino ({}, Arduino) not found");
	}

	// uno | atmega168 | atmega328p | atmega2560 | atmega1280 | atmega32u4
	public Arduino initializeArduino(String key, String boardType, String comPort) {
		setStatus(String.format("initializing %s arduino", key));
		Arduino arduino = (Arduino) Runtime.createAndStart(String.format("arduino%s", key), "Arduino");
		arduino.setBoard(boardType);
		arduino.setSerialDevice(comPort, 57600, 8, 1, 0);
		sleep(1000);
		return arduino;

	}

	public void releaseArduino(String key) {
		if (arduinoLeft != null)
		{
			arduinoLeft.releaseService();
			arduinoLeft = null;
		}
		if (arduinoRight != null)
		{
			arduinoRight.releaseService();
			arduinoRight = null;
		}
	}

	public Hand initializeHand(String key) {
		Arduino arduino = getArduino(key);

		Hand hand = new Hand();
		if (key == left)
		{
			handLeft = hand;
		} else if (key == right)
		{
			handRight = hand;
		}
	
		hand.initialize(arduino, key);
		return hand;
	}
	
	public Hand getHand(String key)
	{
		if (key == left)
		{
			return handLeft;
		} else if (key == right)
		{
			return handRight;
		}
		setError(String.format("%s hand not found"));
		return null;
	}

	public void releaseHand(String key) {
		if (key == left)
		{
			handLeft.release();
			handLeft = null;
			setStatus("released left hand");
		} else if (key == right)
		{
			handRight.release();
			handRight = null;
			setStatus("released right hand");
		}
	
	}

	public Arm initializeArm(String key) {
		Arduino arduino = getArduino(key);
		setStatus(String.format("initializing %s arm", key));
		Arm arm = new Arm();
		arm.initialize(arduino, key);

		if (key == left)
		{
			armLeft = arm;
		} else if (key == right)
		{
			armRight = arm;
		}
	
		return arm;
	}

	public void releaseArm(String key) {
		if (key == left && armLeft != null)
		{
			armLeft.release();
			armLeft = null;
			setStatus("released left arm");
		} else if (key == right && armRight != null)
		{
			armRight.release();
			armRight = null;
			setStatus("released right arm");
		}
	}

	public void release() {
		releaseArm(left);
		releaseArm(right);
		releaseHand(left);
		releaseHand(right);
		releaseArduino(left);
		releaseArduino(left);		
	}

	public void rest() {
		if (armLeft != null)
		{
			armLeft.rest();
		}
		if (armRight != null)
		{
			armRight.rest();
		}
		if (handLeft != null)
		{
			handLeft.rest();
		}
		if (handRight != null)
		{
			handRight.rest();
		}
		if (head != null) {
			head.rest();
		}
	}

	public void initializeHead(String key) {
		initializeHead(getArduino(key));
	}

	public void initializeHead(Arduino arduino) {
		if (arduino == null) {
			log.error("arduino not valid");
		}
		arduinoHead = arduino;
		head = new Head();
		head.initialize(this);
		//head.initialize(arduino);
	}

	// ----------- normalization end ---------------------

	public void initialize(String side, String boardType, String comPort) {
			initializeArduino(side, boardType, comPort);
			initializeHand(side);
			initializeArm(side);
	}

	public void broadcastState() {
		if (armLeft != null)
		{
			armLeft.broadcastState();
		}
		if (armRight != null)
		{
			armRight.broadcastState();
		}
		if (handLeft != null)
		{
			handLeft.broadcastState();
		}
		if (handRight != null)
		{
			handRight.broadcastState();
		}
		if (head != null)
		{
			head.broadcastState();
		}
	}

	public void initialize(String LeftBoardType, String LeftComPort, String RightBoardType, String RightComPort) {

		log.info(String.format("left - %s %s right - %s %s", LeftBoardType, LeftComPort, RightBoardType, RightComPort));
		initialize(left, LeftBoardType, LeftComPort);
		initialize(right, RightBoardType, RightComPort);

	}

	public void initializeHead() {

	}

	// lower higher concepts - 10 degree
	// much higher
	// remember move - save

	// low level moveHand

	//

	public void handOpen(String which) {
		moveHand(which, 0, 0, 0, 0, 0);
	}

	public void handClose(String which) {
		moveHand(which, 130, 180, 180, 180, 180);
	}

	public void handRest(String which) {
		moveHand(which, 60, 40, 30, 40, 40);
	}

	public void openPinch(String which) {
		moveHand(which, 0, 0, 180, 180, 180);
	}

	public void closePinch(String which) {
		moveHand(which, 130, 140, 180, 180, 180);
	}

	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky) {
		moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {

		if (which == left || which == both)
		{
			handLeft.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
		}
		if (which == right || which == both)
		{
			handRight.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
		}
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky) {
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist) {
		if (which == left || which == both)
		{
			handLeft.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
		}
		if (which == right || which == both)
		{
			handRight.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
		}
	}

	public void setArmSpeed(String which, Float bicep, Float rotate, Float shoulder, Float omoplate) {
		if (which == left || which == both)
		{
			armLeft.setSpeed(bicep, rotate, shoulder, omoplate);
		}
		if (which == right || which == both)
		{
			armRight.setSpeed(bicep, rotate, shoulder, omoplate);
		}
	}

	public void moveArm(String which, Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		if (which == left || which == both)
		{
			armLeft.moveTo(bicep, rotate, shoulder, omoplate);
		}
		if (which == right || which == both)
		{
			armRight.moveTo(bicep, rotate, shoulder, omoplate);
		}
	}

	public void moveHead(Integer neck, Integer rothead) {
		if (head != null) {
			head.move(neck, rothead);
		} else {
			log.error("I have a null head");
		}
	}

	public void setHeadSpeed(Float neck, Float rothead) {
		head.setSpeed(neck, rothead);
	}

	public void systemCheck() {
		// check arduinos

		mouth.speak("starting system check");

		rest();
		/*
		 * TRACING APPEARS TO "MESS" THINGS UP --- POSSIBLY FIXME
		 * ArrayList<Arduino> arduinoList = arduinos.get(both); for(int i = 0; i
		 * < arduinoList.size(); ++i) { Arduino arduino = arduinoList.get(i);
		 * arduino.pinMode(17, Arduino.INPUT);
		 * arduino.analogReadPollingStart(17); sleep(250);
		 * arduino.analogReadPollingStop(17); }
		 */
		
		if (armLeft != null)
		{
			armLeft.moveTo(10, 100, 40, 20);
		}
		if (armRight != null)
		{
			armRight.moveTo(10, 100, 40, 20);
		}	
		
		if (handLeft != null)
		{
			handLeft.moveTo(10, 10, 10, 10, 10, 10);
		}
		if (handRight != null)
		{
			handRight.moveTo(10, 10, 10, 10, 10, 10);
		}

		rest();

		// check servos

		// check ear

		// check mount - all my circuits are functioning perfectly
		mouth.speak("completed system check");

		broadcastState();
	}

	public String captureGesture() {
		return captureGesture(null);
	}

	public String captureGesture(String gestureName) {
		StringBuffer script = new StringBuffer();

		String indentSpace = "";

		if (gestureName != null) {
			indentSpace = "  ";
			script.append(String.format("def %s():\n", gestureName));
		}

		if (head != null) {
			script.append(indentSpace);
			script.append(head.getScript(getName()));
		}
		
		if (armLeft != null)
		{
			script.append(indentSpace);
			script.append(armLeft.getScript(getName()));
		}
		if (armRight != null)
		{
			script.append(indentSpace);
			script.append(armRight.getScript(getName()));
		}
		
		if (handLeft != null)
		{
			script.append(indentSpace);
			script.append(handLeft.getScript(getName()));
		}
		if (handRight != null)
		{
			script.append(indentSpace);
			script.append(handRight.getScript(getName()));
		}

		send("python", "appendScript", script.toString());

		return script.toString();
	}

	@Override
	public String getToolTip() {
		return "the InMoov Service";
	}

	public void startListening(String grammar) {
		ear.attach(mouth.getName());
		ear.addListener("recognized", "python", "heard", String.class);
		ear.createGrammar(grammar);
		ear.startListening();

	}

	public void startListening() {
		ear.startListening();
	}

	public void pauseListening() {
		ear.pauseListening();
	}

	public void resumeListening() {
		ear.resumeListening();
	}

	public void stopListening() {
		ear.stopListening();
	}
	
	public void allowHeadMovementFromScript()
	{
		head.allowMove = true;
	}

	public void stopHeadMovementFromScript()
	{
		head.allowMove = false;
	}
	
	/*
	boolean isTracking = false;

	
	public void startTracking() {
		if (isTracking) {
			log.warn("already tracking");
			return;
		}

		isTracking = true;
		eye.addFilter("pyramidDown1", "PyramidDown");
		eye.addFilter("lkOpticalTrack1", "LKOpticalTrack");
		eye.setDisplayFilter("lkOpticalTrack1");
		eye.capture();
		sleep(500);
		eye.invokeFilterMethod("lkOpticalTrack1", "samplePoint", 160, 120);
	}

	public void stopTracking() {
		if (!isTracking) {
			log.warn("already stopped tracking");
			return;
		}

		isTracking = false;
		eye.removeAllFilters();
		eye.stopCapture();
	}
	*/

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		InMoov inMoov = new InMoov("inMoov");
		inMoov.startService();
/*
		Arduino arduino = new Arduino("arduino");

		arduino.setBoard("atmega328");
		arduino.setSerialDevice("COM12",57600,8,1,0);
		
		inMoov.initializeBrain();
		
		inMoov.initializeHead(arduino);
*/		
		/*
		Arduino arduino = new Arduino("arduinox");
		arduino.setBoard("atmega328");
		arduino.setSerialDevice("COM12",57600,8,1,0);
		
		inMoov.initializeHead(arduino);
		*/
		
		Arduino arduino = (Arduino)Runtime.createAndStart("arduino", "Arduino");
		arduino.setBoard("atmega328");
		arduino.setSerialDevice("COM12",57600,8,1,0);
		
		inMoov.initializeHead(arduino);
		// Runtime.createAndStart("python", "Python");
		ServiceInterface gui = Runtime.createAndStart("gui", "GUIService");
		gui.display();
		
		inMoov.eye.setCameraIndex(1);
		//inMoov.tracking.trackLKPoint();
		

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
