package org.myrobotlab.android;

import org.myrobotlab.service.Android;

import android.app.Application;
import android.preference.PreferenceManager;

/**
 * The global singleton shared between all Activities
 *
 */
public class MRL extends Application {

	// shared instance of app data
	private static MRL instance;
	// shared instance of the Android Service
	public static Android android;

	// bundle constants
	final static public String BOUND_SERVICE_NAME = "BOUND_SERVICE_NAME";
	
	
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
        // do all you initialization here

    }

}
