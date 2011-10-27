package org.myrobotlab.jython;
import java.util.logging.Logger;

import org.myrobotlab.framework.MessageType;
import org.myrobotlab.framework.Service;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 * Object factory implementation that is defined
 * in a generic fashion.
 *  
 *  Copied with love from :
 *  http://jythonpodcast.hostjava.net/jythonbook/en/1.0/JythonAndJavaIntegration.html#using-jython-within-java-applications
 *  
 *  References :
 *  http://jythonpodcast.hostjava.net/jythonbook/en/1.0/ModulesPackages.html
 *  
 */

public class JythonObjectFactory {
    private static JythonObjectFactory instance = null;
    private static PyObject pyObject = null;
	public final static Logger LOG = Logger.getLogger(JythonObjectFactory.class
			.getCanonicalName());

    
    protected JythonObjectFactory() {

    }
    /**
     * Create a singleton object. Only allow one instance to be created
     */
    public synchronized static JythonObjectFactory getInstance(){
        if(instance == null){
            instance = new JythonObjectFactory();
        }

        return instance;
    }

    /**
     * The createObject() method is responsible for the actual creation of the
     * Jython object into Java bytecode.
     */
    public static Object createObject(Object interfaceType, String moduleName){
        Object javaInt = null;
        // Create a PythonInterpreter object and import our Jython module
        // to obtain a reference.
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("print dir()");
        interpreter.exec("from " + moduleName + " import " + moduleName);

        pyObject = interpreter.get(moduleName);

        try {
            // Create a new object reference of the Jython module and
            // store into PyObject.
            PyObject newObj = pyObject.__call__();
            // Call __tojava__ method on the new object along with the interface name
            // to create the java bytecode
            javaInt = newObj.__tojava__(Class.forName(interfaceType.toString().substring(
                        interfaceType.toString().indexOf(" ")+1,
                        interfaceType.toString().length())));
        } catch (ClassNotFoundException ex) {
        	Service.logException(ex);
            //Logger.getLogger(JythonObjectFactory.class.getName()).log(Level.SEVERE, null, ex);
        }

        return javaInt;
    }
    
    public static void main(String[] args) {

    // Obtain an instance of the object factory
    JythonObjectFactory factory = JythonObjectFactory.getInstance();

    // Call the createObject() method on the object factory by
    // passing the Java interface and the name of the Jython module
    // in String format. The returning object is casted to the the same
    // type as the Java interface and stored into a variable.
    MessageType msg = (MessageType) JythonObjectFactory.createObject(
        MessageType.class, "Message");
    // Populate the object with values using the setter methods
    Object[] data = msg.getData();
    Object [] newData = new Object[2];
    newData[0] = new String("HELLO");
    newData[1] = new String("WORLD");

    System.out.println(msg.getData());

    }


}