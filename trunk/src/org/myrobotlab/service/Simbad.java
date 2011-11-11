package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

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
 *
 */
public class Simbad extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Simbad.class.getCanonicalName());

	public Simbad(String n) {
		super(n, Simbad.class.getCanonicalName());
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
		
		Simbad template = new Simbad("template");
		template.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
