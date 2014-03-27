package org.myrobotlab.openni;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import SimpleOpenNI.SimpleOpenNI;

public class PApplet {

	public Object g;
	public final static Logger log = LoggerFactory.getLogger(PApplet.class);

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
		log.info("line");
	}

	static public final float sqrt(float a) {
		return (float) Math.sqrt(a);
	}

}
