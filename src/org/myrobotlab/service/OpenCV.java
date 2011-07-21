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
import static com.googlecode.javacv.cpp.opencv_highgui.CV_FOURCC;
import static com.googlecode.javacv.cpp.opencv_highgui.cvCreateCameraCapture;
import static com.googlecode.javacv.cpp.opencv_highgui.cvCreateVideoWriter;
import static com.googlecode.javacv.cpp.opencv_highgui.cvQueryFrame;
import static com.googlecode.javacv.cpp.opencv_highgui.cvReleaseCapture;
import static com.googlecode.javacv.cpp.opencv_highgui.cvWriteFrame;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SimpleTimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.OpenCVFilter;
import org.myrobotlab.image.OpenCVFilterAverageColor;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.data.ColoredPoint;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenKinectFrameGrabber;
import com.googlecode.javacv.VideoInputFrameGrabber;
import com.googlecode.javacv.FrameGrabber.ColorMode;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
import com.googlecode.javacv.cpp.opencv_highgui.CvVideoWriter;

//@Root
public class OpenCV extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCV.class.getCanonicalName());

	int frameIndex = 0;
	int lastImageWidth = 0;

	public int cameraIndex = 0;
	public String useInput = "null";
	String inputMovieFileName = "input.avi";
	boolean loopInputMovieFile = true;
	boolean sendImage = true;
	boolean useCanvasFrame = false;
	
	transient VideoProcess videoProcess = null;
	String displayFilter = "output";
	
	public String kinectMode = "";

	public ArrayList<BufferedImage> images = new ArrayList<BufferedImage>(100);
	
	// P - N Learning
	public ArrayList<SerializableImage> positive = new ArrayList<SerializableImage>(); 
	public ArrayList<SerializableImage> negative = new ArrayList<SerializableImage>(); 
	
	// display
	CanvasFrame cf = null;
	transient IplImage frame = null;
	LinkedHashMap<String, OpenCVFilter> filters = new LinkedHashMap<String, OpenCVFilter>();
	HashMap<String, CvVideoWriter> outputFileStreams = new HashMap<String, CvVideoWriter>();

	// published objects

	static final public class Polygon {
		final public CvRect boundingRectangle;
		final public CvScalar avgColor;
		final public boolean isConvex;
		final public CvPoint centeroid;
		final public int vertices;
		

		public Polygon(CvRect boundingRectangle, CvScalar avgColor,
			boolean isConvex, CvPoint centeroid, int vertices) {
			this.boundingRectangle = boundingRectangle;
			this.avgColor = avgColor;
			this.isConvex = isConvex;
			this.centeroid = centeroid;
			this.vertices = vertices;

		}

		// TODO - static functions in Speech service
		public String getShapeWord() {
			if (vertices > 3 && vertices < 6 && isConvex) 
			{
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
	}

	public void loadDefaultConfiguration() {

		//cfg.set("cameraIndex", 0);
		// cfg.set("pixelsPerDegree", 7);
		//cfg.set("useInput", "null");
		//cfg.set("inputMovieFileName", "outfile1.avi");
		//cfg.set("inputMovieFileLoop", true);
		// cfg.set("outputMovieFileName", "out.avi");
		//cfg.set("performanceTiming", false);
		//cfg.set("sendImage", true);
		//cfg.set("useCanvasFrame", false);
		//cfg.set("displayFilter", "output");
	}

	
	@Override
	public void startService() {
		super.startService();
	}

	@Override
	public void stopService() {
		if (videoProcess != null)
		{
			videoProcess.stop();
		}
		super.stopService();
	}

	public SerializableImage sendImage(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		si.source = source;
		return si;
	}

	// public void invokeFilterMethod (String filterName, String method,
	// MouseEvent me)
	/*
	 * Callback from the GUI to the appropriate filter funnel through here
	 */
	public void invokeFilterMethod(String filterName, String method,
			Object[] params) {
		// LOG.error("invokeFilterMethod here");
		if (filters.containsKey(filterName)) {
			invoke(filters.get(filterName), method, params);
		} else {
			LOG.error("invokeFilterMethod " + filterName + " does not exist");
		}

	}

	public void setFilterData (FilterWrapper filterData)
	{
		if (filters.containsKey(filterData.name)) {
			Service.copyShallowFrom(filters.get(filterData.name), filterData.filter);
		} else {
			LOG.error("setFilterData " + filterData.name + " does not exist");
		}
		
	}
	
	public final class FilterWrapper implements Serializable
	{
		private static final long serialVersionUID = 1L;
		public final String name;
		public final OpenCVFilter filter;
		
		public FilterWrapper(String name, OpenCVFilter filter)
		{
			this.name = name;
			this.filter = filter;
		}
	}
	
	// cannot cast on a return object
	public FilterWrapper publishFilterData(String name)
	{
		if (filters.containsKey(name)) {
			return new FilterWrapper(name, filters.get(name));
		} else {
			LOG.error("setFilterData " + name + " does not exist");
		}
		
		return null;
	}
	
	public Object setFilterCFG(String filterName, String cfgName, Float value) {
		if (filters.containsKey(filterName)) {
			return filters.get(filterName).setCFG(cfgName, value);
		} else {
			LOG.error("setFilterCFG " + filterName + " does not exist");
		}
		return null;
	}

	public Object setFilterCFG(String filterName, String cfgName, Integer value) {
		if (!filters.containsKey(filterName)) {
			LOG.warn("setFilterCFG filter " + filterName + " does not currently exist");
		}
		return cfg.set(OpenCVFilter.FILTER_CFG_ROOT + filterName + "/" + cfgName, value);
	}

	public Object setFilterCFG(String filterName, String cfgName, Boolean value) {
		if (filters.containsKey(filterName)) {
			return filters.get(filterName).setCFG(cfgName, value);
		} else {
			LOG.error("setFilterCFG " + filterName + " does not exist");
		}
		return null;
	}

	public Object setFilterCFG(String filterName, String cfgName, String value) {
		if (filters.containsKey(filterName)) {
			return filters.get(filterName).setCFG(cfgName, value);
		} else {
			LOG.error("setFilterCFG " + filterName + " does not exist");
		}
		return null;
	}

/* removed for chumby jamvm support
	public BufferedImage input(BufferedImage bi) {
		try {
			images.put(bi);
		} catch (InterruptedException e) {
			LOG.error(Service.stackToString(e));
		}
		return bi;
	}
*/
	class VideoProcess implements Runnable {

		boolean isCaptureRunning = false;
		boolean published = false;
		Thread videoThread = null;

		//OpenCVFrameGrabber grabber = null;
		//FFmpegFrameGrabber grabber = null;
	    //CameraDevice.Settings 	cameraSettings;   
	    //CameraDevice cameraDevice = null;
	    //FrameGrabber frameGrabber = null;
	    //CvCapture frameGrabber = cvCreateCameraCapture(0);
		//VideoInputFrameGrabber grabber = null;
		
		SimpleDateFormat sdf = new SimpleDateFormat();

		String grabberType = null;
		FrameGrabber grabber = null;
		CvCapture oldGrabber = null; // TODO - choose index
		
		boolean isWindows = false;		

		public void start()
		{
			LOG.info("starting capture");
			sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
			sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

			videoThread = new Thread(this, "OpenCV_videoProcessor");
			videoThread.start();
		}
		
		public void stop ()
		{
			LOG.debug("stopping capture");
			isCaptureRunning = false;
			videoThread = null;
		}
		

		public void run() {
			
			StringBuffer screenText = new StringBuffer();			
			isCaptureRunning = true;
			
			LOG.info("here1");
			
			try {
				isWindows = Loader.getPlatformName().startsWith("windows");
			} catch(Exception e)
			{
				Service.logException(e);
			}

			LOG.info("here2");
			
			if (useCanvasFrame) {
				// cf = new CanvasFrame(false);
			}
			
			// use VideoInputFrameGrabber if Loader.getPlatformName().startsWith("windows");
			// how to use? Class<? extends FrameGrabber> fg = FrameGrabber.getDefault();						
			//grabber = new OpenCVFrameGrabber(index); //works windows xp (fast) - linux (slow) V4L2 errors
			//FrameGrabber grabber = new FFmpegFrameGrabber("/dev/video0","video4linux2"); 
			//grabber = new FFmpegFrameGrabber("/dev/video0");
			//grabber = new OpenCVFrameGrabber(0);
			//cameraSettings.setQuantity(1);
			//CameraDevice cameraDevice;
			try {
				 //OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0); 
				//int index = cfg.getInt("cameraIndex");
				if ("kinect".equals(useInput))
				{
					grabber = new OpenKinectFrameGrabber(cameraIndex);
					grabber.setFormat(kinectMode);
					
				} else if ("camera".equals(useInput))
				{
					if (isWindows)
					{
						grabber = new VideoInputFrameGrabber(cameraIndex);
						grabber.start();
					} else {
						oldGrabber = cvCreateCameraCapture(cameraIndex);
						if (oldGrabber == null)
						{
							LOG.error("could not create camera capture on camera index " + cameraIndex);
							stop ();
						}
					}
				} else {
					LOG.error("useInput null or not supported " + useInput);
					stop();
				}
				
				if (grabber == null && oldGrabber == null)
				{
					LOG.error("no viable capture or frame grabber with input " + useInput);
					stop();
				}
				
				if (grabber != null)
				{
					grabber.start(); 
				}
				//VideoInputFrameGrabber fg = new VideoInputFrameGrabber(0);
				//CvCapture c = cvCreateCameraCapture(index); old way
				//FrameGrabber.init();
				//cameraSettings = new CameraDevice.Settings();
				//cameraDevice = new CameraDevice(cameraSettings);
				//cameraSettings.setFrameGrabber(FrameGrabber.getDefault());
				//cameraDevice = new CameraDevice("/dev/video0");
				//frameGrabber = cameraDevice.createFrameGrabber();
				//frameGrabber.start();
				//IplImage img = frameGrabber.grab();
				
			} catch (Exception e) {
				LOG.error(stackToString(e));
				stop ();
			}
			
			int kinectInterleave = 0;
			
			while (isCaptureRunning) {
				
				published = false;
				//isCaptureStopped = false;
				++frameIndex;
				logTime("start");

				
				//double x = highgui.cvGetCaptureProperty(grabber, highgui.CV_CAP_PROP_FRAME_WIDTH);
				//cvSetCaptureProperty( oldGrabber, CV_CAP_PROP_FRAME_WIDTH, 320);
				//cvSetCaptureProperty( oldGrabber, CV_CAP_PROP_FRAME_HEIGHT, 240);
					//frame = grabber.grab();
				try {
					if (grabber != null)
					{
						//grabber.setColorMode(ColorMode.BGR);
						/*
						++kinectInterleave;
						if (kinectInterleave%2 == 0)
						{
							grabber.setFormat("");
						} else {
							grabber.setFormat("depth");							
						}
						*/
						frame = grabber.grab();
						//grabber.set
					} else {
						frame = cvQueryFrame(oldGrabber);						
					}
				} catch (Exception e) {
					LOG.error(stackToString(e));
				}


				logTime("read");

				if (frame != null) {
					Iterator<String> itr = filters.keySet().iterator();

					while (itr.hasNext()) {
						String name;
						try {
							name = itr.next();
						} catch (Exception e) {
							Service.logException(e);
							break;
						}

						OpenCVFilter filter = filters.get(name);
						frame = filter.process(frame); // sloppy loose original
														// frame???

						// LOG.error(cfg.get("displayFilter"));
						if (displayFilter.equals(name)) {
							// bi = filter.display(frame, null);
							published = true;
							if (sendImage) {
								// invoke("sendImage", cfg.get("displayFilter"),
								// frame.getBufferedImage()); frame? or current
								// display??
								invoke("sendImage", displayFilter,
										filter.display(frame, null));
							}
						}

					}

					if (frame.width() != lastImageWidth)
					{
						invoke("sizeChange", new Dimension(frame.width(), frame.height()));
						lastImageWidth = frame.width();
					}
					
					// if the display was not published by one of the filters
					// convert the frame now and publish it
					if (!published) {
						BufferedImage bi = frame.getBufferedImage();
						Graphics2D graphics = bi.createGraphics();

						
						/*
						 * graphics.setColor(Color.green);
						 * graphics.drawRect(120, 120, 120, 40);
						 * graphics.drawRect(80, 80, 10, 10);
						 */

						screenText.delete(0, screenText.length());
						screenText.append(sdf.format(new Date())); // TODO - configure to turn off
						screenText.append(" ");
						screenText.append(frameIndex);

						graphics.drawString(screenText.toString(), 10, 10);
						bi.flush();

						if (sendImage) {
							invoke("sendImage", displayFilter, bi);
						}
						// LOG.error(" time");
						published = true;
					}

				} else {
					if (useInput == "file") {
						// re-open avi file -
						//grabber = cvCreateFileCapture(cfg.get("inputMovieFileName"));
						// not supported at the moment
					}

				}

				frame = null; // done with current frame
				// highgui.cvRetrieveFrame(frame.pointerByReference());
				startTimeMilliseconds = 0;

			} // while (isRunning)

			try {
				if (grabber != null)
				{
					grabber.release();
				} else {
					cvReleaseCapture(oldGrabber);
				}
			} catch (Exception e) {
				LOG.error(stackToString(e));
			}
		}
	}

	public Integer setCameraIndex(Integer index) {
		cfg.set("cameraIndex", index);
		return index;
	}

	public String setInputMovieFileName(String filename) {
		//cfg.set("inputMovieFileName", filename);
		inputMovieFileName = filename;
		return filename;
	}

	public String setUseInput(String inputType) {
		useInput = inputType;
		return inputType;
	}

	public void setDisplayFilter(String name) {
		displayFilter = name;
	}

	/*
	 * publish - a series of functions used to publish data from the filters.
	 * This will allow other functions to be called from these sources with data
	 * collected from the sources.
	 */

	public Dimension sizeChange (Dimension d)
	{
		return d;
	}
	
	public String publish(String value) {
		return value;
	}

	/*
	public CvPoint2D32f[] publish(CvPoint2D32f[] features) {
		return features;
	}
	*/

	// CPP interface does not use array - but hides implementation
	public CvPoint2D32f publish(CvPoint2D32f features) {
		return features;
	}
	
	public CvPoint publish(CvPoint point) {
		return point;
	}

	public Rectangle publish(Rectangle rectangle) // TODO - going bothways here
													// - cv & awt
	{
		return rectangle;
	}

	public ArrayList<Polygon> publish(ArrayList<Polygon> polygons) {
		return polygons;
	}

	public ColoredPoint[] publish(ColoredPoint[] points) {
		return points;
	}
	
	public SerializableImage publishTemplate (String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		si.source = source;
		return si;
	}	
	
	public Boolean isTracking(Boolean b)
	{
		return b;
	}
	
	// publish functions end ---------------------------

	public void stopCapture() {
		videoProcess.stop();
		videoProcess = null;
	}
	
	public void capture() {

		if (videoProcess != null)
		{
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
			CvVideoWriter writer = cvCreateVideoWriter(cfg
					.get("outputMovieFileName"), CV_FOURCC('M', 'J','P', 'G'),
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

	public void addFilter(String name, String newFilter) {
		String type = "org.myrobotlab.image.OpenCVFilter" + newFilter;
		Object[] params = new Object[2];
		params[0] = this;
		// params[1] = cfg.getRoot();
		params[1] = name;
		OpenCVFilter filter = (OpenCVFilter) getNewInstance(type, params); // TODO
																			// -
																			// Object[]
																			// parameters
		filter.loadDefaultConfiguration();
		filters.put(name, filter);
	}

	public void removeFilter(String name) {
		filters.remove(name);
	}

	public void removeFilters() {
		filters.clear();
		/*
		 * Iterator<String> itr = filters.keySet().iterator(); while
		 * (itr.hasNext()) { filters.remove(itr.next()); }
		 */
	}

	public IplImage getLastFrame() {
		return frame;
	}

	public LinkedHashMap<String, OpenCVFilter> getFilters() {
		return filters;
	}


	@Override
	public String getToolTip() {
		return "<html>OpenCV (computer vision) service wrapping many of the functions and filters of OpenCV. ";
	}
	
	

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);

		OpenCV opencv = new OpenCV("opencv");				
		opencv.startService();
		
		Arduino arduino = new Arduino("arduino");
		arduino.startService();

		Servo pan = new Servo("pan");
		pan.startService();

		Servo tilt = new Servo("tilt");
		tilt.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		
		//opencv.addFilter("PyramidDown1", "PyramidDown");
		//opencv.addFilter("MatchTemplate1", "MatchTemplate");

		//opencv.setCameraIndex(0);
		LOG.info("starting capture");
/*		
		opencv.useInput = "camera"; // TODO - final static - for capture to take a parameter (Type)
		opencv.capture();
*/		
		gui.display();


	}
	
}
