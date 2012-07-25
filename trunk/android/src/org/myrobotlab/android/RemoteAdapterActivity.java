package org.myrobotlab.android; 

import java.net.InetAddress;
import java.util.ArrayList;

import org.myrobotlab.service.RemoteAdapter;

import android.os.Bundle;

public class RemoteAdapterActivity extends ServiceActivity {

	RemoteAdapter myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.remoteadapter);
        myService = (RemoteAdapter)sw.service;   
        setText(R.id.udpdata, myService.UDPPort);
        ArrayList<InetAddress> addresses = RemoteAdapter.getLocalAddresses();
        StringBuffer ips = new StringBuffer();
        for (int i = 0; i < addresses.size(); ++i)
        {
        	ips.append(addresses.get(i).getHostAddress());
        	ips.append(";");
        }
        
        setText(R.id.local_addresses, ips.toString() );
        
    }

	@Override
	public void attachGUI() {
	}

	@Override
	public void detachGUI() {
	}

}
