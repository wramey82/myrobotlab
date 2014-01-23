package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.DepthMetaData;
import org.OpenNI.GeneralException;
import org.OpenNI.License;
import org.OpenNI.MapOutputMode;
import org.OpenNI.OutArg;
import org.OpenNI.ScriptNode;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.interfaces.VideoSink;
import org.myrobotlab.service.interfaces.VideoSource;

public class GestureRecognition extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(GestureRecognition.class.getCanonicalName());

	// public UserTracker viewer;

	private OpenNIThread openniThread;

	private OutArg<ScriptNode> scriptNode;
	private Context context;
	private DepthGenerator depthGen;
	private byte[] imgbytes;
	private float histogram[];
	private int IM_WIDTH = 640;
	private int IM_HEIGHT = 480;

	ArrayList<VideoSink> sinks = new ArrayList<VideoSink>();

	public GestureRecognition(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	private DepthMetaData depthMD;

	public void configOpenNI() // FIXME - move most to constructor
	// create context and depth generator
	{
		try {
			log.info("configOpenNI begin");

			context = new Context();

			// add the NITE License
			License license = new License("PrimeSense", "0KOIk2JeIBYClPWVnMoRKn5cdY4=");
			// vendor, key
			context.addLicense(license);

			depthGen = DepthGenerator.create(context);
			depthMD = depthGen.getMetaData();

			MapOutputMode mapMode = new MapOutputMode(IM_WIDTH, IM_HEIGHT, 30);
			// xRes, yRes, FPS
			depthGen.setMapOutputMode(mapMode);

			// set Mirror mode for all
			context.setGlobalMirror(true);

			context.startGeneratingAll();

			histogram = new float[10000];
			// IM_WIDTH = depthMD.getFullXRes();
			// IM_HEIGHT = depthMD.getFullYRes();

			imgbytes = new byte[IM_WIDTH * IM_HEIGHT];

			DataBufferByte dataBuffer = new DataBufferByte(imgbytes, IM_WIDTH * IM_HEIGHT);
			Raster raster = Raster.createPackedRaster(dataBuffer, IM_WIDTH, IM_HEIGHT, 8, null);
			bimg = new BufferedImage(IM_WIDTH, IM_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
			bimg.setData(raster);

			System.out.println("Started context generating...");

			// depthMD = depthGen.getMetaData();
			// use depth metadata to access depth info (avoids bug with
			// DepthGenerator)
			log.info("configOpenNI begin");
		} catch (Exception e) {
			logException(e);
		}
	} // end of configOpenNI()

	@Override
	public void startService() {
		super.startService();
		configOpenNI();
	}

	SerializableImage simg;
	private BufferedImage bimg;

	class OpenNIThread extends Thread {

		private boolean shouldRun = true;

		public OpenNIThread(String string) {
			super(string);

		}

		public void run() {
			while (shouldRun) {
				log.debug("pre updateDepth");

				updateDepth(); // viewer.updateDepth()
				// viewer.repaint(); // publish
				log.debug("post updateDepth");

				DataBufferByte dataBuffer = new DataBufferByte(imgbytes, IM_WIDTH * IM_HEIGHT);
				Raster raster = Raster.createPackedRaster(dataBuffer, IM_WIDTH, IM_HEIGHT, 8, null);
				bimg.setData(raster);
				simg = new SerializableImage(bimg, getName());
				invoke("publishFrame", simg);
				for (int i = 0; i < sinks.size(); ++i) {
// FIXME - videosource/sink					sinks.get(i).add(simg);
				}
			}
			// frame.dispose();
		}

	}

	public void capture() {
		log.info("capture");
		if (openniThread != null) {
			openniThread.shouldRun = false;
		}

		openniThread = new OpenNIThread("openniThread");
		openniThread.start();

	}

	public SerializableImage publishFrame(SerializableImage frame) {
		log.debug("publishing frame");
		return frame;
	}

	public void add(VideoSink vs) {
		sinks.add(vs);
	}

	public void remove(VideoSink vs) {
		sinks.remove(vs);
	}

	public void stopCapture() {
		log.info("stopCapture");
		openniThread.shouldRun = false;
		openniThread = null;
	}

	private void calcHist(DepthMetaData depthMD) {
		// reset
		for (int i = 0; i < histogram.length; ++i)
			histogram[i] = 0;

		ShortBuffer depth = depthMD.getData().createShortBuffer();
		depth.rewind();

		int points = 0;
		while (depth.remaining() > 0) {
			short depthVal = depth.get();
			if (depthVal != 0) {
				histogram[depthVal]++;
				points++;
			}
		}

		for (int i = 1; i < histogram.length; i++) {
			histogram[i] += histogram[i - 1];
		}

		if (points > 0) {
			for (int i = 1; i < histogram.length; i++) {
				histogram[i] = (int) (256 * (1.0f - (histogram[i] / (float) points)));
			}
		}
	}

	void updateDepth() {
		try {
			DepthMetaData depthMD = depthGen.getMetaData();

			context.waitAnyUpdateAll();

			calcHist(depthMD);
			ShortBuffer depth = depthMD.getData().createShortBuffer();
			depth.rewind();

			// log.debug(depth.remaining());
			while (depth.remaining() > 0) {
				int pos = depth.position();
				short pixel = depth.get();
				if (pos > imgbytes.length || pixel > histogram.length) {
					log.error(String.format("here %d %d %d %d", pos, pixel, imgbytes.length, histogram.length));
				}
				imgbytes[pos] = (byte) histogram[pixel];
			}
		} catch (Exception e) {
			logException(e);
		}
	}

	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();

		GestureRecognition gr = new GestureRecognition("gr");
		gr.startService();

		Runtime.createAndStart("gui", "GUIService");
		

		/*
		 * JFrame f = new JFrame("OpenNI User Tracker"); f.addWindowListener(new
		 * WindowAdapter() { public void windowClosing(WindowEvent e)
		 * {System.exit(0);} }); UserTrackerApplication app = new
		 * UserTrackerApplication(f);
		 * 
		 * app.viewer = new UserTracker(); f.add("Center", app.viewer);
		 * f.pack(); f.setVisible(true); app.run();
		 */
	}

}
