/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.attic;

import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.OpenCV.Polygon;
import org.myrobotlab.service.Speech;
import org.myrobotlab.service.Sphinx;

// http://ai.eecs.umich.edu/cogarch0/subsump/arch.html  Augmented Finite State Machines
// http://www.ibm.com/developerworks/java/library/j-robots/ 
// timers - input, output, inhibitor
// behaviors - observing, checking learning pad, categorizing, associating, moving toy, processing speech command (supresses) observing 
// inputs - speech commands, visual information (change)

public class Audrey extends Service {

	public final static Logger log = Logger.getLogger(Audrey.class.getCanonicalName());

	//RemoteAdapter remote = new RemoteAdapter("remote");
	//Servo servo = new Servo("servo");
	// TODO - http://www.tolearnenglish.com/free/celebs/audreyg.php
	// Behaviors - Observing, Reporting, Moving Toy (confirmation), Taking command to move, 
	// States - looking for yellow blocks | looking for learning pad | looking for blue blocks | looking for new things | asking questions about things | making relationships with things 
	// 
	private static final long serialVersionUID = 1L;

	Sphinx ear = new Sphinx("ear");
	Speech mouth = new Speech("mouth");

	Arduino arduino = new Arduino("arduino");
	GUIService gui = new GUIService("gui");
	OpenCV camera = new OpenCV("camera");
	
	Motor left = new Motor("left");
	Motor right = new Motor("right");

	Random generator = new Random();
	
	// world-> (location-heading) myView -> objects in view / objects in memory not in view 
	
	public Audrey(String n) {
		this(n, null);
	}

