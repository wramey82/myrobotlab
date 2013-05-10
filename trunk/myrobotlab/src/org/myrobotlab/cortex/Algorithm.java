package org.myrobotlab.cortex;

import static org.myrobotlab.service.Cortex.MEMORY_OPENCV_DATA;
import static org.myrobotlab.service.OpenCV.PART;


import java.awt.Rectangle;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.Node;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service.Cortex;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Tracking;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;

public class Algorithm {// implements Runnable {

	// used to process visual data
	private OpenCV processor;
	private Memory memory;
	private Cortex cortex; // TODO should extract interface from this
	private Tracking tracking;
	
	public final static Logger log = LoggerFactory.getLogger(Cortex.class.getCanonicalName());

	// TODO - apply equal histogram
	
	public Algorithm(Cortex cortex)
	{
		this.cortex = cortex;
		this.processor = cortex.getProcessor();
		this.memory = cortex.getMemory();
		this.tracking = cortex.getTracking();
	}
	
	// TODO Node lends itself to many data types and attributes - should OpenCV follow suite ?
	// TODO - figure out what should be in OpenCV data and what should not !
	// TODO - background "should"
	
	// TODO extract interface ...
	// FIXME - Tracking should probably not publish Memory it should Publish OpenCVData !!!!!
	// depends on input
	public void process(OpenCVData data)
	{			
		// TODO put in background if background - FIXME - decide where attributes should be stored
		// OpenCV specific info "should" be in OpenCVData ... I would think - background and foreground are OpenCV specific
		// or could be...
		
		Node image = new Node(Long.toString(data.getTimestamp()));
		image.put(MEMORY_OPENCV_DATA, data);
		memory.put(String.format("/present/%s", data.getAttribute(PART)), image);
		
		
		OpenCVData processed = processor.add(data.getInputImage());
	
		ArrayList<Rectangle> bb = processed.getBoundingBoxArray();
		if (bb.size() == 1)
		{
			log.info("found faces");
			// conditional put
			Node node = new Node(Long.toString(data.getTimestamp()));
			node.put(MEMORY_OPENCV_DATA, processed);
			memory.put("/present/faces/unknown", node);
			Rectangle rect = bb.get(0);
			// FIXME - TODO - add good features with mask on face only 
			tracking.trackLKPoint(new Point2Df(rect.x, rect.y));
			//
			
		} else if (bb.size() > 1) {
			log.error("many faces dont know what to do");
		}
		

	}
	
	/*
	public void start();
	public void stop();
	*/
	
}
