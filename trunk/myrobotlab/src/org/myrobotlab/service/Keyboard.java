package org.myrobotlab.service;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;

public class Keyboard extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Keyboard.class.getCanonicalName());

	public Keyboard(String n) {
		super(n, Keyboard.class.getCanonicalName());
	}

	public String keyCommand(String cmd) {
		log.info(cmd);
		return cmd;
	}

	@Override
	public String getToolTip() {
		return "keyboard";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Keyboard template = new Keyboard("keyboard");
		template.startService();

		Log log = new Log("log");
		log.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

}
