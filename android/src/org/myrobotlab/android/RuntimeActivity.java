package org.myrobotlab.android; 

import org.myrobotlab.framework.Platform;
import org.myrobotlab.service.Runtime;

import android.os.Bundle;

public class RuntimeActivity extends ServiceActivity {

	Runtime myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.runtime);
        myService = (Runtime)sw.service;
        Platform platform = Runtime.getPlatform();
        setText(R.id.vmName, platform.vmName);
        setText(R.id.arch, platform.arch);
        setText(R.id.bitness, platform.bitness);
        setText(R.id.os, platform.os);
    }

	@Override
	public void attachGUI() {
	}

	@Override
	public void detachGUI() {
	}

}
