/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package org.myrobotlab.android;
package org.myrobotlab.android; 

import org.myrobotlab.service.RemoteAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

// references :
// http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/

//  extends ServiceActivity
public class RemoteAdapterActivity extends ServiceActivity {

	private RemoteAdapter myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//    	super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.remote_adapter_activity);
        
        myService = (RemoteAdapter)sw.service;
        
//        setText(R.id.udp, myService.servicePort);
        // Set result CANCELED incase the user backs out
        // setResult(Activity.RESULT_CANCELED);
            	
    	
        // begin exchange of data from service to UI
//        View layout = getLayoutInflater().inflate(R.layout.remote_adapter_activity, null);
//        setContentView(layout);

        setContentView(layout);        


    }

}
