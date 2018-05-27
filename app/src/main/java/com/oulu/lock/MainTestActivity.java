package com.oulu.lock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.oulu.lock.activities.DeviceScanActivity;
import com.oulu.lock.managers.LockManagers;
import com.oulu.lock.utils.Utils;

import java.util.UUID;

import static com.oulu.lock.BluetoothLeService.ACTION_GETLOGIN_STATUS;

/**
 * Created by liao on 2018/3/15.
 */

public class MainTestActivity extends Activity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean iServiceBind = true;
    private BluetoothLeService mBluetoothLeService;
    private static ProgressDialog progressDialog;// 等待进度圈
    private static final int MSG_STOP_WAIT_BT = 2;
    private static final long WAIT_PERIOD = 10000;
    AlertDialog connectFailAlertDialog;
    public boolean mConnected = false;
    private LockManagers mLockManagers;
    private Button mLogin;
    private Button getToken;
    private Button barttery;
    private Button getlockstatues;
    private Button unlockaction,lockaction;
    private Button modifymane,modifypassword;
    private Button search;
    private TextView devicesinfo;
    private EditText password;
    private EditText devicesname,editpassword;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Utils.Log("Unable to initialize Bluetooth");
                finish();
            }
            mLockManagers.setBluetoothLeService(mBluetoothLeService);
            boolean result = reConnect();
            Utils.Log("onServiceConnected reConnect result=" + result);
            if(mDeviceAddress!=null && !"".equals(mDeviceAddress) && result) {
                devicesinfo.setText(mDeviceName);
            }else{
                Toast.makeText(MainTestActivity.this,"mDeviceAddress id null",Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            mLockManagers.setBluetoothLeService(null);
            mDeviceAddress="";
            devicesinfo.setText("");
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STOP_WAIT_BT:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    if (connectFailAlertDialog == null) {
                        connectFailAlertDialog = new AlertDialog.Builder(MainTestActivity.this)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.connectfailed)
//							.setCancelable(false)
                                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        boolean result = MainTestActivity.this.reConnect();
                                        if (!result) {
                                            Toast.makeText(MainTestActivity.this, R.string.donttoconnect, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null).create();
                    }
                    try {
                        connectFailAlertDialog.show();
                    } catch (Exception e) {
                        connectFailAlertDialog = null;
                    }
                    break;
            }
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Utils.Log("xxxxxxxxxxxxxxxxxx BroadcastReceiver ACTION_GATT_CONNECTED mConnected:" + mConnected);
                if (connectFailAlertDialog != null && connectFailAlertDialog.isShowing()) {
                    connectFailAlertDialog.dismiss();
                }
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    mHandler.removeMessages(MSG_STOP_WAIT_BT);//connect success remove the hint
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Utils.Log("xxxxxxxxxxxxxxxxxx BroadcastReceiver ACTION_GATT_DISCONNECTED mConnected:" + mConnected);
                Toast.makeText(MainTestActivity.this, R.string.light_disconnected, Toast.LENGTH_SHORT).show();
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                try {

                    BluetoothGattService gattService = mBluetoothLeService.getGattService(UUID.fromString(Utils.BT_GET_SERVICE_UUID));
                    BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID.fromString(Utils.BT_GET_CHARACTERISTIC_UUID));
                    mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    Utils.Log("xxxxxxxxxxxxxxxxxx BroadcastReceiver ACTION_GATT_SERVICES_DISCOVERED");
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        Utils.Log(" mHandler.removeMessages=" + mHandler.hasMessages(MSG_STOP_WAIT_BT));
                        mHandler.removeMessages(MSG_STOP_WAIT_BT);//connect success remove the hint
                    }

                } catch (Exception e) {
                    Utils.Log("ACTION_GATT_SERVICES_DISCOVERED   e=" + e.toString());
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                try {
                    Utils.Log("xxxxxxxxxxxxxxxxxx this is respone what i need:" + intent.getStringExtra(BluetoothLeService.EXTRA_DATA_NEED));
                    String responeString = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_NEED);
                    String[] responeStringArray = responeString.split(" ");
                    if ("02".equals(responeStringArray[1])) {

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    } else if ("88".equals(responeStringArray[1])) {
                        Thread.sleep(1000);

                    }
                    //TODO  if receiver FF means the cup is out of power. but i dont want to handle it now.
                } catch (Exception e) {
                }
            }else if(BluetoothLeService.ACTION_GETLOGIN_STATUS.equals(action)) {
                String loginstatus=intent.getStringExtra("login_status");
                Utils.Log("loginstatus===="+loginstatus);
                if("true".equals(loginstatus)){
                    if(mLockManagers!=null){
                        Toast.makeText(MainTestActivity.this,"登入成功",Toast.LENGTH_SHORT).show();
                        //mLockManagers.getTokenCmd();
                    }
                }else{
                    Toast.makeText(MainTestActivity.this,"登入失败",Toast.LENGTH_SHORT).show();
                }
            }else if(BluetoothLeService.ACTION_UNLOCK_VALUE.equals(action)) {
                String unlockflag = intent.getStringExtra("unlock_value");
                Utils.Log("unlockflag11="+unlockflag);
                if("true".equals(unlockflag)){
                    Toast.makeText(MainTestActivity.this,"解锁成功",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainTestActivity.this,"解锁失败",Toast.LENGTH_SHORT).show();
                }
            }else if(BluetoothLeService.ACTION_LOCK_VALUE.equals(action)) {
                String lockflag = intent.getStringExtra("lock_value");
                Utils.Log("lockflag11="+lockflag);
                if("true".equals(lockflag)){
                    Toast.makeText(MainTestActivity.this,"关锁成功",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainTestActivity.this,"关锁失败",Toast.LENGTH_SHORT).show();
                }
            }else if(BluetoothLeService.ACTION_GETTOKEN_STATUS.equals(action)){
                String gettokenflag=intent.getStringExtra("gettokenflag");
                Toast.makeText(MainTestActivity.this,"秘钥:"+gettokenflag,Toast.LENGTH_SHORT).show();
                /*if("true".equals(gettokenflag)){
                    Toast.makeText(MainTestActivity.this,"获取秘钥成功",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainTestActivity.this,"获取秘钥失败",Toast.LENGTH_SHORT).show();
                }*/
            }else if(BluetoothLeService.ACTION_GETPOWER_VALUE.equals(action)){
                String powervalue=intent.getStringExtra("power_value");
                Utils.Log("电量为："+powervalue);
                Toast.makeText(MainTestActivity.this,"当前电量为："+powervalue,Toast.LENGTH_SHORT).show();
            }else if(BluetoothLeService.ACTION_LOCK_STATUS.equals(action)){
                String lockstatus=intent.getStringExtra("lock_status");
                Toast.makeText(MainTestActivity.this,"当前锁状态为："+lockstatus,Toast.LENGTH_SHORT).show();
            }else if(BluetoothLeService.ACTION_MODIFY_PASSWORD_STATUS.equals(action)){
                String modifypasswordstatus=intent.getStringExtra("modifypassword_status");
                if("true".equals(modifypasswordstatus)){
                    Toast.makeText(MainTestActivity.this,"修改密码成功",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainTestActivity.this,"修改密码失败",Toast.LENGTH_SHORT).show();
                }
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);
        mLockManagers=new LockManagers();
        mLogin=(Button) this.findViewById(R.id.login);
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password1=password.getText().toString();
                if(password1.length()!=6){
                    Toast.makeText(MainTestActivity.this,"请输入正确位数",Toast.LENGTH_SHORT).show();
                    mLockManagers.onLockLogin("000000");
                    return;
                }
                mLockManagers.onLockLogin(password1);
            }
        });
        password=(EditText)this.findViewById(R.id.password);
        getToken=(Button)this.findViewById(R.id.token);
        getToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLockManagers.getTokenCmd();
            }
        });
        barttery=(Button)this.findViewById(R.id.barttery);
        barttery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLockManagers.onQueryLockBat();
            }
        });
        getlockstatues=(Button)this.findViewById(R.id.getlockstatues);
        getlockstatues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mLockManagers.onGetLockStatus();
            }
        });
        unlockaction=(Button)this.findViewById(R.id.unlockaction);
        unlockaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mLockManagers.onSetLockUnlock();
            }
        });
        lockaction=(Button)this.findViewById(R.id.lockaction);
        lockaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mLockManagers.onLockLocked();
            }
        });
        devicesname=(EditText) this.findViewById(R.id.devicesname);
        modifymane=(Button)this.findViewById(R.id.modifymane);
        modifymane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(devicesname.length()>8){
                    Toast.makeText(MainTestActivity.this,"名称有误",Toast.LENGTH_SHORT).show();
                    return;
                }
                mLockManagers.onSetLockName(devicesname.getText().toString());
            }
        });
        editpassword=(EditText) this.findViewById(R.id.editpassword);
        modifypassword=(Button)this.findViewById(R.id.modifypassword);
        modifypassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editpassword.length()>6){
                    Toast.makeText(MainTestActivity.this,"密码位数有误",Toast.LENGTH_SHORT).show();
                    return;
                }
                mLockManagers.onSetLockPassword(editpassword.getText().toString());
            }
        });
        search=(Button) this.findViewById(R.id.search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainTestActivity.this, DeviceScanActivity.class);
                MainTestActivity.this.startActivityForResult(i, DeviceScanActivity.REQUEST_SELECT_BT);
            }
        });
        devicesinfo=(TextView) this.findViewById(R.id.devicesinfo1);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DeviceScanActivity.REQUEST_SELECT_BT && resultCode == Activity.RESULT_OK) {
            mDeviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            //one of this can be used
            boolean result=connectToBle(mDeviceName,mDeviceAddress);
            Utils.Log("onActivityResult reConnect request result=" + result);

        } else if (requestCode == DeviceScanActivity.REQUEST_SELECT_BT && resultCode == Activity.RESULT_CANCELED) {
            //Toast.makeText(this, R.string.no_searchdevice, Toast.LENGTH_SHORT).show();
            //finish();
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
    public boolean connectToBle(String devicename,String deviceaddress){
        Utils.Log("devicename="+devicename);
        Utils.Log("deviceaddress="+deviceaddress);
        if (TextUtils.isEmpty(devicename) && TextUtils.isEmpty(deviceaddress)) {
            Toast.makeText(this, R.string.no_searchdevice, Toast.LENGTH_SHORT).show();
            return false;
        }
        //TODO it must be save  every time open activity try to connect bt auto.after it can not connect  it must rescan the bt

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        iServiceBind = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mDeviceAddress=deviceaddress;
        mDeviceName=devicename;
        Utils.Log("mDeviceAddress="+mDeviceAddress);
        boolean result = reConnect();
        return result;

    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_UNLOCK_VALUE);
        intentFilter.addAction(BluetoothLeService.ACTION_LOCK_VALUE);
        intentFilter.addAction(BluetoothLeService.ACTION_LOCK_STATUS);
        intentFilter.addAction(ACTION_GETLOGIN_STATUS);
        intentFilter.addAction(BluetoothLeService.ACTION_GETTOKEN_STATUS);
        intentFilter.addAction(BluetoothLeService.ACTION_GETLOGIN_STATUS);
        intentFilter.addAction(BluetoothLeService.ACTION_GETPOWER_VALUE);
        intentFilter.addAction(BluetoothLeService.ACTION_MODIFY_PASSWORD_STATUS);
        return intentFilter;
    }
    public boolean reConnect() {
        boolean result = false;
        Utils.Log("mBluetoothLeService=" + mBluetoothLeService);
        if (mBluetoothLeService != null) {
            result = mBluetoothLeService.connect(mDeviceAddress);
            Utils.Log("reConnect result=" + result);
            if (result == true && (progressDialog == null || !progressDialog.isShowing())) {
                progressDialog = ProgressDialog.show(MainTestActivity.this, null, getString(R.string.waittoconnect));
                // Stops sending after a pre-defined period.
                Message msg = new Message();
                msg.what = MSG_STOP_WAIT_BT;
                mHandler.sendMessageDelayed(msg, WAIT_PERIOD);
                Utils.Log(" mHandler.sendMessageDelayed=" + mHandler.toString());
            }
        }
        return result;
    }
    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        reConnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (iServiceBind) {
                unbindService(mServiceConnection);
            }
        } catch (Exception e) {
            // if there is no bind this service  close this activity it will show error:service not registered.use iServiceBind to avoid this error
        }
        mBluetoothLeService = null;
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        if (connectFailAlertDialog != null) {
            connectFailAlertDialog.dismiss();
        }
        connectFailAlertDialog = null;
    }
    public LockManagers getLockManagers(){
        return mLockManagers;
    }
}
