package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.MemoryChangeListener;
import org.myrobotlab.memory.Node;
import org.slf4j.Logger;


public class InverseKinematics extends Service {

	public static double x;
	public static double z;
	public static double l1;
	public static double l2;
	public static double x2;
	public static double z2;
	public static double l12;
	public static double l22;
	public static double form;
	public static double teta1;
    public static double form2;
    public static double form3;
    public static double teta2;
	
	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InverseKinematics.class.getCanonicalName());
	
	public InverseKinematics(String n) {
		super(n, InverseKinematics.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}
	
	public void getCoordinates(double a,double b){
		
	     x = a;
	     z = b;
	}
	public void getLenghts(double b,double c){
		 l1 = b;
		 l2 = c;
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
		 
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		InverseKinematics inversekinematics = new InverseKinematics("inversekinematics");
		inversekinematics.startService();
		inversekinematics.getCoordinates(1,0);
		inversekinematics.getLenghts(1,1);
		inversekinematics.computeAngles();
		System.out.println(teta1);
		System.out.println(teta2);
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
