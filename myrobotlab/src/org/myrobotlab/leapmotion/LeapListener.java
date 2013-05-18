package org.myrobotlab.leapmotion;

import org.myrobotlab.service.LeapMotion;

//import com.leapmotion.leap.Controller;
//import com.leapmotion.leap.Frame;
//import com.leapmotion.leap.Listener;
//import com.leapmotion.leap.Pointable;
//import com.leapmotion.leap.Screen;
//import com.leapmotion.leap.ScreenList;
//import com.leapmotion.leap.Vector;

public class LeapListener {//extends Listener {
    int hand1 = -5;
    int id;
    
    private final LeapMotion myService;

    public LeapListener(LeapMotion leapMotion) {
        this.myService = leapMotion;
    }

//    public void onInit(Controller controller) {
//        try {
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void onConnect(Controller controller) {
//    }
//
//    public void onDisconnect(Controller controller) {
//    }
//
//    public void onExit(Controller controller) {
//    }
//
//    public void onFrame(Controller controller) {
//        Frame frame = controller.frame();
//        hand1 = frame.hands().get(0).id();
//        if (frame.hand(hand1).isValid()
//                && frame.hand(hand1).fingers().count() > 0) {
//            Pointable pointable = frame.hand(hand1).pointable(id);
//            if (!pointable.isValid()) {
//                pointable = frame.hand(hand1).pointables().get(0);
//                id = pointable.id();
//            }
//            if (pointable.isValid()) {
//                 ScreenList screenlist = controller.calibratedScreens();
//                 Screen screen = screenlist.closestScreenHit(pointable);
//                 Vector intersect = screen.intersect(pointable, true);
//
//                 //myService.invoke("publishScreenX", new Float(intersect.getX())); 
//                 myService.invoke("publishScreenY", new Float(intersect.getY()));
//                 myService.invoke("publishInvScreenX", new Float(1.0-intersect.getX())); 
//                 //myService.invoke("publishInvScreenY", new Float(1.0-intersect.getY()));
//                 //myService.invoke("publishYaw1", new Float(pointable.direction().yaw())); 
//                 //myService.invoke("publishPitch1", new Float(pointable.direction().pitch()));
//                 //myService.invoke("publishRoll1", new Float(pointable.direction().roll()));
//                 //myService.invoke("publishX1", new Float(pointable.direction().getX())); 
//                // myService.invoke("publishY1", new Float(pointable.direction().getY()));
//                //myService.invoke("publishZ1", new Float(pointable.direction().getZ()));
//
//            }
//        }
//    }


}
 
