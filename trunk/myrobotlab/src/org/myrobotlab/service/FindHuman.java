package org.myrobotlab.service;

import java.awt.Rectangle;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.slf4j.Logger;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;

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
	private int actservox = 90;
	private int actservoy = 90;
	private int frameSkip;
	private int frameSkipHuman;
	private boolean spokeSearch;
	private int x;
	private int y;
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
		// Runtime.createAndStart("runtime123", "Runtime");
		Runtime.createAndStart("java", "Java");
		// Runtime.createAndStart("python", "Python");

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
		twitter.setSecurity("AvSk8qD3vbOUjFID9JS8HQ",
				"BqWc8wiIIzexyYK6I5uBTEsMiJ8qNLt7bPkjaozto",
				"23911266-1OKF25SD1johsa8IXmptY47req2mnPd0aKhvAQ5Z4",
				"AvZHB1SS4pRXjrrv15fG7THU37GbD5PIQt7ztpRPU");
		twitter.configure();
		// twitter.tweet("#myrobotlab is awesome")

		// arduino ==============================================
		arduino.setSerialDevice("/dev/ttyACM0", 57600, 8, 1, 0);

		// opencv ==============================================
		opencv.startService();
		opencv.addFilter("Gray", "Gray");
		// opencv.addFilter("PyramidUp", "PyramidUp")
		// opencv.addFilter("PyramidDown1", "PyramidDown")
		// opencv.addFilter("PyramidUp1", "PyramidUopencv_highgui.cvReleaseCapture(capture);p")
		opencv.addFilter("FaceDetect1", "FaceDetect");
		// opencv.getFrameGrabber().setImageMode(ImageMode.COLOR);
		// opencv.getFrameGrabber().setImageWidth(640);
		// opencv.getFrameGrabber().setImageWidth(480);
		// aftersleep ==============================================
		opencv.addListener("publishOpenCVData", this.getName(), "input",
				OpenCVData.class);
		// opencv.setCameraIndex(1);
		// CvCapture capture=opencv_highgui.cvCreateCameraCapture(1);
		// opencv_highgui.cvSetCaptureProperty(capture,
		// opencv_highgui.CV_CAP_PROP_FRAME_WIDTH,320);
		// opencv_highgui.cvSetCaptureProperty(capture,
		// opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT,240);
		// opencv.getFrameGrabber().setImageWidth(640);
		// opencv.getFrameGrabber().setImageWidth(480);

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
		// opencv.getFrameGrabber().setImageMode(ImageMode.COLOR);
		// opencv.getFrameGrabber().setImageWidth(640);
		// opencv.getFrameGrabber().setImageWidth(480);
		// opencv.stopCapture();
		// opencv.capture();
		;

	}

	public void input(OpenCVData opencvData) {
		if (opencvData.getBoundingBoxArray() != null
				&& opencvData.getBoundingBoxArray().size() > 0) {
			Rectangle rect = opencvData.getBoundingBoxArray().get(0);
			if (frameSkipHuman == 0) {
				for (int i = 0; i < 6; i++) {
					dist -= distdir;
					raddir = ((1.0d - (Math.abs(dist) / 90d)) / speed)
							* (raddir / Math.abs(raddir));
					rad -= raddir;
				}
				raddir = -raddir;
				actservox = (int) (90d + Math.sin(rad) * dist);
				actservoy = (int) (90d + Math.cos(rad) * dist);
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
				if (frameSkipHuman == 30) {
					speech.speak("tweet tweet");
					arduino.digitalWrite(10, 1);
					arduino.digitalWrite(9, 1);
					arduino.digitalWrite(5, 0);
					opencv.setDisplayFilter("input");
					// twitter.uploadImage(opencv.getDisplay(),"Human Detected!");
				}
				frameSkip = 0;
				frameSkipHuman += 1;
				x = (rect.x + (rect.width / 2));
				y = (rect.y + (rect.height / 2));
				xpid.setInput(x);
				xpid.compute();
				actservox += xpid.getOutput();
				ypid.setInput(y);
				ypid.compute();
				actservoy += ypid.getOutput();
				pan.moveTo(actservox);
				tilt.moveTo(actservoy);
			}
		} else {

			frameSkip += 1;
			if (frameSkip > 10) {
				{
					frameSkipHuman = 0;
					if (!spokeSearch) {
						speech.speak("searching");
						spokeSearch = true;
					}
				}
				// actservox += dirx;GUIService
				// if (actservox < 11 || actservox > 169) {
				// dirx = -dirx;
				//import com.googlecode.javacv.cpp.opencv_highgui;
				//import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
				//capture=(opencv_highgui.cvCapture)((Java)java).interpret("((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture");
				//opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 640);
				//opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 480);
				// while (actservox < 11 || actservox > 169)
				// actservox += dirx;
				// actservoy += diry;
				// if (actservoy < 81 || actservoy > 159) {
				// diry = -diry;
				// actservoy += diry;
				// actservoy += diry;
				// while (actservoy < 81 || actservoy > 159)
				// actservoy += diry;
				// }
				//
				// }
				actservox = (int) (90d + Math.sin(rad) * dist);
				actservoy = (int) (90d + Math.cos(rad) * dist);
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

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}
}
//import com.googlecode.javacv.OpenCVFrameGrabber;
//import com.googlecode.javacv.cpp.opencv_highgui;
//import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
//capture=(opencv_highgui.cvCapture)((Java)java).interpret("((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture");
//opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 640);
//opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 480);


//import com.googlecode.javacv.OpenCVFrameGrabber;
//import com.googlecode.javacv.cpp.opencv_highgui;
//import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
//capture1=(opencv_highgui.cvCapture)((Java)java).interpret("((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture");
//opencv_highgui.cvReleaseCapture(capture);
//capture = opencv_highgui.cvCreateCameraCapture(1);
//opencv_highgui.cvSetCaptureProperty(capture1, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 640);
//opencv_highgui.cvSetCaptureProperty(capture1, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 480);

//import com.googlecode.javacv.OpenCVFrameGrabber;
//import com.googlecode.javacv.cpp.opencv_highgui;
//import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
//capture=(CvCapture)(((Java)java).interpret("((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture"));
//System.out.println(capture.getClass());

//import com.googlecode.javacv.OpenCVFrameGrabber;
//import com.googlecode.javacv.cpp.opencv_highgui;
//import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
//capture1=(CvCapture)(((Java)java).interpret("((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture"));
//System.out.println(capture1.getClass());
//opencv_highgui.cvReleaseCapture(capture);
//capture = opencv_highgui.cvCreateCameraCapture(1);
//opencv_highgui.cvSetCaptureProperty(capture1, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 640);
//opencv_highgui.cvSetCaptureProperty(capture1, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 480);



//===================did it!
//import com.googlecode.javacv.OpenCVFrameGrabber;
//import com.googlecode.javacv.cpp.opencv_highgui;
//import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
//capture1=(CvCapture)(((Java)java).interpret("((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture"));
//opencv_highgui.cvSetCaptureProperty(capture1, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 640);
//opencv_highgui.cvSetCaptureProperty(capture1, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 480);