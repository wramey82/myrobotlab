package org.myrobotlab.android; 

import org.myrobotlab.framework.Message;
import org.myrobotlab.service.Logging;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/*
 * Multi-threaded Android UI References
 * http://www.aviyehuda.com/2010/12/android-multithreading-in-a-ui-environment/ (excellent)
 * http://developer.android.com/resources/articles/painless-threading.html
 * http://developer.android.com/guide/topics/fundamentals/processes-and-threads.html (IPC)
 * http://stackoverflow.com/questions/901239/android-using-runonuithread-to-do-ui-changes-from-a-thread (runOnUiThread)
 * (Clock) http://stackoverflow.com/questions/3765161/updating-ui-with-runnable-postdelayed-not-working-with-timer-app
 * http://indyvision.net/2010/02/android-threads-tutorial-part-3/ (Handlers & Threads)
 * http://www.vogella.de/articles/AndroidPerformance/article.html Vogel (Handler example)
 * http://stackoverflow.com/questions/1111980/how-to-handle-screen-orientation-change-when-progress-dialog-and-background-thre
 * http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
 * http://stackoverflow.com/questions/5185015/updating-android-ui-using-threads (the Key)
 * 
 * LifeCycle :
 * http://developer.android.com/reference/android/app/Activity.html
 * http://www.youtube.com/watch?v=ooWKZgJnYVo
 * 
 * So I'm coming to a realization regarding Android UI & Activities.
 * 1. You don't get to handle activities - no creating, no references, no touchy
 * 2. They get recycled faster than Jello in a garbage disposal
 * 3. If if you do get a reference to one, the Android framework has the ability to dis-associate it 
 * from the UI & create another one.  So you end up with a handle to an Activity which can do nothing
 * to the UI ... basically a handle to a memory leak :P
 * 4. Don't bother having member variables in Activities ... there is no point, the lifetime of the activity &
 * the ability of the activity to effect the UI is so nebulous - don't expect local state information to
 * make a difference.
 * 
 * Possible solutions :
 * 1. fight the system :)  ...  make all change to the UI methods static such that the "invoke" in the
 * Service thread invokes a static method
 * 2. Perhaps the Handler class offers a way?
 * http://developer.android.com/resources/articles/timed-ui-updates.html
 * 
 */

public class LoggingActivity extends ServiceActivity {

	Logging myService = null;
	//TextView logText = null;
	//MyHandler myHandler = new MyHandler();
	int zod;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.logging);
		TextView logText = (TextView)layout.findViewById(R.id.log);
		logText.setText("f'ing shit");

    	// FIXME - safe to bury in ServiceActivity !!!        
//------------------------------------------------------------        
       	MRL.handlers.put(boundServiceName, new MyHandler(this));
//------------------------------------------------------------        
        
        myService = (Logging)sw.service;   

    }

    // http://stackoverflow.com/questions/2197744/android-textview-text-not-getting-wrapped
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
    	MyHandler(LoggingActivity a)
    	{
    		super();
    		activity = a;    		
    	}
    	  @Override
    	  public void handleMessage(android.os.Message msg) {
    		  Message m = (Message)msg.obj;
    		  log(m);
    		  // invoke
    		  MRL.android.invoke(activity, m.method, m.data);
    		  super.handleMessage(msg);
    	  }
    }
	

}
