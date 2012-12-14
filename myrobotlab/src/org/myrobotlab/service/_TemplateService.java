package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class _TemplateService extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(_TemplateService.class.getCanonicalName());

	public _TemplateService(String n) {
		super(n, _TemplateService.class.getCanonicalName());
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
		
		_TemplateService template = new _TemplateService("template");
		template.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
