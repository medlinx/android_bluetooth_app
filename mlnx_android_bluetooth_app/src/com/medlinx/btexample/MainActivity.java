package com.medlinx.btexample;

import java.text.DateFormat;
import java.util.Date;

import mlnx.android.bluetooth.app.R;







import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	protected static final String TAG = MainActivity.class.getName();
	private ToggleButton switchButton;
	private TextView status, message;
	private BluetoothAdapter mBluetoothAdapter;
	// Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
	private ProgressDialog pro_dialog;
    private BTExampleApp btClient;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		switchButton = (ToggleButton) findViewById(R.id.switchButton);
		status = (TextView) findViewById(R.id.status);
		message = (TextView) findViewById(R.id.message);
		btClient = (BTExampleApp) getApplication();
		switchButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				if(switchButton.isChecked()){
					// connecting
					Log.i(TAG, "connection");
					connectBT();
				}else{
					Log.i(TAG, "disconnection");
					disconnectBT();
				}
				
			}});
		
		// bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		IntentFilter filter = new IntentFilter();
        filter.addAction(BTExampleApp.BT_STATE_CHANGE_ACTION);
        registerReceiver(stateCastReceiver, filter );
        
        LocalBroadcastManager.getInstance(this).registerReceiver(ecgDataReceiver, 
        		new IntentFilter(BTExampleApp.ECG_RESULT));
        
        
	}
	
	private BroadcastReceiver ecgDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getStringExtra(BTExampleApp.ECG_MSG);
            // do something here.
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            Log.i(TAG, currentDateTimeString + ":" + s);
            status.setText(currentDateTimeString + ":" + s);
        }
    };
	
    @Override
	protected void onDestroy() {
    	unregisterReceiver(stateCastReceiver);
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(ecgDataReceiver);
		super.onDestroy();
	}



	@Override
	protected void onResume() {
		if(BTExampleApp.getBTServiceState(this) == BTExampleApp.BT_STATE_CONNECTED){
			switchButton.setChecked(true);
		}else
			switchButton.setChecked(false);
		super.onResume();
	}



	protected void showProgressDialog(String title, String message) {
		pro_dialog = new ProgressDialog(this);
		pro_dialog.setTitle(title);
		pro_dialog.setMessage(message);
		pro_dialog.setCancelable(false);
		pro_dialog.setIndeterminate(true);
		pro_dialog.show();
		
	}	
    
	private BroadcastReceiver stateCastReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(BTExampleApp.BT_STATE_CHANGE_ACTION)){
				int state = BTExampleApp.getBTServiceState(MainActivity.this);
				String text = null;
				switch (state) {
                case BTExampleApp.BT_STATE_CONNECTED:
                	
                	if(intent.hasExtra(BTExampleApp.BT_STATE_CHANGE_MSG))
                		text = intent.getExtras().getString(BTExampleApp.BT_STATE_CHANGE_MSG); 
            
                		 
                	Toast.makeText(getApplicationContext(), text,
                            Toast.LENGTH_SHORT).show();
                	
                	if(pro_dialog != null)
                		pro_dialog.dismiss();
                	switchButton.setChecked(true);
                    break;
                    
                case BTExampleApp.BT_STATE_CONNECTING:
                	showProgressDialog(getString(R.string.title_connecting),
                			getString(R.string.msg_connecting));
                	Log.i(TAG,"bluetooth connecting");
                    break;
                    
                case BTExampleApp.BT_STATE_LISTEN:
                case BTExampleApp.BT_STATE_NONE:
                	if(pro_dialog != null)
                		pro_dialog.dismiss();
                	switchButton.setChecked(false);
                	if(intent.hasExtra(BTExampleApp.BT_STATE_CHANGE_MSG))
                		text = intent.getExtras().getString(BTExampleApp.BT_STATE_CHANGE_MSG); 
                    
                	if(text != null)
                	{	
                		Toast.makeText(getApplicationContext(), text,
                				Toast.LENGTH_SHORT).show();
                	}
            		Log.v(TAG,"state none");
                    break;
                }
			}
			
		}

	};

	protected void disconnectBT() {
		btClient.disconnect(this);
	}
	

	protected void connectBT() {
		Log.i(TAG, "connect bluetooth");

//		disconnectBT();
		btClient.start(this);
		
		if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
		}
		else if (BTExampleApp.getBTServiceState(this) == BTExampleApp.BT_STATE_NONE) {
    		// Launch the DeviceListActivity to see devices and do scan
    		Intent serverIntent = new Intent(this, DeviceListActivity.class);
    		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    	}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + requestCode + " " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                
                // Attempt to connect to the device
	            btClient.connectDevice(this, address);  
                
            
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
        	if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
        	
                Log.d(TAG, "BT not enabled");
                
                finishDialogNoBluetooth();                
            }
        }
    }
    
	public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       finish();            	
                	   }
               });
        AlertDialog alert = builder.create();
        alert.show(); 
    }
}
