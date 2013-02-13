/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;

/**
 * @author greg (at) myrobotlab.org wrapper service for Apache HTTPClient
 */
public class HTTPClient extends Service {

	public final static Logger log = LoggerFactory.getLogger(HTTPClient.class.getCanonicalName());
	private static final long serialVersionUID = 1L;

	public HTTPClient(String n) {
		super(n, HTTPClient.class.getCanonicalName());
	}

	public static String parse(String in, String beginTag, String endTag) {
		int pos0 = in.indexOf(beginTag);
		int pos1 = in.indexOf(endTag, pos0);

		String ret = in.substring(pos0 + beginTag.length(), pos1);
		ret = ret.replaceAll("<br />", "");
		return ret;

	}

	public static class HTTPData {
		public HttpClient client = null;
		public HttpMethodBase method = null;
		public String dataString = null;
		public int statusCode = -1;

		public String toString() {
			try {
				return method.getResponseBodyAsString();
			} catch (IOException e) {
				logException(e);
				return null;
			}
		}

	}

	public static HTTPData post(String uri) {
		return post(uri, null, null);
	}

	public static HTTPData post(String uri, HashMap<String, String> fields) {
		return post(uri, fields, null);
	}

	public static HTTPData post(String uri, HashMap<String, String> fields, HTTPData data) {
		if (data == null) {
			data = new HTTPData();
		}

		// Create an instance of HttpClient.
		if (data.client == null) {
			data.client = new HttpClient();
		}

		// Create a method instance.
		PostMethod p = new PostMethod(uri);
		data.method = p;

		if (fields != null) {
			Iterator<String> i = fields.keySet().iterator();

			// transfer fields
			while (i.hasNext()) {
				String n = i.next();
				String v = fields.get(n);
				p.addParameter(n, v);
			}
		}

		// p.setFollowRedirects(true);
		// Provide custom retry handler is necessary
		p.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

		// p.setFollowRedirects(true);

		try {
			// Execute the method.CookiePolicy.RFC_2109
			p.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			// p.getParams().setCookiePolicy(CookiePolicy.DEFAULT);
			data.statusCode = data.client.executeMethod(p);

			Cookie[] cookies = data.client.getState().getCookies();
			for (int i = 0; i < cookies.length; ++i) {
				log.info(cookies[i].toExternalForm());
				log.info(cookies[i].getValue());
			}

			// p.execute(data.client.getState(),
			// data.client.getHttpConnectionManager().get)
			// response = httpClient.execute(httpPost,localContext);
			/*
			 * GetMethod g = new GetMethod("http://letsmakerobots.com/node");
			 * g.getParams
			 * ().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			 * data.statusCode = data.client.executeMethod(g);
			 */

		} catch (HttpException e) {
			System.err.println("Fatal protocol violation: " + e.getMessage());
			logException(e);
		} catch (IOException e) {
			System.err.println("Fatal transport error: " + e.getMessage());
			logException(e);
		} catch (Exception e) {
			System.err.println("Fatal error: " + e.getMessage());
			logException(e);
		} finally {
			// Release the connection.
			// p.releaseConnection();
		}

		return data;
	}

	public static HTTPData get(String uri) {
		return get(uri, null);
	}

	public static HTTPData get(String uri, HTTPData data) {
		if (data == null) {
			data = new HTTPData();
		}

		// Create an instance of HttpClient.
		if (data.client == null) {
			data.client = new HttpClient();
		}

		// Create a method instance.
		GetMethod g = new GetMethod(uri);
		data.method = g;

		// Provide custom retry handler is necessary
		g.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

		try {
			// Execute the method.
			g.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
			data.statusCode = data.client.executeMethod(g);

			if (data.statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + g.getStatusLine());
			}

		} catch (HttpException e) {
			System.err.println("Fatal protocol violation: " + e.getMessage());
			logException(e);
		} catch (IOException e) {
			System.err.println("Fatal transport error: " + e.getMessage());
			logException(e);
		} finally {
			// Release the connection.
			// g.releaseConnection();
		}

		return data;
	}

	void shout(String userID, String password, String shoutText) {

		HashMap<String, String> fields = new HashMap<String, String>();

		fields.put("openid_identifier", "");
		fields.put("name", userID);
		fields.put("pass", password);
		fields.put("op", "Log+in");
		fields.put("form_id", "user_login_block");
		fields.put("openid.return_to", "http%3A%2F%2Fletsmakerobots.com%2Fopenid%2Fauthenticate%3Fdestination%3Dfrontpage%252Fpanel");
		HTTPData data = HTTPClient.post("http://letsmakerobots.com/node?destination=frontpage%2Fpanel", fields);

		// go to node page to get token
		HTTPClient.get("http://letsmakerobots.com/node", data);

		// get token
		String form_token = null;
		try {
			form_token = HTTPClient.parse(data.method.getResponseBodyAsString(), "<input type=\"hidden\" name=\"form_token\" id=\"edit-shoutbox-add-form-form-token\" value=\"",
					"\"  />");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

		// post comment
		fields.clear();
		fields.put("nick", "GroG");
		fields.put("message", shoutText);
		fields.put("ajax", "0");
		fields.put("nextcolor", "0");
		fields.put("op", "Shout");
		fields.put("form_token", form_token);
		fields.put("form_id", "shoutbox_add_form");

		HTTPClient.post("http://letsmakerobots.com/node", fields, data);

	}

	void postForumComment(String forumNode, String userID, String password, String subject, String text) {
		HashMap<String, String> fields = new HashMap<String, String>();

		fields.put("openid_identifier", "");
		fields.put("name", userID);
		fields.put("pass", password);
		fields.put("op", "Log+in");
		fields.put("form_id", "user_login_block");
		fields.put("openid.return_to", "http%3A%2F%2Fletsmakerobots.com%2Fopenid%2Fauthenticate%3Fdestination%3Dfrontpage%252Fpanel");
		HTTPData data = HTTPClient.post("http://letsmakerobots.com/node?destination=frontpage%2Fpanel", fields);

		// go to node page to get token
		HTTPClient.get("http://letsmakerobots.com/node/" + forumNode, data);

		// get token
		String form_token = null;
		try {
			form_token = HTTPClient.parse(data.method.getResponseBodyAsString(), "<input type=\"hidden\" name=\"form_token\" id=\"edit-shoutbox-add-form-form-token\" value=\"",
					"\"  />");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

		// post comment
		fields.clear();
		fields.put("subject", subject);
		fields.put("format", "1");
		fields.put("form_token", form_token);
		fields.put("form_id", "comment_form");
		fields.put("op", "form-submit");

		HTTPClient.post("http://letsmakerobots.com/comment/reply/" + forumNode, fields, data);

	}

	@Override
	public String getToolTip() {
		return "an HTTP client, used to fetch information on the web";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// HTTPClient http = (HTTPClient)Runtime.create ("http",
		// (Class<?>)HTTPClient.class);
		HTTPData data = HTTPClient.get("http://localhost/");
		log.info(data.toString());
	}

}
