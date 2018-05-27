package com.oulu.lock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.oulu.lock.activities.DeviceScanActivity;
import com.oulu.lock.activities.PermissionActivity;
import com.oulu.lock.dialogs.CustomDialog;
import com.oulu.lock.managers.LockManagers;
import com.oulu.lock.utils.PermissionHelper;
import com.oulu.lock.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.oulu.lock.BluetoothLeService.ACTION_GETLOGIN_STATUS;

public class MainActivity extends AppCompatActivity {

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
    /*private Button mLogin;
    private Button getToken;
    private Button barttery;
    private Button getlockstatues;
    private Button unlockaction,lockaction;
    private Button modifymane,modifypassword;
    private EditText password;
    private EditText devicesname,editpassword;*/
    RadioGroup myTabRg;
    FragmentLockManager flm;
    FragmentLockList fl;
    FragmentPersonal fp;
    private static final String TAG_LockManager = "TAG_LockManager";
    private static final String TAG_LockList = "TAG_LockList";
    private static final String TAG_Personal = "TAG_Personal";
    private List<Fragment> mTab = new ArrayList<Fragment>();

    private int[] mRadioButton = { R.id.rbLockList, R.id.rbLockManger, R.id.rbPersonal, };
    private Fragment[] mFragmentArray = { fl, flm, fp, };
    private String[] mFragmentTag = { TAG_LockList, TAG_LockManager, TAG_Personal, };
    final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private PermissionHelper mHelper;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i("Lock","onServiceConnected111111111111111");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Utils.Log("Unable to initialize Bluetooth");
                finish();
            }
            mLockManagers.setBluetoothLeService(mBluetoothLeService);
            boolean result = reConnect();
            Utils.Log("onServiceConnected reConnect result=" + result);
            if(mDeviceAddress!=null && !"".equals(mDeviceAddress) && result) {
                getFragmentManager().beginTransaction().show(flm).commit();
                if(fl!=null){
                    fl.setScan(true);
                }
                ((RadioButton)myTabRg.findViewById(R.id.rbLockManger)).setChecked(true);
                showLoginDialog();
            }else{
                Toast.makeText(MainActivity.this,"mDeviceAddress id null",Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            mLockManagers.setBluetoothLeService(null);
            mDeviceAddress="";
        }
    };
    public void showLoginDialog(){
        final CustomDialog.Builder builder=new CustomDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.input_password);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String password=builder.getMesssage();
                if(password==null || "".equals(password)){
                    Toast.makeText(MainActivity.this,getResources().getString(R.string.passwordisnull),Toast.LENGTH_LONG).show();
                    return;
                }
                if(password.length()!=6){
                    Toast.makeText(MainActivity.this,"密码位数为6位",Toast.LENGTH_LONG).show();
                    return;
                }
                mLockManagers.onLockLogin(password);
                dialogInterface.dismiss();
            }
        });
        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

    }
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STOP_WAIT_BT:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    if (connectFailAlertDialog == null) {
                        connectFailAlertDialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.connectfailed)
