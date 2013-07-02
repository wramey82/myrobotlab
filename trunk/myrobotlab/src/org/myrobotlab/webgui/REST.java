package org.myrobotlab.webgui;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;

public class REST {

	// TODO - fixme - 
	public String getServices() {
		
		StringBuffer content = new StringBuffer();
		//String restServiceTemplate = FileIO.fileToString("rest.service.template.html");
		String restServiceTemplate = FileIO.getResourceFile("rest/rest.service.template.html");

		
		Iterator<Map.Entry<URI, ServiceEnvironment>> uriIt = Runtime.getServiceEnvironments().entrySet().iterator();
		while (uriIt.hasNext()) {
			Map.Entry<URI, ServiceEnvironment> pairs = uriIt.next();
			//serviceContent.append(String.format("%s", pairs.getKey()));
			Iterator<Map.Entry<String, ServiceWrapper>> serviceIt = pairs.getValue().serviceDirectory.entrySet().iterator();
			while (serviceIt.hasNext()) {
				Map.Entry<String, ServiceWrapper> servicePair = serviceIt.next();
				String serviceName = servicePair.getKey();
				
				//serviceContent.append(String.format("<tr><td></td><td>%s</td><td></td></tr>", servicePair.getKey()));
				//System.out.println(pairs.getKey() + " = " + pairs.getValue());
				ServiceInterface si = servicePair.getValue().get();
				String serviceType = si.getClass().getSimpleName();
				Method[] methods = si.getClass().getMethods();
				TreeMap<String, Method> ms = new TreeMap<String, Method>();
				
				// building key from method name and ordinal - since the 
				// RESTProcessor's can only handle non-dupes of this signature
				for (int i = 0; i < methods.length; ++i)
				{
					Method m = methods[i];
					ms.put(String.format("%s.%d", m.getName(), (m.getParameterTypes() != null)?m.getParameterTypes().length:0), m);					
				}
				
				StringBuffer service = new StringBuffer("");
				service.append("<table border=\"0\">");
				for (Map.Entry<String,Method> me : ms.entrySet())
				{
					Method m = me.getValue();
					Class<?>[] params = m.getParameterTypes();
					
					if (params.length > 0)
					{
						service.append(String.format("<tr><td><form id=\"%1$s.%2$s\" method=\"GET\" action=\"/services/%1$s/%2$s\" > <a href=\"#\" onClick=\"buildRestURI(document.getElementById('%1$s.%2$s')); return false;\">%2$s</a>",serviceName, m.getName()));
						service.append(String.format("<input id=\"p0\" type=\"hidden\" value=\"%s\"/>", serviceName));
						service.append(String.format("<input id=\"p1\" type=\"hidden\" value=\"%s\"/>", m.getName()));
						for (int i = 0; i < params.length; ++i)
						{
							service.append(String.format("<input id=\"p%d\" type=\"text\" />", i+2));
						}
					
						service.append(String.format("</form></td></tr>"));
						
					} else {						
						service.append(String.format("<tr><td><a href=\"/services/%s/%s\">%s</a></td></tr>", serviceName, m.getName(), m.getName()));
					}
				}
				service.append("</table>");
				
				String tmp1 = restServiceTemplate.replaceAll("%serviceName%", serviceName);
				String tmp2 = tmp1.replaceAll("%serviceType%", serviceType);
				content.append(tmp2.replaceAll("%methods%", service.toString()));
			}
		}
		
		// TODO - resource
		//String restTemplate = FileIO.fileToString("rest.template.html");
		String restTemplate = FileIO.getResourceFile("rest/rest.template.html");
		String html = restTemplate.replaceAll("%content%", content.toString());
		return html;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		
		Runtime.createAndStart("servo01", "Servo");
		Runtime.createAndStart("motor01", "Motor");
		REST rest = new REST();
		FileIO.stringToFile("rest.html", rest.getServices());
		
		Runtime.releaseAll();
	}

}
