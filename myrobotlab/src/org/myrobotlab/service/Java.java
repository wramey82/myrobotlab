package org.myrobotlab.service;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

import edu.rice.cs.dynamicjava.Options;
import edu.rice.cs.dynamicjava.interpreter.Interpreter;
import edu.rice.cs.dynamicjava.interpreter.InterpreterException;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.reflect.PathClassLoader;
import edu.rice.cs.plt.text.ArgumentParser;
//import org.java.core.Py;
//import org.java.core.PyObject;
//import org.java.core.PyString;
//import org.java.core.PySystemState;
//import org.java.util.JavaInterpreter;

/**
 * @author GroG / raver1975
 * 
 *         a Service to access Java interpreter.
 * 
 * 
 */
public class Java extends Service {

	private static final long serialVersionUID = 1L;

	public final static transient Logger log = LoggerFactory
			.getLogger(Java.class.getCanonicalName());

	transient Interpreter interp = null;
	transient PIThread interpThread = null;
	// FIXME - this is messy !
	transient HashMap<String, Script> scripts = new HashMap<String, Script>();
	// TODO this needs to be moved into an actual cache if it is to be used

	// // Cache of compile java code
	// private static final transient HashMap<String, PyObject> objectCache;
	//
	// static {
	// objectCache = new HashMap<String, PyObject>();
	// }

	@Element
	String inputScript = null;
	@Element
	String setupScript = null;
	@Element
	String msgHandlerScript = null;
	@Element
	private Script currentScript = new Script("untitled.py", "");
	boolean javaConsoleInitialized = false;
	@Element
	String initialServiceScript = "";

	public static class Script implements Serializable {
		private static final long serialVersionUID = 1L;
		private String name;
		private String code;

		public Script() {
		}

		public Script(String name, String script) {
			this.name = name;
			this.code = script;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

	class PIThread extends Thread {
		public boolean executing = false;
		private String code;

		PIThread(String code) {
			this.code = code;
		}

		public void run() {
			try {
				executing = true;
				 System.out.println("----------------");
				 System.out.println(code);
				 System.out.println("----------------");
				if (code != null)
					interp.interpret(code);
			} catch (Exception e) {
				Logging.logException(e);
			} finally {
				executing = false;
				invoke("finishedExecutingScript");
			}

		}
	}

	/**
	 * 
	 * @param instanceName
	 */
	public Java(String instanceName) {
		super(instanceName, Java.class.getCanonicalName());

		// get all currently registered services and add appropriate java
		// handles
		HashMap<String, ServiceWrapper> svcs = Runtime.getRegistry();
		StringBuffer initScript = new StringBuffer();
		// initScript.append("from time import sleep\n");
		initScript.append("import org.myrobotlab.service.*;\n");
		Iterator<String> it = svcs.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = svcs.get(serviceName);

			initScript.append(String.format(
					"import org.myrobotlab.service.%s;\n", sw.getSimpleName()));

			// get a handle on running service
			initScript.append(String.format(
					"%s =(%s) org.myrobotlab.service.Runtime.getServiceWrapper(\"%s\").service;\n",
					serviceName,sw.getSimpleName(), serviceName));
		}

		initialServiceScript = initScript.toString();
		exec(initialServiceScript, false); // FIXME - shouldn't be done in the
											// constructor - e.g.
											// "initServicesScripts()"
		// register for addition of new services

		subscribe("registered", Runtime.getInstance().getName(), "registered",
				ServiceWrapper.class);
	}

	public void registered(ServiceWrapper s) {

		String registerScript = "";

		// load the import
		if (!"unknown".equals(s.getSimpleName())) // FIXME - RuntimeGlobals &
													// static values for
													// "unknown"
		{
			registerScript = String.format(
					"import org.myrobotlab.service.%s;\n", s.getSimpleName());
		}

		registerScript += String
				.format("%s = (%s)org.myrobotlab.service.Runtime.getServiceWrapper(\"%s\").service;\n",
						s.getName(),s.getSimpleName(),s.getName());
		exec(registerScript, false);
	}

