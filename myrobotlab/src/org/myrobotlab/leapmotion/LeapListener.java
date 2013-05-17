package org.myrobotlab.leapmotion;

import java.awt.Robot;

import org.myrobotlab.service.LeapMotion;

public class LeapListener { //extends Listener {
	Robot robot;
	int hand1 = -5;
	int id;
	//LeapMotion f;
	
	private final LeapMotion myService;

	private float distx;
	private float disty;
	private float distz;
	private float distxz;
	private boolean sleep;

	public LeapListener(LeapMotion leapMotion) {
		this.myService = leapMotion;
	}

	public void onInit(String controller) {
		try {
			robot = new Robot();

		} catch (Exception e) {
			e.printStackTrace();
		}
		// f.setText("Initialized");
	}

	public void onConnect(String controller) {
		// f.setText("Connected");
	}

	public void onDisconnect(String controller) {
		// f.setText("Disconnected");
	}

	public void onExit(String controller) {
		// f.setText("Exited");
	}

	public void onFrame(String controller) {
		/*
		Frame frame = controller.frame();
		hand1 = frame.hands().get(0).id();
		if (frame.hand(hand1).isValid()
				&& frame.hand(hand1).fingers().count() > 0) {
			Pointable pointable = frame.hand(hand1).pointable(id);
			if (!pointable.isValid()) {
				pointable = frame.hand(hand1).pointables().get(0);
				id = pointable.id();
			}
			if (pointable.isValid()) {
				// ScreenList screenlist = controller.calibratedScreens();
				// Screen screen = screenlist.closestScreenHit(pointable);
				// Vector intersect = screen.intersect(pointable, true);
				// distx = (.5f - intersect.getX()) * 32768;
				// disty = -(.5f - intersect.getY()) * 32768;
				distx = -(pointable.direction().yaw() * 4f + 0.5f) * 16384;
				disty = (pointable.direction().pitch() * 4.0f + 0.5f) * 16384;
				// System.out.println(distx + " " + disty);
				distz = -(pointable.tipPosition().getZ() + 50) * 1000f;
				distxz = -(frame.hand(hand1).palmPosition().getX() - 10.0f) * 1000f;
//				if (distz > 25000)
//					pressKey(KeyEvent.VK_SHIFT);
				// System.out.println(distx+" "+disty);

				// joy.analog[Joystick.ANALOG_AXIS_X] = (int) (16384 - distxz);
				// joy.analog[Joystick.ANALOG_AXIS_Y] = (int) (16384 - distz);
				// joy.analog[Joystick.ANALOG_AXIS_Z] = (int) (16384 - distx);
				// joy.analog[Joystick.ANALOG_ROTATION_Z] = (int) (16384 -disty);
*/
				for (float i = 0; i < 100; ++i) {
				 myService.invoke("publishX", new Float((float)(i/16384f))); 
				 myService.invoke("publishY", new Float((float)(i/16384f))); 
				}
				
				// if (frame.hands().count()==2){
				// joy.digital[7] = Joystick.DIGITAL_ON;
				// } else {
				// joy.digital[7] = Joystick.DIGITAL_OFF;
				// }
				//
				// if (sleep&&frame.hand(hand1).fingers().count() ==5) {
				// joy.digital[1] = Joystick.DIGITAL_ON;
				// } else {
				// joy.digital[1] = Joystick.DIGITAL_OFF;
				// }
				//
				// if (sleep&&frame.hand(hand1).fingers().count() == 4) {
				// joy.digital[8] = Joystick.DIGITAL_ON;
				// } else {
				// joy.digital[8] = Joystick.DIGITAL_OFF;
				// }
				//
				// if (sleep&&frame.hand(hand1).fingers().count() == 3) {
				// joy.digital[3] = Joystick.DIGITAL_ON;
				// } else {
				// joy.digital[3] = Joystick.DIGITAL_OFF;
				// }
				// if(frame.hand(hand1).fingers().count() >2){
				// sleep=true;
				// }
				// else sleep=false;
				// if (frame.hand(hand1).fingers().count() == 2) {
				// joy.digital[6] = Joystick.DIGITAL_OFF;
				// }
				// if (frame.hand(hand1).fingers().count() == 1) {
				// joy.digital[6] = Joystick.DIGITAL_ON;
				// }
				//
				// }
				// }
				// else{
				// for (int n=0;n<8;n++)joy.analog[n] = 16384;
				// for (int n=0;n<16;n++)joy.digital[n] = Joystick.DIGITAL_OFF;
				// }
				// try {
				// //System.out.println("sent");
				// joy.send();
				// } catch (JoystickException e) {
				// e.printStackTrace();
		//	}
		//}
	}

//	public void pressKey(final int key) {
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				robot.keyPress(key);
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				robot.keyRelease(key);
//			}
//		}).start();
//	}

}