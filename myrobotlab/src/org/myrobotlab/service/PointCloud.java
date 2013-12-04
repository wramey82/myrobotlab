package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;

import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.DepthMetaData;
import org.OpenNI.GeneralException;
import org.OpenNI.IRGenerator;
import org.OpenNI.License;
import org.OpenNI.MapOutputMode;
import org.OpenNI.StatusException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.SensorData;
import org.slf4j.Logger;

public class PointCloud extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(PointCloud.class.getCanonicalName());

	private boolean capturing = false;
	private Context context;
	int frameIndex;
	private DepthMetaData depthMD;
	private static final int IM_WIDTH = 640;
	private static final int IM_HEIGHT = 480;
	boolean publishFrame = true;
	boolean isRecording = false;
	FileOutputStream fout = null;
	ObjectOutputStream oos = null;
	FileInputStream fin = null;
	ObjectInputStream ois = null;
	boolean isPlayingFromFile = false;
	static float[] depthLookUp = createDepthLookUpTable();

	SensorCaptureProcess sensorCaptureProcess = null;

	public static float[] createDepthLookUpTable() {
		float[] lookup = new float[2048];
		for (int i = 0; i < lookup.length; i++) {
			lookup[i] = rawDepthToMeters(i);
		}
		return lookup;
	}

	public static float rawDepthToMeters(int depthValue) {
		if (depthValue < 2047) {
			return (float) (1.0 / ((double) (depthValue) * -0.0030711016 + 3.3309495161));
		}
		return 0.0f;
	}

	public void configOpenNI()
	// create context and depth generator
	{
		try {
			context = new Context();

			// add the NITE License
			License license = new License("PrimeSense", "0KOIk2JeIBYClPWVnMoRKn5cdY4=");
			// vendor, key
			context.addLicense(license);

			DepthGenerator depthGen = DepthGenerator.create(context);

			MapOutputMode mapMode = new MapOutputMode(IM_WIDTH, IM_HEIGHT, 30);
			// xRes, yRes, FPS
			depthGen.setMapOutputMode(mapMode);

			// set Mirror mode for all
			context.setGlobalMirror(true);

			context.startGeneratingAll();
			System.out.println("Started context generating...");

			depthMD = depthGen.getMetaData();
			// use depth metadata to access depth info (avoids bug with
			// DepthGenerator)
		} catch (Exception e) {
			logException(e);
		}
	} // end of configOpenNI()

	class SensorCaptureProcess implements Runnable {

		boolean published = false;
		Thread captureThread = null;

		// OpenCVFrameGrabber grabber = null;
		// FFmpegFrameGrabber grabber = null;
		// CameraDevice.Settings cameraSettings;
		// CameraDevice cameraDevice = null;
		// FrameGrabber frameGrabber = null;
		// CvCapture frameGrabber = cvCreateCameraCapture(0);
		// VideoInputFrameGrabber grabber = null;

		// -
		// http://www.java2s.com/Tutorial/Java/0180__File/ReadinganObjectFromaFile.htm
		// "buffered!!!"

		SimpleDateFormat sdf = new SimpleDateFormat();

		public void start() {
			log.info("starting capture");
			sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
			sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

			captureThread = new Thread(this, "PointCloud_SensorCaptureProcess");
			captureThread.start();
		}

		public void stop() {
			log.debug("stopping capture");
			capturing = false;
			captureThread = null;
		}

		SensorData kd = new SensorData();

		public void run() {

			configOpenNI();
			capturing = true;

			while (capturing) {
				published = false;
				++frameIndex;
				//Logging.logTime(timerName, tag)

				try {
					if (!isPlayingFromFile) {
						context.waitAnyUpdateAll();
					}
				} catch (StatusException e) {
					logException(e);
					capturing = false;
					break;
				}

				ShortBuffer depthBuf = null;

				if (!isPlayingFromFile) {
					depthBuf = depthMD.getData().createShortBuffer();
					// "only" copy to data buffer
					depthBuf.get(kd.data);
					depthBuf.rewind();

				} else {
					try {
						kd.data = (short[]) ois.readObject();
					} catch (Exception e) {
						logException(e);
						isPlayingFromFile = false;
					}
				}

				if (isRecording) {
					try {
						oos.writeUnshared(kd.data);
					} catch (IOException e) {
						logException(e);
					}
				}

				if (publishFrame) {
					invoke("publishFrame", kd); // TODO - multiple formats
					published = true;
				} // TODO convert to buffered image?
					// log.error(" time");
			} // close down

			try {
				context.stopGeneratingAll();
			} catch (StatusException e) {
				logException(e);
			}
			context.release();
		} // end of run()

	}

	public PointCloud(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void capture() {

		sensorCaptureProcess = new SensorCaptureProcess();
		sensorCaptureProcess.start();
	}

	public void stopCapture() {
		// set variable - allow capturing thread
		// to terminate cleanly and release resources
		capturing = false;

		if (sensorCaptureProcess != null) {
			sensorCaptureProcess.stop();
			sensorCaptureProcess = null;
		}

	}

	public ShortBuffer publishFrame(ShortBuffer depthData) {
		return depthData;
	}

	public SensorData publishFrame(SensorData kd) {
		return kd;
	}

	// globals
	private static final int MAX_DEPTH_SIZE = 10000;
	private byte[] imgbytes;
	private float histogram[];

	private void updateDepthImage() {
		ShortBuffer depthBuf = depthMD.getData().createShortBuffer();
		// current depths map
		calcHistogram(depthBuf); // convert depths to ints
		depthBuf.rewind();
		// store ints as bytes in imgbytes[] pixel array
		while (depthBuf.remaining() > 0) {
			int pos = depthBuf.position(); // pixel position of depth
			short depth = depthBuf.get(); // depth measure
			imgbytes[pos] = (byte) histogram[depth];
			// store depth's grayscale at depth's pixel pos
		}
	} // end of updateDepthImage()

	// globals
	// private float histogram[];
	private int maxDepth = 0; // largest depth value

	private void calcHistogram(ShortBuffer depthBuf) {
		// reset histogram[] (stage 1)
		for (int i = 0; i <= maxDepth; i++)
			histogram[i] = 0;
		// store depth counts in histogram[];
		// a depth (an integer mm value) is used as an index
		// into the array (stage 2)
		int numPoints = 0;
		maxDepth = 0;
		while (depthBuf.remaining() > 0) {
			short depthVal = depthBuf.get();
			if (depthVal > maxDepth)
				maxDepth = depthVal;
			if ((depthVal != 0) && (depthVal < MAX_DEPTH_SIZE)) {
				// skip histogram[0]
				histogram[depthVal]++;
				numPoints++;
			}
		}
		// convert into a cummulative depth count (skipping histogram[0])
		for (int i = 1; i <= maxDepth; i++)
			// stage 3
			histogram[i] += histogram[i - 1];
		// convert cummulative depth into integers (0-255); stage 4
		if (numPoints > 0) {
			for (int i = 1; i <= maxDepth; i++)
				// skip histogram[0]
				histogram[i] = (int) (256 * (1.0f - (histogram[i] / (float) numPoints)));
		}
	} // end of calcHistogram()

	// global - IR Image hacked from begin ------------------------
	// http://fivedots.coe.psu.ac.th/~ad/jg/nui13/KinectImaging.pdf
	private IRGenerator irGen;
	private BufferedImage image = null;

	private void updateIRImage() {
		try {
			ShortBuffer irSB = irGen.getIRMap().createShortBuffer();
			// scan the IR data, storing the min and max values
			int minIR = irSB.get();
			int maxIR = minIR;
			while (irSB.remaining() > 0) {
				int irVal = irSB.get();
				if (irVal > maxIR)
					maxIR = irVal;
				if (irVal < minIR)
					minIR = irVal;
			}
			irSB.rewind();
			// convert the IR values into 8-bit grayscales
			image = createGrayIm(irSB, minIR, maxIR);
		} catch (GeneralException e) {
			System.out.println(e);
		}
	} // end of updateIRImage()

	private static final int MIN_8_BIT = 0;
	private static final int MAX_8_BIT = 255;

	// for mapping the IR values into a 8-bit range
	private BufferedImage createGrayIm(ShortBuffer irSB, int minIR, int maxIR) {
		// create a grayscale image
		BufferedImage image = new BufferedImage(IM_WIDTH, IM_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
		// access the image's data buffer
		byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		float displayRatio = (float) (MAX_8_BIT - MIN_8_BIT) / (maxIR - minIR);
		// scale the converted IR data over the grayscale range;
		int i = 0;
		while (irSB.remaining() > 0) {
			int irVal = irSB.get();
			int out;
			if (irVal <= minIR)
				out = MIN_8_BIT;
			else if (irVal >= maxIR)
				out = MAX_8_BIT;
			else
				out = (int) ((irVal - minIR) * displayRatio);
			data[i++] = (byte) out; // store in the data buffer
		}
		return image;
	} // end of createGrayIm()

	// global - hacked from end ------------------------
	// http://fivedots.coe.psu.ac.th/~ad/jg/nui13/KinectImaging.pdf

	String lastRecordedFile = "openni_20120809115522660.data";

	public void record() {
		try {
			// isPlayingFromFile = false;
			Date d = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
			formatter.setCalendar(cal);
			lastRecordedFile = String.format("%s_%s.data", getName(), formatter.format(d));
			fout = new FileOutputStream(lastRecordedFile);
			oos = new ObjectOutputStream(new BufferedOutputStream(fout));
			isRecording = true;
		} catch (Exception e) {
			logException(e);
		}
	}

	public void stopRecording() {
		try {
			oos.close();
			fout.close();
		} catch (IOException e) {
			logException(e);
		}
		isRecording = false;
	}

	public void playback() {
		playback(lastRecordedFile);
	}

	public void playback(String filename) {
		if (isRecording) {
			stopRecording();
		}

		try {
			fin = new FileInputStream(filename);
			ois = new ObjectInputStream(new BufferedInputStream(fin));
			isPlayingFromFile = true;
		} catch (Exception e) {
			logException(e);
		}

	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		PointCloud openni = new PointCloud("pc");
		openni.startService();

		Runtime.createAndStart("gui", "GUIService");

		// openni.capture();

	}

}
