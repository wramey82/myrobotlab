package org.myrobotlab.android; 

import org.myrobotlab.service.ArduinoBT;

import android.os.Bundle;

// references :
// http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/

//  extends ServiceActivity
	public class ArduinoBTActivity extends ServiceActivity {

	ArduinoBT myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.remote_adapter_activity);
        
        myService = (ArduinoBT)sw.service;   
        // setText(R.id.udpdata, myService.servicePort);
        //setContentView(layout);        

    }

}
