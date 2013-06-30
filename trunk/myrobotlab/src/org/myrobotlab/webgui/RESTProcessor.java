package org.myrobotlab.webgui;

import java.net.Socket;
import java.util.HashSet;
import java.util.Properties;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.HTTPProcessor;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.webgui.NanoHTTPD.Response;
import org.slf4j.Logger;

public class RESTProcessor implements HTTPProcessor {

	public final static Logger log = LoggerFactory.getLogger(RESTProcessor.class.getCanonicalName());

	private HashSet<String> uris = new HashSet<String>();
	
	@Override
	public Response serve(String uri, String method, Properties header, Properties parms, Socket socket) {
		// TODO top level is return format /html /text /soap /xml /gson /json a default could exist - start with SOAP response
		// default is not specified but adds {/rest/xml} /services ...
		// TODO - custom display /displays
		// TODO - structured rest fault responses
		String[] keys = uri.split("/");
		if ("/services".equals(uri))
		{
			// get runtime list
			log.info("services request");
			REST rest = new REST();
			String services = rest.getServices();
			
			Response response = new Response("200 OK", "text/html", services);
			
			return response;
		} else if (keys.length == 3){
			log.info("here");
			// get a specific service instance - return STATE !
		} else if (keys.length > 3){
			// get a specific service instance - execute method --with parameters--
			String serviceName = keys[2];
			String fn = keys[3];
			Object[] typedParameters = null;

			
			ServiceInterface si = org.myrobotlab.service.Runtime.getService(serviceName);
			
			// get parms
			if (keys.length > 4)
			{
				// copy paramater part of rest uri
				String[] stringParams = new String[keys.length - 4];
				for (int i = 0; i < keys.length - 4; ++i)
				{
					stringParams[i] = keys[i + 4];
				}
				
				TypeConverter.getInstance(); // FIXME - make better singleton - or make threadsafe - new'd
				typedParameters = TypeConverter.getTypedParams(si.getClass(), fn, stringParams);
			}
			
			Object returnObject = si.invoke(fn, typedParameters);
			
			// handle response depending on type
			// TODO - make structured !!! 
			Response response = new Response("200 OK", "text/html", (returnObject == null)?"":returnObject.toString());
			return response; 
		}
		return null;
	}

	@Override
	public HashSet<String> getURIs() {
		return uris;
	}
	
	
	public void addURI(String uri)
	{
		uris.add(uri);
	}

}