//							.setCancelable(false)
                                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        boolean result = MainActivity.this.reConnect();
                                        if (!result) {
                                            Toast.makeText(MainActivity.this, R.string.donttoconnect, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MainActivity.this, R.string.light_disconnected, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this,"登入成功",Toast.LENGTH_SHORT).show();
                        //mLockManagers.getTokenCmd();
                    }
                }else{
                    Toast.makeText(MainActivity.this,"登入失败",Toast.LENGTH_SHORT).show();
                }
            }else if(BluetoothLeService.ACTION_UNLOCK_VALUE.equals(action)) {
                String unlockflag = intent.getStringExtra("unlock_value");
                Utils.Log("unlockflag11="+unlockflag);
                if("true".equals(unlockflag)){
                    Toast.makeText(MainActivity.this,"解锁成功",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this,"解锁失败",Toast.LENGTH_SHORT).show();
                }
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }*/
        setContentView(R.layout.activity_main);
        mHelper=new PermissionHelper(this);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        mLockManagers=new LockManagers();

        createFragment();
        initView();
        getFragmentManager().beginTransaction().show(fl).commit();
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
        intentFilter.addAction(ACTION_GETLOGIN_STATUS);
        return intentFilter;
    }
    public boolean reConnect() {
        boolean result = false;
        Utils.Log("mBluetoothLeService=" + mBluetoothLeService);
        if (mBluetoothLeService != null) {
            result = mBluetoothLeService.connect(mDeviceAddress);
            Utils.Log("reConnect result=" + result);
            if (result == true && (progressDialog == null || !progressDialog.isShowing())) {
                progressDialog = ProgressDialog.show(MainActivity.this, null, getString(R.string.waittoconnect));
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
        boolean permission=mHelper.lacksPermissions(PERMISSIONS);
        if(permission){
            int sdk=android.os.Build.VERSION.SDK_INT;
            if (sdk>=23){
                Intent intent=new Intent(this,PermissionActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle bundle=new Bundle();
                bundle.putStringArray("permission",PERMISSIONS);
                PermissionActivity.startActivityForResult(this,0,PERMISSIONS);
                finish();
            }
        }
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
    private void createFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = null;

        fragment = fm.findFragmentByTag(TAG_LockList);
        if (fragment != null) {// 如果有，则使用，处理翻转之后状态未保存问题
            fl = (FragmentLockList) fragment;
        } else {// 如果为空，才去新建，不新建切换的时候就可以保存状态了。
            fl = FragmentLockList.newInstance(null);
            ft.add(R.id.fragmentfield, fl, TAG_LockList);
        }
        ft.hide(fl);
        mTab.add(fl);

        fragment = fm.findFragmentByTag(TAG_LockManager);
        if (fragment != null) {// 如果有，则使用，处理翻转之后状态未保存问题
            flm = (FragmentLockManager) fragment;
        } else {// 如果为空，才去新建，不新建切换的时候就可以保存状态了。
            flm = FragmentLockManager.newInstance(null);
            ft.add(R.id.fragmentfield, flm, TAG_LockManager);
        }
        ft.hide(flm);
        mTab.add(flm);

        fragment = fm.findFragmentByTag(TAG_Personal);
        if (fragment != null) {// 如果有，则使用，处理翻转之后状态未保存问题
            fp = (FragmentPersonal) fragment;
        } else {// 如果为空，才去新建，不新建切换的时候就可以保存状态了。
            fp = FragmentPersonal.newInstance(null);
            ft.add(R.id.fragmentfield, fp, TAG_Personal);
        }
        ft.hide(fp);
        mTab.add(fp);

        // 处理其他fragment
        fragment = fm.findFragmentById(R.id.fragmentfield);
        if (mTab != null && fragment != null && !mTab.contains(fragment)
                && fragment.isAdded()) {
            ft.remove(fragment);
            int count = fm.getBackStackEntryCount();
            Utils.Log("fragment backstack count:" + count);
            for (int i = 0; i < count; i++) {
                fm.popBackStack();// 切换也签，处理掉已有的backstack
            }
        }

        ft.commit();
        SharedPreferences sharedPreferences=Utils.getSharedPpreference(MainActivity.this);
        Utils.Log("sharedPreferences");
        /*boolean isfirst=sharedPreferences.getBoolean("isfirst",false);
        if(!isfirst){
            LockBeanDao lb=MyApplication.getInstance().getDaoSession().getLockBeanDao();
            for(int i=0;i<5;i++){
                LockBean l=new LockBean();
                l.setLockName("iiii="+i);
                l.setCustomName("custom="+i);
                l.setAddress("23:23:43:12:"+i);
                l.setIsable(i%2==0?1:0);
                lb.insertOrReplace(l);
            }
            SharedPreferences.Editor e=sharedPreferences.edit();
            e.putBoolean("isfirst",true);
        }*/


    }
    private void initView() {

        myTabRg = (RadioGroup) findViewById(R.id.tab_menu);
        myTabRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                createFragment();
                for (int i = 0; i < mRadioButton.length; i++) {
                    if (mRadioButton[i] == checkedId) {
                        FragmentManager fm = getFragmentManager();
                        Utils.Log("xxxxxxxxxxxxxxxxxx i:" + i);
                        FragmentTransaction ft = fm.beginTransaction();
                        // ft.setCustomAnimations(R.animator.slide_left_in,
                        // R.animator.slide_left_out);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        // ft.replace(R.id.fragmentfield,
                        // mTab.get(i),mFragmentTag[i]);
                        ft.show(mTab.get(i));
                        if(i==0){
                            if(fl!=null){
                                Log.i("liao","fffffffffff");
                                fl.setScan(true);
                            }
                        }else{
                            if(fl!=null){
                                Log.i("liao","wwwwww");
                                fl.setScan(false);
                            }
                        }
                        //if (checkedId == R.id.rbData && fData != null) {
                        //    fData.updateUI(false);
                        //}
                        // ft.addToBackStack(null);
                        ft.commit();
                        break;
                    }
                }
            }
        });
    }
    public LockManagers getLockManagers(){
        return mLockManagers;
    }
    public String getAddress(){
        return mDeviceAddress;
    }
}
