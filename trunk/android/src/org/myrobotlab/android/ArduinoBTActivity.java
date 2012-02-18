package org.myrobotlab.android; 

import org.myrobotlab.service.ArduinoBT;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

// references :
// http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/

//  extends ServiceActivity
	public class ArduinoBTActivity extends ServiceActivity {

	ArduinoBT myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.arduinobt);
        
        myService = (ArduinoBT)sw.service;   
        // setText(R.id.udpdata, myService.servicePort);
        //setContentView(layout);        
        
		Button getPort = (Button) layout.findViewById(R.id.getPort);
		getPort.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
	            Intent enableIntent = new Intent(MRL.getInstance(), DeviceListActivity.class);
	            startActivityForResult(enableIntent, BluetoothChat.REQUEST_ENABLE_BT);
			}
		});


    }

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub
		
	}

}
