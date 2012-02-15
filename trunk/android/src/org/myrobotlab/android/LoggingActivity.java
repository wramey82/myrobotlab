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

import org.myrobotlab.framework.Message;
import org.myrobotlab.service.Logging;

import android.os.Bundle;
import android.widget.EditText;

// references :
// http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/

//  extends ServiceActivity
public class LoggingActivity extends ServiceActivity {

	Logging myService = null;
	EditText log = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.logging_activity);
        
        myService = (Logging)sw.service;   
//        setText(R.id.udpdata, myService.);
        log = (EditText)layout.findViewById(R.id.log);
        Message m = new Message();
        m.sender = "fake";
        m.sendingMethod = "method";
        m.data = new Object[]{"blah", "blah2"};
        Log (m);        	
    }
    
    // attachGUI
    
    
    // detachGUI
    
    public void Log(Message m)
    {
		StringBuffer data = null;
		
		if (m.data != null)
		{
			data = new StringBuffer();
			for (int i = 0; i < m.data.length; ++i)
			{
				data.append(m.data[i]);
				if (m.data.length > 1)
				{
					data.append(" ");
				}
			}
		}
		
		log.append(m.sender + "." + m.sendingMethod + " " + data + "\n");
		
		//log.setCaretPosition(log.getDocument().getLength());    	
    	//data.append(m.)
    }
   

}
