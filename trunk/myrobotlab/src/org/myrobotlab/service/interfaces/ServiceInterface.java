package org.myrobotlab.service.interfaces;

import java.net.URI;
import java.util.ArrayList;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;

public interface ServiceInterface {
	public URI getHost();
		
	public void setHost(URI uri);
	
	public String getName();

	/**
	 * in will put a message in the Service's inbox - it will require the Service's thread
	 * to process it.
	 * 
	 * @param msg - message to process
	 */
	public void in(Message msg);

	public void stopService();

	public void startService();

	public void releaseService();

	public ArrayList<String> getNotifyListKeySet();

	public ArrayList<MRLListener> getNotifyList(String key);

	public String getSimpleName();

	public String getDescription();
	
	public boolean save();
	
	public boolean load();
	
	public void subscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public void unsubscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public Object invoke(String method);

	public Object invoke(String method, Object...params);
	
	/**
	 * asked by the framework - to determin if the service needs to be secure
	 * @return
	 */
	public boolean requiresSecurity();

	public boolean isLocal();
}
