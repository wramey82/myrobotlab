package org.myrobotlab.android; 

import org.myrobotlab.framework.Message;
import org.myrobotlab.service.Logging;

import android.os.Bundle;
import android.widget.TextView;

public class LoggingActivity extends ServiceActivity {

	Logging myService = null;
		
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.logging);
        myService = (Logging)sw.service;   
    }

    public Message log(Message m)
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
		logText.append(data + "\n");
		return m;
    }
    
    
    @Override
    public void attachGUI() { 
		sendNotifyRequest("log", "log", Message.class);	
		sendNotifyRequest("publishState", "getState", Logging.class);
    }

	@Override
	public void detachGUI() {
		removeNotifyRequest("log", "log", Message.class);
		removeNotifyRequest("publishState", "getState", Logging.class);
	}

}
