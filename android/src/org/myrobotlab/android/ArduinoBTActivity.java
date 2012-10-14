package org.myrobotlab.android; 

import java.util.HashMap;

import org.myrobotlab.service.ArduinoBT;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.data.PinState;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

	public class ArduinoBTActivity extends ServiceActivity implements OnSeekBarChangeListener {

	// Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE 	= 1;
    public static final int MESSAGE_READ 			= 2;
    public static final int MESSAGE_WRITE 			= 3;
    public static final int MESSAGE_DEVICE_NAME 	= 4;
    public static final int MESSAGE_TOAST 			= 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 2;
    public static final int REQUEST_ENABLE_BT = 3;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    DigitalClickListener digitalClickListener = new DigitalClickListener();
    AnalogClickListener analogClickListener = new AnalogClickListener();
    InOutClickListener inOutClickListener = new InOutClickListener();
    
	ArduinoBT myService = null;
	
	public class PinButtonGroup 
	{		
		public int pinID;
		public PinState pin = null;
		public ImageButton inOut = null;
		public ImageButton signal = null;
		public SeekBar analog = null;
	}
	
	/**
	 * GUI button IDs to a pinGroup.. many gui elements can point to a single
	 * button group, whos responsibility is a single pin - many to one relationship
	 */
	HashMap <Integer, PinButtonGroup> buttons = new HashMap <Integer, PinButtonGroup>();
	
	/**
	 * index for finding the button group based on pin data from the backend
	 */
	HashMap <Integer, PinButtonGroup> pinToButton = new HashMap <Integer, PinButtonGroup>();
	
	/**
	 * container object to contain the "actual" pin control
	 * and all additional UI components to control it
	 * this can include :
	 * mode button - switches from INPUT/OUTPUT
	 * value button - which can be 0 or 1 for digital pins
	 * analogSeekBar - for PWM pins
	 * TextViews - for analog read values
	 * 
	 * many UI elements are needed or can control a single PinState
	 * therefore a Map is used to map the UI id value to the PinControl
	 *
	 */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.arduinobt);
        
        // FIXME - 
        // setup the handler from Android service for 
        // message routing from the service to the activity
        myService = (ArduinoBT)sw.service;   
        myService.setmHandler(mHandler);
                
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_right_text);
        String deviceName = myService.getDeviceName();
        if  (deviceName == null) {
        	mTitle.setText(R.string.not_connected);
        } else {
        	mTitle.setText(deviceName);
        }
        
		Button getPort = (Button) layout.findViewById(R.id.getPort);
		getPort.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
	            Intent enableIntent = new Intent(MRL.getInstance(), DeviceListActivity.class);
	            //startActivityForResult(enableIntent, BluetoothChat.REQUEST_ENABLE_BT);
	            startActivityForResult(enableIntent, REQUEST_CONNECT_DEVICE);
			}
		});

        initPinButtons();
    }
    
    public void initPinButtons()
    {
    	    	
    	for (int i = 0; i < myService.pins.size(); ++i) // 14 digital +6 analog
    	{
    		// digital value button (grey/green/red?)
    		PinButtonGroup p = new PinButtonGroup();
    		pinToButton.put(i, p);
    		
    		String name = "s" + i;
    		int bID = getResources().getIdentifier(name, "id", "org.myrobotlab.android");
    		p.pinID = i;
			p.pin = myService.pins.get(i);
    		if (bID != 0)
		    {
    			p.signal = ((ImageButton) findViewById(bID));
        		if (i < 14)
        		{
        			p.signal.setOnClickListener(digitalClickListener);
        		} else {
        			p.signal.setOnClickListener(analogClickListener);
        		}
    			buttons.put(bID, p);
		    } else {
		    	if(D) Log.e(TAG, "button " + name + " can not be found");	
		    }

    		// 
    		if (i < 14)
    		{
			    name = "b" + i;		    
			    bID = getResources().getIdentifier(name, "id", "org.myrobotlab.android");
			    if (bID != 0)
			    {
	    			p.inOut = ((ImageButton) findViewById(bID));
	    			p.inOut.setOnClickListener(inOutClickListener);
	    			buttons.put(bID, p);
			    } else {
			    	if(D) Log.e(TAG, "button " + name + " can not be found");	
			    }
    		}
		    if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11)
		    {
		    	name = "sb" + i;
		    	bID = getResources().getIdentifier(name, "id", "org.myrobotlab.android");
		    	SeekBar analog = ((SeekBar) findViewById(bID));
		    	p.analog = analog;
		    	p.analog.setOnSeekBarChangeListener(this);
		    	buttons.put(bID, p);
		    }
		    
		    //int resID = getResources().getIdentifier("org.anddev.android.testproject:drawable/bug", null, null);
		    //digitalStatusButtons[i] =
		    //buttons[i][j].setOnClickListener(this);
    	}
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
        	Log.d(TAG, "ERROR - bluetooth adapter not enabled");
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (myService.getBTState() != ArduinoBT.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            myService.write(send);

            // Reset out string buffer to zero and clear the edit text field
           // mOutStringBuffer.setLength(0);
           // mOutEditText.setText(mOutStringBuffer);
        }
    }

	public synchronized void serialSend(int function, int param1, int param2) 
    {
        // Check that we're actually connected before trying anything
        if (myService.getBTState() != ArduinoBT.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        //if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            //byte[] send = message.getBytes();
            myService.write(function, param1, param2);

            // Reset out string buffer to zero and clear the edit text field
           // mOutStringBuffer.setLength(0);
           // mOutEditText.setText(mOutStringBuffer);
        //}
    }
    
    
        
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        /*
        if (myService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (myService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              myService.start();
            }
        }
        */
    }
    
    private TextView mTitle;
    // Name of the connected device
    private String mConnectedDeviceName = null;

    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case ArduinoBT.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
                    break;
                case ArduinoBT.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case ArduinoBT.STATE_LISTEN:
                case ArduinoBT.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
               // mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                // setupChat(); no need all setup
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }    
    
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        
        // Attempt to connect to the device
        myService.connect(device);
    }
    

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //if (myService != null) myService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    
    // does not return PinData, because if you are hooking/registering off the 
    // GUI for publishPin events ... you are doing it wrong..  register off the
    // Service
    public void publishPin(Pin data)
    {
    	Log.e(TAG, "--- publishPin ---");
    	//PinButtonGroup p = pinToButton.get(data.pin);
    	if (data.pin > 13)
    	{
    		// analog pin polling event
    		String btName = "v" + data.pin;
		    int bID = getResources().getIdentifier(btName, "id", "org.myrobotlab.android");
		    TextView value = ((TextView) findViewById(bID));
		    value.setText(Integer.toString(data.value));    		
    	}
    }
    
	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub
		// pinMode change
		// digitalWrite change
		// analogWrite change
		subscribe("publishState", "getState", ArduinoBT.class);
		subscribe("publishPin", "publishPin", Pin.class);
		//myService.send(boundServiceName, "publishState"); TODO - broadcast first state
	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProgressChanged(SeekBar analog, int value, boolean arg2) {

		PinButtonGroup p = buttons.get(analog.getId());
		myService.send(boundServiceName, ArduinoBT.analogWrite, new IOData(p.pin.address, value));
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public class DigitalClickListener implements OnClickListener
	{

		@Override
		public void onClick(View view) {
			PinButtonGroup p = buttons.get(view.getId());
			
			//Toast.makeText(this, p.name + " " + p.type, Toast.LENGTH_SHORT).show();

			// get type
			// check state
			// send message
			// change state
			
			// digital "VALUE" button pressed
				if (p.pin.value == 0) // last pin value 
				{
					// send pinData to ArduinoBT - digitalWrite
					// choice is - set it now - or get it on sync
					// level of sync - call back would be sync with the Service
					// TODO - optimization - create a single IOData per pin
					myService.send(boundServiceName, ArduinoBT.digitalWrite, new IOData(p.pin.address, ArduinoBT.HIGH));
					p.signal.setImageResource(R.drawable.green); // FIXME possibly wrong state
					p.pin.value = ArduinoBT.HIGH; // FIXME - BS - this should be set by event callback
				} else {
					myService.send(boundServiceName, ArduinoBT.digitalWrite, new IOData(p.pin.address, ArduinoBT.LOW));				
					p.signal.setImageResource(R.drawable.grey);
					p.pin.value = ArduinoBT.LOW; // FIXME - BS - this should be set by event callback
				}
				
			}
	}
	
	public class InOutClickListener implements OnClickListener
	{

		@Override
		public void onClick(View view) {
			PinButtonGroup p = buttons.get(view.getId());
			
			//Toast.makeText(this, p.name + " " + p.type, Toast.LENGTH_SHORT).show();
				if (p.pin.mode == PinState.INPUT) // last pin value 
				{
					// send pinData to ArduinoBT - digitalWrite
					// choice is - set it now - or get it on sync
					// level of sync - call back would be sync with the Service
					// TODO - optimization - create a single IOData per pin
					myService.send(boundServiceName, ArduinoBT.pinMode, PinState.OUTPUT);
					myService.send(boundServiceName, ArduinoBT.analogReadPollingStop, p.pin.address);					
					p.inOut.setImageResource(R.drawable.square_out); // FIXME possibly wrong state
					p.pin.mode = ArduinoBT.HIGH; // FIXME - BS - this should be set by event callback
				} else {
					myService.send(boundServiceName, ArduinoBT.pinMode, PinState.INPUT);				
					myService.send(boundServiceName, ArduinoBT.analogReadPollingStart, p.pin.address);					
					p.inOut.setImageResource(R.drawable.square_in);
					p.pin.mode = ArduinoBT.LOW; // FIXME - BS - this should be set by event callback
				}
				
			}
		}
	public class AnalogClickListener implements OnClickListener
	{

		@Override
		public void onClick(View view) {
			PinButtonGroup p = buttons.get(view.getId());
			
			//Toast.makeText(this, p.name + " " + p.type, Toast.LENGTH_SHORT).show();
				if (p.pin.mode == PinState.INPUT) // last pin value 
				{
					// send pinData to ArduinoBT - digitalWrite
					// choice is - set it now - or get it on sync
					// level of sync - call back would be sync with the Service
					// TODO - optimization - create a single IOData per pin
					myService.send(boundServiceName, ArduinoBT.analogReadPollingStop, p.pin.address);
					p.signal.setImageResource(R.drawable.grey); // FIXME possibly wrong state
					p.pin.mode = PinState.OUTPUT; // FIXME - BS - this should be set by event callback
				} else {
					myService.send(boundServiceName, ArduinoBT.analogReadPollingStart, p.pin.address);				
					p.signal.setImageResource(R.drawable.red);
					p.pin.mode = PinState.INPUT; // FIXME - BS - this should be set by event callback
				}
				
			}
		}
		
}