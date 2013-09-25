package org.myrobotlab.opencv;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.OpenCV;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.OpenCVFrameRecorder;
import com.googlecode.javacv.OpenKinectFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

@Root
public class VideoProcessor implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(VideoProcessor.class.getCanonicalName());

	int frameIndex = 0;
	public boolean capturing = false;

	// GRABBER BEGIN --------------------------
	@Element
	public String inputSource = OpenCV.INPUT_SOURCE_CAMERA;
	@Element
	public String grabberType = "com.googlecode.javacv.OpenCVFrameGrabber";

	// grabber cfg
	@Element(required = false)
	public String format = null;
	@Element
	public boolean getDepth = false;
	@Element
	public int cameraIndex = 0;
	@Element
	public String inputFile = "http://localhost/videostream.cgi";
	@Element(required = false)
	public String pipelineSelected = "";
	@Element
	public boolean publishOpenCVData = true;
	// GRABBER END --------------------------
	@Element
	public boolean useBlockingData = false;

	OpenCVData data = null;

	// FIXME - more than 1 type is being used on this in more than one context
	// BEWARE !!!!
	// FIXME - use for RECORDING & another one for Blocking for data !!!
	public BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>(); 

	transient VideoSources sources = new VideoSources();
	// transient HashMap<String, IplImage> sources = new HashMap<String,
	// IplImage>();

	private transient OpenCV opencv;
	private transient FrameGrabber grabber = null;
	transient Thread videoThread = null;

	private ArrayList<OpenCVFilter> filters = new ArrayList<OpenCVFilter>();

	SimpleDateFormat sdf = new SimpleDateFormat();
	private boolean isRecordingOutput = false;
	private boolean recordSingleFrame = false;

	HashMap<String, FrameRecorder> outputFileStreams = new HashMap<String, FrameRecorder>();

	// String initialInputKey = "_OUTPUT";
	public static final String INPUT_KEY = "input";
	public static final String OUTPUT_KEY = "output";

	public String boundServiceName;

	/**
	 * selected display filter unselected defaults to input
	 */
	public String displayFilter = INPUT_KEY;

	// display
	transient IplImage frame;

	public VideoProcessor() {
		// parameterless constructor for simple xml
	}

	public OpenCV getOpencv() {
		return opencv;
	}

	// FIXME - cheesy initialization - put it all in the constructor or before
	// I assume this was done because the load() is difficult to manage !!
	public void setOpencv(OpenCV opencv) {
		this.opencv = opencv;
		this.boundServiceName = opencv.getName();
	}

	public void start() {
		log.info("starting capture");
		sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
		sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

		if (videoThread != null) {
			log.info("video processor already started");
			return;
		}
		videoThread = new Thread(this, String.format("%s_videoProcessor", opencv.getName()));
		videoThread.start();
	}

	public void stop() {
		log.debug("stopping capture");
		capturing = false;
		videoThread = null;
	}

	public void run() {

		capturing = true;

		/*
		 * TODO - check out opengl stuff if (useCanvasFrame) { cf = new
		 * CanvasFrame("CanvasFrame"); }
		 */

		try {

			// inputSource = INPUT_SOURCE_IMAGE_FILE;
			log.info(String.format("video source is %s", inputSource));

			Class<?>[] paramTypes = new Class[1];
			Object[] params = new Object[1];

			if (OpenCV.INPUT_SOURCE_CAMERA.equals(inputSource)) {
				paramTypes[0] = Integer.TYPE;
				params[0] = cameraIndex;
			} else if (OpenCV.INPUT_SOURCE_MOVIE_FILE.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = inputFile;
			} else if (OpenCV.INPUT_SOURCE_IMAGE_FILE.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = inputFile;
			} else if (OpenCV.INPUT_SOURCE_PIPELINE.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = pipelineSelected;
			} else if (OpenCV.INPUT_SOURCE_NETWORK.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = inputFile;
			}

			log.info(String.format("attempting to get frame grabber %s format %s", grabberType, format));
			Class<?> nfg = Class.forName(grabberType);
			// TODO - get correct constructor for Capture Configuration..
			Constructor<?> c = nfg.getConstructor(paramTypes);

			grabber = (FrameGrabber) c.newInstance(params);

			if (format != null) {
				grabber.setFormat(format);
			}

			log.info(String.format("using %s", grabber.getClass().getCanonicalName()));

			if (grabber == null) {
				log.error(String.format("no viable capture or frame grabber with input %s", grabberType));
				stop();
			}

			if (grabber != null) {
				grabber.start();
			}

		} catch (Exception e) {
			Logging.logException(e);
			stop();
		}
		// TODO - utilize the size changing capabilites of the different
		// grabbers
		// grabbler.setImageWidth()
		// grabber.setImageHeight(320);
		// grabber.setImageHeight(240);

		log.info("beginning capture");
		while (capturing) {
			try {

				++frameIndex;
				// Logging.logTime("start");

				frame = grabber.grab();
				if (frame == null)
				{
					log.warn("frame is null");
					Service.sleep(300); // prevent thrashing
					continue;
				}
				
				if (getDepth && grabber.getClass() == OpenKinectFrameGrabber.class) {
					sources.put(boundServiceName, OpenCV.SOURCE_KINECT_DEPTH, ((OpenKinectFrameGrabber) grabber).grabDepth());
				}

				// TODO - option to accumulate? - e.g. don't new
				data = new OpenCVData(boundServiceName);
				// data.put(INPUT_KEY, new
				// SerializableImage(frame.getBufferedImage(), INPUT_KEY));
				// Logging.logTime("read");

				synchronized (filters) {
					Iterator<OpenCVFilter> itr = filters.iterator();
					sources.put(boundServiceName, INPUT_KEY, frame);
					data.put(boundServiceName, INPUT_KEY, frame);
					while (capturing && itr.hasNext()) {

						OpenCVFilter filter = itr.next();
						data.setFilter(filter);

						// get the source image this filter is chained to
						IplImage image = sources.get(filter.sourceKey);
						if (image == null) {
							log.warn("{} has no image - waiting",filter.name);
							Service.sleep(300);
							continue;
						}

						// pre process for image size & channel changes
						image = filter.preProcess(image, data);
						image = filter.process(image, data);
						image = filter.prostProcess(image, data);

						// process the image - push into source as new output
						// other pipelines will pull it off the from the sources
						sources.put(boundServiceName, filter.name, image);

						// if told to publish or last image add a reference
						// to the data
						if (filter.publishImage || !itr.hasNext()) {
							// left to the individual filter to determine if the object is "a copy" or just
							// a reference - don't clone - cvcopy seems the "safest"
							data.put(boundServiceName, filter.name, image);
						}

						// if selected || use has chosen to publish multiple
						if (isRecordingOutput || recordSingleFrame) {
							recordImage(filter, image, data);
						}

						// "display" is typically for human consumption
						// a separate "display" method is in all filters - its left up to the discretion
						// of the filter to produce the appropriate display
						// TODO - make it possible for displayFilter == null which will make "no" display
						if (filter.name.equals(displayFilter) || INPUT_KEY.equals(displayFilter) || filter.publishDisplay) {
							BufferedImage display = null;
							if (INPUT_KEY.equals(displayFilter)) {
								display = frame.getBufferedImage();
							} else {
								display = filter.display(image, data);
							}
							 
							// FIXME - change to serializable image
							opencv.invoke("publishDisplay", displayFilter, display);
						}
					} // capturing && itr.hasNext()
				} // synchronized (filters)

				// publish accumulated data
				if (publishOpenCVData) {
					opencv.invoke("publishOpenCVData", data);
				}

				// no filters - no filters selected
				if (filters.size() == 0) {
					opencv.invoke("publishDisplay", displayFilter, frame.getBufferedImage());
				}

				if (useBlockingData) {
					blockingData.add(data);
				}

			} catch (Exception e) {
				Logging.logException(e);
				log.error("stopping capture");
				stop();
			}

		} // while capturing

		try {
			grabber.release();
			grabber = null;
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void recordImage(OpenCVFilter filter, IplImage image, OpenCVData data) {
		// filter - and "recording" based on what person "see in the display"
		if (filter.name.equals(displayFilter) || filter.publishDisplay) {
			BufferedImage display = filter.display(image, data); // FIXME -
																	// change to
			// SerilizabelImage
			opencv.invoke("publishDisplay", displayFilter, display);

			if (isRecordingOutput == true) {
				// FIXME - from IplImage->BufferedImage->IplImage :P
				record("output", IplImage.createFrom(display));
			}

			if (recordSingleFrame == true) {
				recordSingleFrame(display, frameIndex);
			}
		}
	}

	public OpenCVFilter addFilter(String name, String newFilter) {
		String type = String.format("org.myrobotlab.opencv.OpenCVFilter%s", newFilter);
		Object[] params = new Object[1];
		params[0] = name;

		OpenCVFilter filter = null;
		try {

			filter = (OpenCVFilter) Service.getNewInstance(type, params);
		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}
		
		// returns filter if added - or if dupe returns actual
		return addFilter(filter);
	}

	public OpenCVFilter addFilter(OpenCVFilter filter) {
		filter.vp = this;
		synchronized (filters) {
			
			for (int i = 0; i < filters.size(); ++i)
			{
				if (filter.name.equals(filters.get(i).name))
				{
					log.warn("duplicate filter name {}", filter.name);
					return filters.get(i);
				}
			}
			
			if (filter.sourceKey == null) {
				filter.sourceKey = String.format("%s.%s", boundServiceName, INPUT_KEY);
				if (filters.size() > 0) {
					OpenCVFilter f = filters.get(filters.size() - 1);
					filter.sourceKey = String.format("%s.%s", boundServiceName, f.name);
				}
			}

			filters.add(filter);
			log.info(String.format("added new filter %s.%s, %s", boundServiceName, filter.name, filter.getClass().getCanonicalName()));
		}
		
		return filter;
	}

	public void clearFilters() {
		synchronized (filters) {
			filters.clear();
		}
	}

	/* deprecated
	public void removeFilter(String name) {
		removeFilter(getFilter(name));
	}
	*/
	
	public void removeFilter(OpenCVFilter inFilter) {
		synchronized (filters) {
			Iterator<OpenCVFilter> itr = filters.iterator();
			while (itr.hasNext()) {
				OpenCVFilter filter = itr.next();
				if (filter == inFilter) {
					itr.remove();
					displayFilter = filters.get(filters.size()-1).name;
					log.info("remove and switch displayFilter to {}", displayFilter);
					//opencv.setDisplayFilter(filters.get(filters.size()-1).name);
					return;
				}
			}
		}

		log.error(String.format("removeFilter could not find %s filter", inFilter.name));
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

	public String recordSingleFrame(BufferedImage frame, int frameIndex) {
		try {
			String filename = String.format("%s.%d.jpg", boundServiceName, frameIndex);
			Util.writeBufferedImage(frame, filename);
			
			// FIXME - MESSY - WHAT IF THIS IS ATTEMPTED TO PROCESS THROUGH
			// FILTERS !!!
			blockingData.put(filename);
			recordSingleFrame = false;
			return filename;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}



	public void record(String filename, IplImage frame) {
		try {

			if (!outputFileStreams.containsKey(filename)) {
				// FFmpegFrameRecorder recorder = new FFmpegFrameRecorder
				// (String.format("%s.avi",filename), frame.width(),
				// frame.height());

				FrameRecorder recorder = new OpenCVFrameRecorder(String.format("%s.avi", filename), frame.width(), frame.height());
				// recorder.setCodecID(CV_FOURCC('M','J','P','G'));
				recorder.setFrameRate(15);
				recorder.setPixelFormat(1);
				recorder.start();
				outputFileStreams.put(filename, recorder);
			}

			outputFileStreams.get(filename).record(frame);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// FIXME - different thread issue !!!! solve with Runnable swing like
	// solution
	public void recordOutput(Boolean b) {
		isRecordingOutput = b;
		String filename = "output"; // TODO FIXME
		if (!b && outputFileStreams.containsKey(filename)) {
			FrameRecorder recorder = outputFileStreams.get(filename);
			try {
				log.error("****about to stop recorder*****");
				recorder.stop();
				log.error("****about to release recorder*****");
				recorder.release();
				log.error("****released recorder - Yay*****");
			} catch (com.googlecode.javacv.FrameRecorder.Exception e) {
				Logging.logException(e);
			}
		}
	}

	public String recordSingleFrame(Boolean b) {
		recordSingleFrame = b;
		// blocking until filename is set
		// waiting for return of frame

		try {
			Object o = blockingData.take();
			if (o.getClass() == String.class) {
				return (String) o;
			} else {
				log.error("SHITE !!! - grabbed something which wasn't mine!!!");
			}
		} catch (InterruptedException e) {
			Logging.logException(e);
		}

		return null;
	}

	public FrameGrabber getGrabber() {
		return grabber;
	}

	public void setGrabber(FrameGrabber grabber) {
		this.grabber = grabber;
	}

	public LinkedBlockingQueue<IplImage> requestFork(String filterName, String myName) {
		return null;
	}
}
