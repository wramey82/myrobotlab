package org.myrobotlab.service;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

import simbad.gui.Simbad;
import simbad.sim.Agent;
import simbad.sim.Arch;
import simbad.sim.Box;
import simbad.sim.EnvironmentDescription;
/**
 * @author GroG
 * 
 * 
 * 
 * Dependencies :
 * 	Java3D
 * 	simbad-1.4.jar
 * 
 * Reference :
 * http://simbad.sourceforge.net/guide.php#robotapi
 * http://www.ibm.com/developerworks/java/library/j-robots/ - simbad & subsumption
 * JMonkey
 */
public class Simulator extends Service {

	private static final long serialVersionUID = 1L;
	//Simbad frame = new Simbad(new MyEnv() ,false);
	 
	public final static Logger LOG = Logger.getLogger(Simbad.class.getCanonicalName());

	public static class MyEnv extends EnvironmentDescription {
	    public MyEnv(){
	        add(new Arch(new Vector3d(3,0,-3),this));
	        //add(new Box(new Vector3d(3, 0, 0), new Vector3f(1, 1, 1),this));
	        add(new MyRobot(new Vector3d(0, 0, 0),"my robot"));
	    }
	}	
	
	public static class MyRobot extends Agent {
	    public MyRobot (Vector3d position, String name) {     
	        super(position,name);
	    }
	    public void initBehavior() {}
	    
	    public void performBehavior() {
	        if (collisionDetected()) {
	            // stop the robot
	            setTranslationalVelocity(0.0);
	            setRotationalVelocity(0);
	        } else {
	            // progress at 0.5 m/s
	            setTranslationalVelocity(0.5);
	            // frequently change orientation 
	            if ((getCounter() % 100)==0) 
	               setRotationalVelocity(Math.PI/2 * (0.5 - Math.random()));
	        }
	    }
	}	
	public Simulator(String n) {
		super(n, Simulator.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		MyEnv env = new MyEnv();
		
		Simbad frame = new Simbad(env ,false);
		
		env.add(new Box(new Vector3d(3, 0, 0), new Vector3f(1, 1, 1), env));
		//frame.
		
		Simulator template = new Simulator("simulator");
		template.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
