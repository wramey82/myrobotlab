package org.myrobotlab.webgui;

import java.net.Socket;
import java.util.ArrayList;
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
		
		// handle escape character \ - I know this is pretty "anti"- rest but in a binding between input
		// parameters there has to be some form of escape character
		
		//String[] keys = uri.split("(?<!\\\\)/");
		//String[] keys = uri.split("(?<!/)/");
		//String[] keys = uri.split("/");
		
		
		// scan for first /
		// find the next / 
		// 	if (next != /) - split
		//	scan forward - if the set of ////  is odd split at beginning and remove - so next split is even
		
		// this fails when a parameter ends with / :P - it will incorrectly put the / at the beginning of the next
		// parameter
		
		/*
		ArrayList<String> result = new ArrayList<String>();
		
		int pos0 = 0;
		int pos1 = uri.indexOf("/");
		boolean isOdd = false;
		
		while (pos1 != -1)
		{
			result.add(uri.substring(pos0, pos1));
			pos0 = pos1;
			pos1 = uri.indexOf("/", pos1 + 1);
		}
		*/
		
		String[] keys = uri.split("/");
		
		// decode everything
		for (int i = 0; i < keys.length; ++i)
		{
			keys[i] = decodePercent(keys[i], true);
		}
		
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
			
			// TODO - handle return type - 
			// TODO top level is return format /html /text /soap /xml /gson /json /base16 a default could exist - start with SOAP response
			Object returnObject = si.invoke(fn, typedParameters);
			
			// handle response depending on type
			// TODO - make structured !!! 
			// Right now just return string object
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
	
	/**
	 * Decodes the percent encoding scheme. <br/>
	 * For example: "an+example%20string" -> "an example string"
	 */
	private String decodePercent(String str, boolean decodeForwardSlash)  {
		
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < str.length(); i++) {
				char c = str.charAt(i);
				switch (c) {
				case '+':
					sb.append(' ');
					break;
				case '%':
					if ("2F".equalsIgnoreCase(str.substring(i + 1, i + 3)) && !decodeForwardSlash)
					{
						log.info("found encoded / - leaving");
						sb.append("%2F");
					} else {
						sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
					}
					i += 2;
					break;
				default:
					sb.append(c);
					break;
				}
			}
			return new String(sb.toString().getBytes());
	
	}


}
