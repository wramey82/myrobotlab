package org.myrobotlab.inmoov;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Tracking;
import org.slf4j.Logger;

public class Head {

	public final static Logger log = LoggerFactory.getLogger(Head.class);

	private InMoov inmoov;
	public boolean allowMove = true;

	public void attach(InMoov inmoov) {
		this.inmoov = inmoov;
		
		if (inmoov.arduinoHead == null)
		{
			inmoov.error("arduino for head not set, can not attach head");
		}

		inmoov.neck = (Servo) Runtime.createAndStart("neck", "Servo");
		inmoov.rothead = (Servo) Runtime.createAndStart("rothead", "Servo");
		
		inmoov.arduinoHead.servoAttach(inmoov.neck.getName(), 12);
		inmoov.arduinoHead.servoAttach(inmoov.rothead.getName(), 13);

		// initial position
		rest();

		inmoov.rothead.setPositionMin(30);
		inmoov.rothead.setPositionMax(150);
		inmoov.neck.setPositionMin(20);
		inmoov.neck.setPositionMax(160);
		
		// notify gui
		inmoov.neck.broadcastState();
		inmoov.rothead.broadcastState();
	
		inmoov.tracking = (Tracking) Runtime.create("tracking", "Tracking");
		inmoov.tracking.x = inmoov.rothead;
		inmoov.tracking.y = inmoov.neck;
		inmoov.tracking.eye = inmoov.eye;		
		inmoov.tracking.arduino = inmoov.arduinoHead;
		inmoov.tracking.startService();
		
		
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
	}

	public boolean isValid() {
		if (inmoov == null)
		{
			log.error("head can not find inmoov");
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
		return true;
	}
}
