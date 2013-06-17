package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class InverseKinematics extends Service {

	public double x;
	public double y;
	public double z;
	public double l1;
	public double l2;
	public double x2;
	public double z2;
	public double l12;
	public double l22;
	public double form;
	public double teta1;
    public double form2;
    public double form3;
    public double teta2;
    public double teta3;
	
	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InverseKinematics.class.getCanonicalName());
	
	public InverseKinematics(String n) {
		super(n, InverseKinematics.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public void setCoordinates(double a,double b,double c){
	     x = a;
	     y = b;
	     z = c;
	}
	public void setLengths(double d,double e){
		 l1 = d;
		 l2 = e;
	}
	public void computeAngles (){
		 x2 = Math.pow(x,2);
		 z2 = Math.pow(z,2);
		 l12 = Math.pow(l1,2);
		 l22 = Math.pow(l2,2);
		 form = ((x2+z2+l12-l22)/(2*l1*l2));
		 teta1 = (Math.acos(form)+Math.atan2(z,x));
		 form2 = (z-l1*Math.sin(teta1)); 
		 form3 = (x-l1*Math.cos(teta1));
		 teta1 = Math.round(Math.toDegrees(teta1));
		 teta2 = Math.round(Math.toDegrees(Math.atan2(form2, form3)));
		 teta3 = Math.round(Math.toDegrees(Math.atan2(y,x)));
		 
	}
	
	public double getTeta1(){
		
		return teta1;
		
	}
	
    public double getTeta2(){
		
		return teta2;
		
	}
    
    public double getTeta3(){
		
		return teta3;
		
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		InverseKinematics inversekinematics = new InverseKinematics("inversekinematics");
		inversekinematics.startService();
		inversekinematics.setCoordinates(50,50,0);
		inversekinematics.setLengths(100,100);
		inversekinematics.computeAngles();
		System.out.println("First rod angle is :" + inversekinematics.getTeta1() );
		System.out.println("Second rod angle is " + inversekinematics.getTeta2() );
		System.out.println("Base rotation angle is :" + inversekinematics.getTeta3() );
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
