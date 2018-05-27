package com.oulu.lock.bleservices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.oulu.lock.BluetoothLeService;

/**
 * Created by liao on 2017/9/4.
 */

public class BluetoothUtils extends BroadcastReceiver{
    private Context mContext;
    private static BluetoothUtils bluetoothUtils;
    private static BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BlueInfoCallBcak mBlueInfoCallBcak;
    private boolean isFinddevices = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!isFinddevices) {
                        if (mBlueInfoCallBcak != null) {
                            mBlueInfoCallBcak.onScanTimeout();
                        }
                    }
            }
        }
    };

    public static void initialize(Context context) {
        bluetoothUtils = new BluetoothUtils(context);

    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public BluetoothUtils(Context context){
        mContext=context;
        bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

    }
    public static  BluetoothUtils getBluetoothUtils(){
        return bluetoothUtils;
    }
    public BluetoothUtils(){

    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            Log.i("liao","ACTION_GATT_CONNECTED="+intent.getStringExtra("active_device"));
        } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

        } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

        } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//
        }
    }

    public interface BlueInfoCallBcak {
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord);

        public void onstartLeScan(boolean flag);

        public void onstopLeScan(boolean flag);

        public void onScanTimeout();
    }

    public void setBlueInfoCallBcak(BlueInfoCallBcak blueInfoCallBcak) {
        this.mBlueInfoCallBcak = blueInfoCallBcak;
    }
    private Runnable mRunable = new Runnable() {
        @Override
        public void run() {
            stopLeScan();
            mHandler.sendEmptyMessage(1);
        }
    };
    public void startLeScan() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.startLeScan(mLeScanCallback);
            isFinddevices=false;
            if (mBlueInfoCallBcak != null)
                mBlueInfoCallBcak.onstartLeScan(true);
            mHandler.removeCallbacks(mRunable);
            mHandler.postDelayed(mRunable,10000);
        }

    }
    public void startLeScan(long timeout) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.startLeScan(mLeScanCallback);
            isFinddevices=false;
            if (mBlueInfoCallBcak != null)
                mBlueInfoCallBcak.onstartLeScan(true);
            mHandler.removeCallbacks(mRunable);
            mHandler.postDelayed(mRunable,timeout);
        }

    }
    public void stopLeScan() {
        if (bluetoothAdapter != null) {
            //if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.stopLeScan(mLeScanCallback);
            //}
            isFinddevices=false;
            if (mBlueInfoCallBcak != null)
                mBlueInfoCallBcak.onstopLeScan(true);
            mHandler.removeCallbacks(mRunable);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if(mBlueInfoCallBcak!=null)
                        mBlueInfoCallBcak.onLeScan(device,rssi,scanRecord);
                    isFinddevices=true;
                }
            };

}
