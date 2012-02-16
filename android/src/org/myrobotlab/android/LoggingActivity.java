package org.myrobotlab.android; 

import org.myrobotlab.framework.Message;
import org.myrobotlab.service.Clock;
import org.myrobotlab.service.Logging;

import android.os.Bundle;
import android.widget.EditText;

public class LoggingActivity extends ServiceActivity {

	Logging myService = null;
	EditText log = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.logging);
        
        myService = (Logging)sw.service;   
//        setText(R.id.udpdata, myService.);
        log = (EditText)layout.findViewById(R.id.log);
        Message m = new Message();
        m.sender = "fake";
        m.sendingMethod = "method";
        m.data = new Object[]{"blah", "blah2"};
        Log (m);        	
    }

    // http://stackoverflow.com/questions/2197744/android-textview-text-not-getting-wrapped
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
    }
   
 // TODO - this needs to be associated with Name & Intent !
    
    @Override
    public void attachGUI() { 
		sendNotifyRequest("log", "log", Message.class);	
		sendNotifyRequest("publishState", "getState", Clock.class);
		// myService.send(boundServiceName, "publishState"); no refresh needed
    }

	@Override
	public void detachGUI() {
		removeNotifyRequest("log", "log", Message.class);
		removeNotifyRequest("publishState", "getState", Clock.class);
	}

}
