package org.myrobotlab.android; 

import org.myrobotlab.framework.Message;
import org.myrobotlab.service.Logging;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class LoggingActivity extends ServiceActivity {

	Logging myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.logging);

    	// FIXME - safe to bury in ServiceActivity !!!        
//------------------------------------------------------------        
       	MRL.handlers.put(boundServiceName, new MyHandler(this));
//------------------------------------------------------------        
        myService = (Logging)sw.service;   

    }

    public void log(Message m)
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

		TextView logText = (TextView)layout.findViewById(R.id.log);
		logText.setText(data);
    }
    
    @Override
    public void attachGUI() { 
		sendNotifyRequest("log", "log", Message.class);	
		sendNotifyRequest("publishState", "getState", Logging.class);
		// myService.send(boundServiceName, "publishState"); no refresh needed
    }

	@Override
	public void detachGUI() {
		removeNotifyRequest("log", "log", Message.class);
		removeNotifyRequest("publishState", "getState", Logging.class);
	}

	// FIXME - safe to bury in ServiceActivity !!!
	public class MyHandler extends Handler {
		volatile LoggingActivity activity;

		MyHandler(LoggingActivity a) {
			super();
			activity = a;
		}

		/**
		 * handleMessage recieve's Android Message from backend
		 * data component is a MRL Message - which is routed here (to the
		 * appropriate gui view) by the Android Service and invoked
		 */
		@Override
		public void handleMessage(android.os.Message msg) {
			Message m = (Message) msg.obj;
			log(m);
			// invoke
			MRL.androidService.invoke(activity, m.method, m.data);
			super.handleMessage(msg);
		}
	}
}