	public Audrey(String n, String serviceDomain) {
		super(n, Audrey.class.getCanonicalName(), serviceDomain);
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	boolean go = false;
	

	//-----------------------------FINDERS BEGIN-----------------------------------//
	// TODO - at some point this should be loaded from serialized form
	// In addition - a findObjects should be created and the robot turned into an inquisitor - what is this blob - then get name
	// association from google image database, dictionary.com, etc.
	
	public void foundSomething (ArrayList<Polygon> p)
	{
/*
		if (currentlyLookingForObject != null)
		{
			
		}
*/		
		// remove addListener
	}
	
	public void findYellowBlocks()
	{
		// yellow block
		camera.removeFilters();

		camera.addFilter("Dilate1", "Dilate");
		
		camera.addFilter("InRange", "InRange");		

		camera.setFilterCFG("InRange", "useHue", true);
		camera.setFilterCFG("InRange", "hueMin", 81);
		camera.setFilterCFG("InRange", "hueMax", 94);

		camera.setFilterCFG("InRange", "useSaturation", true);
		camera.setFilterCFG("InRange", "saturationMin", 138);
		camera.setFilterCFG("InRange", "saturationMax", 245);

		camera.setFilterCFG("InRange", "useValue", true);
		camera.setFilterCFG("InRange", "valueMin", 245);
		camera.setFilterCFG("InRange", "valueMax", 256);

		camera.addFilter("Dilate", "Dilate");		
		
		camera.addFilter("FindContours", "FindContours");
		camera.setFilterCFG("FindContours", "minArea", 10);
		camera.setFilterCFG("FindContours", "maxArea", 180);
		camera.setFilterCFG("FindContours", "useMaxArea", true);
		camera.setFilterCFG("FindContours", "useMinArea", true);
		

		
		// set addListener foundSomething
		camera.addListener("publish", getName(), "foundYellowBlocks", Polygon.class);
		
	}

	public void foundYellowBlocks (ArrayList<Polygon> p)
	{
		// increment blocks
		log.info(p);
	}	
	// Override - 
	//public void foundYellowBlocks()
	
	public void findToys()
	{
		
	}
	//-----------------------------FINDERS END -----------------------------------//
	
	public void startRobot() {
		
		mouth.getCFG().set("isATT", true);

		// suppress listening when talking
		mouth.addListener("started", ear.getName(), "stopRecording"); // TODO speak.queue()
		mouth.addListener("stopped", ear.getName(), "startRecording");
		
		// creating static route from ear/speech recognition to special action
		ear.addListener("recognized", this.getName(), "speechToAction", String.class);
		
		// starting services
		mouth.startService();
		right.startService();
		left.startService();
		arduino.startService();
		camera.startService();
		gui.startService();
//		ear.start();

		gui.display();

		//right.attach(arduino.getName(), 5, 12);
		//left.attach(arduino.getName(), 6, 13);
		
		//left.invertDirection();
		
		cameraOn();
		
		// set message path for polygons
		//camera.addListener("publish", name, "publish", Polygon.class.getCanonicalName());
		
		//observer = new Thread(new Observer());
		//observer.start();
		
		
		mouth.speak("I am ready");
		
		mouth.speak("yes Grog I am listening");
		mouth.speak("I can see");
		mouth.speak("3 yellow blocks");
		mouth.speak("2 green blocks");
		mouth.speak("and my bug toy");
		
		mouth.speak("yes I can move my bug toy");
		
		mouth.speak("my bug toy will not move, does it need new batteries?");
		
		/*
		mouth.speak("I am ready now");
		mouth.speak("now I am ready, dood");
		mouth.speak("hello Greg");
		mouth.speak("yes Grog I am listening");
		mouth.speak("yes, Greg");
		mouth.speak("hello Grog");
		mouth.speak("yes Grog");
		mouth.speak("yes, Grog");
		*/

	}

	int speedIncrement = 5; // TODO - put this in GUI

	public void speechToAction(String speech) {
		if (speech.compareTo("audrey") == 0) {
			mouth.speak("yes Grog, I am listening");
		} else if (speech.compareTo("tell me what you see") == 0) {
			
			ear.stopRecording();
			mouth.speak("Yes I will look");
			
			// start observer thread
			
			// lookAtBoard();
			// mouth.speak("board");
		} else if ((speech.compareTo("stop") == 0)
				|| (speech.compareTo("halt") == 0)) {
			// lookAtBoard();
			left.stopAndLock();
			right.stopAndLock();
			// mouth.speak("locked");
		} else if (speech.compareTo("go") == 0) {
			//left.incrementPower(0.3f);
			//right.incrementPower(0.3f);
			// center();
		} else if (speech.compareTo("center") == 0) {
			// center();
			// mouth.speak("center");
		} else if (speech.compareTo("camera on") == 0) {
			// cameraOn();
			// mouth.speak("looking");
		} else if (speech.compareTo("camera off") == 0) {
			// mouth.speak("my eyes are closed");
			// cameraOff();
		} else if (speech.compareTo("watch") == 0) {
			// mouth.speak("looking");
			// filterOn();
		} else if (speech.compareTo("find") == 0) {
			// report();
		} else if (speech.compareTo("left") == 0) {
			// report();
			//left.incrementPower(0.1f);
		} else if (speech.compareTo("right") == 0) {
			// report();
			//right.incrementPower(0.1f);
		} else if (speech.compareTo("clear") == 0) {
			// filterOff();
		} else {
			// mouth.speak("what did you say");
		}
	}

	public void cameraOn() {
		camera.setInpurtSource("camera");
		camera.capture();
	}

	public void cameraOff() {
		camera.setInpurtSource("null");
		camera.capture();
	}

	float amt = 0.03f;

	public void keyPressed(Integer cmd) {
		
		switch (cmd) {
		
		// Toy stuff
		case 104: // numpad 8
			//right.incrementPower(amt);
			break;
			
		case 101: // numpad 5
			//right.incrementPower(-amt);
			break;

		case 103: // numpad 7
			//left.incrementPower(amt);
			break;
			
		case 100: // numpad 4
			//left.incrementPower(-amt);
			break;

		case 32: // space
			left.stopAndLock();
			right.stopAndLock();
			go = false;
			break;

		case 85: // 'u'
			left.unLock();
			right.unLock();
			break;

		case 70: // 'f'
			mouth.speak("I am adding camera filters");
			camera.addFilter("Smooth", "Smooth");
			camera.addFilter("Dilate1", "Dilate"); 
			camera.addFilter("InRange","InRange");
			camera.addFilter("Dilate2", "Dilate");			
			camera.addFilter("FindContours", "FindContours");			
			break;

		case 89: // 'y'
			mouth.speak("I am looking for yellow blocks");
			findYellowBlocks();
			break;
			
		case 82: // 'r'
			// red block
			mouth.speak("I am looking for red blocks");
			camera.setFilterCFG("InRange", "hueMin", 0xb0);
			camera.setFilterCFG("InRange", "hueMax", 0xb3);
			camera.setFilterCFG("InRange", "valueMin", 0xae);
			camera.setFilterCFG("InRange", "valueMax", 0xb0);
			break;

		case 66: // 'b'
			// blue block
			mouth.speak("I am looking for blue blocks");
			camera.setFilterCFG("InRange", "hueMin", 0x6e);
			camera.setFilterCFG("InRange", "hueMax", 0x70);
			camera.setFilterCFG("InRange", "valueMin", 0xa4);
			camera.setFilterCFG("InRange", "valueMax", 0xad);
			break;
			
		case 71: // 'g'
			// green led
			go = true;
			/*
			mouth.speak("I am looking for green L E Dees");
			camera.setFilterCFG("InRange", "hueMin", 0x54);
			camera.setFilterCFG("InRange", "hueMax", 0x5b);
			camera.setFilterCFG("InRange", "valueMin", 0xfd);
			camera.setFilterCFG("InRange", "valueMax", 0xff);
			*/
			break;

		case 67: // c
			// clear/remove filters
			mouth.speak("removing filters");
			camera.removeFilters();
			break;

		case 72: // 'h'
			// clear/remove filters
			mouth.speak("applying H S V");
			camera.removeFilters();
			camera.addFilter("Smooth", "Smooth");
			camera.addFilter("Dilate1", "Dilate"); 
			camera.addFilter("HSV","HSV");
			break;

		case 73: // 'i'
			// forward
			//left.move(1, 0.4f);
			//right.move(1, 0.4f);
			break;

		case 75: // 'k'
			// backword
			//left.move(0, 0.4f);
			//right.move(0, 0.4f);
			break;
			
		case 76: // 'l'
//			observer = new Observer();
//			observer.start();
			break;
			
		default:
			mouth.speak("sorry, I do not do " + cmd);
			break;
			
		}

	}

	// TODO - do in Service
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		Audrey audrey = new Audrey("audrey");
		audrey.startService();
		audrey.startRobot();
	}

