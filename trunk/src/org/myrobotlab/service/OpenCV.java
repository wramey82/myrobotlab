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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SimpleTimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
import com.googlecode.javacv.VideoInputFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
import com.googlecode.javacv.cpp.opencv_highgui.CvVideoWriter;



public class OpenCV extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCV.class.getCanonicalName());

	int frameIndex = 0;

	int lastImageWidth = 0;
	
	//CvCapture grabber = null;
	VideoProcess videoProcess = null;
	
	// display
	CanvasFrame cf = null;
	IplImage frame = null;
	// CvMemStorage storage = null;
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
			if (vertices > 3 && vertices < 6 && isConvex) // fudge a square -
															// fudge is square !
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

		cfg.set("cameraIndex", 0);
		// cfg.set("pixelsPerDegree", 7);
		cfg.set("useInput", "null");
		cfg.set("inputMovieFileName", "outfile1.avi");
		cfg.set("inputMovieFileLoop", true);
		// cfg.set("outputMovieFileName", "out.avi");
		cfg.set("performanceTiming", false);
		cfg.set("sendImage", true);
		cfg.set("useCanvasFrame", false);
		cfg.set("displayFilter", "output");
	}

	
	
	/* TODO - remove
	final public void pause(Integer length) {
		try {
			// videoThread.sleep(length);
			Thread.sleep(length);
		} catch (InterruptedException e) {
			return;
		}
	}
	*/

	@Override
	public void startService() {
		super.startService();
		//grabber();
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

	public BlockingQueue<BufferedImage> images = new ArrayBlockingQueue<BufferedImage>(
			100);

	public BufferedImage input(BufferedImage bi) {
		try {
			images.put(bi);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bi;
	}

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

		FrameGrabber grabber = null;
		CvCapture oldGrabber = null; // TODO - choose index
		
		boolean isWindows = false;		

		public void start()
		{
			sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
			sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

			videoThread = new Thread(this, "OpenCV_videoProcessor");
			videoThread.start();
		}
		
		public void stop ()
		{
			isCaptureRunning = false;
			videoThread = null;
		}
		

		public void run() {
			
			StringBuffer screenText = new StringBuffer();			
			isCaptureRunning = true;
			
			isWindows = Loader.getPlatformName().startsWith("windows");
			
			if (cfg.getBoolean("useCanvasFrame")) {
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
				int index = cfg.getInt("cameraIndex");
				
				if (isWindows)
				{
					grabber = new VideoInputFrameGrabber(index);
					grabber.start();
				} else {
					oldGrabber = cvCreateCameraCapture(index);
					if (oldGrabber == null)
					{
						stop ();
					}
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
			
			while (isCaptureRunning) {
				
				published = false;
				//isCaptureStopped = false;
				++frameIndex;
				logTime("start");


				//double x = highgui.cvGetCaptureProperty(grabber, highgui.CV_CAP_PROP_FRAME_WIDTH);
				//highgui.cvSetCaptureProperty( grabber, highgui.CV_CAP_PROP_FRAME_WIDTH, 320);
				//highgui.cvSetCaptureProperty( grabber, highgui.CV_CAP_PROP_FRAME_HEIGHT, 240);
					//frame = grabber.grab();
					if (!isWindows)
					{
						frame = cvQueryFrame(oldGrabber);
					} else {
						try {
							frame = grabber.grab();						
						} catch (Exception e) {
							LOG.error(stackToString(e));
						}
					}


				logTime("read");

				if (frame != null) {
					Iterator<String> itr = filters.keySet().iterator();

					while (itr.hasNext()) {
						String name;
						try {
							name = itr.next();
						} catch (ConcurrentModificationException c) {
							break;
						}

						OpenCVFilter filter = filters.get(name);
						frame = filter.process(frame); // sloppy loose original
														// frame???

						// LOG.error(cfg.get("displayFilter"));
						if (cfg.get("displayFilter").compareTo(name) == 0) {
							// bi = filter.display(frame, null);
							published = true;
							if (cfg.getBoolean("sendImage")) {
								// invoke("sendImage", cfg.get("displayFilter"),
								// frame.getBufferedImage()); frame? or current
								// display??
								invoke("sendImage", cfg.get("displayFilter"),
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

						if (cfg.getBoolean("sendImage")) {
							invoke("sendImage", cfg.get("displayFilter"), bi);
						}
						// LOG.error(" time");
						published = true;
					}

				} else {
					if (cfg.get("useInput") == "file") {
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
				if (isWindows)
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
		cfg.set("inputMovieFileName", filename);
		return filename;
	}

	public String setUseInput(String inputType) {
		cfg.set("useInput", inputType);
		return inputType;
	}

	public void setDisplayFilter(String name) {
		cfg.set("displayFilter", name);
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

	public CvPoint2D32f[] publish(CvPoint2D32f[] features) {
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

	// publish functions end ---------------------------

	public void stopCapture() {
		videoProcess.stop();
		videoProcess = null;
	}
	
	public void capture() {

		if (videoProcess == null)
		{
			// get type
			videoProcess = new VideoProcess();
		}

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

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		OpenCV opencv = new OpenCV("opencv");
		GUIService2 gui = new GUIService2("gui");
		gui.startService();
		opencv.startService();

		gui.display();

		
		opencv.addFilter("Smooth", "Smooth");
		opencv.addFilter("Dilate1", "Dilate"); 
		opencv.addFilter("InRange","InRange");
		opencv.addFilter("Dilate2", "Dilate");

		// yellow blocks
		/*
		 * opencv.setFilterCFG("InRange", "hueMin", 0x19);
		 * opencv.setFilterCFG("InRange", "hueMax", 0x33); // 1A range
		 * opencv.setFilterCFG("InRange", "valueMin", 0xe1);
		 * opencv.setFilterCFG("InRange", "valueMax", 0xfe);// 1D range
		 */

		// green blocks
		/*
		 * opencv.setFilterCFG("InRange", "hueMin", 0x40);
		 * opencv.setFilterCFG("InRange", "hueMax", 0x4f);
		 * opencv.setFilterCFG("InRange", "valueMin", 0x70);
		 * opencv.setFilterCFG("InRange", "valueMax", 0x80);
		 */

		// green led
		// 58 - 5b
		// f3f5 - f6
		// sat 16 - 17
		/*
		 opencv.setFilterCFG("InRange", "hueMin", 0x6c);
		 opencv.setFilterCFG("InRange", "hueMax", 0x73);
		 opencv.setFilterCFG("InRange", "valueMin", 0xc9);
		 opencv.setFilterCFG("InRange", "valueMax", 0xd8);
		 */
		
		 opencv.setFilterCFG("InRange", "hueMin", 0x6b);
		 opencv.setFilterCFG("InRange", "hueMax", 0x75);
		 opencv.setFilterCFG("InRange", "valueMin", 0xc5);
		 opencv.setFilterCFG("InRange", "valueMax", 0xda);

		/*
		 * String cfgName = "hueMin"; Float cfgValue = 25.0f;
		 * 
		 * Object[] p = new Object[2]; p[0] = cfgName; p[1] = cfgValue;
		 * opencv.invokeFilterMethod("InRange", "setCFG", p);
		 * 
		 * //cfgName = "hueMax"; //cfgValue = 51.0f; p[0] = "hueMax"; p[1] =
		 * 51.0f; opencv.invokeFilterMethod("InRange", "setCFG", p);
		 */

	}

	@Override
	public String getToolTip() {
		return "<html>OpenCV (computer vision) service wrapping many of the functions and filters of OpenCV. ";
	}
	
}
