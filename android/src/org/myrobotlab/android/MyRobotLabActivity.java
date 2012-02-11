package org.myrobotlab.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.Android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

// references - 
// http://code.google.com/p/android-scripting/ TODO send them mrl info

public class MyRobotLabActivity extends ListActivity {
	
	public static final String TAG = "MyRobotLab";
    public static final boolean D = true;
    
    // dialogs
    public static final int DIALOG_MYROBOTLAB_ADD_SERVICE = 1;
    public static final int DIALOG_MYROBOTLAB_CONNECT_HOST_PORT = 2;
    
    Button refresh;
    Button addService;
    Button remoteLogging;
    Spinner availableServices;
    
    Context myContext;
    
    Android androidService; // (singleton)
    
    // android registry?
    ArrayList<String> services = new ArrayList<String>();
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "++ onCreate ++");
 //       setContentView(R.layout.myrobotlab); // os method

        // *****************************************************
        // RuntimeEnvironment.addEnvironmentChangeListener(this);
        
        // getting header & footer - could be done as single xml doc
        // but might be nice to have contiguous header throughout - e.g logging
        // connect etc
		View header = getLayoutInflater().inflate(R.layout.myrobotlab_header, null);
		View footer = getLayoutInflater().inflate(R.layout.myrobotlab_footer, null);

		
		// remote logging
		remoteLogging = (Button) header.findViewById(R.id.remoteLogging);
        remoteLogging.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
			showDialog(DIALOG_MYROBOTLAB_CONNECT_HOST_PORT);
			}
		});        
				
        // available services
        availableServices = (Spinner) header.findViewById(R.id.new_service);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.services_available, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        availableServices.setAdapter(adapter);

        // refresh button     
        addService = (Button) header.findViewById(R.id.addService);		 
        addService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
			refreshServiceView();
			}
 
		});        
        
        
        // add service button     
        addService = (Button) header.findViewById(R.id.addService);		 
        addService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
			showDialog(DIALOG_MYROBOTLAB_ADD_SERVICE);
			}
 
		});        
		
        // active services
		ListView listView = getListView();
		listView.addHeaderView(header);
		listView.addFooterView(footer);
		// http://developer.android.com/reference/android/R.layout.html
		// http://stackoverflow.com/questions/4540754/add-dynamically-elements-to-a-listview-android
		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_single_choice,
				android.R.id.text1, services));
		
		// not so simple
		/*
		SimpleAdapter sa = new SimpleAdapter(
				this, 
				getData("here"),
				android.R.layout.simple_list_item_1, 
				new String[] { "title" },
                new int[] { android.R.id.text1 });
         
		/*******TODO******** 
		 
        Intent intent = getIntent();
        String path = intent.getStringExtra("com.example.android.apis.Path");
        
        if (path == null) {
            path = "";
        }

        setListAdapter(new SimpleAdapter(this, getData(path),
                android.R.layout.simple_list_item_1, new String[] { "title" },
                new int[] { android.R.id.text1 }));
        getListView().setTextFilterEnabled(true);		 
		 
		 
		 */
		

        
    }
    
    // callback from RuntimeEnvironment read:
    // http://docs.oracle.com/javase/tutorial/uiswing/events/api.html
    // http://en.wikipedia.org/wiki/Observer_pattern
    // http://docs.oracle.com/javase/tutorial/uiswing/events/index.html
    public void refreshServiceView()
    {
    	services.clear();

    	//HashMap<URL, ServiceEnvironment> registry = RuntimeEnvironment.getServiceEnvironments();

    	HashMap<String, ServiceWrapper> registry = RuntimeEnvironment.getRegistry();
    	
		Iterator<String> it = registry.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = registry.get(serviceName);
			services.add(serviceName);
			if (sw.host.accessURL == null)
			{
				// local - leave row black
			} else {
				// remote - color view green
			}
		}

    }
    
    //http://developer.android.com/guide/topics/ui/dialogs.html
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        LayoutInflater factory;
        
        switch(id) {
        
        case DIALOG_MYROBOTLAB_ADD_SERVICE:
            factory = LayoutInflater.from(this);
            final View addServiceTextEntryView = factory.inflate(R.layout.myrobotlab_add_service, null);

            return new AlertDialog.Builder(MyRobotLabActivity.this)
                //.setIconAttribute(android.R.attr.alertDialogIcon) TODO
                .setTitle(R.string.add_service)
                .setView(addServiceTextEntryView)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	EditText text = (EditText) addServiceTextEntryView.findViewById(R.id.serviceName);
                    	if(D) Log.e(TAG, "++ service "+text.getText()+" of type " + availableServices.getSelectedItem().toString() + " ++");
                    	String typeName = "org.myrobotlab.service." + availableServices.getSelectedItem().toString();
                    	String serviceName = text.getText().toString();
                    	Service s = (Service)Service.getNewInstance(typeName,  serviceName);
                    	if (s == null)
                    	{
                    		Toast.makeText(getApplicationContext(), " could not create " + serviceName + " of type " + typeName, Toast.LENGTH_LONG).show();
                    	} else {
	                    	s.startService();
	                    	services.add(typeName);
                    	}
                    	
                    	if(D) Log.e(TAG, "++ started new service ++ ");
                    	/* User clicked OK so do some stuff */
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked cancel so do some stuff */
                    }
                })
                .create(); 
        case DIALOG_MYROBOTLAB_CONNECT_HOST_PORT:
            factory = LayoutInflater.from(this);
            final View remoteLoggingEntryView = factory.inflate(R.layout.myrobotlab_connect_host_port, null);

            return new AlertDialog.Builder(MyRobotLabActivity.this)
                //.setIconAttribute(android.R.attr.alertDialogIcon) TODO
                .setTitle(R.string.add_service)
                .setView(remoteLoggingEntryView)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	String host = ((EditText)remoteLoggingEntryView.findViewById(R.id.host)).getText().toString();
                    	String port = ((EditText)remoteLoggingEntryView.findViewById(R.id.port)).getText().toString();
                    	if(D) Log.e(TAG, "++ remote logging to "+host+":"+port+" ++");
                    	Service.addAppender(Service.LOGGING_APPENDER_SOCKET, host, port);
                        /* User clicked OK so do some stuff */
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked cancel so do some stuff */
                    }
                })
                .create(); 
        default:
            dialog = null;
        }
        return dialog;
    }

    // he da man - http://www.vogella.de/articles/AndroidIntent/article.html
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String item = (String) getListAdapter().getItem(position - 1);
		Toast.makeText(this, item + " selected", Toast.LENGTH_LONG).show();
		
		// get service
		
		ServiceWrapper s = RuntimeEnvironment.getService(item);
		// get type
		// get type_"activity" & start it
		// get service type class name
		String serviceClassName = s.service.getClass().getCanonicalName();
		String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
		guiClass = "org.myrobotlab.android" + guiClass + "Activity";
		if(D) Log.e(TAG, "++ attempting to create " + guiClass + " ++");

		
		Intent intent = null;
		try {
			intent = new Intent(this, Class.forName(guiClass));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Map map = (Map) l.getItemAtPosition(position);
        //Intent intent = (Intent) map.get("intent");
		if (intent != null)
		{
			startActivity(intent);
		}

	}
	
    
}
