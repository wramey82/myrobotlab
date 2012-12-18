package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.inmoov.Arm;
import org.myrobotlab.inmoov.Hand;
import org.myrobotlab.inmoov.Head;
import org.myrobotlab.service.interfaces.ServiceInterface;

public class InMoov extends Service {

	// TODO - normalize bi-lateral code parts
	
	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(InMoov.class.getCanonicalName());
	
	String bodyPartContext = null;
	
	public static final String left = "left";
	public static final String right = "right";
	public static final String both = "both";
	
	Head head;
	HashMap<String, ArrayList<Hand>> hands = new HashMap<String, ArrayList<Hand>>();
	HashMap<String, ArrayList<Arm>> arms = new HashMap<String, ArrayList<Arm>>();
	HashMap<String, ArrayList<Arduino>> arduinos = new HashMap<String, ArrayList<Arduino>>();
	
	// head
	public Sphinx ear 		= (Sphinx)Runtime.createAndStart("ear", "Sphinx"); 
	public Speech mouth 	= (Speech)Runtime.createAndStart("mouth", "Speech");
	public OpenCV eye 		= (OpenCV)Runtime.createAndStart("eye", "OpenCV");
	public Python python	= (Python)Runtime.createAndStart("python", "Python");

	public InMoov(String n) {
		super(n, InMoov.class.getCanonicalName());
		ArrayList<Hand> bothHands = new ArrayList<Hand>();
		hands.put(both, bothHands);
		
		ArrayList<Arm> bothArms = new ArrayList<Arm>();
		arms.put(both, bothArms);
		
		ArrayList<Arduino> bothArduinos = new ArrayList<Arduino>();
		arduinos.put(both, bothArduinos);
		
		// get a handle on the python service
		// python = (Python)Runtime.createAndStart("python", "Python");
	}

	// -----------  normalization begin ---------------------
	
	public Arduino getArduino(String key)
	{
		// String.format("arduino%s",key)
		ArrayList<Arduino> arduinoList = arduinos.get(key);
		if (arduinoList == null || arduinoList.size() != 1)
		{
			log.error(String.format("%s arduino not available", key));
			return null;
		}
		
		return arduinoList.get(0);
	}
	
	// uno | atmega168 | atmega328p | atmega2560 | atmega1280 | atmega32u4
	public Arduino initializeArduino(String key, String boardType, String comPort)
	{
		Arduino arduino = (Arduino)Runtime.createAndStart(String.format("arduino%s",key), "Arduino"); 
		arduino.setBoard(boardType);
		arduino.setSerialDevice(comPort, 57600, 8, 1, 0);
		// wait a second for serial ports to come online
		sleep(1000);
		
		//String contextKey = String.format("arduino%s",key);
		
		ArrayList<Arduino> list = new ArrayList<Arduino>();
		list.add(arduino);
		arduinos.put(key, list);
		arduinos.get(both).add(arduino);
		return arduino;

	}
	
	public void releaseArduino(String key)
	{
		ArrayList<Arduino> arduinoList = arduinos.get(key);
		for (int i = 0; i < arduinoList.size(); ++i)
		{
			Arduino a = arduinoList.get(i);
			a.releaseService();
		}
		
		for (Iterator<?> it = arduinoList.iterator(); it.hasNext(); )
		{ 
			it.next();
			it.remove();
		}
		
		arduinos.remove(arduinoList);
	}
	
	public Hand initializeHand(String key)
	{
		Arduino arduino = getArduino(key);

		//String contextKey = String.format("hand%s",key);
		
		// hand
		Hand hand = new Hand();
		hand.initialize(arduino, key);
		
		ArrayList<Hand> handList = new ArrayList<Hand>();
		handList.add(hand);
		hands.put(key, handList);
		hands.get(both).add(hand);
		
		return hand;
		
	}
	
	public void releaseHand(String key)
	{
		ArrayList<Hand> handList = hands.get(key);
		for (int i = 0; i < handList.size(); ++i)
		{
			handList.get(i).release();
		}
		
		
		for (Iterator<?> it = handList.iterator(); it.hasNext(); )
		{ 
			it.next();
			it.remove();
		}
		
		
		hands.remove(handList);
	}


	public Arm initializeArm(String key)
	{
		Arduino arduino = getArduino(key);
		
		//String contextKey = String.format("arm%s", key);
		// arm
		Arm arm = new Arm();
		arm.initialize(arduino, key);
		
		ArrayList<Arm> armList = new ArrayList<Arm>();
		armList.add(arm);
		arms.put(key, armList);
		arms.get(both).add(arm);
		return arm;
		
	}
	
