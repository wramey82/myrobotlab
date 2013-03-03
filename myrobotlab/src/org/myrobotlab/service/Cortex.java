package org.myrobotlab.service;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service.data.Point2Df;
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

	// FIXME - better way to name subservices - Service static method
	// supress creation as an option?
	public String eyeName = "eye"; 
	// FIXME - better way to name subservices - Service static method
	public String earName = "ear"; 

	public String state = STATE_IDLE;
	
	ArrayList<Rectangle> boundingBoxes;

	transient OpenCV eye;
	transient Sphinx ear;
	transient Speech mouth;
	

	// FSM States
	public final static String STATE_IDLE = "STATE_IDLE";
	public final static String STATE_WAITING_FOR_STABILIZATION = "STATE_WAITING_FOR_STABILIZATION";
	public final static String STATE_WAITING_FOR_NEW_OBJECT = "STATE_WAITING_FOR_NEW_OBJECT";
	
	// queries & commands from humans
	public final static String STATE_WHAT_DO_YOU_SEE = "what do you see";
	public final static String STATE_REST = "rest"; // go to a specific position
	
	
	BlockingQueue<ArrayList<OpenCVData>> opencvData = new LinkedBlockingQueue<ArrayList<OpenCVData>>();

	public Cortex(String n) {
		super(n, Cortex.class.getCanonicalName());
	}

	// FIXME - need a createSubServices in constructor ???
	// FIXME - better, robust, more flexible way to handle naming of subservices
	public void createAndStartSubServices() {
		eye = (OpenCV) Runtime.createAndStart(eyeName, "OpenCV");
		ear = (Sphinx) Runtime.createAndStart(earName, "Sphinx");
		mouth = (Speech) Runtime.createAndStart("mouth", "Speech");

		startListening(String.format("%s", STATE_WHAT_DO_YOU_SEE)); // FIXME - incorporate into ear
		
		subscribe("publishOpenCVData", eye.getName(), "getOpenCVData", OpenCVData.class);
	}

	//------------  Event Methods Executed By Different Threads Begin -----------
	/* FIXME - EVENT LISTENERS - MAKE TYPEFULL

	 */
	public OpenCVData getOpenCVData(OpenCVData data)
	{
		// data transfer is a copy (int) so it represents the "last" update
		boundingBoxes = data.getBoundingBoxArray();
		data.setFilterName("findContours");
		if (boundingBoxes != null)
		{
//			log.info("see {} thingies",boundingBoxes.size());
		} else {
//			log.info("see null thingies");
		}
		return data;
	}
	
	public int getThingCount()
	{
		if (boundingBoxes == null)
		{
			return 0;
		}
		
		return boundingBoxes.size();
	}
	
	public String heard(String spoken)
	{
		if (STATE_WHAT_DO_YOU_SEE.equals(spoken))
		{
			int thingCount = getThingCount();
			String objectCount = String.format("I see %d object", thingCount);
			objectCount = String.format("%s%s", objectCount,  (thingCount == 1)?"":"s");
//			mouth.speak(objectCount);
			log.info(objectCount);
		}
		
		return spoken;
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
		eye.removeAllFilters();
		eye.addFilter("pyramidDown", "PyramidDown");
		eye.addFilter("mog2", "BackgroundSubtractorMOG2");
		eye.addFilter("erode", "Erode");
		eye.addFilter("dilate", "Dilate");
		eye.addFilter("findContours", "FindContours");
		
		eye.capture();
		
		// wait to stabilize
		//while()
		// background stabalized 
		// process and save image
	}
	
	public void stabilizeState() {
		state = STATE_WAITING_FOR_STABILIZATION;
		//while
		
	}
	
	
	// FIXME FIXME FIXME - make Listening interface  !!!!
	// which implments "heard"  !!!!
	public void startListening(String grammar) {
		ear.attach(mouth.getName());
		// FIXME - handle Java listener - addListener(real type)
		//ear.addListener("recognized", "python", "heard", String.class); 
		ear.addListener("recognized", this.getName(), "heard", String.class); 
		ear.createGrammar(grammar);
		ear.startListening();

	}


	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// FIXME !!! NOT GOOD - BEHAVIOR DIFFERENT FROM new and start !!!!
		/*
		 * Cortex cortex = new Cortex("cortex"); cortex.startService();
		 */

		Cortex cortex = (Cortex)Runtime.createAndStart("cortex", "Cortex");
		cortex.fadeBackground();
		
		

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		
		
		for (int i = 0; i < 1000; ++i)
		{
			cortex.heard(STATE_WHAT_DO_YOU_SEE);
		}
	}

}
