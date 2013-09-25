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

package org.myrobotlab.service;

/*
 TODO : 
 new filters - http://idouglasamoore-javacv.googlecode.com/git-history/02385ce192fb82f1668386e55ff71ed8d6f88ae3/src/main/java/com/googlecode/javacv/ObjectFinder.java

 static wild card imports for quickly finding static functions in eclipse
 import static com.googlecode.javacv.cpp.opencv_highgui.*;
 import static com.googlecode.javacv.cpp.opencv_imgproc.*;
 import static com.googlecode.javacv.cpp.opencv_objdetect.*;
 import static com.googlecode.javacv.cpp.opencv_core.*;
 import static com.googlecode.javacv.cpp.opencv_features2d.*;
 import static com.googlecode.javacv.cpp.opencv_legacy.*;
 import static com.googlecode.javacv.cpp.opencv_video.*;
 import static com.googlecode.javacv.cpp.opencv_calib3d.*;

 */

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.BlockingQueueGrabber;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.opencv.OpenCVFilterFlip;
import org.myrobotlab.opencv.OpenCVFilterGray;
import org.myrobotlab.opencv.OpenCVFilterPyramidDown;
import org.myrobotlab.opencv.VideoProcessor;
import org.myrobotlab.reflection.Instantiator;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.interfaces.VideoSource;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