	/**
	 * runs the javaConsole.py script which creates a Java Console object and
	 * redirect stdout & stderr to published data - these are hooked by the GUI
	 */
	public void attachPythonConsole() {
		if (!javaConsoleInitialized) {
			// String consoleScript =
			// FileIO.getResourceFile("java/examples/javaConsole.py");
			String consoleScript = getServiceResourceFile("examples/javaConsole.py");
			exec(consoleScript, false);
		}
	}

	// PyObject interp.eval(String s) - for verifying?

	String rootPath = null;
	String modulesDir = "javaModules";

	/**
	 * 
	 */
	public void createJavaInterpreter() {
		// TODO - check if exists - destroy / de-initialize if necessary
		// PySystemState.initialize();
		ArgumentParser argParser = new ArgumentParser();
		argParser.supportOption("classpath",
				IOUtil.WORKING_DIRECTORY.toString());
		argParser.supportAlias("cp", "classpath");
		ArgumentParser.Result parsedArgs = argParser.parse(".");
		Iterable<File> cp = IOUtil.parsePath(parsedArgs
				.getUnaryOption("classpath"));

		Options o=new Options(){
			@Override
			public boolean requireVariableType(){
				return false;
			}
			@Override
			public boolean enforceAllAccess(){
				return true;
			}
			@Override
			public boolean prohibitUncheckedCasts(){
				return false;
			}

			
		};
		interp = new Interpreter(o, new PathClassLoader(cp));
		// interp = new Interpreter();

		// PySystemState sys = Py.getSystemState();
		// if (rootPath != null) {
		// sys.path.append(new PyString(rootPath));
		// }
		// if (modulesDir != null) {
		// sys.path.append(new PyString(modulesDir));
		// }

		// add self reference
		// Java scripts can refer to this service as 'java' regardless
		// of the actual name
		String selfReferenceScript = String
				.format(// "import org.myrobotlab.service.Runtime;\n" +
						// "import org.myrobotlab.service.Java;\n"
				"java = org.myrobotlab.service.Runtime.create(\"%1$s\",\"Java\");\n\n" // TODO
																						// -
				// deprecate
						+ "runtime = org.myrobotlab.service.Runtime.getInstance();\n\n"
						+ "myService = org.myrobotlab.service.Runtime.create(\"%1$s\",\"Java\");\n",
						this.getName());
		// PyObject compiled = getCompiledMethod("initializeJava",
		// selfReferenceScript, interp);
		// System.out.println("----------------");
		// System.out.println(selfReferenceScript);
		// System.out.println("----------------");

		try {
			interp.interpret(selfReferenceScript);
		} catch (InterpreterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * replaces and executes current Java script
	 * 
	 * @param code
	 */
	public void exec(String code) {
		exec(code, true);
	}

	public void exec() {
		exec(currentScript.getCode(), false);
	}

	/**
	 * replaces and executes current Java script if replace = false - will not
	 * replace "script" variable can be useful if ancillary scripts are needed
	 * e.g. monitors & consoles
	 * 
	 * @param code
	 *            the code to execute
	 * @param replace
	 *            replace the current script with code
	 */
	public void exec(String code, boolean replace) {
		// System.out.println("<"+code);
		log.info(String.format("exec %s", code));
		if (interp == null) {
			createJavaInterpreter();
		}
		if (replace) {
			currentScript.setCode(code);
		}
		try {
			interpThread = new PIThread(code);
			interpThread.start();

			// interp.exec(code);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	/**
	 * event method when script has finished executing
	 */
	public void finishedExecutingScript() {
	}

	/**
	 * Get the current script.
	 * 
	 * @return
	 */
	public Script getScript() {
		return currentScript;
	}

	@Override
	public String getToolTip() {
		return "Java IDE";
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public String publishStdOut(String data) {
		return data;
	}

	/**
	 * preProcessHook is used to intercept messages and process or route them
	 * before being processed/invoked in the Service.
	 * 
	 * Here all messages allowed to go and effect the Java service will be let
	 * through. However, all messsages not found in this filter will go "into"
	 * they Java script. There they can be handled in the scripted users code.
	 * 
	 * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.framework.Message)
	 */
	public boolean preProcessHook(Message msg) {
		// let the messages for this service
		// get processed normally
		if (methodSet.contains(msg.method)) {
			return true;
		}
		// otherwise its target is for the
		// scripting environment
		// set the data - and call the call-back function
		if (interp == null) {
			createJavaInterpreter();
		}

		StringBuffer msgHandle = new StringBuffer().append("msg_")
				.append(msg.sender).append("_").append(msg.sendingMethod);
		log.debug(String.format("calling %1$s", msgHandle));
		// use a compiled version to make it easier on us
		// PyObject compiledObject = getCompiledMethod(msgHandle.toString(),
		// String.format("%1$s()", msg.method), interp);
		// System.out.println("----------------");
		String fi = msg.sender + "." + String.format("%1$s()", msg.method)
				+ ";";
		// System.out.println(fi);
		// System.out.println("----------------");
		try {
			interp.interpret(fi);
		} catch (InterpreterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// interp.exec(compiledObject);

		return false;
	}

	/**
	 * Get a compiled version of the java call.
	 * 
	 * @param msg
	 * @param interp
	 * @return
	 */
	// private static synchronized PyObject getCompiledMethod(String name,
	// String code, JavaInterpreter interp) {
	// // TODO change this from a synchronized method to a few blocks to
	// // improve concurrent performance
	// if (objectCache.containsKey(name)) {
	// return objectCache.get(name);
	// }
	// PyObject compiled = interp.compile(code);
	// if (objectCache.size() > 5) {
	// // keep the size to 6
	// objectCache.remove(objectCache.keySet().iterator().next());
	// }
	// objectCache.put(name, compiled);
	// return compiled;
	// }

	/**
	 * Get rid of the interpreter.
	 */
	public void stop() {
		if (interp != null) {
			if (interpThread != null) {
				interpThread.interrupt();
				interpThread = null;
			}
			// PySystemState.exit(); // the big hammar' throws like Thor
			// interp..cleanup();
			interp = null;
		}
	}

	public void stopService() {
		super.stopService();
		stop();// release the interpeter
	}

	public boolean loadAndExec(String filename) {
		boolean ret = loadScript(filename);
		exec();
		return ret;
	}

	// FIXME - need to replace "script" with Hashmap<filename, script> to
	// support and IDE muti-file view

	/**
	 * this method can be used to load a Java script from the Java's local file
	 * system, which may not be the GUI's local system. Because it can be done
	 * programatically on a different machine we want to broadcast our changed
	 * state to other listeners (possibly the GUI)
	 * 
	 * @param filename
	 *            - name of file to load
	 * @return - success if loaded
	 */
	public boolean loadScript(String filename) {
		String newCode = FileIO.fileToString(filename);
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s",
					filename));

			currentScript = new Script(filename, newCode);

			// tell other listeners we have changed
			// our current script
			broadcastState();
			return true;
		} else {
			log.warn(String.format("%1s a not valid script", filename));
			return false;
		}
	}

	public static int untitledDocuments = 0;

	/*
	 * public static String getName(String filename) { if (filename == null) {
	 * ++untitledDocuments; filename = String.format("untitled.%d",
	 * untitledDocuments);
	 * 
	 * } int end = filename.lastIndexOf(".py"); int begin =
	 * filename.lastIndexOf(File.separator); if (begin > 0) { ++begin; } else {
	 * begin = 0; } if (end < 0) { end = filename.length(); } return
	 * filename.substring(begin, end); }
	 */

	public boolean loadScriptFromResource(String filename) {
		log.debug(String.format("loadScriptFromResource scripts/%1s", filename));
		String newCode = getServiceResourceFile(String.format("examples/%1s",
				filename));

		log.info(String.format("loaded new scripts/%1s size %d", filename,
				newCode.length()));
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s",
					filename));

			currentScript = new Script(filename, newCode);

			// tell other listeners we have changed
			// our current script
			broadcastState();
			return true;
		} else {
			log.warn(String.format("%1s a not valid script", filename));
			return false;
		}
	}

	public String appendScript(String data) {
		currentScript.setCode(String.format("%s\n%s", currentScript.getCode(),
				data));
		return data;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// String f = "C:\\Program Files\\blah.1.py";
		// log.info(getName(f));

		Runtime.createAndStart("java", "Java");
		Runtime.createAndStart("gui", "GUIService");

	}

}
