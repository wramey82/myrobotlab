package org.myrobotlab.inmoov;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilterGray;
import org.myrobotlab.opencv.OpenCVFilterPyramidDown;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Tracking;
import org.slf4j.Logger;

public class Head {

	public final static Logger log = LoggerFactory.getLogger(Head.class);

	private InMoov inmoov;
	public boolean allowMove = true;
	// ------------- added pins and defaults
	//public int eyeXPin=3;
	//public int eyeYPin=5;
	public int neckPin=12;
	public int rotHeadPin=13;
	// ------------- added set pins
	public void setpins(Integer eyeX, Integer eyeY, Integer neck, Integer rotHead) {
		//eyeXPin=eyeX;
		//eyeYPin=eyeY;
		neckPin=neck;
		rotHeadPin=rotHead;
	}
	
	public void attach(InMoov inmoov) {
		this.inmoov = inmoov;
		
		if (inmoov.arduinoHead == null)
		{
			inmoov.error("arduino for head not set, can not attach head");
		}

		inmoov.neck = (Servo) Runtime.createAndStart("neck", "Servo");
		inmoov.rothead = (Servo) Runtime.createAndStart("rothead", "Servo");
		inmoov.eye = (OpenCV) Runtime.createAndStart("eye", "OpenCV");
		inmoov.tracking = (Tracking) Runtime.create("tracking", "Tracking");
		inmoov.eyeX = (Servo) Runtime.createAndStart("eyeX", "Servo");
		inmoov.eyeY = (Servo) Runtime.createAndStart("eyeY", "Servo");
				
		/*
		inmoov.arduinoHead.servoAttach(inmoov.neck.getName(), 12);
		inmoov.arduinoHead.servoAttach(inmoov.rothead.getName(), 13);
		*/

		// initial position

		inmoov.rothead.setPositionMin(30);
		inmoov.rothead.setPositionMax(150);
		inmoov.neck.setPositionMin(20);
		inmoov.neck.setPositionMax(160);
	
		inmoov.tracking.arduino = inmoov.arduinoHead;
		
		// name binding
		//inmoov.tracking.xName = inmoov.rothead.getName();
		//inmoov.tracking.yName = inmoov.neck.getName();
		//inmoov.tracking.opencvName = inmoov.eye.getName();
		
		inmoov.tracking.attachServos(rotHeadPin, neckPin);
		
		inmoov.tracking.eye.setInputSource("camera");
		inmoov.tracking.subscribe("publishOpenCVData", inmoov.eye.getName(), "setOpenCVData", OpenCVData.class);
		
		inmoov.tracking.startService();

		inmoov.eye.broadcastState();
		inmoov.neck.broadcastState();
		inmoov.rothead.broadcastState();
		
		rest();		
	}
	
	public void cameraOn()
	{
		cameraGray();
		cameraReduce();
		inmoov.tracking.eye.capture();
	}

	public void cameraOff()
	{
		inmoov.tracking.eye.stopCapture();
	}
	
	public void cameraGray()
	{
		OpenCVFilterGray gray = new OpenCVFilterGray("gray");
		inmoov.tracking.eye.addFilter(gray);
	}
	
	public void cameraColor()
	{
		inmoov.tracking.eye.removeFilter("gray");
	}
	
	public void cameraReduce()
	{
		OpenCVFilterPyramidDown pyramidDown = new OpenCVFilterPyramidDown("pyramidDown");
		inmoov.tracking.eye.addFilter(pyramidDown);
	}
	
	public void cameraEnlarge()
	{
		inmoov.tracking.eye.removeFilter("pyramidDown");
	}

	public void move(Integer neck, Integer rothead) {
		if (!allowMove)
		{
			return;
		}
		inmoov.neck.moveTo(neck);
		inmoov.rothead.moveTo(rothead);
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveHead(%d,%d)\n", inMoovServiceName, inmoov.neck.getPosition(), inmoov.rothead.getPosition());
	}

	public void setSpeed(Float neck2, Float rothead2) {
		inmoov.neck.setSpeed(neck2);
		inmoov.rothead.setSpeed(rothead2);
	}

	public void rest() {
		setSpeed(1.0f,1.0f);
		inmoov.neck.moveTo(90);
		inmoov.rothead.moveTo(90);
	}

	public void broadcastState() {
		inmoov.neck.broadcastState();
		inmoov.rothead.broadcastState();
	}

	public void release() {
		inmoov.rothead.releaseService();
		inmoov.rothead = null;
		inmoov.neck.releaseService();
		inmoov.neck = null;
		inmoov.tracking.releaseService();
		inmoov.tracking = null;
		inmoov.eyeX.releaseService();
		inmoov.eyeX = null;
		inmoov.eyeY.releaseService();
		inmoov.eyeY = null;
	}

	public boolean isValid() {
		if (inmoov == null)
		{
			log.error("head not attached");
			return false;
		}
		if (inmoov.arduinoHead == null)
		{
			inmoov.error("can not attach to arduino head invalid");
			return false;
		}
		if ((inmoov.rothead == null) || !inmoov.rothead.isAttached())
		{
			inmoov.error("head rotation servo not attached");
			return false;
		}
		if ((inmoov.neck == null) || !inmoov.neck.isAttached())
		{
			inmoov.error("head neck servo not attached");
			return false;
		}
		
		inmoov.neck.moveTo(92);
		inmoov.rothead.moveTo(92);
		
		return true;
	}
}
