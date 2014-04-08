package org.myrobotlab.openni;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenNI;
import org.slf4j.Logger;

import SimpleOpenNI.SimpleOpenNI;

public class PApplet {
	// FIXME - EXTRACT INTERFACE ----
	private OpenNI gr = null;

	public Object g;
	public final static Logger log = LoggerFactory.getLogger(PApplet.class);
	
	public PApplet(OpenNI gr) {
		this.gr = gr;
	}

	public String dataPath(String recordPath) {
		log.info("dataPath");
		return null;
	}

	public void registerDispose(SimpleOpenNI simpleOpenNI) {
		log.info("registerDispose");
	}

	public void createPath(String path) {
		log.info("createPath");
	}

	public void line(Object x, Object y, Object x2, Object y2) {
		log.info(String.format("line %f %f %f %f", x, y, x2, y2));
		gr.line((Float)x, (Float)y, (Float)x2, (Float)y2);
	}

	static public final float sqrt(float a) {
		return (float) Math.sqrt(a);
	}
	
	// FIXME - EXTRACT USER INTERFACE BEGIN ----
	public void onNewUser(SimpleOpenNI openni, int userId){
		gr.onNewUser(openni, userId);
	}
	
	public void onLostUser(SimpleOpenNI openni, int userId){
		gr.onLostUser(openni, userId);
	}
	
	public void onOutOfSceneUser(SimpleOpenNI openni, int userId){
		gr.onOutOfSceneUser(openni, userId);
	}

	public void onNewHand(SimpleOpenNI openni, int userId, PVector v){
		gr.onNewHand(openni, userId, v);
	}
	
	public void onTrackedHand(SimpleOpenNI openni, int userId, PVector v){
		log.info("here");
		gr.onTrackedHand(openni, userId, v);
		//gr.on
	}
	
	public void onLostHand(SimpleOpenNI openni, int userId){
		gr.onLostHand(openni, userId);
	}

}
