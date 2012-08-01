package org.myrobotlab.service;

import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.DepthMetaData;
import org.OpenNI.License;
import org.OpenNI.MapOutputMode;
import org.OpenNI.StatusException;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class OpenNI extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(OpenNI.class.getCanonicalName());

	private boolean capturing = false;
	private Context context;
	int frameIndex;
	private DepthMetaData depthMD;
	private static final int IM_WIDTH = 640;
	private static final int IM_HEIGHT = 480;
	boolean publishFrame = true;
	
	SensorCaptureProcess sensorCaptureProcess =  null;

	public void configOpenNI()
	// create context and depth generator
	{
		try {
			context = new Context();

			// add the NITE License
			License license = new License("PrimeSense",
					"0KOIk2JeIBYClPWVnMoRKn5cdY4=");
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

		SimpleDateFormat sdf = new SimpleDateFormat();

		public void start() {
			log.info("starting capture");
			sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
			sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

			captureThread = new Thread(this, "OpenNI_SensorCaptureProcess");
			captureThread.start();
		}

		public void stop() {
			log.debug("stopping capture");
			capturing = false;
			captureThread = null;
		}

		public void run() {
			configOpenNI();

			capturing = true;

			while (capturing) {
				published = false;
				++frameIndex;
				logTime("start");

				try {
					context.waitAnyUpdateAll();
				} catch (StatusException e) {
					System.out.println(e);
					System.exit(1);
				}

				ShortBuffer depthBuf = depthMD.getData().createShortBuffer();
				// ptsShape.updateDepthCoords(depthBuf); - change to publish
				if (publishFrame) {
					// invoke("publishFrame", displayFilter, bi);
					invoke("publishFrame", depthBuf); // TODO - multiple formats - raw - Polar - Cartesian, Units etc.
					published = true;
				} // TODO convert to buffered image?
					// log.error(" time");

				// this call will not return until the 3D scene has been updated
			}
			// close down
			try {
				context.stopGeneratingAll();
			} catch (StatusException e) {
				logException(e);
			}
			context.release();
		} // end of run()

	}

	public OpenNI(String n) {
		super(n, OpenNI.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}


	public void capture() {

		sensorCaptureProcess = new SensorCaptureProcess();
		sensorCaptureProcess.start();
	}
	
	public void stopCapture() 
	{
		// set variable - allow capturing thread
		// to terminate cleanly and release resources
		capturing = false;

		if (sensorCaptureProcess != null) {
			sensorCaptureProcess.stop();
			sensorCaptureProcess = null;
		}

	}

	
	public ShortBuffer publishFrame (ShortBuffer depthData)
	{
		return depthData;
	}
	
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		//Logger.getRootLogger().setLevel(Level.WARN);

		OpenNI openni = new OpenNI("openni");
		openni.startService();
		
		Runtime.createAndStart("gui", "GUIService");
		
		openni.capture();

	}
	
}