	public abstract class Behavior
	{
		public abstract boolean isActive ();
		
		public abstract void act();
	}
	
	
	int currentBehaviorIndex = 0;
	private Behavior[] behaviors;
	private boolean suppresses[][];
	protected void performBehavior() {
		   boolean isActive[] = new boolean[behaviors.length];
		   for (int i = 0; i < isActive.length; i++) {
		      isActive[i] = behaviors[i].isActive();
		   }
		   boolean ranABehavior = false;
		   while (!ranABehavior) {
		      boolean runCurrentBehavior = isActive[currentBehaviorIndex];
		      if (runCurrentBehavior) {
		         for (int i = 0; i < suppresses.length; i++) {
		            if (isActive[i] && suppresses[i][currentBehaviorIndex]) {
		               runCurrentBehavior = false;

		               break;
		            }
		         }
		      }

		      if (runCurrentBehavior) {
		         if (currentBehaviorIndex < behaviors.length) {
		        	 behaviors[currentBehaviorIndex].act();
		        	 /*
		            Velocities newVelocities = behaviors[currentBehaviorIndex].act();
		            this.setTranslationalVelocity(newVelocities
		                  .getTranslationalVelocity());
		            this
		                  .setRotationalVelocity(newVelocities
		                        .getRotationalVelocity());
		                        */
		         }
		         ranABehavior = true;
		      }

		      if (behaviors.length > 0) {
		         currentBehaviorIndex = (currentBehaviorIndex + 1)
		               % behaviors.length;
		      }
		   }
		}	
	
	@Override
	public String getToolTip() {		
		return "<html>Audrey is a test behavioral service, used to interface and control bug toy.<br>" +
		"for more information see http://myrobotlab.org/node/74</html>";
	}
	
	
}