@Root
public class OpenCV extends VideoSource {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCV.class.getCanonicalName());

	// FIXME - make more simple
	transient public final static String INPUT_SOURCE_CAMERA = "camera";
	transient public final static String INPUT_SOURCE_MOVIE_FILE = "file";
	transient public final static String INPUT_SOURCE_NETWORK = "network";
	transient public final static String INPUT_SOURCE_PIPELINE = "pipeline";
	transient public final static String INPUT_SOURCE_IMAGE_FILE = "imagefile";
	
	// TODO - OpenCV constants / enums ? ... hmm not a big fan ...
	transient public static final String FILTER_LK_OPTICAL_TRACK = "LKOpticalTrack";
	transient public static final String FILTER_PYRAMID_DOWN = "PyramidDown";
	transient public static final String FILTER_GOOD_FEATURES_TO_TRACK = "GoodFeaturesToTrack";
	transient public static final String FILTER_DETECTOR = "Detector";
	transient public static final String FILTER_ERODE = "Erode";
	transient public static final String FILTER_DILATE = "Dilate";
	transient public static final String FILTER_FIND_CONTOURS = "FindContours";
	transient public static final String FILTER_FACE_DETECT = "FaceDetect";
		
	// directional constants
	transient final static public String DIRECTION_FARTHEST_FROM_CENTER = "DIRECTION_FARTHEST_FROM_CENTER";
	transient final static public String DIRECTION_CLOSEST_TO_CENTER = "DIRECTION_CLOSEST_TO_CENTER";
	transient final static public String DIRECTION_FARTHEST_LEFT = "DIRECTION_FARTHEST_LEFT";
	transient final static public String DIRECTION_FARTHEST_RIGHT = "DIRECTION_FARTHEST_RIGHT";
	transient final static public String DIRECTION_FARTHEST_TOP = "DIRECTION_FARTHEST_TOP";
	transient final static public String DIRECTION_FARTHEST_BOTTOM = "DIRECTION_FARTHEST_BOTTOM";
	
	transient final static public String FOREGROUND = "foreground";
	transient final static public String BACKGROUND = "background";
	transient final static public String PART = "part";
	

	transient public final static String SOURCE_KINECT_DEPTH = "SOURCE_KINECT_DEPTH";

	// yep its public - cause a whole lotta data
	// will get set on it before a setState

	@Element
	public VideoProcessor videoProcessor = new VideoProcessor();;
	
	// mask for each named filter
	transient public HashMap<String, IplImage> masks = new HashMap<String, IplImage>();
	
	public SerializableImage lastDisplay;

	// P - N Learning TODO - remove - implement on "images"
	public ArrayList<SerializableImage> positive = new ArrayList<SerializableImage>();
	public ArrayList<SerializableImage> negative = new ArrayList<SerializableImage>();

	public OpenCV(String n) {
		super(n, OpenCV.class.getCanonicalName());
		
		load(); // FIXME - go into service frame work .. after construction .. somewhere ...
		videoProcessor.setOpencv(this);
	}

	@Override
	public void stopService() {
		if (videoProcessor != null) {
			videoProcessor.stop();
		}
		super.stopService();
	}

	public final SerializableImage publishDisplay(String source, BufferedImage img)
	{
		lastDisplay = new SerializableImage(img, source);
		return lastDisplay;
	}
	
	public final SerializableImage publishFrame(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img, source);
		return si;
	}

	public final SerializableImage publishMask(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img, source);
		return si;
	}
	
	// publishing the big kahuna <output>
	public final OpenCVData publishOpenCVData(OpenCVData data) {
		return data;
	}
	
	// the big switch <input>
	public void publishOpenCVData(boolean b)
	{
		videoProcessor.publishOpenCVData = b;
	}
	
	public Integer setCameraIndex(Integer index) {
		videoProcessor.cameraIndex = index;
		return index;
	}

	public String setInputFileName(String inputFile) {
		videoProcessor.inputFile = inputFile;
		return inputFile;
	}

	public String setInputSource(String inputSource) {
		videoProcessor.inputSource = inputSource;
		return inputSource;
	}

	public String setFrameGrabberType(String grabberType) {
		videoProcessor.grabberType = grabberType;
		return grabberType;
	}

	public FrameGrabber getFrameGrabber() {
		return videoProcessor.getGrabber();
	}
	
	public void setDisplayFilter(String name) {
		log.info("pre setDisplayFilter displayFilter{}", videoProcessor.displayFilter);
		videoProcessor.displayFilter = name;
		log.info("post setDisplayFilter displayFilter{}", videoProcessor.displayFilter);
	}
	
	public OpenCVData add(SerializableImage image)
	{
		IplImage src = IplImage.createFrom(image.getImage());
		//return new SerializableImage(dst.getBufferedImage(), image.getSource());
		return add(src);
	}
	
	/**
	 * blocking safe exchange of data between different threads
	 * external thread adds image data which can be retrieved
	 * from the blockingData queue
	 * 
	 * @param image
	 */
	public OpenCVData add(IplImage image)
	{
		FrameGrabber grabber = videoProcessor.getGrabber();
		if (grabber == null || grabber.getClass() != BlockingQueueGrabber.class)
		{
			log.error("can't add an image to the video processor - grabber must be not null and BlockingQueueGrabber");
			return null;
		}
		
		BlockingQueueGrabber bqgrabber = (BlockingQueueGrabber)grabber;
		bqgrabber.add(image);
		
		try {
			OpenCVData ret = (OpenCVData)videoProcessor.blockingData.take();
			return ret;
		} catch (InterruptedException e) {
			return null;
		}
	}

	/**
	 * when the video image changes size this function will be called with the
	 * new dimension
	 * 
	 * @param d
	 * @return
	 */
	public Dimension sizeChange(Dimension d) {
		return d;
	}

	public String publish(String value) {
		return value;
	}

	// CPP interface does not use array - but hides implementation
	public CvPoint2D32f publish(CvPoint2D32f features) {
		return features;
	}

	public double[] publish(double[] data) {
		return data;
	}

	public CvPoint publish(CvPoint point) {
		return point;
	}

	public Point2Df publish(Point2Df point) {
		return point;
	}

	public Rectangle publish(Rectangle rectangle) // TODO - going bothways here
													// - cv & awt
	{
		return rectangle;
	}

	// when containers are published the <T>ypes are unknown to the publishing
	// function
	public ArrayList<?> publish(ArrayList<?> polygons) {
		return polygons;
	}

	public ColoredPoint[] publish(ColoredPoint[] points) {
		return points;
	}

	public SerializableImage publishTemplate(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img, source);
		return si;
	}

	public IplImage publishIplImageTemplate(IplImage img) {
		return img;
	}

	public Boolean isTracking(Boolean b) {
		return b;
	}

	// publish functions end ---------------------------

	public void stopCapture() {
		videoProcessor.stop();
	}

	public void capture() {
		save();
		// stopCapture(); // restart?
		videoProcessor.start();
	}


	public void stopRecording(String filename) {
		// cvReleaseVideoWriter(outputFileStreams.get(filename).pointerByReference());
	}

	public void setMask(String name, IplImage mask) {
		masks.put(name, mask);
	}

	public void addFilter(OpenCVFilter filter) {

		videoProcessor.addFilter(filter);
		broadcastState(); // let everyone know
	}
	public void addFilter(String filterName) {

		videoProcessor.addFilter(filterName, filterName);
		broadcastState(); // let everyone know
	}
	public OpenCVFilter addFilter(String name, String newFilter) {

		OpenCVFilter f = videoProcessor.addFilter(name, newFilter);
		broadcastState(); // let everyone know
		return f;
	}

	public void clearFilters() {
		videoProcessor.clearFilters();
		broadcastState();
	}

	public void removeFilter(String name) {
		OpenCVFilter f = videoProcessor.getFilter(name);
		if (f != null){
			videoProcessor.removeFilter(f);
		} else {
			log.warn("can not remove filter {} - it does not exits", name);
		}
		broadcastState();
	}
	
	public void removeFilter(OpenCVFilter filter) {
		videoProcessor.removeFilter(filter);
		broadcastState();
	}

	public ArrayList<OpenCVFilter> getFiltersCopy() {
		return videoProcessor.getFiltersCopy();
	}

	public OpenCVFilter getFilter(String name) {
		return videoProcessor.getFilter(name);
	}

	@Override
	public String getToolTip() {
		return "OpenCV (computer vision) service wrapping many of the functions and filters of OpenCV. ";
	}

	// filter dynamic data exchange begin ------------------
	public void broadcastFilterState() {
		invoke("publishFilterState");
	}

	/**
	 * @param otherFilter
	 *            - data from remote source
	 * 
	 *            This updates the filter with all the non-transient data in a
	 *            remote copy through a reflective field update. If your filter
	 *            has JNI members or pointer references it will break, mark all
	 *            of these.
	 */
	public void setFilterState(FilterWrapper otherFilter) {

		OpenCVFilter filter = getFilter(otherFilter.name);
		if (filter != null) {
			Service.copyShallowFrom(filter, otherFilter.filter);
		} else {
			log.error(String.format("setFilterState - could not find %s ", otherFilter.name));
		}

	}

	/**
	 * Callback from the GUI to the appropriate filter funnel through here
	 */
	public void invokeFilterMethod(String filterName, String method, Object... params) {
		OpenCVFilter filter = getFilter(filterName);
		if (filter != null) {
			Instantiator.invokeMethod(filter, method, params);
		} else {
			log.error("invokeFilterMethod " + filterName + " does not exist");
		}
	}

	public FilterWrapper publishFilterState(String name) {
		OpenCVFilter filter = getFilter(name);
		if (filter != null) {
			return new FilterWrapper(name, filter);
		} else {
			log.error(String.format("publishFilterState %s does not exist ", name));
		}

		return null;
	}

	public void recordOutput(Boolean b) {
		videoProcessor.recordOutput(b);
	}

	public String recordSingleFrame(Boolean b) {
		return videoProcessor.recordSingleFrame(b);
	}

	// filter dynamic data exchange end ------------------
	public static Rectangle cvToAWT(CvRect rect) {
		Rectangle boundingBox = new Rectangle();
		boundingBox.x = rect.x();
		boundingBox.y = rect.y();
		boundingBox.width = rect.width();
		boundingBox.height = rect.height();
		return boundingBox;

	}
	
	
	// blocking method
	public OpenCVData getOpenCVData() {
		OpenCVData data = null;
		try {
			videoProcessor.blockingData.clear();

			boolean oldPublishOpenCVData = videoProcessor.publishOpenCVData;
			videoProcessor.publishOpenCVData = true;
			videoProcessor.useBlockingData = true;
			data = (OpenCVData) videoProcessor.blockingData.take(); // TODO - poll or timeout
														// value parameter
			videoProcessor.publishOpenCVData = oldPublishOpenCVData;
			videoProcessor.useBlockingData = false;
			return data;

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public OpenCVData getGoodFeatures() {
		addFilter(FILTER_GOOD_FEATURES_TO_TRACK, FILTER_GOOD_FEATURES_TO_TRACK);
		OpenCVData d = getOpenCVData();
		removeFilter(FILTER_GOOD_FEATURES_TO_TRACK);
		return d;
	}
	
	public OpenCVData getFaceDetect() {
		OpenCVFilterFaceDetect fd = new OpenCVFilterFaceDetect();
		addFilter(fd);
		OpenCVData d = getOpenCVData();
		removeFilter(fd);
		return d;
	}


	public static Point2Df findPoint(ArrayList<Point2Df> data, String direction, Double minValue) {

		double distance = 0;
		int index = 0;
		double targetDistance = 0.0f;

		if (data == null || data.size() == 0) {
			log.error("no data");
			return null;
		}

		if (minValue == null) {
			minValue = 0.0;
		}

		if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
			targetDistance = 1;
		} else {
			targetDistance = 0;
		}

		for (int i = 0; i < data.size(); ++i) {
			Point2Df point = data.get(i);

			if (DIRECTION_FARTHEST_FROM_CENTER.equals(direction)) {
				distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
				distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_LEFT.equals(direction)) {
				distance = point.x;
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_RIGHT.equals(direction)) {
				distance = point.x;
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_TOP.equals(direction)) {
				distance = point.y;
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_BOTTOM.equals(direction)) {
				distance = point.y;
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			}

		}

		Point2Df p = data.get(index);
		log.info(String.format("findPointFarthestFromCenter %s", p));
		return p;
	}
	
	public SerializableImage getDisplay()
	{
		return lastDisplay;
	}

	public void useBlockingData(Boolean b) {
		videoProcessor.useBlockingData = true;
	}

	public int getCameraIndex() {
		return videoProcessor.cameraIndex;
	}
	
	public void setPipeline(String pipeline)
	{
		videoProcessor.pipelineSelected = pipeline;
		videoProcessor.inputSource = "pipeline";
		videoProcessor.grabberType = "org.myrobotlab.opencv.PipelineFrameGrabber";
	}

	public static void main(String[] args) throws Exception {

		// TODO - Avoidance / Navigation Service
		// ground plane
		// http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
		// radio lab - map cells location cells yatta yatta
		// lkoptical disparity motion Time To Contact
		// https://www.google.com/search?aq=0&oq=opencv+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv+obstacle+avoidance
		// 
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		OpenCV opencv = (OpenCV) Runtime.createAndStart("opencv", "OpenCV");
		//opencv.addFilter(new OpenCVFilterPyramidDown());
		//opencv.addFilter(new OpenCVFilterGray());
		OpenCVFilterFlip flip = new OpenCVFilterFlip();
		flip.flipCode = 0; // horizontal
		opencv.addFilter(flip);
				
		GUIService gui = (GUIService)Runtime.createAndStart("gui", "GUIService");
		gui.display();
		
	}

	
}
