package org.myrobotlab.service;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;

/**
 * simple Java class which allows interaction of classes which can not be
 * instanciated on local platform
 * 
 */
public class Proxy extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Proxy.class.getCanonicalName());

	private String mimicName = null;
	private String mimicType = null;
	private Service target = null;

	// TODO - override getName & getType depending on OS/JVM

	public Proxy(String n) {
		super(n, Proxy.class.getCanonicalName());
	}

	public void setTargetService(Service s) {
		target = s;
		mimicName = s.getName();
		// mimicType = s.getClass(). FIXME - no direct getClass calls..
	}

	@Override
	public String getToolTip() {
		return "a Proxy service capable of proxying classes which can not or should not be created";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Proxy template = new Proxy("proxy");
		template.startService();
	}

}
