package org.myrobotlab.android;

import org.myrobotlab.service.Servo;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class ServoActivity extends ServiceActivity implements OnClickListener {

	Servo myService = null;
	Spinner pin;
	Button attach;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.servo_activity);
		myService = (Servo) sw.service;

		attach = (Button) findViewById(R.id.attach);
		attach.setOnClickListener(this);

		// available services
		pin = (Spinner) findViewById(R.id.pin);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.servoPins, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		pin.setAdapter(adapter);

	}

	@Override
	public void onClick(View arg0) {

	}

}
