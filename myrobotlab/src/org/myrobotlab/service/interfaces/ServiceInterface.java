package org.myrobotlab.service.interfaces;

import java.util.ArrayList;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;

public interface ServiceInterface {
	public String getName();

	public void in(Message msg);

	public void stopService();

	public void startService();

	public void releaseService();

	public ArrayList<String> getNotifyListKeySet();

	public ArrayList<MRLListener> getNotifyList(String key);

	public String getSimpleName();

	public String getDescription();

	public boolean hasDisplay();
	
	public boolean allowExport();

	public void display();
	
	public boolean save();
	
	public boolean load();
	
	public void subscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public void unsubscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType);
	
	public Object invoke(String method);

	public Object invoke(String method, Object...params);
	
}
