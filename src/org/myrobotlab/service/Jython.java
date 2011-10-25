package org.myrobotlab.service;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
 */
public class Jython extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Jython.class.getCanonicalName());

	String inputScript = null;
	String setupScript = null;
	String loopScript  = null;
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
	
	public void exec (String code)
	{
		if (interp == null)
		{
			createPythonInterpreter();
		}
		script = code;		
		interp.exec(script);
	}
	
	public String getScript()
	{
		return script;
	}
	
	/*
	public Object input (Message msg)
	{
		if (interp == null)
		{
			createPythonInterpreter();
		}
		
		//PyObject data = new PyObject(msg);
		
		return null;
	}
	*/
	
	public String input (String s)
	{
		StringBuffer callback = new StringBuffer();
		callback.append("input ('");
		callback.append(s);
		callback.append("')");
		//exec(callback.toString());
		if (interp == null)
		{
			createPythonInterpreter();
		}
		interp.exec(callback.toString());
		return s;
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		
		Jython jython = new Jython("jython");
		jython.startService();

/*		
String s = "# input.py\n" + 
		"def input(object):\n" + 
		"    print 'object is ', object\n" + 
		"    return object\n";
	
		s += "input(5)";
		
		//jython.exec("print \"Hello World\" ;");
		jython.exec(s);
*/		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}


}
