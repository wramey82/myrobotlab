package org.myrobotlab.service;

import java.awt.Color;
import java.util.ArrayList;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.openni.PApplet;
import org.myrobotlab.openni.PImage;
import org.myrobotlab.openni.PVector;
import org.myrobotlab.service.interfaces.VideoSink;
import org.slf4j.Logger;

import SimpleOpenNI.SimpleOpenNI;

public class GestureRecognition extends Service // implements
// UserTracker.NewFrameListener,
// HandTracker.NewFrameListener
{

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(GestureRecognition.class);
	SimpleOpenNI context;

	ArrayList<VideoSink> sinks = new ArrayList<VideoSink>();

	// user begin
	Color[] userClr = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.CYAN };
	PVector com = new PVector();
	PVector com2d = new PVector();

	transient Worker worker = null;

	// user end

	public GestureRecognition(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override
	public void startService() {
		super.startService();
		initContext(); // FIXME - manual or auto call to initContext ?
	}

	@Override
	public void stopService() {
		super.stopService();
		closeContext();
	}

	public void initContext() {

		// System.load("C:\\mrlx\\myrobotlab\\libraries\\native\\x86.64.windows\\OpenNI2.dll");

		// String s = SimpleOpenNI.getLibraryPathWin();
		SimpleOpenNI.start();

		SimpleOpenNI.initContext();
		int cnt = SimpleOpenNI.deviceCount();
		info("initContext found %d devices", cnt);

		if (cnt < 1) {
			error("found 0 devices - Jenga software not initialized :P");
		}

		PApplet fake = new PApplet(this);
		context = new SimpleOpenNI(fake);

		// b.init();
		/*
		 * context.enableDepth(); context.enableRGB();
		 * 
		 * int r = 0; while (r < 1000) { r++; context.update(); PImage p =
		 * context.depthImage(); int z = p.get(240, 320); log.info("d{}", z);
		 * 
		 * invoke("publishFrame", new SerializableImage(p.getImage(),
		 * getName()));
		 * 
		 * PImage p2 = context.rgbImage();
		 * 
		 * int z2 = p2.get(240, 320); log.info("r{}", z2); }
		 */

	}

	public void closeContext() {
		stopWorker();
		if (context != null) {
			context.close();
		}
	}

	/**
	 * FIXME - input needs to be OpenCVData THIS IS NOT USED ! VideoProcessor
	 * NOW DOES OpenCVData - this will return NULL REMOVE !!
	 */
	public final SerializableImage publishDisplay(SerializableImage img) {
		// lastDisplay = new SerializableImage(img, source);
		// return lastDisplay;
		return img;
	}

	public SerializableImage publishFrame(SerializableImage frame) {
		log.debug("publishing frame");
		return frame;
	}

	public void add(VideoSink vs) {
		sinks.add(vs);
	}

	public void remove(VideoSink vs) {
		sinks.remove(vs);
	}

	// USER BEGIN ---------------------------------------------

	public void startUserTracking() {
		setUpUser();
		info("starting user worker");
		if (worker != null) {
			stopWorker();
		}
		worker = new Worker("user");
		worker.start();
	}

	public void stopWorker() {
		if (worker != null) {
			info(String.format("stopping worker %s", worker.type));
			worker.isRunning = false;
			worker = null;
		}
	}

	void setUpUser() {

		if (context.isInit() == false) {
			error("Can't init SimpleOpenNI, maybe the camera is not connected!");

			return;
		}

		// enable depthMap generation
		context.enableDepth();

		// enable skeleton generation for all joints
		context.enableUser();

	}

	public class Worker extends Thread {
		public boolean isRunning = false;
		public String type = null;

		public Worker(String type) {
			super(String.format("%s.worker", type));
			this.type = type;
		}

		public void run() {
			try {
				isRunning = true;
				while (isRunning) {
					if ("user".equals(type)) {
						drawUser();
					} else {
						error("unkown worker %s", type);
						isRunning = false;
					}

				}

			} catch (Exception e) {
				Logging.logException(e);
			}
		}
	}

	void drawUser() {
		// update the cam
		context.update();

		// draw depthImageMap
		// image(context.depthImage(),0,0);
		PImage p = context.depthImage();
		invoke("publishFrame", new SerializableImage(p.getImage(), getName()));
		// context.userImage();

		// draw the skeleton if it's available
		int[] userList = context.getUsers();
		for (int i = 0; i < userList.length; i++) {
			if (context.isTrackingSkeleton(userList[i])) {
				// stroke(userClr[(userList[i] - 1) % userClr.length]);
				drawSkeleton(userList[i]);
			}

			// draw the center of mass
			if (context.getCoM(userList[i], com)) {
				context.convertRealWorldToProjective(com, com2d);
				/*
				 * stroke(100, 255, 0); strokeWeight(1); beginShape(LINES);
				 * vertex(com2d.x, com2d.y - 5); vertex(com2d.x, com2d.y + 5);
				 * 
				 * vertex(com2d.x - 5, com2d.y); vertex(com2d.x + 5, com2d.y);
				 * endShape();
				 * 
				 * 
				 * fill(0, 255, 100); text(Integer.toString(userList[i]),
				 * com2d.x, com2d.y);
				 */

				Integer.toString(userList[i]);
			}
		}

	}

	// draw the skeleton with the selected joints
	void drawSkeleton(int userId) {
		log.info(String.format("i would be drawing user %d if i knew how", userId));
		// to get the 3d joint data
		/*
		 * PVector jointPos = new PVector();
		 * context.getJointPositionSkeleton(userId
		 * ,SimpleOpenNI.SKEL_NECK,jointPos); println(jointPos);
		 */

		PVector jointPos = new PVector();
		context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_NECK, jointPos);
		// println(jointPos);
		log.info("jointPos skeleton neck {} ", jointPos);

		context.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);

		context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);

		context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);

		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);

		context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);

		context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);
	}

	// -----------------------------------------------------------------
	// SimpleOpenNI events

	public void onNewUser(SimpleOpenNI context, int userId) {
		info("onNewUser - userId: " + userId);
		info("\tstart tracking skeleton");

		context.startTrackingSkeleton(userId);
	}

	public void onLostUser(SimpleOpenNI curContext, int userId) {
		info("onLostUser - userId: " + userId);
	}

	public void onVisibleUser(SimpleOpenNI curContext, int userId) {
		log.info("onVisibleUser - userId: " + userId);
	}
	
	public void onOutOfSceneUser(SimpleOpenNI curContext, int userId){
		log.info("onOutOfSceneUser - userId: " + userId);
		
	}

	public void onNewHand(SimpleOpenNI openni, int userId, PVector v) {
		log.info("onVisibleUser - userId: " + userId);

	}

	// USER END ---------------------------------------------

	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();

		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");

		GestureRecognition gr = new GestureRecognition("gr");
		gr.startService();
		gr.startUserTracking();
	}

}
