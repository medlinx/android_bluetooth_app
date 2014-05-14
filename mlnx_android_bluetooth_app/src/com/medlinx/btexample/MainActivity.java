package com.medlinx.btexample;

import java.text.DateFormat;
import java.util.Date;

import com.medlinx.ECGSignal;
import com.medlinx.MlnxBTClient;
import com.medlinx.MlnxEventListener;
import com.medlinx.vstp.VstpDataType;
import com.medlinx.vstp.VstpDeviceMode;
import com.medlinx.vstp.VstpDevicePosition;



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
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	protected static final String TAG = MainActivity.class.getName();
	private ToggleButton switchButton, dataTransfer;
	private TextView status, message, heartRate, deviceInfo;
	private BluetoothAdapter mBluetoothAdapter;
	// Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
	private ProgressDialog pro_dialog;
	private int totalECGBytes;
	

	
	private MlnxBTClient btClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Init BT client
		initBTClient();
		
		switchButton = (ToggleButton) findViewById(R.id.switchButton);
		status = (TextView) findViewById(R.id.status);
		message = (TextView) findViewById(R.id.message);
		heartRate = (TextView) findViewById(R.id.heartrate);
		deviceInfo = (TextView) findViewById(R.id.deviceInfo);
		switchButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				if(switchButton.isChecked()){
					// connecting
					Log.i(TAG, "connection");
					connectBT();
					setDataTransferButton(true);
				}else{
					Log.i(TAG, "disconnection");
					disconnectBT();
					setDataTransferButton(false);
				}
				
			}});
		
		dataTransfer = (ToggleButton) findViewById(R.id.dataTransfer);
		dataTransfer.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(switchButton.isChecked()){
					if(dataTransfer.isChecked()){
						// turn it on
						btClient.setDataTransferState(true);
					}else{
						// turn it off
						btClient.setDataTransferState(false);
					}
				}else{
					// When BT disconnects, dataTransfer is always unchecked
					setDataTransferButton(false);
				}
			}
			
		});
		
		// bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		IntentFilter filter = new IntentFilter();
        filter.addAction(MlnxBTClient.BT_STATE_CHANGE_ACTION);
        registerReceiver(btStatetReceiver, filter );
        
        btClient.setOnEventListener(new MlnxEventListener(){
        	/**
        	 * Mlnx device enters data transfer mode. ���ݴ��俪ʼ
        	 */
        	public void onResume(){
        		setDataTransferButton(true);
        		
        	}
        	
        	/**
        	 * Mlnx device enters idle mode. ���ݴ���ֹͣ�����Ǳ�����������
        	 */
        	public void onIdle(){
        		setDataTransferButton(false);
        		
        	}

        	/**
        	 * this method will be called when ECG data is received. 
        	 * @param ecg
        	 */
			@Override
			public void onECGSignalUpdate(ECGSignal ecg) {
				Log.i(TAG, "ECG signal updated");
				float[][] data = ecg.getSignals();
				totalECGBytes += data.length;
	    		final String msg = "receive ecg data " + totalECGBytes + " points";
	    		final String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
	            Log.i(TAG, currentDateTimeString + ":" + msg);
	            runOnUiThread(new Runnable(){

					@Override
					public void run() {
						status.setText(currentDateTimeString + ":" + msg);
					}
	            	
	            });
	            
			}

			/**
			 * this method will be called when the heart rate is updated.
			 */
			@Override
			public void onHeartRateUpdate(final int heartrate) {
				Log.i(TAG, "heartate: " + heartrate);
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						heartRate.setText("���ʣ�" + 
								(heartrate == 255 ? "δ֪" : heartrate)
								);
					}
	            	
	            });
			}

			/**
			 * this method will be called when the battery of the Bluetooth device changes.
			 * @param battery
			 */
			@Override
			public void onBatteryRemainingChanged(final int battery) {
				Log.i(TAG, "remaining battery: " + battery + "%");
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						deviceInfo.setText("�豸ʣ�������" + battery + "%");
					}
	            	
	            });
			}

			/**
			 * this method will be called when the electrode of the Bluetooth device changes.
			 * @param electrode: electrode connection status
			 * �缫�迹�ֽڶ��壺<V6�迹><V5�迹><V4�迹><V3�迹><V2�迹><V1�迹><RL�迹><RA�迹><LL�迹><LA�迹>
			 * �迹bits�� 1���缫�Ͽ����迹�����0���缫�迹Ϊ0��������
			 */
			@Override
			public void onElectrodeChanged(final int electrode) {
				Log.i(TAG, "Electrode flags: " + electrode);
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						deviceInfo.setText("�缫��־λ��" + Integer.toBinaryString(electrode));
					}
	            	
	            });
			}


			/**
			 * this method will be called when the acceleration type is updated.
			 * @param acceleration ����ֽ�Ŀǰֻ�����λ(MSB)�͵���λ��Ч���������Ϊ���Ϊ1��ʾ���������˶���0��ʾ���˾�ֹ��
			 * �����λ��ʾ������̬��������豸����ķ�ʽ�йء�����Ŀǰ�ݶ���
			 * 000 �� 001��010��011������վ�� ��ʵ����010��011�ǵ��������������������˲��ᵹ����Ŀǰ������ΪҲ��վ����������
			 * 100�����������
			 * 101�������Ҳ���
			 * 110������ƽ��
			 * 111�����˸���
			 */
			@Override
			public void onDevicePositionChanged(
					final VstpDevicePosition devicePosition) {
				Log.i(TAG, "device position changed: " + devicePosition);
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						deviceInfo.setText("�豸λ�ã�" + devicePosition);
					}
	            	
	            });
			}
        	
        });
               
	}
	

	private void setDataTransferButton(final boolean checked){
		this.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				dataTransfer.setChecked(checked);
				if(checked)
					Toast.makeText(getApplicationContext(), "���ݴ��俪ʼ", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), "���ݴ���ֹͣ", Toast.LENGTH_SHORT).show();
			}
			
		});
	}
	
	
    private void initBTClient() {
    	btClient = MlnxBTClient.getClient(1, VstpDeviceMode.ECG_ADVANCED, VstpDataType.ECG_8CH);
	}



	@Override
	protected void onDestroy() {
    	super.onDestroy();
	}



	@Override
	protected void onResume() {
		if(MlnxBTClient.getBTServiceState(this) == MlnxBTClient.BT_STATE_CONNECTED){
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
    
	private BroadcastReceiver btStatetReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(MlnxBTClient.BT_STATE_CHANGE_ACTION)){
				int state = MlnxBTClient.getBTServiceState(MainActivity.this);
				String text = null;
				switch (state) {
                case MlnxBTClient.BT_STATE_CONNECTED:
                	
                	if(intent.hasExtra(MlnxBTClient.BT_STATE_CHANGE_MSG))
                		text = intent.getExtras().getString(MlnxBTClient.BT_STATE_CHANGE_MSG); 
            
                		 
                	Toast.makeText(getApplicationContext(), text,
                            Toast.LENGTH_SHORT).show();
                	
                	if(pro_dialog != null)
                		pro_dialog.dismiss();
                	switchButton.setChecked(true);
                    break;
                    
                case MlnxBTClient.BT_STATE_CONNECTING:
                	showProgressDialog(getString(R.string.title_connecting),
                			getString(R.string.msg_connecting));
                	Log.i(TAG,"bluetooth connecting");
                    break;
                    
                case MlnxBTClient.BT_STATE_LISTEN:
                case MlnxBTClient.BT_STATE_NONE:
                	if(pro_dialog != null)
                		pro_dialog.dismiss();
                	switchButton.setChecked(false);
                	setDataTransferButton(false);
                	if(intent.hasExtra(MlnxBTClient.BT_STATE_CHANGE_MSG))
                		text = intent.getExtras().getString(MlnxBTClient.BT_STATE_CHANGE_MSG); 
                    
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
		// Unregister Bluetooth receiver
		unregisterReceiver(btStatetReceiver);
	}
	

	protected void connectBT() {
		Log.i(TAG, "connect bluetooth");
		// Register bluetooth state receiver
		registerReceiver(btStatetReceiver, new IntentFilter(MlnxBTClient.BT_STATE_CHANGE_ACTION));
		btClient.start(this);
		
		if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
		}
		else if (MlnxBTClient.getBTServiceState(this) == MlnxBTClient.BT_STATE_NONE) {
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
