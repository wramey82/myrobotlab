package org.myrobotlab.android;

import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.android.MyRobotLabActivity.ServiceListAdapter;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Android;
import org.myrobotlab.service.ArduinoBT;
import org.myrobotlab.service.Logging;
import org.myrobotlab.service.RemoteAdapter;

import android.app.Application;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * The global singleton shared between all Activities
 *
 * 
 * References : 
 * 		http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/
 *		http://code.google.com/p/android-scripting/ TODO send them mrl info
 *		http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/ 
 *		http://stackoverflow.com/questions/2197744/android-textview-text-not-getting-wrapped
  * Multi-threaded Android UI References
 * http://www.aviyehuda.com/2010/12/android-multithreading-in-a-ui-environment/ (excellent)
 * http://developer.android.com/resources/articles/painless-threading.html
 * http://developer.android.com/guide/topics/fundamentals/processes-and-threads.html (IPC)
 * http://stackoverflow.com/questions/901239/android-using-runonuithread-to-do-ui-changes-from-a-thread (runOnUiThread)
 * (Clock) http://stackoverflow.com/questions/3765161/updating-ui-with-runnable-postdelayed-not-working-with-timer-app
 * http://indyvision.net/2010/02/android-threads-tutorial-part-3/ (Handlers & Threads)
 * http://www.vogella.de/articles/AndroidPerformance/article.html Vogel (Handler example)
 * http://stackoverflow.com/questions/1111980/how-to-handle-screen-orientation-change-when-progress-dialog-and-background-thre
 * http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
 * http://stackoverflow.com/questions/5185015/updating-android-ui-using-threads (the Key)
 * 
 * LifeCycle :
 * http://developer.android.com/reference/android/app/Activity.html
 * http://www.youtube.com/watch?v=ooWKZgJnYVo
 * 
 * So I'm coming to a realization regarding Android UI & Activities.
 * 1. You don't get to handle activities - no creating, no references, no touchy
 * 2. They get recycled faster than Jello in a garbage disposal
 * 3. If if you do get a reference to one, the Android framework has the ability to dis-associate it 
 * from the UI & create another one.  So you end up with a handle to an Activity which can do nothing
 * to the UI ... basically a handle to a memory leak :P
 * 4. Don't bother having member variables in Activities ... there is no point, the lifetime of the activity &
 * the ability of the activity to effect the UI is so nebulous - don't expect local state information to
 * make a difference.
 * 
 * Possible solutions :
 * 1. fight the system :)  ...  make all change to the UI methods static such that the "invoke" in the
 * Service thread invokes a static method
 * 2. Perhaps the Handler class offers a way?
 * http://developer.android.com/resources/articles/timed-ui-updates.html
*
 * 
 */
public class MRL extends Application {

	// shared global instance of application data
	private static MRL instance;
	// shared global instance of the Android Service
	public static Android androidService;

	// android registry
	public final static HashMap<String, Intent> intents = new HashMap<String, Intent>();
	
	// for initialization from Service -to-> ServiceActivity
	public final static HashMap<String, Boolean> GUIAttached = new HashMap<String, Boolean>();
	
	// android "tab" view TODO change to backing HashMap
	public final static ArrayList<String> services = new ArrayList<String>();
	
	public static ServiceListAdapter runningServices;

	// driving under the influence or debug user interface
    static boolean DUI = true;

	// temporary proxy
	// public static Proxy proxy; // FIXME - temporary until Runtime export is figured out
	
	// bundle constants
	final static public String BOUND_SERVICE_NAME = "BOUND_SERVICE_NAME";
	
	public static final String TAG = "MRL";
	public static boolean D = true;

	  
	/**
	 * single handler for non-User back end UI updates
	 * Android Service uses this to let events & messages
	 * from other Services interact with the current
	 * Android Activity UI
	 */
	//public static Handler handler = new Handler();
	
	// the active Activity FIXME - you should also save
	// its current state e.g. ONPAUSE and not send messages then
	//private static ServiceActivity currentActivity = null;
	public volatile static String currentServiceName = null;
	public volatile static HashMap <String, Handler> handlers = new HashMap <String, Handler>();
	
	
    public static MRL getInstance() {
        return instance;
   }
	
	@Override
    public void onCreate() {
        /*
         * This populates the default values from the preferences XML file. See
         * {@link DefaultValues} for more details.
         */
        PreferenceManager.setDefaultValues(this, R.xml.default_values, false);
        instance = this;
        instance.initializeInstance();
    }