	public void releaseArm(String key)
	{
		ArrayList<Arm> armList = arms.get(key);
		for (int i = 0; i < armList.size(); ++i)
		{
			armList.get(i).release();
		}
		
		for (Iterator<?> it = armList.iterator(); it.hasNext(); )
		{ 
			it.next();
			it.remove();
		}
		
		arms.remove(armList);
	}
	
	
	public void release()
	{
	    Iterator<String> it = hands.keySet().iterator();
	    while (it.hasNext()) {
	    	releaseHand(it.next());
	        it.remove(); 
	    }
	    
	    it = arms.keySet().iterator();
	    while (it.hasNext()) {
	    	releaseArm(it.next());
	        it.remove(); 
	    }
	    
	    it = arduinos.keySet().iterator();
	    while (it.hasNext()) {
	    	releaseArduino(it.next()); 
	    	it.remove();
	    }
	}
	
	public void rest()
	{
		ArrayList<Arm> armList = arms.get(both);
		for(int i = 0; i < armList.size(); ++i)
		{
			armList.get(i).rest();
		}
		ArrayList<Hand> handList = hands.get(both);
		for(int i = 0; i < handList.size(); ++i)
		{
			handList.get(i).rest();
		}
		
		if (head != null)
		{
			head.rest();
		}
	}
	
	public void initializeHead(String key)
	{
		initializeHead(getArduino(key));
	}
	
	public void initializeHead(Arduino arduino)
	{	
		if (arduino == null)
		{
			log.error("arduino not valid");
		}
		head = new Head();
		head.initialize(arduino);
	}
	// -----------  normalization end ---------------------
	
	public void initialize(String side, String boardType, String comPort)
	{
		//String arduinoKey = String.format("arduino%s", side);
		//String armKey = String.format("arm%s", side);
		//String handKey = String.format("hand%s", side);
		
		if (!arduinos.containsKey(side))
		{
			initializeArduino(side, boardType, comPort);
		} else {
			log.warn(String.format("already initialized %s", side));
		}
		
		if (!hands.containsKey(side))
		{
			initializeHand(side);
		} else {
			log.warn(String.format("already initialized %s", side));
		}
		
		if (!arms.containsKey(side))
		{
			initializeArm(side);
		} else {
			log.warn(String.format("already initialized %s", side));
		}

	}
	

	public void broadcastState()
	{
		ArrayList<Arduino> arduinoList = arduinos.get(both);
		for(int i = 0; i < arduinoList.size(); ++i)
		{
			arduinoList.get(i).broadcastState();
		}
		ArrayList<Arm> armList = arms.get(both);
		for(int i = 0; i < armList.size(); ++i)
		{
			armList.get(i).broadcastState();
		}
		ArrayList<Hand> handList = hands.get(both);
		for(int i = 0; i < handList.size(); ++i)
		{
			handList.get(i).broadcastState();
		}

	}
	
	public void initialize(String LeftBoardType, String LeftComPort, String RightBoardType, String RightComPort) {

		log.info(String.format("left - %s %s right - %s %s", LeftBoardType, LeftComPort, RightBoardType, RightComPort));
		initialize(left, LeftBoardType, LeftComPort);
		initialize(right, RightBoardType, RightComPort);

	}
	
	public void initializeBrain()
	{
		
	}
	
	// lower higher concepts - 10 degree
	// much higher 
	// remember move - save 
	
	// low level moveHand
	
	// 
	
	public void handOpen(String which)
	{
		moveHand(which, 0, 0, 0, 0, 0);
	}
	
	public void handClose(String which)
	{
		moveHand(which, 130, 180, 180, 180, 180);
	}
	
	public void handRest(String which)
	{
		moveHand(which, 60, 40, 30, 40, 40);		
	}
	
	public void openPinch(String which)
	{
		moveHand(which, 0, 0, 180, 180, 180);		
	}

