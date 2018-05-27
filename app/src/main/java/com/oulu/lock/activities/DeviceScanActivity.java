/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oulu.lock.activities;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.oulu.lock.MainActivity;
import com.oulu.lock.R;
import com.oulu.lock.bleservices.BluetoothUtils;
import com.oulu.lock.utils.AESUtil;
import com.oulu.lock.utils.Utils;
import com.oulu.lock.view.CircleWaveView;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private boolean isFindBtDevices=false;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_SELECT_BT = 2;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    CircleWaveView device_circle_wave_view;
    ListView device_list;
    AlertDialog alertDialog;
    TextView custom_action_bar;
    LinearLayout devices_list;
    ImageView search_tv;
    LinearLayout searching;
    TextView back_tv;
    ImageView loading;

    public static final int ACCESS_LOCATION=101;
    private BluetoothUtils mBluetoothUtils;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getActionBar().setTitle(R.string.title_devices);
        setContentView(R.layout.scan_bt);
        mBluetoothUtils=new BluetoothUtils();
        mBluetoothUtils.setBlueInfoCallBcak(new BluetoothUtils.BlueInfoCallBcak() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                isFindBtDevices=true;
                String scanRecordstring =  AESUtil.BytetohexString(scanRecord);
                Log.i("liao","onLeScan=="+ AESUtil.BytetohexString(scanRecord));
                Log.i("liao","name=="+ device.getName());
                Log.i("liao","address=="+ device.getAddress());
                String[] split = device.getAddress().split(":");
                String filterS = "6688";
                for(String s : split){
                    filterS += s;
                }
                Log.i("liao","filterS="+filterS);
                if(scanRecordstring != null ){
                    if(scanRecordstring.contains(filterS)){
                        mLeDeviceListAdapter.addDevice(device);
                    }
                }
            }

            @Override
            public void onstartLeScan(boolean flag) {
                isFindBtDevices=false;
                mScanning = true;
                Log.i("liao","onstartLeScan");
            }

            @Override
            public void onstopLeScan(boolean flag) {
                mScanning = false;
                Log.i("liao","onstopLeScan");
                search_tv.setVisibility(View.VISIBLE);
                searching.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onScanTimeout() {
                Log.i("liao","onScanTimeout");
                if (alertDialog == null) {
                    alertDialog=new AlertDialog.Builder(DeviceScanActivity.this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.no_devices)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mBluetoothUtils.startLeScan();
                                }
                            })
                            .setNegativeButton(R.string.exist, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    setResult(RESULT_CANCELED, intent);
                                    finish();
                                }
                            }).create();
                }
                try {
                    if(!isFindBtDevices){
                        alertDialog.show();

                        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                Intent intent=new Intent();
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, "");
                                intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, "");
                                setResult(RESULT_OK, intent);
                                finish();
                                return false;
                            }
                        });
                    }
                } catch (Exception e) {
                    alertDialog=null;
                }
            }
        });
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {

            }
        };

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        device_circle_wave_view=(CircleWaveView)findViewById(R.id.device_circle_wave_view);

        device_list=(ListView)findViewById(R.id.device_list);
        custom_action_bar=(TextView)this.findViewById(R.id.custom_action_bar);
        devices_list=(LinearLayout)this.findViewById(R.id.devices_list);

        search_tv = (ImageView)this.findViewById(R.id.search_tv);
        searching = (LinearLayout)this.findViewById(R.id.searching);
        search_tv.setVisibility(View.VISIBLE);
        search_tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                search_tv.setVisibility(View.INVISIBLE);
                searching.setVisibility(View.VISIBLE);
                if (!mBluetoothUtils.getBluetoothAdapter().isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

                }else{
                    mBluetoothUtils.startLeScan();
                }
            }
        });
        searching.setVisibility(View.INVISIBLE);
        back_tv = (TextView) this.findViewById(R.id.back_tv);
        back_tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceScanActivity.this.finish();
            }
        });
        loading = (ImageView) this.findViewById(R.id.loading);
        ObjectAnimator anim = ObjectAnimator.ofFloat(loading, "rotation", 0, 360);
        anim.setRepeatCount(-1);
        anim.setDuration(1500L);
        anim.setInterpolator(new LinearInterpolator());
        anim.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        device_list.setAdapter(mLeDeviceListAdapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        Log.i("liao","onActivityResult");
        mBluetoothUtils.startLeScan();
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothUtils.stopLeScan();
        mLeDeviceListAdapter.clear();


        //add for umeng
        // MobclickAgent.onPause(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //to avoid  android.view.WindowLeaked
        if(alertDialog!=null){
            alertDialog.dismiss();
        }
        alertDialog=null;
        //mBluetoothUtils.stopLeScan();
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            Utils.Log("deviceName="+deviceName+"111");
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());


            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("liao","onClick");
                    onListItemClick(i);
                }
            });
            return view;
        }
        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            devices_list.setVisibility(View.VISIBLE);
            isFindBtDevices=true;
        }
    }
    protected void onListItemClick(int position) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
//        final Intent intent = new Intent(this, DeviceControlActivity.class);
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
//        startActivity(intent);

        if (mScanning) {
            mBluetoothUtils.stopLeScan();
            mScanning = false;
        }
        Intent intent=new Intent();
        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        setResult(RESULT_OK, intent);
        finish();
    }



    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}