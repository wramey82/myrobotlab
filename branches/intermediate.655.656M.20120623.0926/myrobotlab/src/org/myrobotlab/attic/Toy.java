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

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.RobotPlatform;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Graphics;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.OpenCV.Polygon;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.data.PinState;

import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

public class Toy extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Toy.class.getCanonicalName());

	public Arduino arduino = new Arduino("arduino");
	public Motor left = new Motor("left");
	public Motor right = new Motor("right");
	public RobotPlatform platform = new RobotPlatform("platform");	
	public GUIService gui = new GUIService("gui");
	public OpenCV camera = new OpenCV("camera");
	public Graphics graphics = new Graphics("graphics");
	public Servo jaw = new Servo("jaw"); // pin 13
	public Servo neck = new Servo("neck"); // pin 12
			
	float powerRight = 0.13f;
	float powerLeft  = 0.16f;
	
	// TODO bury in platform
	float pixelsPerInch = 12.2f;
	int cameraDistanceFromFloorInches = 49;
	
	// TODO - bury in platform - absolute vs relative pos
	//CvPoint2D32f[] lastToyPosition = new CvPoint2D32f[3];
	// arrays handled internally
	CvPoint2D32f lastToyPosition = new CvPoint2D32f(3);
	CvPoint2D32f toyPoint = new CvPoint2D32f(); 
	CvPoint2D32f toyCenteroid = new CvPoint2D32f(); 

	// a single focal point - attention is on only one object at a time
	Target currentTarget = null;
	
	int bottomSegment = 0;

	float newHeading = 0;
	int lastHeading = 0;	

	public ArrayList<TargetCriteria> thingiesToLookFor = new ArrayList<TargetCriteria>();

	// Objects of Interest Oi will continue to have indexes and relationships	
	public HashMap<String,Target> targets = new HashMap<String,Target>();

	Object foundLock = new Object();
	public class ThingyFinder implements Runnable
	{

		@Override
		public void run() {
			try {
					for (int i = 0; i < thingiesToLookFor.size(); ++i)
					{
						findThingy(thingiesToLookFor.get(i));
						synchronized (foundLock) {
							log.error("waiting for things to look for ");
							foundLock.wait(5000); // wait for a second or found
						}					
						clearFilters();
					}
					
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	public class TargetCriteria
	{
		public String type;
		public int hueMin;		
		public int hueMax;
		public int saturationMin;		
		public int saturationMax;
		public int valueMin;		
		public int valueMax;
		public int minArea;		
		public int maxArea;
		
		public TargetCriteria (String type, int hueMin, int hueMax, int saturationMin, int saturationMax, int valueMin, int valueMax, int minArea, int maxArea)
		{
			this.type = type;
			this.hueMin = hueMin;
			this.hueMax = hueMax;
			this.saturationMin = saturationMax;
			this.saturationMax = saturationMax;
			this.valueMin = valueMin;
			this.valueMax = valueMax;
			this.minArea = minArea;
			this.maxArea = maxArea;
		}
		
	}
	
	public class Target {
		public String name;
		public String type;
		public Point centeroid = null;
		public int size;
		public int bearing;
		public float distance;
		public int vertices;
		public boolean isConvex;
		public Color avgColor;
		
		public Target(String name, String type, int x, int y, int size, int bearing, float distance, int vertices, boolean isConvex, Color avgColor)
		{
			this.name = name;
			this.type = type;
			this.size = size;
			this.bearing = bearing;
			this.distance = distance;
			this.centeroid = new Point(x,y);
			this.vertices = vertices;
			this.isConvex = isConvex;
			this.avgColor = new Color(avgColor.getRed(), avgColor.getGreen(), avgColor.getBlue());
		}
	}
			
	public Toy(String n) {
		this(n, null);
	}

	public Toy(String n, String serviceDomain) {
		super(n, Toy.class.getCanonicalName(), serviceDomain);
	}

	@Override
	public void loadDefaultConfiguration() {
	}
	

	public void yesYes()
	{
		try {
			// quick down yes
			neck.moveTo(120);
			Thread.sleep(340);
			neck.moveTo(160);
			Thread.sleep(140);
			neck.moveTo(120);
			Thread.sleep(140);
			neck.moveTo(160);
			Thread.sleep(340);
			neck.moveTo(90);
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	public void noNo()
	{
		try {

			// quick down no
			neck.moveTo(120);
			Thread.sleep(340);
			neck.moveTo(160);
			Thread.sleep(140);
			neck.moveTo(90);
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	

	public void jawsOpen()
	{
		jaw.moveTo(160);
	}	

	public void jawsClose()
	{
		jaw.moveTo(90);
	}	
	

	public void neckUp()
	{
		neck.moveTo(50);
	}	

	public void neckDown()
	{
		neck.moveTo(120);
	}	

	public void attach() 
	{
		try {
			
			right.attach(arduino.getName(), 3, 11); 
			Thread.sleep(160);
			left.attach(arduino.getName(), 6, 7);
			Thread.sleep(160);
			arduino.pinMode(7, PinState.OUTPUT);
			Thread.sleep(160);
			arduino.pinMode(11, PinState.OUTPUT);
			Thread.sleep(160);
			arduino.pinMode(12, PinState.OUTPUT);
			Thread.sleep(160);
			arduino.pinMode(13, PinState.OUTPUT);
			
			platform.attach(left, right);
			
			neck.attach(arduino.getName(),12);
			Thread.sleep(160);
			jaw.attach(arduino.getName(),13);
			Thread.sleep(80);
			right.move(0);
			left.move(0);
						
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//jaw.attach(arduino.getName(), 13);
	}
	
	
	public void findToy()
	{
		camera.removeFilters();

		camera.addFilter("LKOpticalTrack", "LKOpticalTrack");
		camera.setFilterCFG("InRange", "useValue", true);

		
		// set addListener foundSomething
		camera.addListener("publish", getName(), "foundToy", Polygon.class);
				
	}
		

	
	public void foundToy (CvPoint2D32f trackingPoints[])
	{
		if (trackingPoints.length == 3)
		{			
			// erase graphics
//			graphics.setColor(Color.gray);
//			graphics.drawString((int)toyCenteroid.x + "," + (int)toyCenteroid.y() + "h " + (int)newHeading, (int)toyCenteroid.x + 10, (int)toyCenteroid.y());


			if (bottomSegment == 0)
			{
				graphics.drawLine((int)lastToyPosition.position(0).x(), (int)lastToyPosition.position(0).y(), (int)lastToyPosition.position(1).x(), (int)lastToyPosition.position(1).y());	
				graphics.drawLine((int)toyPoint.x(), (int)toyPoint.y(), (int)lastToyPosition.position(2).x(), (int)lastToyPosition.position(2).y());	
			} else if (bottomSegment == 1)
			{
				graphics.drawLine((int)lastToyPosition.position(1).x(), (int)lastToyPosition.position(1).y(), (int)lastToyPosition.position(2).x(), (int)lastToyPosition.position(2).y());				
				graphics.drawLine((int)toyPoint.x(), (int)toyPoint.y(), (int)lastToyPosition.position(0).x(), (int)lastToyPosition.position(0).y());	
			} else {
				graphics.drawLine((int)lastToyPosition.position(2).x(), (int)lastToyPosition.position(2).y(), (int)lastToyPosition.position(0).x(), (int)lastToyPosition.position(0).y());				
				graphics.drawLine((int)toyPoint.x(), (int)toyPoint.y(), (int)lastToyPosition.position(1).x(), (int)lastToyPosition.position(1).y());					
			}
							
			// TODO persist configuration - so appropriate segment is always subsequently chosen & 
			// get segment lengths
			int seg0 = (int) (Math
					.sqrt((trackingPoints[0].x() - trackingPoints[1].x())
							* (trackingPoints[0].x() - trackingPoints[1].x())
							+ (trackingPoints[0].y() - trackingPoints[1].y())
							* (trackingPoints[0].y() - trackingPoints[1].y())));

			int seg1 = (int) (Math
					.sqrt((trackingPoints[1].x() - trackingPoints[2].x())
							* (trackingPoints[1].x() - trackingPoints[2].x())
							+ (trackingPoints[1].y() - trackingPoints[2].y())
							* (trackingPoints[1].y() - trackingPoints[2].y())));

			int seg2 = (int) (Math
					.sqrt((trackingPoints[2].x() - trackingPoints[0].x())
							* (trackingPoints[2].x() - trackingPoints[0].x())
							+ (trackingPoints[2].y() - trackingPoints[0].y())
							* (trackingPoints[2].y() - trackingPoints[0].y())));


			// find shortest seg
			graphics.setColor(Color.white);			
			if (seg0 < seg1 && seg0 < seg2)
			{
				toyPoint.x((trackingPoints[0].x() + trackingPoints[1].x())/2);
				toyPoint.y((int)(trackingPoints[0].y() + trackingPoints[1].y())/2);
				graphics.drawLine((int)trackingPoints[0].x(), (int)trackingPoints[0].y(), (int)trackingPoints[1].x(), (int)trackingPoints[1].y());	
				toyCenteroid.x((toyPoint.x() + trackingPoints[2].x())/2);
				toyCenteroid.y((toyPoint.y() + trackingPoints[2].y())/2);
				graphics.drawLine((int)toyPoint.x(), (int)toyPoint.y(), (int)trackingPoints[2].x(), (int)trackingPoints[2].y());	
				newHeading = (float)Math.toDegrees(Math.atan2(trackingPoints[2].x() - toyPoint.x(), trackingPoints[2].y() - toyPoint.y()));
				bottomSegment = 0;
			} else if (seg1 < seg0 && seg1 < seg2)
			{
				toyPoint.x((trackingPoints[1].x() + trackingPoints[2].x())/2);
				toyPoint.y((int)(trackingPoints[1].y() + trackingPoints[2].y())/2);
				graphics.drawLine((int)trackingPoints[1].x(), (int)trackingPoints[1].y(), (int)trackingPoints[2].x(), (int)trackingPoints[2].y());				
				toyCenteroid.x((toyPoint.x() + trackingPoints[0].x())/2);
				toyCenteroid.y((toyPoint.y() + trackingPoints[0].y())/2);
				graphics.drawLine((int)toyPoint.x(), (int)toyPoint.y(), (int)trackingPoints[0].x(), (int)trackingPoints[0].y());	
				newHeading = (float)Math.toDegrees(Math.atan2(trackingPoints[0].x() - toyPoint.x(), trackingPoints[0].y() - toyPoint.y()));
				bottomSegment = 1;
			} else{
				toyPoint.x((trackingPoints[2].x() + trackingPoints[0].x())/2);
				toyPoint.y((int)(trackingPoints[2].y() + trackingPoints[0].y())/2);
				graphics.drawLine((int)trackingPoints[2].x(), (int)trackingPoints[2].y(), (int)trackingPoints[0].x(), (int)trackingPoints[0].y());				
				toyCenteroid.x((toyPoint.x() + trackingPoints[1].x())/2);
				toyCenteroid.y( (toyPoint.y() + trackingPoints[1].y())/2);
				graphics.drawLine((int)toyPoint.x(), (int)toyPoint.y(), (int)trackingPoints[1].x(), (int)trackingPoints[1].y());	
				newHeading = (float)Math.toDegrees(Math.atan2(trackingPoints[1].x() - toyPoint.x(), trackingPoints[1].y() - toyPoint.y()));
				bottomSegment = 2;
			}
			
//			graphics.setColor(Color.white);
//			graphics.drawString((int)toyCenteroid.x() + "," + (int)toyCenteroid.y() + "h " + (int)newHeading, (int)toyCenteroid.x() + 10, (int)toyCenteroid.y());

			graphics.setColor(Color.red);
			graphics.drawLine((int)toyCenteroid.x(), (int)toyCenteroid.y(), (int)toyCenteroid.x(), (int)toyCenteroid.y());

			if ((int)lastHeading != (int)newHeading)
			{
				// TODO make interface listener - add listener - 
				// FEEDBACK here
				platform.setHeading((int)newHeading);
				// not quite right - if staying on the same heading you could fail to update position
				platform.setPosition((int)toyCenteroid.x(), (int)toyCenteroid.y());
				
			}				
			
			lastToyPosition.position(0).x(trackingPoints[0].x());
			lastToyPosition.position(0).y(trackingPoints[0].y());
			lastToyPosition.position(1).x(trackingPoints[1].x());
			lastToyPosition.position(1).y(trackingPoints[1].y());
			lastToyPosition.position(2).x(trackingPoints[2].x());
			lastToyPosition.position(2).y(trackingPoints[2].y());
			
			// TODO verify dimensions angle and lengths
			
		} else {
			log.warn("expecting 3 tracking points " + trackingPoints.length);			
		}
		
		graphics.refreshDisplay();
	}	
	
	
	public void findThingy (TargetCriteria t)
	{
		findThingy(t.type, t.hueMin, t.hueMax, t.saturationMin, t.saturationMax, t.valueMin, t.valueMax, t.minArea, t.maxArea);
	}
	
	public void findThingy (String thingy, int hueMin, int hueMax, int saturationMin, int saturationMax, int valueMin, int valueMax, int minArea, int maxArea)
	{
		log.error("looking for " + thingy);
		// set the name and color globally - then try a search
		// if the search is successful the name and color will be associated with the target(s) found
		currentlyLookingForName = thingy;
		// yellow 238 253 15  RGB = hex EEFD0F == HSV 45 240 253 = 2DF0FD
		// 
		// TODO : WARNING - TOTAL CONFUSION - Camera is BGR so values have to be swapped !
//		int temp = Color.HSBtoRGB((hueMin+hueMax)/2, (saturationMin+saturationMax)/2, (valueMin+valueMax)/2);
		float h = (float)(hueMin+hueMax)/512;
		float s = (float)(saturationMin+saturationMax)/512;
		float v = (float)(valueMin+valueMax)/512;
		int temp = Color.HSBtoRGB(h, s, v);
		Color c = new Color(temp); //BGR to RGB yech
		//currentlyLookingForColorAvg = new Color(temp);
		currentlyLookingForColorAvg = c;
		//currentlyLookingForColorAvg = new Color((hueMin+hueMax)/2, (saturationMin+saturationMax)/2, (valueMin+valueMax)/2);
		
		camera.removeFilters();

		camera.addFilter("Smooth", "Smooth");
		camera.addFilter("Dilate1", "Dilate");
		camera.addFilter("Dilate2", "Dilate");
		camera.addFilter("Smooth1", "Smooth");
		
		camera.addFilter("InRange", "InRange");		

		camera.setFilterCFG("InRange", "useHue", true);
		camera.setFilterCFG("InRange", "hueMin", hueMin);
		camera.setFilterCFG("InRange", "hueMax", hueMax);

		camera.setFilterCFG("InRange", "useSaturation", true);
		camera.setFilterCFG("InRange", "saturationMin", saturationMin);
		camera.setFilterCFG("InRange", "saturationMax", saturationMax);

		camera.setFilterCFG("InRange", "useValue", true);
		camera.setFilterCFG("InRange", "valueMin", valueMin);
		camera.setFilterCFG("InRange", "valueMax", valueMax);
		
		camera.addFilter("FindContours", "FindContours");
		camera.setFilterCFG("FindContours", "minArea", minArea);
		camera.setFilterCFG("FindContours", "maxArea", maxArea);
		camera.setFilterCFG("FindContours", "useMaxArea", true);
		camera.setFilterCFG("FindContours", "useMinArea", true);
				
		// set addListener foundSomething
		camera.addListener("publish", this.getName(), "foundThingy", Polygon.class);

	}
	
	String currentlyLookingForName = "";
	Color currentlyLookingForColorAvg = null;
	
	public void foundThingy (ArrayList<Polygon> polygons)
	{
		graphics.setColor(currentlyLookingForColorAvg); // hmm avgColor

		//log.error("found " + polygons.size() + " " + currentlyLookingForName);
		
		for (int i = 0; i < polygons.size(); ++i)
		{
			Polygon p = polygons.get(i);

			int x = p.centeroid.x();
			int y = p.centeroid.y();
			int width = p.boundingRectangle.width();
			int height = p.boundingRectangle.height();
			
			//graphics.drawRect(p.boundingRectangle.x(), p.boundingRectangle.y(),  p.boundingRectangle.width, p.boundingRectangle.height);
			if (p.vertices < 8)
			{
				graphics.fillRect(p.boundingRectangle.x(), p.boundingRectangle.y(),  p.boundingRectangle.width(), p.boundingRectangle.height());
			} else {
				graphics.fillOval(p.boundingRectangle.x(), p.boundingRectangle.y(),  p.boundingRectangle.width(), p.boundingRectangle.height());
			}
			
			int h = (int)Math.toDegrees(Math.atan2(x - toyPoint.x(), y - toyPoint.y()));
			int d = (int)(Math.sqrt((x-toyPoint.x())*(x-toyPoint.x()) + (y-toyPoint.y())*(y-toyPoint.y()))/pixelsPerInch); 
			
			targets.put(currentlyLookingForName + i, new Target(currentlyLookingForName + " " + i, currentlyLookingForName, x, y, width * height, h, d, p.vertices, p.isConvex, currentlyLookingForColorAvg));
		}
		
		if (polygons.size() > 0) // TODO optimization OpenCV - only send > 0 polys
		{
			// if i found something - let the finder know
			synchronized (foundLock) {
				log.error(Thread.currentThread().getName() + " found " + polygons.size() + " " + currentlyLookingForName);
				foundLock.notifyAll();
			}
			
		}
		
		graphics.refreshDisplay();
		
	}

	
	public void clearFilters()
	{
		camera.removeFilters();
		//camera.removeListener();
		camera.removeListener("publish", getName(), "foundThingy", Polygon.class);
		camera.removeListener("publish", getName(), "foundToy", Polygon.class);

	}
	
	
	public void startRobot() {

		arduino.startService();
		gui.startService();
		jaw.startService();
		neck.startService();
		left.startService();
		right.startService();
		camera.startService();
		graphics.startService();
		platform.startService();
		graphics.attach(gui.getName());
		gui.display();

		camera.setInpurtSource("camera");
		camera.capture();
		
		right.invertDirection();
		
		// prepare graphics screen
		graphics.createGraph(640, 480);
		graphics.setColor(Color.gray);
		graphics.fillRect(0, 0, 640, 480);
		
		// last position of toy
		//lastToyPosition.position(0) = new CvPoint2D32f();
		//lastToyPosition.position(1) = new CvPoint2D32f();
		//lastToyPosition.position(2) = new CvPoint2D32f();
		
		thingiesToLookFor.add(new TargetCriteria("yellow blocks", 26, 37, 160, 256, 220, 256, 120, 1600));
		thingiesToLookFor.add(new TargetCriteria("red blocks", 0, 12, 224, 256, 183, 222, 120, 1600));
		thingiesToLookFor.add(new TargetCriteria("blue blocks", 109, 140, 72, 160, 63, 164, 120, 1600));
		thingiesToLookFor.add(new TargetCriteria("laptop", 12, 29, 50, 121, 145, 211, 2000, 20600));
		thingiesToLookFor.add(new TargetCriteria("orange", 12, 19, 228, 256, 198, 241, 110, 1800));
		thingiesToLookFor.add(new TargetCriteria("red car", 77, 121, 80, 237, 59, 256, 400, 81800));
		
	}
	


	// TODO - do in Service
	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		Toy toy = new Toy("toy");
		toy.startService();
		toy.startRobot();
	}
	
	
	public void displayTargets()
	{
		log.error("display " + targets.size() + " targets");
		Iterator<String> it = targets.keySet().iterator();
		graphics.setColor(Color.green);
		while (it.hasNext()) {
			String key = (String) it.next();
			Target t = targets.get(key);
			currentTarget = t;
			graphics.drawLine(t.centeroid.x, t.centeroid.y, (int)toyPoint.x(), (int)toyPoint.y());
			graphics.drawString(t.name, t.centeroid.x + 10, t.centeroid.y);
			graphics.drawString("position " + t.centeroid.x + "," + t.centeroid.y + " size " + t.size, t.centeroid.x + 10, t.centeroid.y + 10);
			graphics.drawString("bearing " + t.bearing + " distance " + t.distance, t.centeroid.x + 10, t.centeroid.y + 20);
			graphics.drawString("vertices " + t.vertices + " convex " + t.isConvex + 
					" avg color " + t.avgColor.getRed() + " " + t.avgColor.getGreen() + " " + t.avgColor.getBlue(), t.centeroid.x + 10, t.centeroid.y + 30);

		}		
		
		graphics.refreshDisplay();
	}
	
	public void clearTarget()
	{
		targets.clear();
	}

	public void keyPressed(Integer cmd) {
		
		switch (cmd) {

		
		case 32: // space
			platform.stop();
			break;

		case 49: // '1'
			jawsOpen();
			break;

		case 50: // '2'
			jawsClose();
			break;

		case 51: // '3'
			neckUp();
			break;

		case 52: // '4'
			neckDown();
			break;
			
		case 59: // ';'
			platform.incrementRightPower(-powerRight);
			break;
			
		case 65: // 'a'
			attach();
			break;

		case 66: // 'b'
			//send(platform.getName(), "calibrate");
			//platform.setTargetHeading(-30);
			//right.moveFor(0.14f, 40*200); -> < 180
			//right.moveFor(0.16f, 1*200); // good 1 degree
			//right.moveFor(0.20f, 1*200);
			
			// near straight line
//			left.moveFor(0.22f, 40*200);
//			right.moveFor(0.18f, 40*200);
			
//			right.moveFor(0.30f, 1*200); // 3 degrees
			
//			right.moveFor(0.50f, 1*200);
//			left.moveFor(-0.50f, 1*200);
			
			break;

		case 67: // 'c'
			clearFilters();
			break;

		case 68: // 'd'
			break;
			
		case 70: // 'f'
			//platform.move(0.20f);
			break;

		case 71: // 'g'
			break;

		case 74: // 'j'
			jawsOpen();
			break;

		case 75: // 'k'
			jawsClose();
			break;
			
		case 76: // 'l'
			platform.incrementLeftPower(-powerLeft);
			break;

		case 78: // 'n'
			//noNo();
			//yesYes();
			findThingy ("tennis ball", 0, 255, 0, 255, 0, 255, 400, 1800);
			break;

		case 79: // 'o'
			platform.incrementLeftPower(powerLeft);
			break;

		case 80: // 'p'
			platform.incrementRightPower(powerRight);
			break;

		case 81: // 'q'
			noNo();
			break;
			
		case 82: // 'r'
			platform.spinRight(0.2f);
			break;

		case 84: // 't'oy
			findToy();
			break;
			
		case 85: // 'u'
			//left.unLock();
			//right.unLock();
			displayTargets();
			break;

		case 87: // 'w'
			//yesYes();
			graphics.setColor(Color.gray);
			graphics.fillRect(0, 0, 640, 480);
			graphics.refreshDisplay();
			break;

		case 88: // 'x'
			displayTargets();
			if (currentTarget != null)
			{
				//platform.setHeading(currentTarget.bearing);
				platform.setTargetHeading(currentTarget.bearing);
				platform.setTargetPosition(currentTarget.centeroid.x, currentTarget.centeroid.y);
				platform.startPID();
			}
			break;
			
		case 89: // 'y'

			// daylight - sun 11:05 am
			//findThingy ("yellow blocks", 30, 40, 182, 256, 183, 224, 120, 1600);
			//findThingy ("yellow blocks", 26, 37, 160, 256, 183, 256, 120, 1600); // diff
			//findThingy ("red blocks", 0, 12, 224, 256, 146, 222, 120, 1600);
			//findThingy ("blue blocks", 109, 140, 72, 160, 26, 164, 120, 1600);
			  
			// artifical light - "constant"			
			//findThingy ("yellow blocks", 26, 37, 160, 256, 220, 256, 120, 1600);
			//findThingy ("red blocks", 0, 12, 224, 256, 183, 222, 120, 1600);
			//findThingy ("blue blocks", 109, 140, 72, 160, 63, 164, 120, 1600);
			//findThingy ("laptop", 12, 29, 50, 121, 145, 211, 2000, 20600);
			//findThingy ("orange", 12, 19, 228, 256, 198, 241, 110, 1800);
			
			
			//thingiesToLookFor.add(new TargetCriteria("orange", 12, 19, 228, 256, 198, 241, 110, 1800));
			Thread t = new Thread (new ThingyFinder(), "thingyFinder");
			t.start();
			//displayTargets();
			break;

		case 90: // 'z'
			//yes();
			//findLeaningPad();
			clearTarget();
			break;
			
			default:
				log.error("unknown cmd " + cmd);
				break;
				
		}
	}

	@Override
	public String getToolTip() {
		return "<html>Bug Toy and Audrey - see http://myrobotlab.org/node/74</html>";
	}
	
	
}
