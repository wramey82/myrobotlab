package org.myrobotlab.android;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.Android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public abstract class ServiceActivity extends Activity {

    private static final String TAG = "ServiceActivity";
    private static final boolean D = true;
    
    private static Android myAndroid; 
	public String boundServiceName;

	public Bundle bundle = null;
	public ServiceWrapper sw = null;
	public View layout = null;

	public Bundle getBundle() 
	{
		return bundle;
	}		
	
	private void showServiceIcon(String icon) {
		String path = "drawable/" + icon;
	    int imageResource = getResources().getIdentifier(path, null, getPackageName());
	    ImageButton imageView = (ImageButton) layout.findViewById(R.id.icon);
	    Drawable image = getResources().getDrawable(imageResource);
	    imageView.setImageDrawable(image);
	}
	
	public void setUIHeaderData()
	{
		TextView text = (TextView) layout.findViewById(R.id.name);
		text.setText(sw.service.getName());
		
		text = (TextView) layout.findViewById(R.id.type);
		text.setText(sw.service.getShortTypeName());

		showServiceIcon(sw.service.getShortTypeName().toLowerCase());
		
		ImageButton help = (ImageButton) layout.findViewById(R.id.help);
		help.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(Intent.ACTION_VIEW, 
					       Uri.parse("http://myrobotlab.org/content/" + sw.service.getShortTypeName()));
					startActivity(i);
			}
		});
	}
	
    protected void onCreate(Bundle savedInstanceState, int layoutID) {
    	super.onCreate(savedInstanceState);
    	MRL.getInstance(); // FIXME - most likely not necessary as only the class definition will construct it
    	myAndroid = MRL.androidService; // FIXME - remove if not used
    	    	
        // begin exchange of data from service to UI
        layout = getLayoutInflater().inflate(layoutID, null);        		
    	
        // get service reference
        bundle = this.getIntent().getExtras();
        if (bundle == null)
        {
        	boundServiceName = "android"; // FIXME - at least final static it in MRL
        } else {
        	boundServiceName = bundle.getString(MRL.BOUND_SERVICE_NAME);
        }

        // FIXME - safe to bury in ServiceActivity !!!        
        //------------------------------------------------------------        
        MRL.handlers.put(boundServiceName, new MyHandler(this));
        //------------------------------------------------------------        
        
        
    	// set the context
    	// we are now the currently active view ! FIXME - does not work !        
    	// setting the current service name context
    	MRL.currentServiceName = boundServiceName;

        // set up notification routes on first initialization
    	if (!MRL.GUIAttached.containsKey(boundServiceName) || !MRL.GUIAttached.get(boundServiceName))
    	{
    		attachGUI();
    		MRL.GUIAttached.put(boundServiceName, true);
    	}
    	
        if (boundServiceName == null || boundServiceName.length() == 0)
        {
        	MRL.toast("name empty! need key in Bundle !");
        	return;
        }
        
        sw = Runtime.getService(boundServiceName);
        if (sw == null)
        {
        	MRL.toast("bad service reference - name " + boundServiceName + " not valid !");
        	return;
        }
                
        setUIHeaderData();
        setContentView(layout);
        
        ImageButton release = (ImageButton) layout.findViewById(R.id.release);
        release.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				MRL.release(boundServiceName);	
				finish();
			}
		});
        
    }
    
    public void setText(int field, String data)
    {
		TextView text = (TextView) layout.findViewById(field);
		if (text == null)
		{
			MRL.toast(sw.service.getName() + " could not find field " + field);
			return;
		}
		
		text.setText(data);
    }

    public void setText(int field, int data)
    {
		TextView text = (TextView) layout.findViewById(field);
		if (text == null)
		{
			MRL.toast(sw.service.getName() + " could not find field " + field);
			return;
		}
		StringBuffer d = new StringBuffer();
		d.append(data);
		text.setText(d.toString());
    }
    
    
	/**
	 * attachGUI is called to initialize any communication routes which need to be
	 * setup between the service & the UI
	 */
	abstract public void attachGUI();

	/**
	 * removes communication routes and releases resources when the UI is 
	 * removed
	 */
	abstract public void detachGUI();
	
	// TODO - event listener model
	public void sendNotifyRequest(String outMethod, String inMethod, Class<?> parameterType) 
	{
		NotifyEntry ne = null;
		if (parameterType != null) {
			ne = new NotifyEntry(outMethod, myAndroid.getName(), inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myAndroid.getName(), inMethod, null);
		}
		
		myAndroid.send(boundServiceName, "notify", ne);

	}

	public void removeNotifyRequest(String outMethod, String inMethod,
			Class<?> parameterType) {

		NotifyEntry ne = null;
		if (parameterType != null) {
			ne = new NotifyEntry(outMethod, myAndroid.getName(), inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myAndroid.getName(), inMethod, null);
		}
		myAndroid.send(boundServiceName, "removeNotify", ne);

	}
	
	//---handlers for message routings between services & gui components--------
	/**
	 * handleMessage recieve's Android Message from backend
	 * data component is a MRL Message - which is routed here (to the
	 * appropriate gui view) by the Android Service and invoked
	 */

	public class MyHandler extends Handler {
		volatile ServiceActivity activity;

		MyHandler(ServiceActivity a) {
			super();
			activity = a;
		}

		@Override
		public void handleMessage(android.os.Message msg) {
			Message m = (Message) msg.obj;
			if(D) 
			{
				String inMsg = "++ msg from " + m.sender + " to android gui " + m + " ++";
				Log.e(TAG, inMsg);
				//MRL.toast(inMsg);
			}
			MRL.androidService.invoke(activity, m.method, m.data);
			super.handleMessage(msg);
		}
	}
	
	
	
	
}
