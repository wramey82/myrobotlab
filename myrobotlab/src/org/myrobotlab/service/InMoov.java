package org.myrobotlab.service;

import java.util.HashMap;

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
	// ------------- added 
	public static final String body = "body";
		
	// port map
	HashMap <String, Arduino> arduinos = new HashMap <String, Arduino>();

	Head head;

	// head
	transient public Sphinx ear;
	transient public Speech mouth;
	transient public OpenCV opencv;
	transient public Python python;
	transient public Tracking headTracking;
	transient public Tracking eyesTracking;
	transient public Arduino arduinoHead, arduinoRight, arduinoLeft;
	transient public MouthControl mouthcontrol;
	
	// servos
	transient public Servo rothead;
	transient public Servo neck;
	transient public Servo eyeX;
	
	transient public Servo thumbRight;
	transient public Servo indexRight;
	transient public Servo majeureRight;
	transient public Servo ringFingerRight;
	transient public Servo pinkyRight;
	transient public Servo wristRight;
	
	transient public Servo thumbLeft;
	transient public Servo indexLeft;
	transient public Servo majeureLeft;
	transient public Servo ringFingerLeft;
	transient public Servo pinkyLeft;
	transient public Servo wristLeft;
	
	// TODO - goes in jaw
	transient public Servo jaw;
	
	transient public Servo eyeY;
	
	// left side
	transient public Hand handLeft;
	transient public Arm armLeft;

	// right side
	transient public Hand handRight;
	transient public Arm armRight;

	transient public Keyboard keyboard;
	
	// ------ arduino references -----
	transient public Arduino arduinoJaw;
	transient public Arduino arduinoBody;
	
	
	// FIXME - calling reserved on an existing reserve is an error ?
	public InMoov(String n) {
		super(n, InMoov.class.getCanonicalName());
		
		// service which do not require user input
		reserve("ear", "Sphinx", "InMoov speech recognition service");
		reserve("mouth", "Speech", "InMoov speech service");
		reserve("opencv", "OpenCV", "OpenCV service");
		reserve("python", "Python", "Python service");
		reserve("keyboard", "Keyboard", "Keyboard service");
		
		// head servos
		reserve("rothead", "Servo", "rotate/pan servo");
		reserve("neck", "Servo", "neck/tilt servo");

		// hands
		reserve("thumbRight", "Servo", "thumbRight servo");
		reserve("thumbLeft", "Servo", "thumbLeft servo");
		reserve("indexRight", "Servo", "indexRight servo");
		reserve("indexLeft", "Servo", "indexLeft servo");
		reserve("majeureRight", "Servo", "majeureRight servo");
		reserve("majeureLeft", "Servo", "majeureLeft servo");
		reserve("ringFingerRight", "Servo", "ringFingerRight servo");
		reserve("ringFingerLeft", "Servo", "ringFingerLeft servo");
		reserve("pinkyRight", "Servo", "pinkyRight servo");
		reserve("pinkyLeft", "Servo", "pinkyLeft servo");
		reserve("wristRight", "Servo", "wristRight servo");
		reserve("wristLeft", "Servo", "wristLeft servo");
		
		// arms
		
		
		// composite and complex services which require use input
		reserve("headTracking", "Tracking", "Tracking service for InMoov head");
		// ----- head tracking peers begin -------
		reserve("headX", "Servo", "servo for pan"); // rothead
		reserve("headY", "Servo", "servo for tilt");
		reserve("headXPID", "PID", "PID for pan");
		reserve("headYPID", "PID", "PID for tilt");
		reserve("head-arduino", "Arduino", "arduino to control the servos");
		
		reserve("eyes-tracking", "Tracking", "Tracking service for InMoov eyes");
		// ----- head tracking peers begin -------
		reserve("eyesX", "Servo", "servo for pan");
		reserve("eyesY", "Servo", "servo for tilt");
		reserve("eyesXPID", "PID", "PID for pan");
		reserve("eyesYPID", "PID", "PID for tilt");
		reserve("opencv", "OpenCV", "camera");
		reserve("eyes-arduino", "Arduino", "arduino to control the servos");
		
		// ----- tracking peers begin -------
		
		reserve("jaw", "Servo", "Servo for the jaw");
		reserve("mouthControl", "mouthControl", "Mouth control");
		
	}

	// ----------- normalization begin ---------------------
	
	// ----------- start routines begin ---------------------
	// low level start routines are bundled in higher and higher routines defaulting
	// hardware as they go up the chain, but allowing low level changes if manually
	// performed
	
	// FIXME - override error & info - replace with (if (mouth != null & speakStatus) = mouth.speak(infomsg)
	
	public boolean startSimpleServices()
	{
		boolean success = true;
		success &= startSpeech();
		success &= startEye();
		success &= startPython();
		success &= startKeyboard();
		return success;
	}
	
	// ------ simple services which do not require user input begin --------
	public boolean startSpeech()
	{
		info("starting ear and mouth");
		
		ear = (Sphinx) startReserved("ear");
		mouth = (Speech) startReserved("mouth");
		ear.attach(mouth);
		
		return true;
	}
	
	public boolean startEye()
	{
		info("starting opencv");
		// the one shared opencv !!!
		opencv = (OpenCV) startReserved("opencv");
		
		return true;
	}
	
	public boolean startPython()
	{
		info("starting python engine");
		python = (Python) startReserved("python");
		return true;
	}
	
	public boolean startKeyboard()
	{
		info("starting keyboard");
		keyboard = (Keyboard) startReserved("keyboard");
		return true;
	}

	// ------ simple services which do not require user input end --------
	
	public boolean startHeadTracking(String port, Integer xpin, Integer ypin)
	{
		info("starting head tracking");
		headTracking = (Tracking) createReserved("head-tracking");
		reserveAs("x", "rot");
		reserveAs("y", "headY");
		
		reserveAs("xpid", "headXPID");
		reserveAs("ypid", "headYPID");
		// shared opencv
		reserveAs("opencv", "opencv");
		//reserveAs("arduino", "head-arduino");
		reserveAs("arduino", port);
		
		headTracking.startService();
		headTracking.connect(port);
		if (!headTracking.arduino.isConnected())
		{
			error("error with headtracking could not connect %s", headTracking.arduino.getName());
			return false;
		}
		headTracking.attachServos(xpin, ypin);
		return true;
	}
	
	public boolean startEyesTracking(String port, Integer xpin, Integer ypin)
	{
		info("starting eyes tracking");
		eyesTracking = (Tracking) createReserved("eyes-tracking");
		reserveAs("x", "eyesX");
		reserveAs("y", "eyesY");
		
		reserveAs("xpid", "eyesXPID");
		reserveAs("ypid", "eyesYPID");
		// shared opencv
		reserveAs("opencv", "opencv");
		reserveAs("arduino", port);
		
		eyesTracking.startService();
		eyesTracking.connect(port);
		if (!eyesTracking.arduino.isConnected())
		{
			error("error with eyestracking could not connect %s", eyesTracking.arduino.getName());
			return false;
		}
		eyesTracking.attachServos(xpin, ypin);
		return true;
	}
	
	public Arduino getArduino(String port)
	{
		log.info(String.format("request for port %s", port));
		if (arduinos.containsKey(port))
		{
			return arduinos.get(port);
		}
		
		Arduino arduino = (Arduino)Runtime.createAndStart(port, "Arduino");
		arduino.connect(port);
		if (!arduino.isConnected()){
			log.error(String.format("arduino %s not connected", port));
		}
		
		return arduino;
	}
	
	
	public boolean startHead(String port)
	{
		boolean init = true;
		init &= startHeadTracking(port, 12, 13);
		//init &= startEyesTracking(port, ??, ??);  FIXME - implement defaults
		return init;

	}
	
	public boolean startJaw(String port, int jawPin)
	{
		arduinoJaw = getArduino(port);
		jaw = (Servo)startReserved("jaw");
		arduinoJaw.servoAttach(jaw.getName(), jawPin);
		return true;
	}
	
	public Hand startHand(String port, String key)
	{
		return startHand(port, key, 2, 3, 4, 5, 6, 7);
	}
	
	public Hand startHand(String port, String key,  int thumb, int index, int majeure, int ringFinger, int pinky, int wrist)
	{
		info("starting %s hand with port %s and default pin configuration", port, key);
		return startHand(port, key, 2, 3, 4, 5, 6, 7);		
	}
	

	
	// ----------- start routines end ---------------------
	
	// ------------- added function with set pins
	// FIXME FIXME FIXME REFACTOR BELOW
	
	public Hand attachHand(String key) {
		Arduino arduino = getArduino(key);

		Hand hand = new Hand();
		if (key == left) {
			handLeft = hand;
		} else if (key == right) {
			handRight = hand;
		} else {
			error(String.format("could not attach %s hand", key));
		}

		//hand.start(arduino, key);
		return hand;
	}

	public Hand getHand(String key) {
		if (key == left) {
			return handLeft;
		} else if (key == right) {
			return handRight;
		}
		error(String.format("%s hand not found", key));
		return null;
	}
	// ------------- added function with set arduino and set pins
	public Arm attachArm(String key,String arduinoName,Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		Arduino arduino = getArduino(arduinoName);
		info(String.format("initializing %s arm", key));
		Arm arm = new Arm();
		arm.setpins(bicep, rotate, shoulder, omoplate);
		arm.attach(arduino, key);

		if (key == left) {
			armLeft = arm;
		} else if (key == right) {
			armRight = arm;
		} else {
			error(String.format("%s - bad arm initialization ", key));
			return null;
		}

		return arm;
	}
	// ------------- added function with set pins
	public Arm attachArm(String key,Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		Arduino arduino = getArduino(key);
		info(String.format("initializing %s arm", key));
		Arm arm = new Arm();
		arm.setpins(bicep, rotate, shoulder, omoplate);
		arm.attach(arduino, key);

		if (key == left) {
			armLeft = arm;
		} else if (key == right) {
			armRight = arm;
		} else {
			error(String.format("%s - bad arm initialization ", key));
			return null;
		}

		return arm;
	}
	public Arm attachArm(String key) {
		Arduino arduino = getArduino(key);
		info(String.format("initializing %s arm", key));
		Arm arm = new Arm();
		
		arm.attach(arduino, key);

		if (key == left) {
			armLeft = arm;
		} else if (key == right) {
			armRight = arm;
		} else {
			error(String.format("%s - bad arm initialization ", key));
			return null;
		}

		return arm;
	}

	public void release() {
		if (handLeft != null)
			handLeft.release();
		handLeft = null;
		if (handRight != null)
			handRight.release();
		handRight = null;
		if (armLeft != null)
			armLeft.release();
		armLeft = null;
		if (armRight != null)
			armRight.release();
		armRight = null;
		if (arduinoLeft != null)
			arduinoLeft.releaseService();
		arduinoLeft = null;
		if (arduinoRight != null)
			arduinoRight.releaseService();
		if (arduinoBody != null)
			arduinoBody.releaseService();
		arduinoRight = null;
		if (head != null)
			head.release();
		head = null;
	}

	public void rest() {
		if (handLeft != null)
			handLeft.rest();
		if (handRight != null)
			handRight.rest();
		if (armLeft != null)
			armLeft.rest();
		if (armRight != null)
			armRight.rest();
		if (head != null)
			head.rest();
	}

	public Head attachHead() {
		return attachHead(left);
	}
	// ------------- added function if arduino head been set
	public Head attachHead(String key) {
		if (key =="head"){
			return attachHead2(getArduino(key));
		} else {
			return attachHead(getArduino(key));
		}
		
	}
	// ------------- added function with set pins
	public Head attachHead(String key,Integer eyeX, Integer eyeY, Integer neck, Integer rotHead) {
		if (key =="head"){
			return attachHead2(getArduino(key), eyeX,  eyeY,  neck,  rotHead);
		} else {
			return attachHead(getArduino(key), eyeX,  eyeY,  neck,  rotHead);
		}
		
	}
	public Head attachHead(Arduino arduino) {
		if (arduino == null) {
			error("invalid arduino and not attach head");
		}
		if (head != null) {
			log.info("head already attached - must release first");
			return head;
		}
		arduinoHead = arduino;
		head = new Head();
		head.attach(this);
		return head;
	}
	// ------------- added function with set pins
	
	public Head attachHead(Arduino arduino,Integer eyeX, Integer eyeY, Integer neck, Integer rotHead) {
		if (arduino == null) {
			error("invalid arduino and not attach head");
		}
		if (head != null) {
			log.info("head already attached - must release first");
			return head;
		}
		arduinoHead = arduino;
		head = new Head();
		head.setpins( eyeX,  eyeY,  neck,  rotHead);
		head.attach(this);
		
		return head;
	}

	public Head attachHead2(Arduino arduino) {
		if (arduino == null) {
			error("invalid arduino and not attach head");
		}
		if (head != null) {
			log.info("head already attached - must release first");
			return head;
		}
		//arduinoHead = arduino;
		head = new Head();
		head.attach(this);
		return head;
	}
	// ------------- added function with set pins
	public Head attachHead2(Arduino arduino,Integer eyeX, Integer eyeY, Integer neck, Integer rotHead) {
		if (arduino == null) {
			error("invalid arduino and not attach head");
		}
		if (head != null) {
			log.info("head already attached - must release first");
			return head;
		}
		//arduinoHead = arduino;
		head = new Head();
		head.setpins( eyeX,  eyeY,  neck,  rotHead);
		head.attach(this);
		return head;
	}
	// ----------- normalization end ---------------------

	public void attachSide(String side, String boardType, String comPort) {
		//attachArduino(side, boardType, comPort);
		attachHand(side);
		attachArm(side);
	}

	public void broadcastState() {
		if (armLeft != null)
			armLeft.broadcastState();
		if (armRight != null)
			armRight.broadcastState();
		if (handLeft != null)
			handLeft.broadcastState();
		if (handRight != null)
			handRight.broadcastState();
		if (head != null)
			head.broadcastState();
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
		if ((which == left || which == both) && handLeft != null)
			handLeft.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
		if ((which == right || which == both) && handRight != null)
			handRight.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky) {
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist) {
		if ((which == left || which == both) && handLeft != null)
			handLeft.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
		if ((which == right || which == both) && handRight != null)
			handRight.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
	}

	public void setArmSpeed(String which, Float bicep, Float rotate, Float shoulder, Float omoplate) {
		if (which == left || which == both)
			armLeft.setSpeed(bicep, rotate, shoulder, omoplate);
		if (which == right || which == both)
			armRight.setSpeed(bicep, rotate, shoulder, omoplate);
	}

	public void moveArm(String which, Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		if (which == left || which == both)
			armLeft.moveTo(bicep, rotate, shoulder, omoplate);
		if (which == right || which == both)
			armRight.moveTo(bicep, rotate, shoulder, omoplate);
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

		if (arduinoRight != null) {
			mouth.speakBlocking("Right arduino");
			if (!arduinoRight.isValid()) {
				mouth.speakBlocking("is not valid");
				mouth.speakBlocking(arduinoRight.getLastError());
			}

		}

		if (arduinoLeft != null) {
			mouth.speakBlocking("left arduino");
			if (!arduinoLeft.isValid()) {
				mouth.speakBlocking("is not valid");
				mouth.speakBlocking(arduinoLeft.getLastError());
			}

		}

		if (head != null) {
			mouth.speakBlocking("head");
			if (head.isValid()) {
				mouth.speakBlocking(getLastError());
			} else {
				head.move(100, 100);
			}
		}

		if (armLeft != null) {
			mouth.speakBlocking("left arm");
			armLeft.moveTo(10, 100, 40, 20);
		}
		if (armRight != null) {
			mouth.speakBlocking("right arm");
			armRight.moveTo(10, 100, 40, 20);
		}

		if (handLeft != null) {
			mouth.speakBlocking("left hand");
			handLeft.moveTo(10, 10, 10, 10, 10, 10);
		}
		if (handRight != null) {
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

		if (armLeft != null) {
			script.append(indentSpace);
			script.append(armLeft.getScript(getName()));
		}
		if (armRight != null) {
			script.append(indentSpace);
			script.append(armRight.getScript(getName()));
		}

		if (handLeft != null) {
			script.append(indentSpace);
			script.append(handLeft.getScript(getName()));
		}
		if (handRight != null) {
			script.append(indentSpace);
			script.append(handRight.getScript(getName()));
		}

		send("python", "appendScript", script.toString());

		return script.toString();
	}

	@Override
	public String getDescription() {
		return "the InMoov Service";
	}

	/*
	public void startListening(String grammar) {
		ear.attach(mouth);
		ear.addListener("recognized", "python", "heard", String.class);
		ear.createGrammar(grammar);
		ear.startListening();

	}
	*/
	/*
	public void lockOutAllGrammarExcept(String keyPhrase) {
		if (ear == null) {
			warn("ear not attached");
			return;
		}
		ear.lockOutAllGrammarExcept(keyPhrase);
	}
	*/

	/*
	public void clearGrammarLock() {
		ear.clearLock();
	}
	*/

	/*
	public void stopListening() {
		ear.stopListening();
	}
	*/

	public void allowHeadMovementFromScript() {
		head.allowMove = true;
	}

	public void stopHeadMovementFromScript() {
		head.allowMove = false;
	}

	public boolean setLanguage(String lang) {
		if (mouth == null)
			return false;
		mouth.setLanguage(lang);
		return true;
	}

	public void cameraOn() {
		head.cameraOn();
	}

	public void cameraOff() {
		head.cameraOff();
	}

	public void cameraEnlarge() {
		head.cameraEnlarge();
	}

	public void cameraReduce() {
		head.cameraReduce();
	}

	public void cameraGray() {
		head.cameraGray();
	}

	public void cameraColor() {
		head.cameraColor();
	}

	// gestures begin ---------------

	public void hello() {
		setHeadSpeed(1.0f, 1.0f);
		setArmSpeed("left", 1.0f, 1.0f, 1.0f, 1.0f);
		setArmSpeed("right", 1.0f, 1.0f, 1.0f, 1.0f);
		setHandSpeed("left", 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		setHandSpeed("right", 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		moveHead(105, 78);
		moveArm("left", 78, 48, 37, 10);
		moveArm("right", 90, 144, 60, 75);
		moveHand("left", 112, 111, 105, 102, 81, 10);
		moveHand("right", 0, 0, 0, 50, 82, 180);
	}

	public void giving() {
		moveHead(44, 82);
		moveArm("left", 15, 55, 68, 10);
		moveArm("right", 13, 40, 74, 13);
		moveHand("left", 61, 0, 14, 0, 0, 180);
		moveHand("right", 0, 24, 24, 19, 21, 25);
	}

	public void fighter() {
		moveHead(160, 87);
		moveArm("left", 31, 75, 152, 10);
		moveArm("right", 3, 94, 33, 16);
		moveHand("left", 161, 151, 133, 127, 107, 83);
		moveHand("right", 99, 130, 152, 154, 145, 180);
	}

	public void fistHips() {
		moveHead(138, 80);
		moveArm("left", 71, 41, 20, 39);
		moveArm("right", 71, 40, 14, 39);
		moveHand("left", 161, 151, 133, 127, 107, 83);
		moveHand("right", 99, 130, 152, 154, 145, 180);
	}

	public void lookAtThis() {
		moveHead(66, 79);
		moveArm("left", 89, 75, 78, 19);
		moveArm("right", 90, 91, 72, 26);
		moveHand("left", 92, 106, 133, 127, 107, 29);
		moveHand("right", 86, 51, 133, 162, 153, 180);
	}

	public void victory() {
		moveHead(114, 90);
		moveArm("left", 90, 91, 106, 10);
		moveArm("right", 0, 73, 30, 17);
		moveHand("left", 170, 0, 0, 168, 167, 0);
		moveHand("right", 98, 37, 34, 67, 118, 166);
	}

	public void armsUp() {
		moveHead(160, 97);
		moveArm("left", 9, 85, 168, 18);
		moveArm("right", 0, 68, 180, 10);
		moveHand("left", 61, 38, 14, 38, 15, 64);
		moveHand("right", 0, 0, 0, 50, 82, 180);
	}

	public void armsFront() {
		moveHead(99, 82);
		moveArm("left", 9, 115, 96, 51);
		moveArm("right", 13, 104, 101, 49);
		moveHand("left", 61, 0, 14, 38, 15, 0);
		moveHand("right", 0, 24, 54, 50, 82, 180);
	}

	public void daVinci() {
		moveHead(75, 79);
		moveArm("left", 9, 115, 28, 80);
		moveArm("right", 13, 118, 26, 80);
		moveHand("left", 61, 49, 14, 38, 15, 64);
		moveHand("right", 0, 24, 54, 50, 82, 180);
	}
	
	public Sphinx getEar()
	{
		return ear;
	}


	// gestures end --------------
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		String test = "could not attach servo to pin 7 serial port in null - not initialized?.mp3";
		log.info(test.replace("?:", ""));

		InMoov inMoov = new InMoov("inMoov");
		inMoov.startService();
		inMoov.startHead("COM12");
		
		Runtime.createAndStart("gui", "GUIService");
		inMoov.handRight.index.setPositionMax(155);
		inMoov.handRight.index.setPositionMin(15);

		/*
		 * Arduino arduino = new Arduino("arduino");
		 * 
		 * arduino.setBoard("atmega328");
		 * arduino.setSerialDevice("COM12",57600,8,1,0);
		 * 
		 * inMoov.initializeBrain();
		 * 
		 * inMoov.initializeHead(arduino);
		 */
		/*
		 * Arduino arduino = new Arduino("arduinox");
		 * arduino.setBoard("atmega328");
		 * arduino.setSerialDevice("COM12",57600,8,1,0);
		 * 
		 * inMoov.initializeHead(arduino);
		 */
		/*
		 * Arduino arduino = (Arduino)Runtime.createAndStart("arduino",
		 * "Arduino"); arduino.setBoard("atmega328");
		 * arduino.setSerialDevice("COM12",57600,8,1,0);
		 * 
		 * inMoov.initializeHead(arduino); // Runtime.createAndStart("python",
		 * "Python"); ServiceInterface gui = Runtime.createAndStart("gui",
		 * "GUIService"); gui.display();
		 * 
		 * inMoov.opencv.setCameraIndex(1);
		 */
		// inMoov.tracking.trackLKPoint();
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
