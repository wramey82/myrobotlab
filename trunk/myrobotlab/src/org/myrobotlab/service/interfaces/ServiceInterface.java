package org.myrobotlab.service.interfaces;

import java.util.ArrayList;
import java.util.Set;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.NotifyEntry;

public interface ServiceInterface {
	public String getName();
	public void in(Message msg);
	public void stopService();
	public void startService();
	public void releaseService();
	public Set<String> getNotifyListKeySet();
	public ArrayList<NotifyEntry> getNotifyList(String key);
	public String getShortTypeName();
	public String getToolTip();
	public boolean hasDisplay();
	public void display();
	//public Class<?> getServiceClass();
}
