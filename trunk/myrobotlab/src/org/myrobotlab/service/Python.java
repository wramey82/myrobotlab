package org.myrobotlab.service;

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
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         a Service to access Python interpreter.
 * 
 *         references : http://wiki.python.org/python/InstallationInstructions
 *         http://www.python.org/javadoc/org/python/util/PythonInterpreter.html
 *         http
 *         ://etutorials.org/Programming/Python+tutorial/Part+V+Extending+and
 *         +Embedding
 *         /Chapter+25.+Extending+and+Embedding+Python/25.2+Embedding+Python
 *         +in+Java/ http://wiki.python.org/moin/PythonEditors - list of editors
 *         http://java-source.net/open-source/scripting-languages
 *         http://java.sun.com/products/jfc/tsc/articles/text/editor_kit/ -
 *         syntax highlighting text editor
 *         http://download.oracle.com/javase/tutorial
 *         /uiswing/components/generaltext.html#editorkits
 *         http://download.oracle
 *         .com/javase/tutorial/uiswing/components/editorpane.html
 *         http://stackoverflow
 *         .com/questions/2441525/how-to-use-netbeans-platform
 *         -syntax-highlight-with-jeditorpane
 *         http://book.javanb.com/jfc-swing-tutorial
 *         -the-a-guide-to-constructing-guis-2nd/ch03lev2sec6.html
 * 
 *         http://ostermiller.org/syntax/editor.html Text Editor Tutorial - with
 *         syntax highlighting
 *         http://stackoverflow.com/questions/4151950/syntax-
 *         highlighting-in-jeditorpane-in-java - example of non-tokenized
 *         highlighting
 *         http://saveabend.blogspot.com/2008/06/java-syntax-highlighting
 *         -with.html
 * 
 *         swing components http://fifesoft.com/rsyntaxtextarea/ <- AMAZING
 *         PROJECT
 *         http://www.pushing-pixels.org/2008/06/27/syntax-coloring-for-the
 *         -swing-editor-pane.html
 * 
 *         Java Python integration
 *         http://pythonpodcast.hostjava.net/pythonbook/en
 *         /1.0/PythonAndJavaIntegration
 *         .html#using-python-within-java-applications
 * 
 *         Redirecting std out
 *         http://bytes.com/topic/python/answers/40880-redirect
 *         -standard-output-python-jtextarea
 *         http://stefaanlippens.net/redirect_python_print
 *         http://stackoverflow.com
 *         /questions/1000360/python-print-on-stdout-on-a-terminal
 *         http://coreygoldberg
 *         .blogspot.com/2009/05/python-redirect-or-turn-off-stdout-and.html
 *         https
 *         ://www.ibm.com/developerworks/mydeveloperworks/blogs/PythonSwing/
 *         ?lang=en
 * 
 */
public class Python extends Service {

	private static final long serialVersionUID = 1L;

	public final static transient Logger log = LoggerFactory.getLogger(Python.class.getCanonicalName());

	transient PythonInterpreter interp = null;
	transient PIThread interpThread = null;
	// FIXME - this is messy !
	transient HashMap<String, Script> scripts = new HashMap<String, Script>();
	// TODO this needs to be moved into an actual cache if it is to be used
	// Cache of compile python code
	private static final transient HashMap<String, PyObject> objectCache;

	static {
		objectCache = new HashMap<String, PyObject>();
	}

	@Element
	String inputScript = null;
	@Element
	String setupScript = null;
	@Element
	String msgHandlerScript = null;
	@Element
	private Script currentScript = new Script("untitled.py", "");
	boolean pythonConsoleInitialized = false;
	@Element
	String initialServiceScript = "";
	
	String rootPath = null;
	String modulesDir = "pythonModules";


	public static class Script implements Serializable{
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
				interp.exec(code);
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
	public Python(String instanceName) {
		super(instanceName, Python.class.getCanonicalName());

		// get all currently registered services and add appropriate python
		// handles
		HashMap<String, ServiceWrapper> svcs = Runtime.getRegistry();
		StringBuffer initScript = new StringBuffer();
		initScript.append("from time import sleep\n");
		initScript.append("from org.myrobotlab.service import Runtime\n");
		Iterator<String> it = svcs.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = svcs.get(serviceName);

			initScript.append(String.format("from org.myrobotlab.service import %s\n", sw.getSimpleName()));

			// get a handle on running service
			initScript.append(String.format("%s = Runtime.getServiceWrapper(\"%s\").service\n", serviceName, serviceName));
		}

		initialServiceScript = initScript.toString();
		exec(initialServiceScript, false); // FIXME - shouldn't be done in the
											// constructor - e.g.
											// "initServicesScripts()"
		// register for addition of new services

		subscribe("registered", Runtime.getInstance().getName(), "registered", ServiceWrapper.class);
	}

