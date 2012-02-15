package org.myrobotlab.android;

import android.app.Application;
import android.preference.PreferenceManager;

public class MyRobotLabApplication extends Application {
    
	private static MyRobotLabApplication instance;

    public static MyRobotLabApplication getInstance() {
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
