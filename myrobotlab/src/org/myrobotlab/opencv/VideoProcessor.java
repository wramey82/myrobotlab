package org.myrobotlab.opencv;

import static org.myrobotlab.service.OpenCV.FILTER_GOOD_FEATURES_TO_TRACK;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
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
public class VideoProcessor implements Runnable {

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
	@Element
	public boolean publishOpenCVData = true;
	// GRABBER END --------------------------
	@Element
	public boolean useBlockingData = false;
	
	String lastRecordedFrameFileName;

	OpenCVData data = new OpenCVData();
	public BlockingQueue<OpenCVData> blockingData = new LinkedBlockingQueue<OpenCVData>();
	HashMap<String, IplImage> sources = new HashMap<String, IplImage>();

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
	
	/**
	 * selected display filter
	 * unselected defaults to input
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

	public void setOpencv(OpenCV opencv) {
		this.opencv = opencv;
	}

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

		capturing = true;

		/*
		 * TODO - check out opengl stuff if (useCanvasFrame) { cf = new
		 * CanvasFrame("CanvasFrame"); }
		 */

		try {

			// inputSource = INPUT_SOURCE_IMAGE_FILE;

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
			}

			Class<?> nfg = Class.forName(grabberType);
			// TODO - get correct constructor for Capture Configuration..
			Constructor<?> c = nfg.getConstructor(paramTypes);

			grabber = (FrameGrabber) c.newInstance(params);

			if (format != null) {
				grabber.setFormat(format);
			}

			log.error(String.format("using %s", grabber.getClass().getCanonicalName()));

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

