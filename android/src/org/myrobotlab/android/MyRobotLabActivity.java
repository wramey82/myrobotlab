package org.myrobotlab.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.Proxy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// references - 
// http://code.google.com/p/android-scripting/ TODO send them mrl info

/**
 * @author GroG
 * 
 *         MyRobotLabActivity handles the Android Service - this correlates to
 *         the GUIService and Swing control components of the Java Swing
 *         implementation
 *         
 *         This is the "Application" & starting point of the UI
 *         Android specifics are in the AndroidActivity
 *         
 *         This Activity is responsible for starting other Services,
 *         Logging, connecting to other instances, and other global
 *         procedures
 * 
 */
public class MyRobotLabActivity extends ListActivity {

	public static final String TAG = "MyRobotLab";

	// dialogs
	public static final int DIALOG_MYROBOTLAB_ADD_SERVICE = 1;
	public static final int DIALOG_MYROBOTLAB_CONNECT_HOST_PORT = 2;

	Button refresh;
	Button addService;
	Button remoteLogging;
	Spinner availableServices;
	ServiceListAdapter runningServices;
	
	ImageButton help;

	//public static Android androidService; // (make singleton)
	public Proxy proxyService; // FIXME temporary

	// android "tab" view TODO change to backing HashMap
	ArrayList<String> services = new ArrayList<String>();

		
	public class ServiceListAdapter extends ArrayAdapter<String> {
	    //private int[] colors = new int[] { 0x30FF0000, 0x300000FF };
	    public ServiceListAdapter(Context context, int resource, int resourceID, List<String> items) {
	    	super(context, resource, resourceID, items);
	    }

	 @Override
	 public View getView(int position, View convertView, ViewGroup parent) {
	   View view = super.getView(position, convertView, parent);
	   ServiceWrapper sw = RuntimeEnvironment.getService(getItem(position));
	   if (sw.host.accessURL != null)
	   {
		   view.setBackgroundColor(0xFF007000);
	   }
	   
	   //int colorPos = position % colors.length;
	   //view.setBackgroundColor(colors[colorPos]);
	   return view;
	 }

	}

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (MRL.D)
			Log.e(TAG, "++ onCreate ++");
		
	
		View header = getLayoutInflater().inflate(R.layout.myrobotlab_header, null);

		// manual service header
		TextView text = (TextView) header.findViewById(R.id.name);
		text.setText(MRL.android.getName());
		
		text = (TextView) header.findViewById(R.id.type);
		text.setText(MRL.android.getShortTypeName());

		
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
				this, R.array.services_available,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		availableServices.setAdapter(adapter);

		// refresh button - TODO - DEPRICATE
		refresh = (Button) header.findViewById(R.id.refresh);
		refresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				refreshServiceView();
			}

		});
		
		// help
		help = (ImageButton) header.findViewById(R.id.help);
		help.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(Intent.ACTION_VIEW, 
					       Uri.parse("http://myrobotlab.org/content/Android"));
					startActivity(i);
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

		// http://developer.android.com/reference/android/R.layout.html
		// http://stackoverflow.com/questions/4540754/add-dynamically-elements-to-a-listview-android
		if (runningServices == null) // TODO - test for null vs init in member list?
		{
			runningServices = new ServiceListAdapter(this,
					android.R.layout.simple_list_item_single_choice,
					android.R.id.text1, services);
			setListAdapter(runningServices);
		}		
		refreshServiceView();
	}

	// callback from RuntimeEnvironment read:
	// http://docs.oracle.com/javase/tutorial/uiswing/events/api.html
	// http://en.wikipedia.org/wiki/Observer_pattern
	// http://docs.oracle.com/javase/tutorial/uiswing/events/index.html
	public void refreshServiceView() {
		services.clear();

		// HashMap<URL, ServiceEnvironment> registry =
		// RuntimeEnvironment.getServiceEnvironments();

		HashMap<String, ServiceWrapper> registry = RuntimeEnvironment.getRegistry();

		Iterator<String> it = registry.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = registry.get(serviceName);
			services.add(serviceName);
			if (sw.host.accessURL == null) {
				// local - leave row black
			} else {
				// remote - color view green
			}
		}

		runningServices.notifyDataSetChanged();
	}

	
	// http://developer.android.com/guide/topics/ui/dialogs.html
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		LayoutInflater factory;

		switch (id) {

		case DIALOG_MYROBOTLAB_ADD_SERVICE:
			factory = LayoutInflater.from(this);
			final View addServiceTextEntryView = factory.inflate(
					R.layout.myrobotlab_add_service, null);

			return new AlertDialog.Builder(MyRobotLabActivity.this)
					// .setIconAttribute(android.R.attr.alertDialogIcon) TODO
					.setTitle(R.string.add_service)
					.setView(addServiceTextEntryView)
					.setPositiveButton(R.string.add,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									EditText text = (EditText) addServiceTextEntryView.findViewById(R.id.serviceName);
									
									if (MRL.D) Log.e(TAG, "++ service " + text.getText() + " of type " + availableServices.getSelectedItem().toString() + " ++");
									
									String typeName = "org.myrobotlab.service." + availableServices.getSelectedItem().toString();
									String serviceName = text.getText().toString();
									//---------------------------------------------------------------
									MRL.getInstance().createAndStartService(serviceName, typeName);
									services.add(serviceName); // adding													
									text.setText("");
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		case DIALOG_MYROBOTLAB_CONNECT_HOST_PORT:
			factory = LayoutInflater.from(this);
			final View remoteLoggingEntryView = factory.inflate(
					R.layout.myrobotlab_connect_host_port, null);

			return new AlertDialog.Builder(MyRobotLabActivity.this)
					// .setIconAttribute(android.R.attr.alertDialogIcon) TODO
					.setTitle(R.string.add_service)
					.setView(remoteLoggingEntryView)
					.setPositiveButton(R.string.add,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String host = ((EditText) remoteLoggingEntryView
											.findViewById(R.id.host)).getText()
											.toString();
									String port = ((EditText) remoteLoggingEntryView
											.findViewById(R.id.port)).getText()
											.toString();
									if (MRL.D) Log.e(TAG, "++ remote logging to " + host + ":" + port + " ++");
									Service.addAppender(
											Service.LOGGING_APPENDER_SOCKET,
											host, port);
									/* User clicked OK so do some stuff */
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		default:
			dialog = null;
		}
		return dialog;
	}

	// he da man - http://www.vogella.de/articles/AndroidIntent/article.html
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String name = (String) getListAdapter().getItem(position - 1);
		Toast.makeText(this, name + " selected", Toast.LENGTH_LONG).show();

		Intent intent = MRL.intents.get(name);
		if (intent != null) {
			startActivity(intent);
		} else {
			Log.e(TAG, "++ could not get intent for " + name );			
			Toast.makeText(this, "could not get intent for " + name, Toast.LENGTH_LONG).show();
		}

	}

}
