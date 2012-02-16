package org.myrobotlab.service;

import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.android.ServiceActivity;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
public class Android extends Service implements SensorEventListener {

	private static final long serialVersionUID = 1L;
	private SensorManager sensorManager;
	private boolean color = false; 
	//private View view;
	private long lastUpdate;

	// TODO - what things to put in MRL singleton ???
	transient HashMap<String, ServiceActivity> serviceActivityMap = null;
	HashMap<String, Object> commandMap = new HashMap<String, Object>(); 

	private Context context;
	public final static Logger LOG = Logger.getLogger(Android.class.getCanonicalName());

	public Android(String n) {
		super(n, Android.class.getCanonicalName());
		
		RuntimeEnvironment.getRuntime().notify(n, "registered", String.class);
		
		// TODO - dynamically reflect to load map
		commandMap.put("registerServicesEvent", null);
		commandMap.put("registerServices", null);
		commandMap.put("loadTabPanels", null);
		commandMap.put("registerServicesNotify", null);
		commandMap.put("notify", null);
		commandMap.put("removeNotify", null);
		commandMap.put("guiUpdated", null);
		commandMap.put("registered", null);
		commandMap.put("released", null);
		commandMap.put("setRemoteConnectionStatus", null);

	}
	
	public String registered (String n)
	{
		LOG.info("got registered event " + n);
		return n;
	}
	
	@Override
	public void loadDefaultConfiguration() {	
	}
	
	public void setContext(Context context)
	{
		this.context = context;
	}
	
	public void startService()
	{
		super.startService();
	}
	
	public void startSensors()
	{
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		lastUpdate = System.currentTimeMillis();
	}
	
	public void stopService()
	{
		super.stopService();
		sensorManager.unregisterListener(this);
	}
	
	@Override
	public String getToolTip() {
		return "used as a general android";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Android android = new Android("android");
		android.startService();
		
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
		
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float[] values = event.values;
			// Movement
			float x = values[0];
			float y = values[1];
			float z = values[2];

			float accelationSquareRoot = (x * x + y * y + z * z)
					/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
			long actualTime = System.currentTimeMillis();
			if (accelationSquareRoot >= 2) //
			{
				if (actualTime - lastUpdate < 200) {
					return;
				}
				lastUpdate = actualTime;
				/*
				Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT)
						.show();
				if (color) {
					view.setBackgroundColor(Color.GREEN);
					
				} else {
					view.setBackgroundColor(Color.RED);
				}
				*/
				color = !color;
				LOG.info("color " + color);
			}

		}
		
	}

	protected void onResume() {
		// register this class as a listener for the orientation and
		// accelerometer sensors
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	protected void onPause() {
		// unregister listener
		sensorManager.unregisterListener(this);
	}
	
	public HashMap<String, ServiceActivity> getServiceGUIMap() {
		return serviceActivityMap;
	}


	public boolean preProcessHook(Message m)
	{
		if (commandMap.containsKey(m.method))
		{
			return true;
		} 
		
		ServiceActivity sg = serviceActivityMap.get(m.sender);
		if (sg == null) {
			LOG.error("attempting to update sub-gui - sender "
					+ m.sender + " not available in map " + getName());
		} else {
			invoke(serviceActivityMap.get(m.sender), m.method, m.data);
		}
		
		return false;
	}
	
}
