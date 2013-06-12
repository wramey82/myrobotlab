package org.myrobotlab.service;

import java.awt.Rectangle;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilterLKOpticalTrack;
import org.slf4j.Logger;

public class FindHuman extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(FindHuman.class
			.getCanonicalName());
	private Arduino arduino;
	private Speech speech;
	private Servo pan;
	private Servo tilt;
	private Twitter twitter;
	private OpenCV opencv;
	private PID xpid;
	private PID ypid;
	private Java java;
	private int actservox = 90;
	private int actservoy = 90;
	private int frameSkip;
	private int frameSkipHuman;
	private boolean spokeSearch;
	private int x;
	private int y;
	private double dx = 90d;
	private double dy = 90d;
	private double rad = 0;
	private double dist = 2;
	private double raddir = .2d;
	private double distdir = .3;
	private double speed = 3d;

	public FindHuman(String n) {
		super(n, FindHuman.class.getCanonicalName());

		// create services ==============================================
		arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
		speech = (Speech) Runtime.createAndStart("speech", "Speech");
		pan = (Servo) Runtime.createAndStart("pan", "Servo");
		tilt = (Servo) Runtime.createAndStart("tilt", "Servo");
		twitter = (Twitter) Runtime.createAndStart("twitter", "Twitter");
		opencv = (OpenCV) Runtime.create("opencv", "OpenCV");
		xpid = (PID) Runtime.createAndStart("xpid", "PID");
		ypid = (PID) Runtime.createAndStart("ypid", "PID");
		java = (Java) Runtime.createAndStart("java", "Java");

		// xpid ==============================================
		xpid.setMode(1);
		xpid.setOutputRange(-1, 1);
		xpid.setPID(7.0, 0.2, 0.5);
		xpid.setControllerDirection(0);
		xpid.setSetpoint(80);// #setpoint now is 80 instead of 160 because of 2
								// PD filters

		// ypid ==============================================
		ypid.setMode(1);
		ypid.setOutputRange(-1, 1);
		ypid.setPID(7.0, 0.2, 0.5);
		ypid.setControllerDirection(0);
		ypid.setSetpoint(60); // set point is now 60 instead of 120 because of 2
								// PD filters
		xpid.invert();

		// twitter ==============================================
		twitter.setSecurity("","","","");
		twitter.configure();
		// twitter.tweet("#myrobotlab is awesome")

		// arduino ==============================================
		arduino.setSerialDevice("/dev/ttyACM0", 57600, 8, 1, 0);

		// opencv ==============================================
		opencv.startService();
		opencv.addFilter("Gray", "Gray");
		// opencv.addFilter("PyramidUp", "PyramidUp")
		// opencv.addFilter("PyramidDown1", "PyramidDown");
//		opencv.addFilter("FaceDetect", "FaceDetect");
		opencv.addFilter("lk", "LKOpticalTrack");

		opencv.addListener("publishOpenCVData", this.getName(), "input",
				OpenCVData.class);
		opencv.setCameraIndex(1);

		// CvCapture capture = opencv_highgui.cvCreateCameraCapture(1);
		// opencv_highgui.cvSetCaptureProperty(capture,
		// opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 640);
		// opencv_highgui.cvSetCaptureProperty(capture,
		// opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 480);
		// IplImage grabbedImage = opencv_highgui.cvQueryFrame(capture);
		// CanvasFrame frame = new CanvasFrame("Webcam");
		// while (frame.isVisible() && (grabbedImage =
		// opencv_highgui.cvQueryFrame(capture)) != null) {
		// frame.showImage(grabbedImage);
		// }
		// frame.dispose();
		// opencv_highgui.cvReleaseCapture(capture);
		opencv.capture();

		arduino.attach(pan.getName(), 14);
		arduino.attach(tilt.getName(), 15);
		pan.moveTo(90);
		tilt.moveTo(90);

		// extra stuff to get 640x480
		// sleep(4000);
		// java.interpret("import com.googlecode.javacv.OpenCVFrameGrabber;import com.googlecode.javacv.cpp.opencv_highgui;import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;");
		// java.interpret("System.out.println(\"here\");capture=(CvCapture)(((Java)java).interpret(\"((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture\"));opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 320);opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 240);");
	}

	public void input(OpenCVData opencvData) {
		dx += Math.random() * 2d - 1d;
		dy += Math.random() * 2d - 1d;

		if (dx < 10 || dx > 170 || dy < 10 || dy > 170) {
			dx = 90d;
			dy = 90d;
		}
		if ((opencvData.getImage().getSource().equals("lk")&&opencvData.getPoints()!=null &&opencvData.getPoints().size()>0)
				|| (opencvData.getBoundingBoxArray() != null && opencvData
						.getBoundingBoxArray().size() > 0)) {
			if (opencvData.getImage().getSource().equals("lk")) {
				x=(int) (opencvData.getPoints().get(0).x*160f);
				y=(int) (opencvData.getPoints().get(0).y*120f);
			}else{
				Rectangle rect = opencvData.getBoundingBoxArray().get(0);
				x = (rect.x + (rect.width / 2));
				y = (rect.y + (rect.height / 2));
			}
			if (frameSkipHuman == 0) {
				for (int i = 0; i < 6; i++) {
					dist -= distdir;
					raddir = ((1.0d - (Math.abs(dist) / 90d)) / speed)
							* (raddir / Math.abs(raddir));
					rad -= raddir;
				}
				raddir = -raddir;
				actservox = (int) ((double) dx + Math.sin(rad) * dist);
				actservoy = (int) ((double) dy + Math.cos(rad) * dist);
				pan.moveTo(actservox);
				tilt.moveTo(actservoy);
				frameSkipHuman++;
			} else {
				if (frameSkipHuman == 5) {
					speech.speak("hello");
					spokeSearch = false;
					arduino.digitalWrite(10, 1);
					arduino.digitalWrite(9, 0);
					arduino.digitalWrite(5, 1);
				}
				frameSkip = 0;
				frameSkipHuman += 1;
				if (frameSkipHuman == 40) {
					speech.speak("tweet tweet");
					arduino.digitalWrite(10, 1);
					arduino.digitalWrite(9, 1);
					arduino.digitalWrite(5, 0);
					opencv.setDisplayFilter("input");
					//twitter.uploadImage(opencv.getDisplay(), "Human Detected!");
					opencv.removeFilter("FaceDetect");
					OpenCVFilterLKOpticalTrack jj = new OpenCVFilterLKOpticalTrack(
							"lk");
					opencv.addFilter(jj);
					jj.samplePoint(x, y);
					System.out.println("LK-TRACKING");

				}
				if (frameSkipHuman < 30 || frameSkipHuman > 39) {
					xpid.setInput(x);
					xpid.compute();
					actservox += xpid.getOutput();
					ypid.setInput(y);
					ypid.compute();
					actservoy += ypid.getOutput();
					pan.moveTo(actservox);
					tilt.moveTo(actservoy);
				}
			}
		} else {
			frameSkip += 1;
			if (frameSkip > 10) {
				{
					frameSkipHuman = 0;
					if (!spokeSearch) {
						speech.speak("searching");
						opencv.removeFilter("lk");
						opencv.addFilter("FaceDetect", "FaceDetect");
						System.out.println("FACE DETECT");
						spokeSearch = true;
					}
				}
				actservox = (int) (dx + Math.sin(rad) * dist);
				actservoy = (int) (dy + Math.cos(rad) * dist);
				raddir = ((1.0d - (Math.abs(dist) / 90d)) / speed)
						* (raddir / Math.abs(raddir));
				rad += raddir;
				dist += distdir;
				if (actservox < 40 || actservox > 140 || actservoy < 40
						|| actservoy > 140 || dist < 2) {
					distdir = -distdir;
					dist += distdir;
					dist += distdir;
				}
				pan.moveTo(actservox);
				tilt.moveTo(actservoy);
				arduino.digitalWrite(10, 0);
				arduino.digitalWrite(9, 1);
				arduino.digitalWrite(5, 1);
				x = actservox;
				y = actservoy;
			}
		}
	}

	@Override
	public String getToolTip() {
		return "Find Human pan/tilt camera";
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public void releaseService() {
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);
		FindHuman findhuman = new FindHuman("findhuman");
		findhuman.startService();
		Runtime.createAndStart("runtime", "Runtime");
		Runtime.createAndStart("gui", "GUIService");
	}
}
// import com.googlecode.javacv.OpenCVFrameGrabber;import
// com.googlecode.javacv.cpp.opencv_highgui;import
// com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
// capture=(CvCapture)(((Java)java).interpret("((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture"));opencv_highgui.cvSetCaptureProperty(capture,
// opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT,
// 640);opencv_highgui.cvSetCaptureProperty(capture,
// opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 480);

