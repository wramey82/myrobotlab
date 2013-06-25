package org.myrobotlab.framework;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.arduino.compiler.RunnerException;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.service.Clock;
import org.slf4j.Logger;

// Service processing should be subset of "any" class processing

public class SOAP {

	// http://www.soapclient.com/soaptest.html
	public final static Logger log = LoggerFactory.getLogger(Clock.class.getCanonicalName());

	String getWSDL(Class<?> type) {
		HashSet<String> filter = new HashSet<String>();
		filter = new HashSet<String>();
		filter.add("startClock");
		filter.add("stopClock");
		filter.add("pulse");
		filter.add("setData");
		return getWSDL(type, filter, true);
	}

	public static final Set<Class<?>> WRAPPER_TYPES = new HashSet<Class<?>>(Arrays.asList(Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class,
			Float.class, Double.class, Void.class));

	public HashSet<String> getFilter()
	{
		HashSet<String> filter = new HashSet<String>();
		filter.add("main");
		filter.add("invoke");
		filter.add("load");
		filter.add("in");
		filter.add("out");
		filter.add("sleep");
		filter.add("save");
		filter.add("initialize");
		filter.add("getHost");
		filter.add("getHostName");
		filter.add("loadServiceDefaultConfiguration");
		filter.add("loadGlobalMachineDefaults");
		filter.add("createAndStartSubServices");
		filter.add("preRoutingHook");
		filter.add("preProcessHook");
		filter.add("createMessage");
		filter.add("getIntanceName");
		filter.add("getNewInstance");
		filter.add("getNewInstance");
		filter.add("stackToString");
		filter.add("removeListener");
		filter.add("removeAllListeners");
		filter.add("getThisThread");
		filter.add("setThisThread");
		filter.add("connectionBroken");
		filter.add("logException");
		filter.add("getCFG");
		filter.add("getHostCFG");
		filter.add("getComm");
		filter.add("getOutbox");
		filter.add("getInbox");
		filter.add("setHost");
		filter.add("wait");
		filter.add("equals");
		filter.add("notify");
		filter.add("notifyAll");
		
		return filter;
	}

	// public boolean isPrimitive(Class<?>)
	
	public boolean parameterHasComplexType(Method m)
	{
		Class<?>[] params = m.getParameterTypes();
		for (int i = 0; i < params.length; ++i)
		{
			Class<?> c = params[i];
			if (!c.isPrimitive() && !WRAPPER_TYPES.contains(c))
			{
				return false;
			}
		}
		
		return true;
	}

	String getPrimitiveWSDL(Class<?> type, HashSet<String> filter, boolean includeFilter)
	{
		ArrayList<Method> ret = new ArrayList<Method>();
		Method[] methods = type.getMethods();
		for (int i = 0; i < methods.length; ++i)
		{
			Method m = methods[i];
			
			if (m.getReturnType().isPrimitive() || WRAPPER_TYPES.contains(m.getReturnType()) &&
					(parameterHasComplexType(m))
					)
			{
				ret.add(m);
			} else {
				log.warn("filtering out {} because of complex type", m.getName());
			}
		}
		
		return getWSDL(type, methods, filter, includeFilter);
	}

	String getPrimitiveWSDL(Class<?> type)
	{
		/* test
		HashSet<String> filter = new HashSet<String>();
		filter.add("addClockEvent");
		return getPrimitiveWSDL(type, filter, true);
		*/

		return getPrimitiveWSDL(type, getFilter(), false);
	}
	
	String getWSDL(Class<?> type, HashSet<String> filter, boolean includeFilter) {
		return getWSDL(type, null, filter, includeFilter);
	}

