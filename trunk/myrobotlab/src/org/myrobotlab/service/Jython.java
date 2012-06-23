package org.myrobotlab.service;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * @author GroG
 *
 * a Service to access Jython interpreter.  
 * 
 * references : 
 * http://wiki.python.org/jython/InstallationInstructions
 * http://www.jython.org/javadoc/org/python/util/PythonInterpreter.html
 * http://etutorials.org/Programming/Python+tutorial/Part+V+Extending+and+Embedding/Chapter+25.+Extending+and+Embedding+Jython/25.2+Embedding+Jython+in+Java/
 * http://wiki.python.org/moin/PythonEditors - list of editors
 * http://java-source.net/open-source/scripting-languages
 * http://java.sun.com/products/jfc/tsc/articles/text/editor_kit/ - syntax highlighting text editor
 * http://download.oracle.com/javase/tutorial/uiswing/components/generaltext.html#editorkits
 * http://download.oracle.com/javase/tutorial/uiswing/components/editorpane.html
 * http://stackoverflow.com/questions/2441525/how-to-use-netbeans-platform-syntax-highlight-with-jeditorpane
 * http://book.javanb.com/jfc-swing-tutorial-the-a-guide-to-constructing-guis-2nd/ch03lev2sec6.html
 * 
 * http://ostermiller.org/syntax/editor.html Text Editor Tutorial - with syntax highlighting
 * http://stackoverflow.com/questions/4151950/syntax-highlighting-in-jeditorpane-in-java -
 * example of non-tokenized highlighting
 * http://saveabend.blogspot.com/2008/06/java-syntax-highlighting-with.html
 * 
 * swing components
 * http://fifesoft.com/rsyntaxtextarea/ <- AMAZING PROJECT
 * http://www.pushing-pixels.org/2008/06/27/syntax-coloring-for-the-swing-editor-pane.html
 * 
 * Java Jython integration
 * http://jythonpodcast.hostjava.net/jythonbook/en/1.0/JythonAndJavaIntegration.html#using-jython-within-java-applications
 * 
 * Redirecting std out
 * http://bytes.com/topic/python/answers/40880-redirect-standard-output-jython-jtextarea
 * http://stefaanlippens.net/redirect_python_print
 * http://stackoverflow.com/questions/1000360/python-print-on-stdout-on-a-terminal
 * http://coreygoldberg.blogspot.com/2009/05/python-redirect-or-turn-off-stdout-and.html
 * https://www.ibm.com/developerworks/mydeveloperworks/blogs/JythonSwing/?lang=en
 * 
 */
public class Jython extends Service {

	private static final long serialVersionUID = 1L;

	public final static transient Logger log = Logger.getLogger(Jython.class.getCanonicalName());
	// using a HashMap means no duplicates
	private static final transient HashMap<String, Object> commandMap = new HashMap<String, Object>();
	// TODO this needs to be moved into an actual cache if it is to be used
	// Cache of compile jython code
	private static final transient HashMap<String, PyObject> objectCache = new HashMap<String, PyObject>();
	
	transient PythonInterpreter interp = null;

	String inputScript = null;
	String setupScript = null;
	String msgHandlerScript  = null;
	String script = null;
	
	boolean jythonConsoleInitialized = false;
	
	static
	{
		Method[] methods = Jython.class.getMethods();
		for (int i = 0; i < methods.length; ++i)
		{
			log.info(String.format("will filter method ", methods[i].getName()));
			commandMap.put(methods[i].getName(), null);
		}
	}
	
	/**
	 * 
	 * @param instanceName
	 */
	public Jython(String instanceName) {
		super(instanceName, Jython.class.getCanonicalName());
	}
	
	/**
	 * runs the jythonConsole.py script which creates a Jython Console object
	 * and redirect stdout & stderr to published data - these are hooked by the 
	 * GUI 
	 */
	public void attachJythonConsole()
	{
		if (!jythonConsoleInitialized)
		{
			String consoleScript = FileIO.getResourceFile("python/examples/jythonConsole.py");
			exec(consoleScript, false);
		}
	}
	
	// PyObject interp.eval(String s) - for verifying?
	
	/**
	 * 
	 */
	public void createPythonInterpreter ()
	{
		// TODO - check if exists - destroy / de-initialize if necessary
		PySystemState.initialize();
		interp = new PythonInterpreter();	

		// add self reference
		// Python scripts can refer to this service as 'jython' regardless 
		// of the actual name
		String selfReferenceScript = String.format("from org.myrobotlab.service import Runtime\n"
				+ "from org.myrobotlab.service import Jython\n"
				+ "jython = Runtime.create(\"%1$s\",\"Jython\")\n\n" // TODO - deprecate
				+ "runtime = Runtime.getInstance()\n\n"
				+ "myService = Runtime.create(\"%1$s\",\"Jython\")\n",
			this.getName());
		PyObject compiled = getCompiledMethod("initializeJython", selfReferenceScript, interp);
		interp.exec(compiled);
	}
	
