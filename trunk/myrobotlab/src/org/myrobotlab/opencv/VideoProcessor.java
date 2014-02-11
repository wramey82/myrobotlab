package org.myrobotlab.opencv;

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;

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
import org.myrobotlab.image.SerializableImage;
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
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

@Root
public class VideoProcessor implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(VideoProcessor.class);

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
	transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);
	OpenCVData lastData = null;

	OpenCVData data = null;

	// FIXME - more than 1 type is being used on this in more than one context
	// BEWARE !!!!
	// FIXME - use for RECORDING & another one for Blocking for data !!!
	public BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>(); 

	/**
	 * map of video sources - allows filters
	 * to process any named source
	 */
	transient VideoSources sources = new VideoSources();

	private transient OpenCV opencv;
	private transient FrameGrabber grabber = null;
	transient Thread videoThread = null;

	private ArrayList<OpenCVFilter> filters = new ArrayList<OpenCVFilter>();

	transient SimpleDateFormat sdf = new SimpleDateFormat();
	
	transient HashMap<String, FrameRecorder> outputFileStreams = new HashMap<String, FrameRecorder>();

	public static final String INPUT_KEY = "input";

	public String boundServiceName;

	/**
	 * selected display filter unselected defaults to input
	 */
	public String displayFilter = INPUT_KEY;

	transient IplImage frame;

	private int minDelay = 0;

	/**
	 * determines if filter will process a display view and
	 * whether is will be published
	 */
	public boolean publishDisplay = true;

	/**
	 * creates a copy of the frame data leaving the original
	 * data unmarked 
	 */
	public boolean forkDisplay = false;

	private boolean recordOutput = false;
	private boolean closeOutputs = false;
	public String recordingSource = INPUT_KEY;

	private boolean showFrames = true;


	public VideoProcessor() {
		// parameterless constructor for simple xml
	}

	public OpenCV getOpencv() {
		return opencv;
	}
	
	public OpenCVData getLastData()
	{
		return lastData;
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
			
			// determine by file type - what input it is
			

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
				Logging.logTime("start");
				frame = grabber.grab();
				Logging.logTime(String.format("post-grab %d", frameIndex));
				
				//log.info(String.format("frame %d", frameIndex));
				
				if (minDelay  > 0)
				{
					Service.sleep(minDelay);
				}
				
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
				data = new OpenCVData(boundServiceName, frameIndex, displayFilter);		

				Logging.logTime("pre-synchronized-filter");
				synchronized (filters) {
					Logging.logTime("post-synchronized-filter");
					Iterator<OpenCVFilter> itr = filters.iterator();

					// sources are global - need boundServiceName for key
					sources.put(boundServiceName, INPUT_KEY, frame);
					data.put(INPUT_KEY, frame);
					
					while (capturing && itr.hasNext()) {

						OpenCVFilter filter = itr.next();
						Logging.logTime(String.format("pre set-filter %s", filter.name));
						data.setFilter(filter);
						Logging.logTime(String.format("set-filter %s", filter.name));

						// get the source image this filter is chained to
						IplImage image = sources.get(filter.sourceKey);
						if (image == null) {
							log.warn(String.format("%s has no image - waiting",filter.sourceKey));
							Service.sleep(300);
							continue;
						}
						
						// pre process for image size & channel changes
						image = filter.preProcess(frameIndex, image, data);
						Logging.logTime(String.format("preProcess-filter %s", filter.name));
						image = filter.process(image, data);
						Logging.logTime(String.format("process-filter %s", filter.name));
						image = filter.postProcess(image, data);
						Logging.logTime(String.format("postProcess-filter %s", filter.name));

						// process the image - push into source as new output
						// other pipelines will pull it off the from the sources
						sources.put(boundServiceName, filter.name, image);
						data.put(filter.name, image);

						// initial "display" is just a key to the untampered data
						// guarantees a display - even if it hasn't been processed as a display
						sources.put(boundServiceName, String.format("%s.display", filter.name), image);
						data.put(String.format("%s.display", filter.name), image);
						
						//no display || merge display || fork display
						if (publishDisplay){
							if (forkDisplay){
								// fork by making a copy, run the display on the copy, then reset the display keys
								IplImage copy = cvCreateImage(cvGetSize(image), image.depth(), image.nChannels());
								cvCopy(image, copy, null);
								filter.display(copy, data);
								sources.put(boundServiceName, String.format("%s.display", filter.name), copy);
								data.put(String.format("%s.display", filter.name), copy);
							} else {
								// run the display process on the image
								filter.display(image, data);
							}
						}
						
					} // capturing && itr.hasNext()
					Logging.logTime("filters done");
				} // synchronized (filters)
				Logging.logTime("sync done");
				
				lastData = data;
				
				// publish accumulated data
				if (publishOpenCVData) {
					opencv.invoke("publishOpenCVData", data);
				}
				
				// TODO various OpenCVData methods exposed from the OpenCV service by setting 
				// lastOpenCVData to the latest reference and returning the data from that reference
				// setting a reference to a new reference is thread safe Yay !
				
				// display needs a different publishing point
				// since it can be forked
				// make note input & output might be of interest besides display
				if (publishDisplay){
					// FIXME JUST SO YOU KNOW IT IMAGE IS SEPERATED FROM DISPLAY AT THIS POINT WITHOUT FORKING !!!! I CAN PROVE IT !!!S
					// STILL DOING COPIES !!!! 
					// if display frame
					if (showFrames ){
						//cvPutText(data.getImage(displayFilter), String.format("frame %d %d", frameIndex, System.currentTimeMillis()), cvPoint(10,20), font, CvScalar.BLACK);
						cvPutText(data.getImage(displayFilter), String.format("frame %d %d", frameIndex, System.currentTimeMillis()), cvPoint(10,20), font, CvScalar.BLACK);
					}
					
					SerializableImage display = new SerializableImage(data.getJPGBytes(displayFilter), data.getDisplayName(), frameIndex);
					//SerializableImage display2 = new SerializableImage(data.getBufferedImageDisplay(), data.getDisplayName(), frameIndex);
					Logging.logTime(String.format("post-SerializableImage frame %d %d", frameIndex, System.currentTimeMillis()));

					if (display != null) {
						opencv.invoke("publishDisplay", display);
					}
					
					if (recordOutput) {
						// TODO - add input, filter, & display
						record(data);
					}
				}

				if (useBlockingData) {
					blockingData.add(data);
				}

			} catch (Exception e) {
				Logging.logException(e);
				log.error("stopping capture");
				stop();
			}

			Logging.logTime("finished pass");
		} // while capturing

		try {
			grabber.release();
			grabber = null;
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// ------- filter methods begin ------------------
	public OpenCVFilter addFilter(String name, String newFilter) {
		String type = String.format("org.myrobotlab.opencv.OpenCVFilter%s", newFilter);
		Object[] params = new Object[1];
		params[0] = name;

		OpenCVFilter filter = (OpenCVFilter) Service.getNewInstance(type, params);
		// returns filter if added - or if dupe returns actual
		return addFilter(filter);
	}

	public OpenCVFilter addFilter(OpenCVFilter filter) {
		// important for filter to access parent data
		// and call-backs
		filter.setVideoProcessor(this);
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

	public void removeFilters() {
		synchronized (filters) {
			filters.clear();
		}
	}

	
	public void removeFilter(OpenCVFilter inFilter) {
		synchronized (filters) {
			Iterator<OpenCVFilter> itr = filters.iterator();
			while (itr.hasNext()) {
				OpenCVFilter filter = itr.next();
				if (filter == inFilter) {
					itr.remove();
					if (filters.size()-1 > 0){
						displayFilter = filters.get(filters.size()-1).name;
						log.info("remove and switch displayFilter to {}", displayFilter);
					}
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
	// ------- filter methods end ------------------
	
	/**
	 * thread safe recording of avi
	 * @param key - input, filter, or display
	 * @param data
	 */
	public void record(OpenCVData data) {
		try {

			if (!outputFileStreams.containsKey(recordingSource)) {
				// FFmpegFrameRecorder recorder = new FFmpegFrameRecorder
				// (String.format("%s.avi",filename), frame.width(),
				// frame.height());

				FrameRecorder recorder = new OpenCVFrameRecorder(String.format("%s.avi", recordingSource), frame.width(), frame.height());
				// recorder.setCodecID(CV_FOURCC('M','J','P','G'));
				// TODO - set frame rate to framerate
				recorder.setFrameRate(15);
				recorder.setPixelFormat(1);
				recorder.start();
				outputFileStreams.put(recordingSource, recorder);
			}

			// TODO - add input, filter & display
			outputFileStreams.get(recordingSource).record(data.getImage(recordingSource));
			
			if (closeOutputs){
				OpenCVFrameRecorder output = (OpenCVFrameRecorder) outputFileStreams.get(recordingSource);
				outputFileStreams.remove(output);
				output.stop();
				output.release();
				recordOutput = false;
				closeOutputs = false;
			}

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void recordOutput(Boolean b) {
		
		if (b)
		{
			recordOutput = b;
		} else {
			closeOutputs  = true;
		}
	}
	
	public FrameGrabber getGrabber() {
		return grabber;
	}
     
	public LinkedBlockingQueue<IplImage> requestFork(String filterName, String myName) {
		return null;
	}

	public void setMinDelay(int minDelay) {
		this.minDelay = minDelay;
	}
	
	public boolean showFrames(boolean b){
		showFrames = b;
		return b;
	}
}
