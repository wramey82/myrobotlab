package org.myrobotlab.service;

import org.myrobotlab.control.ServiceGUI;
import org.myrobotlab.dynamicGUI.Desktop;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;




public class GUIDynamic extends GUIService {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(GUIDynamic.class.getCanonicalName());
	
	public Desktop desktop;
	
	public GUIDynamic(String n) {
		super(n);
		desktop=new Desktop();
		desktop.setVisible(true);
	}

	@Override
	public String getDescription() {
		return "drag and drop registry service";
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
	
	@Override
	public ServiceGUI createTabbedPanel(String serviceName, String guiClass, ServiceWrapper sw) {
		ServiceGUI sg=super.createTabbedPanel(serviceName, guiClass, sw);
			desktop.facelift(sg.getDisplay());
		return sg;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		GUIDynamic gui = new GUIDynamic("GUIDynamic");
		gui.startService();		
		gui.display();
		//Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
