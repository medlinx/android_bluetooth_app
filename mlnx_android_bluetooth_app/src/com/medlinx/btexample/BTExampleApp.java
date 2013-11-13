package com.medlinx.btexample;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.medlinx.ECGSignal;
import com.medlinx.MedLinXApp;

public class BTExampleApp extends MedLinXApp{

	private static final String TAG = "BTExampleApp";

	public static final String ECG_RESULT = "com.medlinx.bluetooth.ecg";

	public static final String ECG_MSG = "ecg_msg";

	private LocalBroadcastManager broadcaster;
	
	
	@Override
	public void onCreate() {
		broadcaster = LocalBroadcastManager.getInstance(this);
		super.onCreate();
	}

	@Override
	public void onECGSignalUpdate(ECGSignal ecg) {
		float[][] data = ecg.getSignals();
		Log.i(TAG, "receive ecg data " + data.length);
		sendResult("receive ecg data " + data.length + " points");
	}

	public void sendResult(String message) {
	    Intent intent = new Intent(ECG_RESULT);
	    if(message != null)
	        intent.putExtra(ECG_MSG, message);
	    broadcaster.sendBroadcast(intent);
	}
}
