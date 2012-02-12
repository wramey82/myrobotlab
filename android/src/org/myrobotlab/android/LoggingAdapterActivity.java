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

import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.ServiceWrapper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

// references :
// http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/

public class LoggingAdapterActivity extends Activity {
    // Debugging
    private static final String TAG = "RemoteAdapterActivity";
    private static final boolean D = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle bundle = this.getIntent().getExtras();
        String name = bundle.getString(MyRobotLabActivity.SERVICE_NAME);
        ServiceWrapper sw = RuntimeEnvironment.getService(name);
        
        View layout = getLayoutInflater().inflate(R.layout.remote_adapter_activity, null);		
		
        // TODO - encapsulate
        // <!-- === Service header begin ===============-->
		TextView text = (TextView) layout.findViewById(R.id.name);
		text.setText(name);
		
		text = (TextView) layout.findViewById(R.id.type);
		text.setText(sw.service.getShortTypeName());
        // <!-- === Service header end ===============-->
        
        // Setup the window
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        // setContentView(R.layout.remote_adapter_activity);
		
		setContentView(layout);

        // Set result CANCELED incase the user backs out
        //setResult(Activity.RESULT_CANCELED);

    }

}