		while (capturing) {
			try {

				++frameIndex;
				// Logging.logTime("start");

				frame = grabber.grab();
				if (getDepth) {
					sources.put(OpenCV.SOURCE_KINECT_DEPTH, ((OpenKinectFrameGrabber) grabber).grabDepth());
				}

				// TODO - option to accumulate? - e.g. don't new
				data = new OpenCVData(opencv.getName());
				data.put(INPUT_KEY, new SerializableImage(frame.getBufferedImage(), INPUT_KEY));
				// Logging.logTime("read");

				synchronized (filters) {
					Iterator<OpenCVFilter> itr = filters.iterator();
					sources.put(INPUT_KEY, frame);
					while (capturing && itr.hasNext()) {

						OpenCVFilter filter = itr.next();
						data.setFilterName(filter.name);

						// get the source image this filter is chained to
						IplImage image = sources.get(filter.sourceKey);
						if (image == null) {
							log.error(filter.name);
						}

						// pre process for image size & channel changes
						image = filter.preProcess(image, data);
						image = filter.process(image, data);

						// process the image - push into source as new output
						sources.put(filter.name, image);
						
						// if told to publish or last image
						if (filter.publishImage || !itr.hasNext()) {
// FIXME - memory waste ???							
							// FIXME - correct way to clone BufferedImage
							// FIXME - first bufferedImage put into data does not need to be cloned
							IplImage copy = image.clone();
							SerializableImage out = new SerializableImage(copy.getBufferedImage(), filter.name);
							data.put(filter.name, out);
						}

						// if selected || use has chosen to publish multiple
						if (isRecordingOutput || recordSingleFrame) {
							recordImage(filter, image);
						}

						// FIXME - different concept of user-viewing versus a published frame
						// its preferrable to have computer consumed published data in segregated binary forms
						// like arrays of points or arrays of boundingboxes
						// however to view - a user wants to see the overlay
						// publish display
						if (displayFilter.equals(INPUT_KEY))
						{
							opencv.invoke("publishDisplay", displayFilter, frame.getBufferedImage());							
						} else if (filter.name.equals(displayFilter) || filter.publishDisplay) {
							BufferedImage display = filter.display(image, data); 
							// FIXME - change to Serialiable image
							opencv.invoke("publishDisplay", displayFilter, display);
						}
					} // capturing && itr.hasNext()
				} // synchronized (filters)

				// publish accumulated data
				if (publishOpenCVData) {
					opencv.invoke("publishOpenCVData", data);
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

	public void recordImage(OpenCVFilter filter, IplImage image) {
		// filter - and "recording" based on what person "see in the display"
		if (filter.name.equals(displayFilter) || filter.publishDisplay) {
			BufferedImage display = filter.display(image, null); // FIXME - change to
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

	public void addFilter(String name, String newFilter) {

		log.info(String.format("request to addFilter %s, %s", name, newFilter));

		synchronized (filters) {

			String type = String.format("org.myrobotlab.opencv.OpenCVFilter%s", newFilter);
			Object[] params = new Object[4];
			params[0] = this;
			params[1] = name;
			params[2] = sources;

			// default join to the last filter
			// if there are no filters - then grab _OUTPUT

			String inputkey = INPUT_KEY;
			if (filters.size() > 0) {
				OpenCVFilter f = filters.get(filters.size() - 1);
				inputkey = String.format("%s", f.name);
			}

			params[3] = inputkey;

			OpenCVFilter filter = null;
			try {

				filter = (OpenCVFilter) Service.getNewInstance(type, params);
				filters.add(filter);

				log.info(String.format("added new filter %s, %s", name, newFilter));
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
	}

	public void removeAllFilters() {
		synchronized (filters) {
			Iterator<OpenCVFilter> itr = filters.iterator();
			while (itr.hasNext()) {
				OpenCVFilter filter = itr.next();
//ZOD				if (!filter.name.equals("input")) {
					itr.remove();
//					return;
//				}
			}
		}
	}

	public void removeFilter(String name) {
		synchronized (filters) {
			Iterator<OpenCVFilter> itr = filters.iterator();
			while (itr.hasNext()) {
				OpenCVFilter filter = itr.next();
				if (filter.name.equals(name)) {
					itr.remove();
					return;
				}
			}
		}

		log.error(String.format("removeFilter could not find %s filter", name));
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
		String filename = String.format("%s.%d.jpg", opencv.getName(), frameIndex);
		Util.writeBufferedImage(frame, filename);
		lastRecordedFrameFileName = filename;
		recordSingleFrame = false;
		return filename;
	}
	
	public String getLastRecordedFrameFileName()
	{
		return lastRecordedFrameFileName;
	}

	// FIXME
	public OpenCVData getOpenCVData() {
		OpenCVData data = null;
		try {
			blockingData.clear();

			boolean oldPublishOpenCVData = publishOpenCVData;
			publishOpenCVData = true; 
			useBlockingData = true;
			data = blockingData.take(); // TODO - poll or timeout value parameter
			publishOpenCVData = oldPublishOpenCVData;
			useBlockingData = false;
			return data;

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public OpenCVData getGoodFeatures()
	{
		removeAllFilters();
		//addFilter(FILTER_PYRAMID_DOWN, FILTER_PYRAMID_DOWN);
		addFilter(FILTER_GOOD_FEATURES_TO_TRACK, FILTER_GOOD_FEATURES_TO_TRACK);
		//setDisplayFilter(FILTER_GOOD_FEATURES_TO_TRACK);
		
		OpenCVData d = getOpenCVData();
		log.info("good features {}", d.keySet());
		removeAllFilters();
		
		return d;
		
	}
	
	// FIXME - TODO -
	/*
	 * public ArrayList<Point2Df> GoodFeaturesToTrack() { ArrayList<Point2Df>
	 * goodfeatures = null; try { filters.clear(); opencvData.clear();
	 * subscribe("publish", opencv.getName(), "GoodFeaturesToTrack",
	 * double[].class); while (!interrupted) { goodfeatures = opencvData.take();
	 * unsubscribe("publish", opencv.getName(), "GoodFeaturesToTrack",
	 * double[].class); return goodfeatures; } } catch (InterruptedException e)
	 * { e.printStackTrace(); }
	 * 
	 * return null; }
	 */

	public void record(String filename, IplImage frame) {
		try {

			/*
			 * FIXME
			 * 
			 * FFmpegFrameRecorder recorder = new
			 * FFmpegFrameRecorder("/sdcard/test.mp4",320,214); try {
			 * recorder.setAudioCodec(AV_CODEC_ID_AAC);
			 * recorder.setAudioBitrate(32000); recorder.setAudioChannels(2);
			 * recorder.setVideoCodec(AV_CODEC_ID_MPEG4);
			 * recorder.setFrameRate(10);
			 * recorder.setPixelFormat(PIX_FMT_YUV420P);
			 * recorder.setFormat("mp4"); recorder.start();
			 * recorder.record(ByteBuffer.wrap(buffer)); recorder.stop();
			 * Log.d("Recorder","Stopped"); recorder.release(); } catch
			 * (Exception e){ e.printStackTrace(); }
			 */

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

	// FIXME - different thread issue !!!! solve with Runnable swing like solution
	public void recordOutput(Boolean b) {
		isRecordingOutput = b;
		String filename = "output"; // TODO FIXME
		if (!b && outputFileStreams.containsKey(filename))
		{
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

	public void recordSingleFrame(Boolean b) {
		recordSingleFrame = b;
	}

	public FrameGrabber getGrabber() {
		return grabber;
	}

	public void setGrabber(FrameGrabber grabber) {
		this.grabber = grabber;
	}
}