	/**
	 * replaces and executes current Python script
	 * @param code
	 */
	public void exec (String code)
	{
		exec(code, true);
	}
	
	public void exec ()
	{
		exec(script, false);
	}
	
	/**
	 * replaces and executes current Python script
	 * if replace = false - will not replace "script" variable
	 * can be useful if ancillary scripts are needed e.g. monitors & consoles
	 * 
	 * @param code the code to execute
	 * @param replace replace the current script with code
	 */
	public void exec (String code, boolean replace)
	{
		if (interp == null)
		{
			createPythonInterpreter();
		}
		if (replace)
		{
			script = code;
		}
		try {
			interp.exec(code);
		} catch (Exception e) {
			Service.logException(e);
		} finally {
			invoke("finishedExecutingScript");
		}
	}
	
	/**
	 * event method when script has finished executing
	 */
	public void finishedExecutingScript()
	{
	}
	
	/**
	 * Get the current script.
	 * 
	 * @return
	 */
	public String getScript()
	{
		return script;
	}
	
	@Override
	public String getToolTip() {
		return "Jython IDE";
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
		
	/**
	 * 
	 * @param data
	 * @return
	 */
	public String publishStdOut(String data)
	{
		return data;
	}

	/**
	 * preProcessHook is used to intercept messages and process or route them before being
	 * processed/invoked in the Service.
	 * 
	 * Here all messages allowed to go and effect the Jython service will be let through.
	 * However, all messsages not found in this filter will go "into" they Jython script.
	 * There they can be handled in the scripted users code.
	 * 
	 * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.framework.Message)
	 */
	public boolean preProcessHook(Message msg)
	{
		// let the messages for this service
		// get processed normally
		if (commandMap.containsKey(msg.method))
		{
			return true;
		}
		// otherwise its target is for the
		// scripting environment 
		// set the data - and call the call-back function
		if (interp == null)
		{
			createPythonInterpreter();
		}

		StringBuffer msgHandle = new StringBuffer()
			.append("msg_")
			.append(msg.sender)
			.append("_")
			.append(msg.sendingMethod);
		log.debug(String.format("calling %1$s", msgHandle));
		// use a compiled version to make it easier on us
		PyObject compiledObject = getCompiledMethod(msgHandle.toString(), String.format("%1$s()", msg.method), interp);
		interp.set(msgHandle.toString(), msg);
		interp.exec(compiledObject);
		
		return false;
	}
	
	/**
	 * Get a compiled version of the python call.
	 * 
	 * @param msg
	 * @param interp
	 * @return
	 */
	private static synchronized PyObject getCompiledMethod(String name, String code, PythonInterpreter interp) {
		// TODO change this from a synchronized method to a few blocks to improve concurrent performance
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
	public void restart()
	{
		if (interp != null)
		{
			interp.cleanup();
			interp = null;
		}
	}

	
	// FIXME - need to replace "script" with Hashmap<filename, script> to 
	// support and IDE muti-file view
	
	/**
	 * this method can be used to load a Python script from the
	 * Jython's local file system, which may not be the GUI's local system. 
	 * Because it can be done programatically on a different machine we want
	 * to broadcast our changed state to other listeners (possibly the GUI)
	 * @param filename - name of file to load
	 * @return - success if loaded
	 */
	public boolean loadPythonScript(String filename)
	{
		String newScript = FileIO.fileToString(filename);
		if (newScript != null && !newScript.isEmpty()){
			log.info(String.format("replacing current script with %1s",filename));

			script = newScript;
			
			// tell other listeners we have changed
			// our current script
			broadcastState(); 
			return true;
		} else {
			log.warn(String.format("%1s a not valid script", filename));
			return false;
		}
	}

	public boolean loadPythonScriptFromResource(String filename)
	{
		String newScript = FileIO.getResourceFile(String.format("scripts/%1s",filename));

		if (newScript != null && !newScript.isEmpty()){
			log.info(String.format("replacing current script with %1s",filename));

			script = newScript;
			
			// tell other listeners we have changed
			// our current script
			broadcastState(); 
			return true;
		} else {
			log.warn(String.format("%1s a not valid script", filename));
			return false;
		}
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
				
		Runtime.createAndStart("jython", "Jython");
		Runtime.createAndStart("gui", "GUIService");
		
	}

}