	public void registered(ServiceWrapper s) {
		
		String registerScript = "";
				
		// load the import
		if (!"unknown".equals(s.getSimpleName())) // FIXME - RuntimeGlobals & static values for "unknown"
		{
			registerScript = String.format("from org.myrobotlab.service import %s\n", s.getSimpleName());
		} 

		registerScript += String.format("%s = Runtime.getServiceWrapper(\"%s\").service\n", s.getName(), s.getName());
		exec(registerScript, false);
	}

	/**
	 * runs the pythonConsole.py script which creates a Python Console object
	 * and redirect stdout & stderr to published data - these are hooked by the
	 * GUI
	 */
	public void attachPythonConsole() {
		if (!pythonConsoleInitialized) {
			/** REMOVE IF FLAKEY BUGS APPEAR !! */
			String consoleScript = getServiceResourceFile("examples/pythonConsole.py");
			exec(consoleScript, false);
		}
	}

	// PyObject interp.eval(String s) - for verifying?
	

	/**
	 * 
	 */
	public void createPythonInterpreter() {
		// TODO - check if exists - destroy / de-initialize if necessary
		PySystemState.initialize();
		interp = new PythonInterpreter();
		
		PySystemState sys = Py.getSystemState();
		if (rootPath != null) {
			sys.path.append(new PyString(rootPath));
		}
		if (modulesDir != null) {
        sys.path.append(new PyString(modulesDir));
		}

		// add self reference
		// Python scripts can refer to this service as 'python' regardless
		// of the actual name
		String selfReferenceScript = String.format("from org.myrobotlab.service import Runtime\n" + "from org.myrobotlab.service import Python\n"
				+ "python = Runtime.create(\"%1$s\",\"Python\")\n\n" // TODO -
																		// deprecate
				+ "runtime = Runtime.getInstance()\n\n" + "myService = Runtime.create(\"%1$s\",\"Python\")\n", this.getName());
		PyObject compiled = getCompiledMethod("initializePython", selfReferenceScript, interp);
		interp.exec(compiled);
	}

	/**
	 * replaces and executes current Python script
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
	 * replaces and executes current Python script if replace = false - will not
	 * replace "script" variable can be useful if ancillary scripts are needed
	 * e.g. monitors & consoles
	 * 
	 * @param code
	 *            the code to execute
	 * @param replace
	 *            replace the current script with code
	 */
	public void exec(String code, boolean replace) {
		log.info(String.format("exec %s", code));
		if (interp == null) {
			createPythonInterpreter();
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
		return "Python IDE";
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
	 * Here all messages allowed to go and effect the Python service will be let
	 * through. However, all messsages not found in this filter will go "into"
	 * they Python script. There they can be handled in the scripted users code.
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
			createPythonInterpreter();
		}

		StringBuffer msgHandle = new StringBuffer().append("msg_").append(msg.sender).append("_").append(msg.sendingMethod);
		log.debug(String.format("calling %1$s", msgHandle));
		// use a compiled version to make it easier on us
		PyObject compiledObject = getCompiledMethod(msgHandle.toString(), String.format("%1$s()", msg.method), interp);
		interp.set(msgHandle.toString(), msg);
		interp.exec(compiledObject);

		return false;
	}
	
	public void execFile(String filename)
	{
		String script = FileIO.fileToString(filename);
		exec(script);
	}

	/**
	 * Get a compiled version of the python call.
	 * 
	 * @param msg
	 * @param interp
	 * @return
	 */
	private static synchronized PyObject getCompiledMethod(String name, String code, PythonInterpreter interp) {
		// TODO change this from a synchronized method to a few blocks to
		// improve concurrent performance
		if (objectCache.containsKey(name)) {
			return objectCache.get(name);
		}
		PyObject compiled = interp.compile(code);
		if (objectCache.size() > 5) {
			// keep the size to 6
			objectCache.remove(objectCache.keySet().iterator().next());
		}
		objectCache.put(name, compiled);
		return compiled;
	}

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
			interp.cleanup();
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
	 * this method can be used to load a Python script from the Python's local
	 * file system, which may not be the GUI's local system. Because it can be
	 * done programatically on a different machine we want to broadcast our
	 * changed state to other listeners (possibly the GUI)
	 * 
	 * @param filename
	 *            - name of file to load
	 * @return - success if loaded
	 */
	public boolean loadScript(String filename) {
		String newCode = FileIO.fileToString(filename);
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s", filename));

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
		String newCode = getServiceResourceFile(String.format("examples/%1s", filename));

		log.info(String.format("loaded new scripts/%1s size %d", filename, newCode.length()));
		if (newCode != null && !newCode.isEmpty()) {
			log.info(String.format("replacing current script with %1s", filename));

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
		currentScript.setCode(String.format("%s\n%s", currentScript.getCode(), data));
		return data;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// String f = "C:\\Program Files\\blah.1.py";
		// log.info(getName(f));

		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("gui", "GUIService");

	}

}
