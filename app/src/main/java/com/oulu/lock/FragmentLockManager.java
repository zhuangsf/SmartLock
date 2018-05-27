package com.oulu.lock;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.lock.greendao.gen.LockBeanDao;
import com.oulu.lock.activities.DeviceScanActivity;
import com.oulu.lock.beans.LockBean;
import com.oulu.lock.bleservices.BluetoothUtils;
import com.oulu.lock.dialogs.CustomDialog;
import com.oulu.lock.managers.LockManagers;
import com.oulu.lock.utils.AESUtil;
import com.oulu.lock.utils.Utils;

import java.util.ArrayList;

/**
 * Created by liao on 2017/12/2.
 */
public class FragmentLockManager extends Fragment {

    private LockBeanDao lockBeanDao;
    private LockManagers mLockManagers;
    private TextView deviceName;
    private LockBean mLockbean;
    private String deviceNameString;
    private ImageView lock;
    private TextView power_value;
    private LinearLayout lock_setting;
    private boolean tokenstatus = false;
    private boolean loginstatus = false;
    private String lockbat = "";
    private WindowManager mWindowManager;
    private  ProgressDialog progressDialog=null;
    private BluetoothUtils mBluetoothUtils;
    private Handler mhandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    String name = (String)msg.obj;
                    Log.i("liao","mod="+name);
                    deviceName.setText(name);
            }
        }
    };
    private Runnable mRunable = new Runnable() {
        @Override
        public void run() {
            mBluetoothUtils=new BluetoothUtils();
            mBluetoothUtils.startLeScan(5000L);
            mBluetoothUtils.setBlueInfoCallBcak(new BluetoothUtils.BlueInfoCallBcak() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.i("liao","111onLeScan=="+ AESUtil.BytetohexString(scanRecord));
                    Log.i("liao","111name=="+ device.getName());
                    Log.i("liao","111address=="+ device.getAddress());
                    String address = device.getAddress();
                    if(address.equals(((MainActivity) getActivity()).getAddress())){
                        deviceNameString = device.getName();
                        Message m =new Message();
                        m.what=1;
                        m.obj = deviceNameString;
                        mhandler.sendMessage(m);
                        Log.i("liao","deviceNameString=="+deviceNameString);
                        if (mLockManagers != null && mLockManagers.hasBluetoothLeService()) {
                            tokenstatus = false;
                            loginstatus = false;
                            //mLockManagers.connectBle(((MainActivity) getActivity()).getAddress());
                            ((MainActivity)getActivity()).reConnect();
                            Log.i("liao","deviceNameString1111");
                            mBluetoothUtils.stopLeScan();
                            if(progressDialog !=null && progressDialog.isShowing()){
                                progressDialog.dismiss();
                            }
                        }
                    }

                }

                @Override
                public void onstartLeScan(boolean flag) {
                    Log.i("liao","onstartLeScan");
                }

                @Override
                public void onstopLeScan(boolean flag) {

                }

                @Override
                public void onScanTimeout() {
                    if(progressDialog !=null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getActivity(),"修改失败",Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    public static FragmentLockManager newInstance(Bundle b) {
        FragmentLockManager fd = new FragmentLockManager();
        fd.setArguments(b);
        return fd;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_lockmanager, null);
        mWindowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        deviceName = (TextView) view.findViewById(R.id.lockName);
        lock = (ImageView) view.findViewById(R.id.lock);
        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLockManagers != null && mLockManagers.hasBluetoothLeService()) {
                    Utils.Log("mLockManagers========");
                    if (tokenstatus) {
                        Utils.Log("mLockManagers========111");
                        mLockManagers.onSetLockUnlock();
                    } else {
                        if (loginstatus) {
                            Utils.Log("mLockManagers========222");
                            mLockManagers.getTokenCmd();
                        } else {
                            Utils.Log("connect=" + (((MainActivity) getActivity()).mConnected));
                            if (((MainActivity) getActivity()).mConnected == true) {
                                ((MainActivity) getActivity()).showLoginDialog();
                            } else {
                                String address = ((MainActivity) getActivity()).getAddress();
                                if (address != null && !"".equals(address)) {
                                    mLockManagers.connectBle(((MainActivity) getActivity()).getAddress());
                                } else {
                                    Toast.makeText(getActivity(), R.string.no_connect_device, Toast.LENGTH_SHORT).show();
                                }

                            }
                        }

                    }
                    //mLockManagers.onSetLockUnlock();
                } else {
                    Toast.makeText(getActivity(), R.string.no_connect_device, Toast.LENGTH_SHORT).show();
                    Utils.Log("mLockManagers========33333");
                }

            }
        });
        lockBeanDao = MyApplication.getInstance().getDaoSession().getLockBeanDao();
        mLockManagers = ((MainActivity) getActivity()).getLockManagers();
        power_value = (TextView) view.findViewById(R.id.power_value);
        lock_setting = (LinearLayout) view.findViewById(R.id.lock_setting);
        lock_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View popupView = getActivity().getLayoutInflater().inflate(R.layout.lock_setting_popwindow, null);

                // TODO: 2016/5/17 创建PopupWindow对象，指定宽度和高度
                int width = mWindowManager.getDefaultDisplay().getWidth() * 2 / 5;
                int height = mWindowManager.getDefaultDisplay().getHeight() / 6;
                final PopupWindow window = new PopupWindow(popupView, width, height);
                window.setWidth(mWindowManager.getDefaultDisplay().getWidth() / 3);
                // TODO: 2016/5/17 设置动画
                window.setAnimationStyle(R.style.popup_window_anim);
                // TODO: 2016/5/17 设置背景颜色
                // window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F8F8F8")));
                // TODO: 2016/5/17 设置可以获取焦点
                window.setFocusable(true);
                // TODO: 2016/5/17 设置可以触摸弹出框以外的区域
                window.setOutsideTouchable(true);
                // TODO：更新popupwindow的状态
                window.update();
                // TODO: 2016/5/17 以下拉的方式显示，并且可以设置显示的位置
                window.showAsDropDown(lock_setting, 0, 5);
                TextView change_lock_name = (TextView) popupView.findViewById(R.id.change_lock_name);
                TextView change_lock_password = (TextView) popupView.findViewById(R.id.change_lock_password);
                change_lock_name.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        window.dismiss();
                        if (mLockManagers != null && mLockManagers.hasBluetoothLeService()) {
                            if (tokenstatus) {
                                final CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
                                builder.setTitle(R.string.changelockname);
                                builder.setMessage(deviceNameString);
                                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String newName = builder.getMesssage();
                                        if (newName.length() != 8) {
                                            Toast.makeText(getActivity(), "智能锁名称为8个字符", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        mLockManagers.onSetLockName(newName);
                                        dialogInterface.dismiss();
                                        createProgressDialog();
                                        mhandler.post(mRunable);
                                    }
                                });
                                builder.create().show();
                            }
                        }
                    }
                });
                change_lock_password.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        window.dismiss();
                        if (mLockManagers != null && mLockManagers.hasBluetoothLeService()) {
                            if (tokenstatus) {
                                final CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
                                builder.setTitle(R.string.changelockpassword);
                                builder.setHintMessage(getActivity().getResources().getString(R.string.input_newpassword));
                                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String newpassword = builder.getMesssage();
                                        if (newpassword.length() != 6) {
                                            Toast.makeText(getActivity(), "智能锁密码为6个字符", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        mLockManagers.onSetLockPassword(newpassword);
                                        dialogInterface.dismiss();
                                    }
                                });
                                builder.create().show();
                            }
                        }
                    }
                });
            }
        });
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("liao", "onResume1111");
        getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (deviceName != null) {
            if (deviceNameString == null || "".equals(deviceNameString)) {
                deviceNameString = getString(R.string.mylock);
            }
            deviceName.setText(deviceNameString);
        }
        if (power_value != null) {
            power_value.setText(lockbat);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("liao", "onPause");
        getActivity().unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createProgressDialog(){
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());//1.创建一个ProgressDialog的实例
            progressDialog.setTitle(null);//2.设置标题
            progressDialog.setMessage("正在修改设备名称");//3.设置显示内容
            progressDialog.setCancelable(false);//4.设置可否用back键关闭对话框
        }
        progressDialog.show();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mLockManagers = ((MainActivity) getActivity()).getLockManagers();
                String[] result = intent.getStringArrayExtra("addressanddeivcename");
                Log.i("liao", "ACTION_GATT_CONNECTEDresult=" + result.toString());
                if (result != null && result.length == 2) {
                    Log.i("liao", "ACTION_GATT_CONNECTED11111");
                    String address = result[0];
                    String lockname = result[1];
                    Utils.Log("address=" + address);
                    Utils.Log("lockname=" + lockname);
                    if (address != null && !"".equals(address)) {
                        ArrayList<LockBean> devicelists = (ArrayList<LockBean>) lockBeanDao.queryRaw("where  ADDRESS=?", address);
                        String device = "";
                        Utils.Log("size=" + (devicelists.size() > 0));
                        Utils.Log("devicelists=" + (devicelists != null));
                        if (devicelists != null && devicelists.size() > 0) {
                            mLockbean = devicelists.get(0);
                            Utils.Log("lockname=" + lockname);
                            Utils.Log("lockname11=" + mLockbean.getLockName());
                            if (lockname != null && !"".equals(lockname)) {
                                if (!lockname.equals(mLockbean.getLockName())) {
                                    mLockbean.setLockName(lockname);
                                    lockBeanDao.insertOrReplace(mLockbean);
                                }
                            }
                            deviceNameString = mLockbean.getCustomName();

                            if (device == null || "".equals(device)) {
                                deviceNameString = mLockbean.getLockName();
                            }
                        } else {
                            mLockbean = new LockBean();
                            mLockbean.setAddress(address);
                            mLockbean.setLockName(lockname);
                            lockBeanDao.insertOrReplace(mLockbean);
                            deviceNameString = mLockbean.getCustomName();
                            if (deviceNameString == null || "".equals(deviceNameString)) {
                                deviceNameString = mLockbean.getLockName();
                            }
                        }
                    }
                    Utils.Log("deviceNameString=" + deviceNameString);
                    if (deviceName != null) {
                        deviceName.setText(deviceNameString);
                    }
                }

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                loginstatus = false;
                if (deviceName != null) {
                    deviceName.setText(R.string.mylock);
                }
            } else if (BluetoothLeService.ACTION_GETTOKEN_STATUS.equals(action)) {
                String gettokenflag = intent.getStringExtra("gettokenflag");
                Log.i("liao", "gettokenflag11=" + gettokenflag);
                if (!"01".equals(gettokenflag)) {
                    Log.i("liao", "gettokenflag2222=");
                    Toast.makeText(getActivity(), "获取秘钥成功", Toast.LENGTH_SHORT).show();
                    tokenstatus = true;
                    mLockManagers.onQueryLockBat();
                } else {
                    //Toast.makeText(getActivity(),"获取秘钥失败",Toast.LENGTH_SHORT).show();
                    tokenstatus = false;
                }
            } else if (BluetoothLeService.ACTION_GETLOGIN_STATUS.equals(action)) {
                String loginstatusflag = intent.getStringExtra("login_status");
                if ("true".equals(loginstatusflag)) {
                    loginstatus = true;
                    Log.i("liao", "11111");
                    mLockManagers.getTokenCmd();
                } else {
                    loginstatus = false;
                }
            } else if (BluetoothLeService.ACTION_GETPOWER_VALUE.equals(action)) {
                String powervalue = intent.getStringExtra("power_value");
                Log.i("liao", "powervalue=" + powervalue);
                Log.i("liao", "powervalue1111=" + Integer.parseInt(powervalue, 16) + "");
                Utils.Log("电量为：" + Integer.parseInt(powervalue, 16) + "");
                lockbat = Integer.parseInt(powervalue, 16) + "";
                if (power_value != null) {
                    power_value.setText(lockbat);
                }
            } else if (BluetoothLeService.ACTION_MODIFY_PASSWORD_STATUS.equals(action)) {
                String password = intent.getStringExtra("modifypassword_status");
                Log.i("liao", "password=" + password);
                if (password == null || "-1".equals(password)) {
                    Toast.makeText(getActivity(), "修改密码失败", Toast.LENGTH_SHORT).show();
                } else {
                    /*if (mLockManagers != null && mLockManagers.hasBluetoothLeService()) {
                        loginstatus = false;
                        tokenstatus =false;
                        mLockManagers.onLockLogin(password);
                    }*/
                    loginstatus = false;
                    tokenstatus =false;
                    Toast.makeText(getActivity(), "修改密码成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GETTOKEN_STATUS);
        intentFilter.addAction(BluetoothLeService.ACTION_GETLOGIN_STATUS);
        intentFilter.addAction(BluetoothLeService.ACTION_GETPOWER_VALUE);
        intentFilter.addAction(BluetoothLeService.ACTION_MODIFY_PASSWORD_STATUS);
        return intentFilter;
    }

}
