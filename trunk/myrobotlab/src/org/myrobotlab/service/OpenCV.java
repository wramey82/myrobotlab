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

import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_FOURCC;
import static com.googlecode.javacv.cpp.opencv_highgui.cvCreateVideoWriter;
import static com.googlecode.javacv.cpp.opencv_highgui.cvWriteFrame;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SimpleTimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterAverageColor;
import org.myrobotlab.service.data.Point2Df;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenKinectFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui.CvVideoWriter;

@Root
public class OpenCV extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(OpenCV.class.getCanonicalName());

	int frameIndex = 0;
	int lastImageWidth = 0;

	public final static String INPUT_SOURCE_CAMERA = "camera";
	public final static String INPUT_SOURCE_FILE = "file";
	public final static String INPUT_SOURCE_NETWORK = "network";

	// GRABBER BEGIN --------------------------
	@Element
	public String inputSource = INPUT_SOURCE_CAMERA;
	@Element
	public String grabberType = "com.googlecode.javacv.OpenCVFrameGrabber";
	FrameGrabber grabber = null;

	// grabber cfg
	@Element(required = false)
	public String format = null;
	@Element
	public boolean getDepth = false;
	@Element
	public int cameraIndex = 0;
	@Element
	public String inputFile = "http://localhost/videostream.cgi";
	boolean loopInputMovieFile = true;
	boolean publishFrame = true;
	boolean useCanvasFrame = false;
	// public String url;
	// GRABBER END --------------------------

	transient VideoProcess videoProcess = null;
	String displayFilter = "output";

	private boolean publishIplImage = false;

	// mask for each named filter
	public HashMap<String, IplImage> masks = new HashMap<String, IplImage>();

	// multi-plexing keys
	public HashMap<String, IplImage> multiplex = new HashMap<String, IplImage>();

	transient public HashMap<String, Object> storage = new HashMap<String, Object>();

	// P - N Learning TODO - remove - implement on "images"
	public ArrayList<SerializableImage> positive = new ArrayList<SerializableImage>();
	public ArrayList<SerializableImage> negative = new ArrayList<SerializableImage>();

	// display
	CanvasFrame cf = null;
	transient IplImage frame = null;
	transient IplImage depthFrame = null;
	transient IplImage imageFrame = null;
	transient IplImage kinectMask = null;
	transient IplImage temp = null;
	transient IplImage black = null;

	/*
	 * preLoadFilters - are filters which other Services can use to add, remove,
	 * or modify the filter set currently used by OpenCV without having to stop
	 * video processing Access to the preLoadFilters is synchronized and thread
	 * safe. The actual filters are not thread safe, however, the takes an event
	 * of load, merge, or remove for the actual filters to be modified. The
	 * activity of moving the filters over is taken care of by the video
	 * processing thread, so only 1 thread has access (the video processor) to
	 * the actual filters The "commit" action will take care of invalid states
	 * do to partial work flows
	 */
	// LinkedHashMap<String, OpenCVFilter> addFilters = new
	// LinkedHashMap<String, OpenCVFilter>();
	// ArrayList<String> removeFilters = new ArrayList<String>();
	// @ElementMap
	// LinkedHashMap<String, OpenCVFilter> filters = new LinkedHashMap<String,
	// OpenCVFilter>();
	// filters is thread safe ! - 2 accessory threads - videoprocessor & opencv
	// inbox
	private ArrayList<OpenCVFilter> filters = new ArrayList<OpenCVFilter>();
	// ArrayList<String> removefilters = new ArrayList<String>();
	// ArrayList<OpenCVFilter> addfilters = new ArrayList<OpenCVFilter>();
	// not thread safe - chose not to use concurrenthashmap because small jvm's
	// e.g. gnu-classpath doe not support it
	// should only be used by opencv inbox OR gui thread but not at the same
	// time - gui when operating locally & opencv inbox when remote

	HashMap<String, CvVideoWriter> outputFileStreams = new HashMap<String, CvVideoWriter>();

	// published objects

	static final public class Polygon {
		final public CvRect boundingRectangle;
		final public CvScalar avgColor;
		final public boolean isConvex;
		final public CvPoint centeroid;
		final public int vertices;

		public Polygon(CvRect boundingRectangle, CvScalar avgColor, boolean isConvex, CvPoint centeroid, int vertices) {
			this.boundingRectangle = boundingRectangle;
			this.avgColor = avgColor;
			this.isConvex = isConvex;
			this.centeroid = centeroid;
			this.vertices = vertices;
		}

		// TODO - static functions in Speech service
		public String getShapeWord() {
			if (vertices > 3 && vertices < 6 && isConvex) {
				return "square";
			} else if (vertices == 3 && isConvex) {
				return "triangle";
			} else if (vertices > 5 && isConvex) {
				return "circle";
			} else {
				return "thingy";
			}
		}

		public String getSizeWord() {
			int area = boundingRectangle.width() * boundingRectangle.height();
			if (area > 1500) {
				return "big";
			} else if (area < 1500 && area > 500) {
				return "medium";
			} else {
				return "small";
			}
		}

		public String getColorWord() {
			return OpenCVFilterAverageColor.getColorName2(avgColor);
		}

	}

	public OpenCV(String n) {
		super(n, OpenCV.class.getCanonicalName());
		load();
	}

	@Override
	public void stopService() {
		if (videoProcess != null) {
			videoProcess.stop();
		}
		super.stopService();
	}

	public final static SerializableImage publishFrame(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		si.source = source;
		si.timestamp = System.currentTimeMillis();
		return si;
	}

	public final static SerializableImage publishMask(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		si.source = source;
		return si;
	}

	public void publishIplImage(boolean t) {
		publishIplImage = t;
	}

	public IplImage publishIplImage(IplImage image) {
		return image;
	}

	public boolean capturing = false;

	class VideoProcess implements Runnable {

		boolean published = false;
		Thread videoThread = null;

		// OpenCVFrameGrabber grabber = null;
		// FFmpegFrameGrabber grabber = null;
		// CameraDevice.Settings cameraSettings;
		// CameraDevice cameraDevice = null;
		// FrameGrabber frameGrabber = null;
		// CvCapture frameGrabber = cvCreateCameraCapture(0);
		// VideoInputFrameGrabber grabber = null;

		SimpleDateFormat sdf = new SimpleDateFormat();

		public void start() {
			log.info("starting capture");
			sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
			sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

			videoThread = new Thread(this, "OpenCV_videoProcessor");
			videoThread.start();
		}

		public void stop() {
			log.debug("stopping capture");
			capturing = false;
			videoThread = null;
		}

		public void run() {

			StringBuffer screenText = new StringBuffer();
			capturing = true;
			boolean filtersChanged = false;

			if (useCanvasFrame) {
				cf = new CanvasFrame("CanvasFrame");
			}

			try {

				Class<?>[] paramTypes = new Class[1];
				Object[] params = new Object[1];

				if (INPUT_SOURCE_CAMERA.equals(inputSource)) {
					paramTypes[0] = Integer.TYPE;
					params[0] = cameraIndex;
				} else if (INPUT_SOURCE_FILE.equals(inputSource)) {
					paramTypes[0] = String.class;
					params[0] = inputFile;
				}

				Class<?> nfg = Class.forName(grabberType);
				// TODO - get correct constructor for Capture Configuration..
				Constructor<?> c = nfg.getConstructor(paramTypes);

				grabber = (FrameGrabber) c.newInstance(params);

				if (format != null) {
					grabber.setFormat(format);
				}

				log.error("using " + grabber.getClass().getCanonicalName());

				if (grabber == null) {
					log.error("no viable capture or frame grabber with input " + grabberType);
					stop();
				}

				if (grabber != null) {
					grabber.start();
				}

			} catch (Exception e) {
				logException(e);
				stop();
			}
			// TODO - utilize the size changing capabilites of the different
			// grabbers
			// grabbler.setImageWidth()

			while (capturing) {

				published = false;
				++frameIndex;
				logTime("start");

				try {
					if (!getDepth) {
						frame = grabber.grab();
					} else { // OpenKinect
						depthFrame = ((OpenKinectFrameGrabber) grabber).grabDepth();
						storage.put("kinectDepth", depthFrame);
						imageFrame = ((OpenKinectFrameGrabber) grabber).grabVideo();
						frame = imageFrame;
					}
					storage.put("input", frame);

				} catch (Exception e) {
					log.error(stackToString(e));
				}

				logTime("read");

				if (frame != null) {
					synchronized (filters) {
						Iterator<OpenCVFilter> itr = filters.iterator();
						while (capturing && itr.hasNext()) {

							OpenCVFilter filter = itr.next();

							frame = filter.preProcess(frame);
							frame = filter.process(frame);

							String filterName = filter.name;
							// fork frame off - if destination is local frame
							// overwrites can occur
							// a true fork is a copy
							if (multiplex.containsKey(filterName)) {
								// FIXME - change of size will crash
								if (multiplex.get(filterName) == null) {
									multiplex.put(filterName, cvCreateImage(cvGetSize(frame), frame.depth(), frame.nChannels()));
								}
								IplImage forked = multiplex.get(filterName);
								cvCopy(frame, forked);
								invoke("publishFrame", filterName, forked.getBufferedImage());
								// published = true;
							}

							// frame is selected in gui - publish this filters
							// frame
							if (!published && displayFilter.equals(filterName)) {
								published = true;
								if (publishFrame) {
									invoke("publishFrame", displayFilter, filter.display(frame, null));
								}
							}

						} // capturing && itr.hasNext()
					}

					if (frame.width() != lastImageWidth) {
						invoke("sizeChange", new Dimension(frame.width(), frame.height()));
						lastImageWidth = frame.width();
					}

					if (filtersChanged) {
						broadcastState();
						filtersChanged = false;
					}

					// different data type
					if (publishIplImage) {
						invoke("publishIplImage", frame);
					}
					// if the display was not published by one of the filters
					// convert the frame now and publish it with timestamp
					if (!published) {
						BufferedImage bi = frame.getBufferedImage();
						Graphics2D graphics = bi.createGraphics();

						// timestamp paint
						screenText.delete(0, screenText.length());
						screenText.append(sdf.format(new Date()));
						screenText.append(" ");
						screenText.append(frameIndex);

						graphics.drawString(screenText.toString(), 10, 10);
						bi.flush();

						if (publishFrame) {
							invoke("publishFrame", displayFilter, bi);
						}
						// log.error(" time");
						published = true;
					}
				}

				frame = null; // done with current frame
				startTimeMilliseconds = 0;

			} // while capturing

			try {
				grabber.release();
				grabber = null;
			} catch (Exception e) {
				log.error(stackToString(e));
			}
		}
	}

	// multiplexing
	public void fork(String filter) {
		multiplex.put(filter, null);
	}

	public Integer setCameraIndex(Integer index) {
		cameraIndex = index;
		return cameraIndex;
	}

	public String setInputFileName(String inputFile) {
		this.inputFile = inputFile;
		return inputFile;
	}

	public String setInpurtSource(String inputSource) {
		this.inputSource = inputSource;
		return inputSource;
	}

	public String setFrameGrabberType(String grabberType) {
		this.grabberType = grabberType;
		return grabberType;
	}

	public void setDisplayFilter(String name) {
		displayFilter = name;
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
		SerializableImage si = new SerializableImage(img);
		si.source = source;
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
		// set variable - allow capturing thread
		// to terminate cleanly and release resources
		capturing = false;

		if (videoProcess != null) {
			videoProcess.stop();
			videoProcess = null;
		}
	}

	public void capture() {

		save();

		if (videoProcess != null) {
			stopCapture();
		}

		videoProcess = new VideoProcess();
		videoProcess.start();
	}

	public void captureOutput(String filename, IplImage frame) {
		// TODO - configurable to grabber input and filtered output
		CvSize imgSize = new CvSize();
		imgSize.width(frame.width());
		imgSize.height(frame.height());
		double fps = 16;
		int isColor = 1;

		// could not stop - Compiler did not align stack variables. Libavcodec
		// has been miscompiled

		if (!outputFileStreams.containsKey(filename)) {
			CvVideoWriter writer = cvCreateVideoWriter(cfg.get("outputMovieFileName"), CV_FOURCC('M', 'J', 'P', 'G'),
			// CV_FOURCC('F', 'L', 'V', '1'),
					fps, imgSize, isColor);

			outputFileStreams.put(filename, writer);
		}

		cvWriteFrame(outputFileStreams.get(filename), frame);

		// cvReleaseVideoWriter(&writer);
		// cvReleaseCapture(&input);
	}

	public void releaseCaptureOutput(String filename) {
		// cvReleaseVideoWriter(outputFileStreams.get(filename).pointerByReference());
	}

	public void setMask(String name, IplImage mask) {
		masks.put(name, mask);
	}

	public void addFilter(String name, String newFilter) {

		// WARNING - not thread safe at all
		// we don't want this thread directly modifying filters
		// we will add to addFilters and let the video processor merge it in
		// how its used - the video processor can take a relatively long time
		// going through a workflow - typically add/remove filters is done in
		// small bursts with a much longer time in-between
		// recent issues have been about the small bursts destroying the
		// workflow
		// now the video processor will only move the filters when its done with
		// a workflow - if the new workflow is moved over and more is added at
		// the same time
		// a concurrent modification exception will occur

		log.info(String.format("request to addFilter %s, %s", name, newFilter));

		String type = "org.myrobotlab.opencv.OpenCVFilter" + newFilter;
		Object[] params = new Object[2];
		params[0] = this;
		params[1] = name;

		OpenCVFilter filter = null;
		try {
			filter = (OpenCVFilter) getNewInstance(type, params);
			synchronized (filters) {
				filters.add(filter);
			}
			log.info(String.format("added new filter %s, %s", name, newFilter));

			broadcastState(); // let everyone know
		} catch (Exception e) {
			logException(e);
		}

	}

	public void removeAllFilters() {
		synchronized (filters) {
			filters.clear();
		}
		broadcastState(); // let everyone know
	}

	public void removeFilter(String name) {
		synchronized (filters) {
			Iterator<OpenCVFilter> itr = filters.iterator();
			while (itr.hasNext()) {
				OpenCVFilter filter = itr.next();
				if (filter.name.equals(name)) {
					itr.remove();
					broadcastState(); // let everyone know
					return;
				}
			}
		}

		log.error(String.format("removeFilter could not find %s filter", name));
	}

	public IplImage getLastFrame() {
		return frame;
	}

	public ArrayList<OpenCVFilter> getFiltersCopy() {
		synchronized (filters) {
			return new ArrayList<OpenCVFilter>(filters);
		}
	}

	public OpenCVFilter getFilter(String name) {

		synchronized (filters) {
			Iterator<OpenCVFilter> itr = filters.iterator();
			while (itr.hasNext()) {
				OpenCVFilter filter = itr.next();
				if (filter.name.equals(name)) {
					return filter;
				}
			}
		}
		log.error(String.format("removeFilter could not find %s filter", name));
		return null;
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
			invoke(filter, method, params);
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

	// filter dynamic data exchange end ------------------

	public static void main(String[] args) {

		// TODO - Avoidance / Navigation Service
		// ground plane
		// http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
		// radio lab - map cells location cells yatta yatta
		// lkoptical disparity motion Time To Contact
		// https://www.google.com/search?aq=0&oq=opencv+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv+obstacle+avoidance
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		/*
		 * IplImage imgA = cvLoadImage( "hand0.jpg", CV_LOAD_IMAGE_GRAYSCALE);
		 * IplImage imgB = cvLoadImage( "hand1.jpg", CV_LOAD_IMAGE_GRAYSCALE);
		 * try { ObjectFinder of = new ObjectFinder(imgA); of.find(imgB); }
		 * catch (Exception e) { // TODO Auto-generated catch block
		 * logException(e); }
		 */

		OpenCV opencv = (OpenCV) Runtime.createAndStart("opencv", "OpenCV");
		Runtime.createAndStart("remote", "RemoteAdapter");
		// opencv.startService();
		// opencv.addFilter("PyramidDown1", "PyramidDown");
		// opencv.addFilter("KinectDepthMask1", "KinectDepthMask");
		// opencv.addFilter("InRange1", "InRange");
		// opencv.setFrameGrabberType("camera");
		// opencv.grabberType = "com.googlecode.javacv.OpenCVFrameGrabber";
		// opencv.grabberType = "com.googlecode.javacv.OpenKinectFrameGrabber";
		// opencv.grabberType = "com.googlecode.javacv.FFmpegFrameGrabber";

		// opencv.addFilter("pd", "PyramidDown");
		// opencv.addFilter("gft", "GoodFeaturesToTrack");
		// opencv.publishFilterData("gft");
		// opencv.setDisplayFilter("gft");
		// opencv.addFilter("lkOpticalTrack1", "LKOpticalTrack");

		// opencv.capture();

		GUIService gui = new GUIService("opencv_gui");
		gui.startService();
		gui.display();

		// opencv.addFilter("ir","InRange");
		// opencv.setDisplayFilter("ir");

		// opencv.addFilter("pyramdDown", "PyramidDown");
		// opencv.addFilter("floodFill", "FloodFill");

		// opencv.capture();

	}

}