	public void closePinch(String which)
	{
		moveHand(which, 130, 140, 180, 180, 180);		
	}

	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky)
	{
		moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
	}
	
	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist)
	{
		
		ArrayList<Hand> whichHands = hands.get(which);
		for (int i = 0; i < whichHands.size(); ++i)
		{
			Hand hand = whichHands.get(i);
			hand.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
		}
	}
	

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky)
	{
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
	}
	
	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist)
	{
		ArrayList<Hand> whichHands = hands.get(which);
		for (int i = 0; i < whichHands.size(); ++i)
		{
			Hand hand = whichHands.get(i);
			hand.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
		}
	}
	
	public void setArmSpeed(String which, Float bicep, Float rotate, Float shoulder, Float omoplate)
	{
		ArrayList<Arm> whichArms = arms.get(which);
		for (int i = 0; i < whichArms.size(); ++i)
		{
			Arm arm = whichArms.get(i);
			arm.setSpeed(bicep, rotate, shoulder, omoplate);
		}
	}
	
	
	public void moveArm(String which, Integer bicep, Integer rotate, Integer shoulder, Integer omoplate)
	{
		ArrayList<Arm> armList = arms.get(which);
		for (int i = 0; i < armList.size(); ++i)
		{
			Arm arm = armList.get(i);
			arm.moveTo(bicep, rotate, shoulder, omoplate);
		}
	}
	
	public void moveHead(Integer neck, Integer rothead)
	{
		if (head != null)
		{
			head.move(neck, rothead);
		} else {
			log.error("I have a null head");
		}
	}
	
	public void setHeadSpeed(Float neck, Float rothead)
	{
		head.setSpeed(neck, rothead);
	}

	public void systemCheck() {
		// check arduinos

		mouth.speak("starting system check");
		
		rest();
		/*  TRACING APPEARS TO "MESS" THINGS UP --- POSSIBLY  FIXME
		ArrayList<Arduino> arduinoList = arduinos.get(both);
		for(int i = 0; i < arduinoList.size(); ++i)
		{
			Arduino arduino = arduinoList.get(i);
			arduino.pinMode(17, Arduino.INPUT);
			arduino.analogReadPollingStart(17);
			sleep(250);
			arduino.analogReadPollingStop(17);
		}
		*/
		

		ArrayList<Arm> armList = arms.get(both);
		for(int i = 0; i < armList.size(); ++i)
		{
			Arm arm = armList.get(i);
			
			arm.bicep.moveTo(10);
			arm.rotate.moveTo(100);
			arm.shoulder.moveTo(40);
			arm.omoplate.moveTo(20);
			sleep(1);			
		}
		
		ArrayList<Hand> handList = hands.get(both);
		for(int i = 0; i < armList.size(); ++i)
		{
			Hand hand = handList.get(i);
			hand.moveTo(10, 10, 10, 10, 10, 10);
			sleep(1);

		}		
		
		rest();

		// check servos

		// check ear

		// check mount - all my circuits are functioning perfectly
		mouth.speak("completed system check");
		
		broadcastState();
	}

	public String captureGesture()
	{
		return captureGesture(null);
	}
	
	public String captureGesture(String gestureName)
	{
		StringBuffer script = new StringBuffer();
		
		String indentSpace = "";
		
		if (gestureName != null)
		{
			indentSpace = "  ";
			script.append(String.format("def %s():\n", gestureName));
		}
		
		if (head != null)
		{
			script.append(indentSpace);
			script.append(head.getScript(getName()));
		}	

		ArrayList<Arm> armList = arms.get(both);
		for(int i = 0; i < armList.size(); ++i)
		{
			Arm arm = armList.get(i);
			script.append(indentSpace);
			script.append(arm.getScript(getName()));
		}
		
		ArrayList<Hand> handList = hands.get(both);
		for(int i = 0; i < armList.size(); ++i)
		{
			Hand hand = handList.get(i);
			script.append(indentSpace);
			script.append(hand.getScript(getName()));
		}		
		
		
		send("python", "appendScript", script.toString());
		
		return script.toString();
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	@Override
	public String getToolTip() {
		return "the InMoov Service";
	}
	
	public void startListening(String grammar)
	{
		ear.attach(mouth.getName());
		ear.addListener("recognized", "python", "heard", String.class); 
		ear.createGrammar(grammar);
		ear.startListening();

	}
	
	public void stopListening()
	{
		ear.stopListening();
	}

	boolean isTracking = false;
	
	public void startTracking()
	{
		if (isTracking)
		{
			log.warn("already tracking");
			return;
		}
		
		isTracking = true;
		eye.addFilter("pyramidDown1","PyramidDown");
		eye.addFilter("lkOpticalTrack1","LKOpticalTrack");
		eye.setDisplayFilter("lkOpticalTrack1");
		eye.capture();
		sleep(500);
		eye.invokeFilterMethod("lkOpticalTrack1","samplePoint", 160, 120);
	}
	
	public void stopTracking()
	{
		if (!isTracking)
		{
			log.warn("already stopped tracking");
			return;
		}
			
		isTracking = false;
		eye.removeFilters();
		eye.stopCapture();
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		InMoov inMoov = new InMoov("inMoov");
		inMoov.startService();
		
		//Runtime.createAndStart("python", "Python");
		ServiceInterface si = Runtime.createAndStart("gui", "GUIService");
		si.display();
		
		
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
