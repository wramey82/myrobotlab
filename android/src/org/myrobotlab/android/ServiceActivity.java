package org.myrobotlab.android;

import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.ServiceWrapper;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ServiceActivity extends Activity {

	// Debugging
    private static final String TAG = "ServiceActivity";
    private static final boolean D = true;

	public Bundle bundle = null;
	public ServiceWrapper sw = null;
	//public Service myService = null;
	public View layout = null;
	
	public Bundle getBundle() 
	{
		return bundle;
	}		
	
	public void setUIHeaderData()
	{
		TextView text = (TextView) layout.findViewById(R.id.name);
		text.setText(sw.service.name);
		
		text = (TextView) layout.findViewById(R.id.type);
		text.setText(sw.service.getShortTypeName());

	}

    protected void onCreate(Bundle savedInstanceState, int layoutID) {
    	super.onCreate(savedInstanceState);
    	
        // begin exchange of data from service to UI
        layout = getLayoutInflater().inflate(layoutID, null);        		
    	
        // get service reference
        bundle = this.getIntent().getExtras();
        String name = bundle.getString(MyRobotLabActivity.SERVICE_NAME);
        if (name == null || name.length() == 0)
        {
        	inform("name empty! need key in Bundle !");
        	return;
        }
        
        sw = RuntimeEnvironment.getService(name);
        if (sw == null)
        {
        	inform("bad service reference - name " + name + " not valid !");
        	return;
        }
                
        setUIHeaderData();
        setContentView(layout);
    }
    
    public void setText(int field, String data)
    {
		TextView text = (TextView) layout.findViewById(field);
		if (text == null)
		{
			inform(sw.service.name + " could not find field " + field);
			return;
		}
		
		text.setText(data);
    }

    public void setText(int field, int data)
    {
		TextView text = (TextView) layout.findViewById(field);
		if (text == null)
		{
			inform(sw.service.name + " could not find field " + field);
			return;
		}
		StringBuffer d = new StringBuffer();
		d.append(data);
		text.setText(d.toString());
    }
    
    public void inform (String s)
    {
    	Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    	if(D) Log.e(TAG, s);
    	
    }
	
}