	String getWSDL(Class<?> type, Method[] methods, HashSet<String> filter, boolean includeFilter) {

		// get all public methods

		// get <!-- [[%wsdl:types%]] --> (return types? parameter types?)
		if (methods == null) {
			methods = type.getMethods();
		}

		StringBuffer types = new StringBuffer();
		// String typesTemplate = FileIO.getResourceFile("soap/types.xml");
		String typesTemplate = "";

		//
		String params = "";
		String returnType = "     <element name=\"%methodName%Response\">\n" + "       <complexType/>\n" + "   </element>\n";

		for (int i = 0; i < methods.length; ++i) {
			Method m = methods[i];
			if ((!filter.contains(m.getName()) && !includeFilter) || (filter.contains(m.getName()) && includeFilter)) {

				// return type <element><complexType><sequence>....
				if (m.getReturnType() == String.class) {
					returnType = "     <element name=\"%methodName%Response\">\n" + "    <complexType>\n" + "     <sequence>\n"
							+ "      <element name=\"%methodName%Return\" type=\"xsd:string\"/>\n" + "     </sequence>\n" + "    </complexType>\n" + "   </element>\n";

				} else {
					log.warn("dont know how to handle return type {}", m.getReturnType().getSimpleName());
				}

				Class<?>[] p = m.getParameterTypes();
				// parameter type <element><complexType><sequence>....
				if (p.length == 0) {
					params = "     <element name=\"%methodName%\">\n" + "       <complexType/>\n" + "   </element>\n";

				} else {
					params = "     <element name=\"%methodName%\">\n" + "    <complexType>\n" + "     <sequence>\n";
					for (int j = 0; j < p.length; ++j) {
						params += "      <element name=\"p" + j + "\" type=\"xsd:string\"/>\n";
					}
					params +=  "     </sequence>\n" + "    </complexType>\n";
					params += "   </element>\n";
				}

				typesTemplate = params + returnType;
				types.append(typesTemplate.replaceAll("%methodName%", m.getName()));
			}
		}

		//log.info("[{}]", types);

		// get <!-- [[%wsdl:message%]] --> message
		StringBuffer messages = new StringBuffer();
		String messagesTemplate = FileIO.getResourceFile("soap/messages.xml");
		for (int i = 0; i < methods.length; ++i) {
			Method m = methods[i];
			if ((!filter.contains(m.getName()) && !includeFilter) || (filter.contains(m.getName()) && includeFilter)) {
				messages.append(messagesTemplate.replaceAll("%methodName%", m.getName()));
			}
		}

		// get <!-- [[%portType:wsdl:operation%]] --> porttype
		StringBuffer portTypes = new StringBuffer();
		String portTypesTemplate = FileIO.getResourceFile("soap/portTypes.xml");
		for (int i = 0; i < methods.length; ++i) {
			Method m = methods[i];
			if ((!filter.contains(m.getName()) && !includeFilter) || (filter.contains(m.getName()) && includeFilter)) {
				portTypes.append(portTypesTemplate.replaceAll("%methodName%", m.getName()));
			}
		}

		StringBuffer bindings = new StringBuffer();
		String bindingsTemplate = FileIO.getResourceFile("soap/bindings.xml");
		for (int i = 0; i < methods.length; ++i) {
			Method m = methods[i];
			if ((!filter.contains(m.getName()) && !includeFilter) || (filter.contains(m.getName()) && includeFilter)) {
				bindings.append(bindingsTemplate.replaceAll("%methodName%", m.getName()));
			}
		}

		// put it all together
		String wsdlTemplate = FileIO.getResourceFile("soap/wsdl.tmp.xml");
		wsdlTemplate = wsdlTemplate.replaceAll("<!-- \\[\\[%wsdl:types%\\]\\] -->", types.toString());
		wsdlTemplate = wsdlTemplate.replaceAll("<!-- \\[\\[%wsdl:messages%\\]\\] -->", messages.toString());
		wsdlTemplate = wsdlTemplate.replaceAll("<!-- \\[\\[%wsdl:portTypes%\\]\\] -->", portTypes.toString());
		wsdlTemplate = wsdlTemplate.replaceAll("<!-- \\[\\[%wsdl:bindings%\\]\\] -->", bindings.toString());
		wsdlTemplate = wsdlTemplate.replaceAll("%service%", type.getSimpleName());

		// log.info(String.format("[{}]"), wsdlTemplate);

		// get <!-- [[%binding:wsdl:operation%]] -->

		// get wsdl template

		return wsdlTemplate;
	}

	public static void main(String[] args) throws RunnerException, SerialDeviceException, IOException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Class<?> clazz = Clock.class;

		SOAP soap = new SOAP();
		//String xml = soap.getWSDL(Clock.class);
		String xml = soap.getPrimitiveWSDL(Clock.class);
		
		FileIO.stringToFile(String.format("%s.wsdl", clazz.getSimpleName()), xml);
		//log.info(xml);

	}
}
