package org.myrobotlab.android; 

import org.myrobotlab.service.RemoteAdapter;

import android.os.Bundle;

public class RemoteAdapterActivity extends ServiceActivity {

	RemoteAdapter myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.remoteadapter);
        myService = (RemoteAdapter)sw.service;   
        setText(R.id.udpdata, myService.UDPPort);
    }

	@Override
	public void attachGUI() {
	}

	@Override
	public void detachGUI() {
	}

}