    @Override
    public void onTerminate() {
    	// TODO - run through and shutdown services
    }
    
    protected void initializeInstance() {

		if (androidService == null)
		{
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			
			String name = String.format("%d.%d.%d.%d",
					(ipAddress & 0xff),
					(ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff),
					(ipAddress >> 24 & 0xff));
			

			/*
			android = new Android(name);
			android.startService();
			intents.put(name, getIntent()); // TODO - normalize calls into one call
			// TODO - FIXME - figure how to consolidate and what it means !!!
			services.add(name);
			*/
			
			createAndStartService(name, Android.class.getCanonicalName());
			androidService = (Android)Runtime.getService(name).service;
			androidService.setContext(getApplicationContext()); // FIXME - cheesey
//			android.startSensors();

			/*
			TODO - temporary - only necessary for different JVMs sharing resources
			*/
			/*
			createAndStartService(name + "Proxy", Proxy.class.getCanonicalName());
			proxy = (Proxy)Runtime.getService(name + "Proxy").service;
			proxy.setTargetService(android);
			*/
					
			createAndStartService("remote", RemoteAdapter.class.getCanonicalName());
			createAndStartService("logger", Logging.class.getCanonicalName());			
			createAndStartService("arduino", ArduinoBT.class.getCanonicalName());
			/*
			createAndStartService("left", Servo.class.getCanonicalName());
			createAndStartService("right", Servo.class.getCanonicalName());
			*/
			
		}
    }
    
    public static boolean release (String serviceName)
    {
    	boolean ret = Runtime.release(serviceName);
    	if (ret)
    	{
    		services.remove(serviceName);
    		//runningServices.remove(serviceName);
    		runningServices.notifyDataSetChanged();
    		intents.remove(serviceName);
    		GUIAttached.remove(serviceName);    		
    		toast(serviceName + " released");
    	} else {
    		toast("could not release " + serviceName);
    	}
    	return ret;
    }
    
    public static void releaseAll()
    {
    	services.clear();
    	runningServices.clear();
    	intents.clear();
    	GUIAttached.clear();
    	Runtime.releaseAll();
    	// reference
    	// http://stackoverflow.com/questions/2092951/how-to-close-android-application
    	System.runFinalizersOnExit(true);    	
    	int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);         
    	//System.exit(0);
    }
    
    public static void toast(String msg)
    {
    	toast("MRL", msg);
    }
    
    
    public static void toast(String tag, String msg)
    {
    	if (DUI) Toast.makeText(MRL.getInstance().getApplicationContext(),
				msg, Toast.LENGTH_SHORT).show();
    	if (D) Log.e(tag, msg);
    }
    
	public static boolean createAndStartService(String name, String type)
	{
		Service s = (Service) Service.getNewInstance(type, name);
		if (s == null) {
			toast(" could not create " + name + " of type " + type);
		} else {
			s.startService();
			if (!addServiceActivityIntent(s.getName(), s.getShortTypeName()))
			{
				return false;
			}
		}

		if (D) Log.e(TAG, "++ started new service ++ ");
		/* User clicked OK so do some stuff */		
		return true;
	}
	
	public static boolean addServiceActivityIntent(String name, String shortTypeName)
	{
		try {
			Bundle bundle = new Bundle();
			String activityCanonicalName = "org.myrobotlab.android." + shortTypeName + "Activity";
			// adding boundServiceName
			bundle.putString(MRL.BOUND_SERVICE_NAME, name);
			
			if (D) Log.e(TAG, "++ attempting to create " + activityCanonicalName + " ++");
			
			Intent intent = null;
			intent = new Intent(MRL.getInstance(),Class.forName(activityCanonicalName));				
			intent.putExtras(bundle);
			// add it to "servicePanels"
			// FIXME - when you add attachGUI & detachGUI - that will 
			// be useful - but it doesn't help the "root cause" of change
			// Runtime.register(...)
			// which is changes to the Runtime				
			intents.put(name, intent);
			// c = classForName (type + Activity)
			// c.attachGUI(serviceName, name) // one time static call
			// when you detach you can remove route
		} catch (ClassNotFoundException e) {
			Log.e(TAG, Service.stackToString(e));
			return false;
		}
		
		return true;
	}

}
