package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.inmoov.Arm;
import org.myrobotlab.inmoov.Hand;
import org.myrobotlab.inmoov.Head;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class InMoov extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoov.class.getCanonicalName());

	public static final String left = "left";
	public static final String right = "right";
	public static final String both = "both";

	Head head;

	// head
	transient public Sphinx ear;
	transient public Speech mouth;
	transient public OpenCV eye;
	transient public Python python;
	transient public Tracking tracking;
	transient public Arduino arduinoHead;
	
	transient public Servo rothead;
	transient public Servo neck;
	
	// left side
	transient public Arduino arduinoLeft;
	transient public Hand handLeft;
	transient public Arm armLeft;
	
	// right side
	transient public Arduino arduinoRight;
	transient public Hand handRight;
	transient public Arm armRight;
	
	public InMoov(String n) {
		super(n, InMoov.class.getCanonicalName());
	}
	
	public void startService()
	{
		super.startService();
		// FIXME - big assumption they have the hardware or 
		// desire to start these services ....
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
		error("getArduino ({}) not found");
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
		log.error("setArduino ({}, Arduino) must be left or right");
	}

	// uno | atmega168 | atmega328p | atmega2560 | atmega1280 | atmega32u4
	public Arduino attachArduino(String key, String boardType, String comPort) {
		info(String.format("initializing %s arduino", key));
		Arduino arduino = (Arduino) Runtime.createAndStart(String.format("arduino%s", key), "Arduino");
		arduino.setBoard(boardType);
		arduino.setSerialDevice(comPort, 57600, 8, 1, 0);
		sleep(1000);
		setArduino(key, arduino);
		return arduino;
	}


	public Hand attachHand(String key) {
		Arduino arduino = getArduino(key);

		Hand hand = new Hand();
		if (key == left)
		{
			handLeft = hand;
		} else if (key == right)
		{
			handRight = hand;
		} else {
			error(String.format("could not attach %s hand", key));
		}
	
		hand.attach(arduino, key);
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
		error(String.format("%s hand not found"));
		return null;
	}

	public Arm attachArm(String key) {
		Arduino arduino = getArduino(key);
		info(String.format("initializing %s arm", key));
		Arm arm = new Arm();
		arm.attach(arduino, key);

		if (key == left)
		{
			armLeft = arm;
		} else if (key == right)
		{
			armRight = arm;
		} else {
			error(String.format("%s - bad arm initialization ", key));
			return null;
		}
	
		return arm;
	}

	public void release() {
		if (handLeft != null) 		handLeft.release();				handLeft = null;
		if (handRight != null) 		handRight.release();			handRight = null;
		if (armLeft != null) 		armLeft.release();				armLeft = null;				
		if (armRight != null) 		armRight.release();				armRight = null;
		if (arduinoLeft != null) 	arduinoLeft.releaseService();	arduinoLeft = null;
		if (arduinoRight != null) 	arduinoRight.releaseService();	arduinoRight = null;
		if (head != null) 			head.release(); 				head = null;
	}

	public void rest() {
		if (handLeft != null)	handLeft.rest();
		if (handRight != null)	handRight.rest();
		if (armLeft != null)	armLeft.rest();
		if (armRight != null) 	armRight.rest();
		if (head != null) head.rest();
	}

	public Head attachHead()
	{
		return attachHead(left);
	}
	public Head attachHead(String key)
	{
		return attachHead(getArduino(key));
	}
	public Head attachHead(Arduino arduino) {
		if (arduino == null) {
			log.error("arduino not valid");
		}
		if (head != null)
		{
			log.info("head already attached - must release first");
			return head;
		}
		head = new Head();
		head.attach(this);
		return head;
	}

	// ----------- normalization end ---------------------

	public void attachSide(String side, String boardType, String comPort) {
			attachArduino(side, boardType, comPort);
			attachHand(side);
			attachArm(side);
	}

	public void broadcastState() {
		if (armLeft != null) armLeft.broadcastState();
		if (armRight != null) armRight.broadcastState();
		if (handLeft != null) handLeft.broadcastState();
		if (handRight != null) handRight.broadcastState();
		if (head != null) head.broadcastState();
	}

	public void attachAll(String LeftBoardType, String LeftComPort, String RightBoardType, String RightComPort) {
		log.info(String.format("left - %s %s right - %s %s", LeftBoardType, LeftComPort, RightBoardType, RightComPort));
		attachSide(left, LeftBoardType, LeftComPort);
		attachSide(right, RightBoardType, RightComPort);
	}

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
		if ((which == left  || which == both) && handLeft != null) 	handLeft.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
		if ((which == right || which == both) && handRight != null) handRight.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky) {
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist) {
		if ((which == left  || which == both) && handLeft != null) 	handLeft.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
		if ((which == right || which == both) && handRight != null)	handRight.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
	}

	public void setArmSpeed(String which, Float bicep, Float rotate, Float shoulder, Float omoplate) {
		if (which == left  || which == both) armLeft.setSpeed(bicep, rotate, shoulder, omoplate);
		if (which == right || which == both) armRight.setSpeed(bicep, rotate, shoulder, omoplate);
	}

	public void moveArm(String which, Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		if (which == left  || which == both) armLeft.moveTo(bicep, rotate, shoulder, omoplate);
		if (which == right || which == both) armRight.moveTo(bicep, rotate, shoulder, omoplate);
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

		mouth.speakBlocking("starting system check");
		mouth.speakBlocking("testing");

		rest();
		sleep(500);

		if (arduinoRight != null)
		{
			mouth.speakBlocking("Right arduino");
			if (!arduinoRight.isValid())
			{
				mouth.speakBlocking("is not valid");
				mouth.speakBlocking(arduinoRight.getLastError());
			}
			
		}
		
		if (arduinoLeft != null)
		{
			mouth.speakBlocking("left arduino");
			if (!arduinoLeft.isValid())
			{
				mouth.speakBlocking("is not valid");
				mouth.speakBlocking(arduinoLeft.getLastError());
			}
			
		}
		
		if (head != null)
		{
			mouth.speakBlocking("head");
			head.move(100, 100);
		}
		
		if (armLeft != null)
		{
			mouth.speakBlocking("left arm");
			armLeft.moveTo(10, 100, 40, 20);
		}
		if (armRight != null)
		{
			mouth.speakBlocking("right arm");
			armRight.moveTo(10, 100, 40, 20);
		}	
		
		if (handLeft != null)
		{
			mouth.speakBlocking("left hand");
			handLeft.moveTo(10, 10, 10, 10, 10, 10);
		}
		if (handRight != null)
		{
			mouth.speakBlocking("right hand");
			handRight.moveTo(10, 10, 10, 10, 10, 10);
		}

		sleep(500);
		rest();

		// check servos

		// check ear

		// check mount - all my circuits are functioning perfectly

		broadcastState();
		mouth.speakBlocking("system check completed");
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
		ear.attach(mouth);
		ear.addListener("recognized", "python", "heard", String.class);
		ear.createGrammar(grammar);
		ear.startListening();

	}

	public void lockOutAllGrammarExcept(String keyPhrase) {
		if (ear == null)
		{
			warn("ear not attached");
			return;
		}
		ear.lockOutAllGrammarExcept(keyPhrase);
	}
	
	public void clearGrammarLock()
	{
		ear.clearLock();
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
	
	public boolean setLanguage(String lang)
	{
		if (mouth == null) return false;
		mouth.setLanguage(lang);
		return true;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		
		String test = "could not attach servo to pin 7 serial port in null - not initialized?.mp3";
		log.info(test.replace("?:", ""));
		
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
/*		
		Arduino arduino = (Arduino)Runtime.createAndStart("arduino", "Arduino");
		arduino.setBoard("atmega328");
		arduino.setSerialDevice("COM12",57600,8,1,0);
		
		inMoov.initializeHead(arduino);
		// Runtime.createAndStart("python", "Python");
		ServiceInterface gui = Runtime.createAndStart("gui", "GUIService");
		gui.display();
		
		inMoov.eye.setCameraIndex(1);
		*/
		//inMoov.tracking.trackLKPoint();
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
