package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         initialize load serialized data go to initial position if no data at
 *         this position - learn scene | if data -> compare (learn saved data) -
 *         load new starts with blank - or memorized scene motion -> fires
 *         motion event -> waits for settle -> stabalize (time)? -> (no | yes)
 *         -> determine new (object | or hole -missing) isolate object ->
 *         memorize object -> load into memory - ( *** compare with existing
 *         info *** ) -> make determination (should query (voice | email)|
 *         should guess ) process response (or non-response)
 * 
 */
public class Cortex extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Cortex.class.getCanonicalName());

	public String opencvName = "opencv"; // FIXME - better way to handle
											// sub-services

	public String state = STATE_IDLE;
	
	transient OpenCV opencv;
	
	// FSM States
	public final static String STATE_IDLE = "STATE_IDLE";
	public final static String STATE_WAITING_FOR_STABILIZATION = "STATE_WAITING_FOR_STABILIZATION";
	public final static String STATE_WAITING_FOR_NEW_OBJECT = "STATE_WAITING_FOR_NEW_OBJECT";

	public Cortex(String n) {
		super(n, Cortex.class.getCanonicalName());
	}

	// FIXME - need a createSubServices in constructor ???
	// FIXME - better, robust, more flexible way to handle naming of subservices
	public void createAndStartSubServices() {
		opencv = (OpenCV) Runtime.createAndStart(opencvName, "OpenCV");
	}

	// FIXME - remove start replacing javadoc & tooltips with annotations
	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public void releaseService() {
		super.releaseService();
	}

	public void fadeBackground() {
		opencv.removeAllFilters();
		opencv.addFilter("pyramidDown", "PyramidDown");
		opencv.addFilter("mog2", "BackgroundSubtractorMOG2");
		opencv.addFilter("erode", "Erode");
		opencv.addFilter("dilate", "Dilate");
		opencv.addFilter("findContours", "FindContours");
		
		opencv.capture();
		
		// wait to stabilize
		//while()
		// background stabalized 
		// process and save image
	}
	
	public void stabalizeState() {
		state = STATE_WAITING_FOR_STABILIZATION;
		//while
		
	}
	

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		// FIXME !!! NOT GOOD - BEHAVIOR DIFFERENT FROM new and start !!!!
		/*
		 * Cortex cortex = new Cortex("cortex"); cortex.startService();
		 */

		Cortex cortex = (Cortex)Runtime.createAndStart("cortex", "Cortex");
		cortex.fadeBackground();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

}
