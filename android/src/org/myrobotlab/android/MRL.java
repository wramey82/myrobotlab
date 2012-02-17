package org.myrobotlab.android;

import java.util.HashMap;

import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Android;
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
 * 
 */
public class MRL extends Application {

	// shared global instance of application data
	private static MRL instance;
	// shared global instance of the Android Service
	public static Android android;

	// android registry
	public final static HashMap<String, Intent> intents = new HashMap<String, Intent>();
	
	// for initialization from Service -to-> ServiceActivity
	public final static HashMap<String, Boolean> GUIAttached = new HashMap<String, Boolean>();
	
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

		if (android == null)
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
			android = (Android)RuntimeEnvironment.getService(name).service;
			android.setContext(getApplicationContext()); // FIXME - cheesey
			android.startSensors();

			/*
			TODO - temporary - only necessary for different JVMs sharing resources
			*/
			/*
			createAndStartService(name + "Proxy", Proxy.class.getCanonicalName());
			proxy = (Proxy)RuntimeEnvironment.getService(name + "Proxy").service;
			proxy.setTargetService(android);
			*/
			
			createAndStartService("remote", RemoteAdapter.class.getCanonicalName());
			createAndStartService("logger", Logging.class.getCanonicalName());
			
		}
    }
    
	public boolean createAndStartService(String name, String type)
	{
		Service s = (Service) Service.getNewInstance(type, name);
		if (s == null) {
			Toast.makeText(getApplicationContext(),
					" could not create " + name + " of type " + type, Toast.LENGTH_LONG).show();
		} else {
			s.startService();

			Intent intent = null;

			String serviceClassName = s.getClass().getCanonicalName();
			String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
			guiClass = "org.myrobotlab.android" + guiClass + "Activity";

			if (D) Log.e(TAG, "++ attempting to create " + guiClass + " ++");

			try {
				Bundle bundle = new Bundle();
				
				// adding boundServiceName
				bundle.putString(MRL.BOUND_SERVICE_NAME,s.getName());
				
				intent = new Intent(MRL.this,Class.forName(guiClass));				
				intent.putExtras(bundle);
				// add it to "servicePanels"
				// FIXME - when you add attachGUI & detachGUI - that will 
				// be useful - but it doesn't help the "root cause" of change
				// RuntimeEnvironment.register(...)
				// which is changes to the RuntimeEnvironment				
				intents.put(s.getName(), intent);
				// c = classForName (type + Activity)
				// c.attachGUI(serviceName, name) // one time static call
				// when you detach you can remove route
			} catch (ClassNotFoundException e) {
				Log.e(TAG, Service.stackToString(e));
				return false;
			}

		}

		if (D) Log.e(TAG, "++ started new service ++ ");
		/* User clicked OK so do some stuff */		
		return true;
	}

/*
	public static ServiceActivity getCurrentActivity() {
		return currentActivity;
	}

	public static void setCurrentActivity(ServiceActivity ca) {
		currentActivity = ca;
	}

	public static String getCurrentServiceName() {
		return currentServiceName;
	}

	public static void setCurrentServiceName(String currentServiceName) {
		MRL.currentServiceName = currentServiceName;
	}
*/
    

}
