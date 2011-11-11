package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * @author grog
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

	public final static Logger LOG = Logger.getLogger(Jython.class.getCanonicalName());

	String inputScript = null;
	String setupScript = null;
	String msgHandlerScript  = null;
	String script = null;
	
	public Jython(String n) {
		super(n, Jython.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "Jython IDE";
	}
	
	PythonInterpreter interp = null;
	// PyObject interp.eval(String s) - for verifying?
	
	public void createPythonInterpreter ()
	{
		// TODO - check if exists - destroy / de-initialize if necessary
		PySystemState.initialize();
		interp = new PythonInterpreter();		
	}

	public void monitorAttach()
	{
		String monitorScript = FileIO.getResourceFile("python/examples/monitor.py");
		exec(monitorScript, false);
	}

	
	/**
	 * replaces and executes current Python script
	 * @param code
	 */
	public void exec (String code)
	{
		exec(code, true);
	}	
	
	
	/**
	 * replaces and executes current Python script
	 * if replace = false - will not replace "script" variable
	 * can be useful if ancillary scripts are needed e.g. monitors
	 * 
	 * @param code
	 * @param replace
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
		interp.exec(code);
	}
	
	public String getScript()
	{
		return script;
	}
	
	public void restart()
	{
		if (interp != null)
		{
			interp.cleanup();
			interp = null;
		}
		
	}

	public boolean preProcessHook(Message m)
	{
		if (m.method.equals("input"))
		{
			invoke("input", m);
			return false; // FIXME why is it boolean and not Object?
		}
		return true;
	}
	
	
	/**
	 * input 
	 * 
	 * is the callback point of data coming from MRL into the Jython code.
	 * This will handle Java input messages from other Services.  There is
	 * a corresponding "input" function in any Python code which wants to 
	 * intercept & use this data.
	 * 
	 * @param msg
	 * @return
	 */
	public Message input(Message msg)
	{
		if (interp == null)
		{
			createPythonInterpreter();
		}

		StringBuffer msgHandle = new StringBuffer();
		msgHandle.append("msg_");
		msgHandle.append(msg.sender);
		msgHandle.append("_");
		msgHandle.append(msg.sendingMethod);
		
		interp.set(msgHandle.toString(), msg);
		interp.exec("input()");
		return msg;
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
				
		Jython jython = new Jython("jython");
		jython.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
