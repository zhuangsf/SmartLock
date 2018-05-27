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

package com.oulu.lock;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.oulu.lock.bleservices.SampleGattAttributes;
import com.oulu.lock.listener.OnLockStatusListener;
import com.oulu.lock.managers.LockManagers;
import com.oulu.lock.utils.AESUtil;
import com.oulu.lock.utils.Utils;

import java.util.List;
import java.util.UUID;

import static com.oulu.lock.utils.AESUtil.BytetohexString;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private LockManagers lockManagers;
    private OnLockStatusListener mOnLockStatusListener = null;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.oulu.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.oulu.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.oulu.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.oulu.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.oulu.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_DATA_NEED = "com.example.bluetooth.le.EXTRA_DATA_NEED";
    public final static String ACTION_GETLOGIN_STATUS = "com.oulu.bluetooth.le.ACTION_GETLOGIN_STATUS";
    public final static String ACTION_GETTOKEN_STATUS = "com.oulu.bluetooth.le.ACTION_GETTOKEN_STATUS";
    public final static String ACTION_GETPOWER_VALUE = "com.oulu.bluetooth.le.ACTION_GETPOWER_VALUE";
    public final static String ACTION_UNLOCK_VALUE = "com.oulu.bluetooth.le.ACTION_UNLOCK_VALUE";
    public final static String ACTION_LOCK_VALUE = "com.oulu.bluetooth.le.ACTION_LOCK_VALUE";
    public final static String ACTION_LOCK_STATUS = "com.oulu.bluetooth.le.ACTION_LOCK_STATUS";
    public final static String ACTION_MODIFY_PASSWORD_STATUS = "com.oulu.bluetooth.le.ACTION_MODIFY_PASSWORD_STATUS";
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    private String token = "";
    private static String password = "";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction, "addressanddeivcename", new String[]{gatt.getDevice().getAddress(), gatt.getDevice().getName()});
                Utils.Log("Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Utils.Log("Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Utils.Log("Disconnected from GATT server.");
                broadcastUpdate(intentAction);
                token = "";
                broadcastUpdate(ACTION_GETTOKEN_STATUS, "gettokenflag", "01");
                broadcastUpdate(ACTION_GETLOGIN_STATUS, "login_status", "false");
                //add
                close();
            }
        }

        //mBluetoothGatt.getServices()
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                List<BluetoothGattService> list = gatt.getServices();
                /*for (BluetoothGattService b : list) {
                    Utils.Log("b=" + b.getUuid());
                    List<BluetoothGattCharacteristic> bgl = b.getCharacteristics();
                    for (BluetoothGattCharacteristic bg : bgl) {
                        Utils.Log("bg=" + bg.getUuid());
                    }
                }*/
            } else {
                Utils.Log("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Utils.Log("liao onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Utils.Log("onCharacteristicChanged123321");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            //Utils.Log("xxxxxxxxxxxxxxxxxx liao   onCharacteristicChanged:" + characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//        	 Utils.Log("xxxxxxxxxxxxxxxxxx onCharacteristicWrite:"+characteristic+",status:"+status);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, String extra, String[] value) {
        final Intent intent = new Intent(action);
        intent.putExtra(extra, value);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, String extra, String value) {
        final Intent intent = new Intent(action);
        intent.putExtra(extra, value);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            Utils.Log("12123");
            if (ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = characteristic.getValue();
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                Utils.Log("stringBuilder=" + stringBuilder);
                byte[] resultdata = new byte[data.length];
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                try {
                    Utils.Log("xxxxxxxxxxxxxxxxxx broadcastUpdate just need:" + stringBuilder.toString() + "   ==" + BytetohexString(AESUtil.decrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), data)));
                    resultdata = AESUtil.decrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String cmd1 = BytetohexString(new byte[]{resultdata[0]});
                int tokenlenght = token.length() / 2;
                Utils.Log("cmd1=" + cmd1);
                if ("26".equals(cmd1)) {
                    String cmd2 = BytetohexString(new byte[]{resultdata[1]});
                    Utils.Log("cmd2=" + cmd2);
                    if ("04".equals(cmd2)) {
                        String gettokenflag = getToken(resultdata);
                        Utils.Log("getToken=" + gettokenflag);
                        if (mOnLockStatusListener != null) {
                            Utils.Log("getToken123454656");
                            //mOnLockStatusListener.getToken(gettokenflag);
                        }
                        broadcastUpdate(ACTION_GETTOKEN_STATUS, "gettokenflag", gettokenflag + "");
                    } else if ("02".equals(cmd2)) {
                        if ("01".equals(BytetohexString(new byte[]{resultdata[2]}))) {
                            boolean loginstatus = getLoginStatus(resultdata);
                            Utils.Log("loginstatus=" + loginstatus);
                            if (mOnLockStatusListener != null) {
                                //mOnLockStatusListener.getLogin(loginstatus);
                            }
                            broadcastUpdate(ACTION_GETLOGIN_STATUS, "login_status", loginstatus + "");
                        }
                    }
                } else {
                    byte[] tokenbyte = new byte[tokenlenght];
                    for (int i = 0; i < tokenlenght; i++) {
                        tokenbyte[i] = resultdata[i];
                    }
                    if (token != "" && token.equals(BytetohexString(tokenbyte))) {
                        String cmd2 = BytetohexString(new byte[]{resultdata[tokenlenght], resultdata[tokenlenght + 1]});
                        Utils.Log("cmd2=" + cmd2);
                        if ("0601".equals(cmd2)) {
                            getChangeLockNameStatus(resultdata);
                        } else if ("0801".equals(cmd2)) {
                            String powervalue = getPower(resultdata);
                            broadcastUpdate(ACTION_GETPOWER_VALUE, "power_value", powervalue);
                            if (mOnLockStatusListener != null) {
                                mOnLockStatusListener.getBar(powervalue);
                            }

                        } else if ("0A01".equals(cmd2)) {
                            boolean unlockflag = getUnLockActionstatus(resultdata);
                            if (mOnLockStatusListener != null) {
                                mOnLockStatusListener.getUnlock(unlockflag);
                            }
                            Utils.Log("unlockflag=" + unlockflag);
                            broadcastUpdate(ACTION_UNLOCK_VALUE, "unlock_value", unlockflag + "");
                        } else if ("0C01".equals(cmd2)) {
                            boolean lockflag = getLockActionstatus(resultdata);
                            if (mOnLockStatusListener != null) {
                                mOnLockStatusListener.getLock(lockflag);
                            }
                            Utils.Log("lockflag=" + lockflag);
                            broadcastUpdate(ACTION_LOCK_VALUE, "lock_value", lockflag + "");
                        } else if ("0E01".equals(cmd2)) {
                            boolean lockstatus = getLockbacktatus(resultdata);
                            if (mOnLockStatusListener != null) {
                                mOnLockStatusListener.getLockstatus(lockstatus);
                            }
                            broadcastUpdate(ACTION_LOCK_STATUS, "lock_status", lockstatus + "");
                        } else if ("1107".equals(cmd2)) {
                            String modifyPassword = modifyPassword(resultdata);
                           /* if (mOnLockStatusListener != null) {
                                mOnLockStatusListener.getPassword(modifyPassword);
                            }*/
                            broadcastUpdate(ACTION_MODIFY_PASSWORD_STATUS, "modifypassword_status", modifyPassword);
                        } else if ("1101".equals(cmd2)) {
                            if (mOnLockStatusListener != null) {
                                mOnLockStatusListener.getPassword(false);
                            }
                            broadcastUpdate(ACTION_MODIFY_PASSWORD_STATUS, "modifypassword_status", "-1");
                        }
                    }

                }

            } else {
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                    intent.putExtra(EXTRA_DATA_NEED, stringBuilder.toString());
                    Utils.Log("xxxxxxxxxxxxxxxxxx broadcastUpdate just need:" + stringBuilder.toString());
                }
            }
        }
        sendBroadcast(intent);
    }

    private String modifyPassword(byte[] data) {
        String cmd3 = BytetohexString(new byte[]{data[3]});
        Utils.Log("cm3=" + cmd3);
        if (cmd3 != null && "00".equals(cmd3)) {
            byte[] btresult = new byte[]{data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9]};
            Utils.Log("11=" + AESUtil.BytetohexString(new byte[]{AESUtil.getXOR(btresult)}));
            Utils.Log("22=" + AESUtil.BytetohexString(new byte[]{data[10]}));
            if (data[10] == AESUtil.getXOR(btresult)) {
                Utils.Log("passs success");
                return  AESUtil.BytetohexString(new byte[]{data[4], data[5], data[6], data[7], data[8], data[9]});
            }
        }

        return "-1";
    }

    private boolean getLockbacktatus(byte[] data) {
        byte[] btresult = new byte[]{data[0], data[1], data[2], data[3]};
        if (data[4] == AESUtil.getXOR(btresult)) {
            if ("00".equals(BytetohexString(new byte[]{data[3]}))) {
                //Toast.makeText(getApplicationContext(),"开锁状态",Toast.LENGTH_SHORT).show();
                Utils.Log("开锁状态");
                return true;
            }
        }
        //Toast.makeText(getApplicationContext(),"关锁状态",Toast.LENGTH_SHORT).show();
        Utils.Log("关锁状态");
        return false;
    }

    private boolean getUnLockActionstatus(byte[] data) {
        byte[] btresult = new byte[]{data[0], data[1], data[2], data[3]};
        if (data[4] == AESUtil.getXOR(btresult)) {
            if ("00".equals(BytetohexString(new byte[]{data[3]}))) {
                //Toast.makeText(this,"开锁成功",Toast.LENGTH_SHORT).show();
                Utils.Log("开锁成功");
                return true;
            }
        }
        // Toast.makeText(this,"开锁失败",Toast.LENGTH_SHORT).show();
        Utils.Log("开锁失败");
        return false;
    }

    private boolean getLoginStatus(byte[] data) {
        byte[] btresult = new byte[]{data[0], data[1], data[2], data[3]};
        if (data[4] == AESUtil.getXOR(btresult)) {
            if ("00".equals(BytetohexString(new byte[]{data[3]}))) {
                //Toast.makeText(this,"登录成功",Toast.LENGTH_SHORT).show();
                Utils.Log("登录成功");
                return true;
            }
        }
        //Toast.makeText(this,"登录失败",Toast.LENGTH_SHORT).show();
        password = "";
        Utils.Log("登录失败");
        return false;

    }

    private String getToken(byte[] data) {
        Utils.Log("getToken111");

        Utils.Log("ppp=" + password);
        int tokenlenght = Integer.parseInt(BytetohexString(new byte[]{data[2]}));
        byte[] btresult = new byte[3 + tokenlenght];
        for (int i = 0; i < 3 + tokenlenght; i++) {
            btresult[i] = data[i];
        }
        if (data[3 + tokenlenght] == AESUtil.getXOR(btresult)) {
            byte[] tokenbyte = new byte[tokenlenght];
            for (int i = 0; i < tokenlenght; i++) {
                tokenbyte[i] = data[3 + i];
            }

            token = BytetohexString(tokenbyte);
            Utils.Log("getToken=" + token);
            return token;
        }
        Utils.Log("getToken1114444");
        //Toast.makeText(this,"token="+token,Toast.LENGTH_SHORT).show();
        Utils.Log("token=" + token);
        return "-1";

    }

    private String getPower(byte[] data) {
        byte[] btresult = new byte[]{data[0], data[1], data[2], data[3]};
        if (data[4] == AESUtil.getXOR(btresult)) {
            // Toast.makeText(this,"电量为="+AESUtil.BytetohexString(new byte[]{data[3]}),Toast.LENGTH_SHORT).show();
            Utils.Log("电量为=" + BytetohexString(new byte[]{data[3]}));
            return BytetohexString(new byte[]{data[3]});
        }
        return "-1";
    }

    private boolean getChangeLockNameStatus(byte[] data) {

        byte[] btresult = new byte[]{data[0], data[1], data[2], data[3]};
        if (data[4] == AESUtil.getXOR(btresult)) {
            if ("00".equals(BytetohexString(new byte[]{data[3]}))) {
                Utils.Log("修改名称成功");
                return true;
            }
        }
        return false;

    }

    private boolean getLockActionstatus(byte[] data) {
        byte[] btresult = new byte[]{data[0], data[1], data[2], data[3]};
        if (data[4] == AESUtil.getXOR(btresult)) {
            if ("00".equals(BytetohexString(new byte[]{data[3]}))) {
                //Toast.makeText(getApplicationContext(),"关锁成功",Toast.LENGTH_SHORT).show();
                Utils.Log("关锁成功");
                return true;
            }
        }
        //Toast.makeText(getApplicationContext(),"关锁失败",Toast.LENGTH_SHORT).show();
        Utils.Log("关锁失败");
        return false;
    }

    public void onGetLockStatus() {
        String[] slist = new String[password.length()];
        for (int i = 0; i < slist.length; i++) {
            Utils.Log("1222=" + password.charAt(i));
            slist[i] = "0" + password.charAt(i);
        }
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append(token)
                .append("0D")
                .append("06");
        //.append(slist.toString());
        for (int i = 0; i < slist.length; i++) {
            paramString1.append(slist[i]);
        }
        byte rtc = AESUtil.getXOR(AESUtil.hexStringToBytes(paramString1.toString()));
        paramString1.append(BytetohexString(new byte[]{rtc}));
        paramString1.append("686868686868");
        Utils.Log(paramString1.toString());
        try {
            Utils.Log("11111");
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), AESUtil.hexStringToBytes(paramString1.toString())));
            Utils.Log("2222");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onLockLogin(String paramString) {
        Utils.Log("paramString=" + paramString);
        password = paramString;
        Utils.Log("ppa=" + password);
        if (paramString.length() != 6 || paramString == null || "".equals(paramString)) {
            Toast.makeText(this, "mimaweishubudui", Toast.LENGTH_SHORT).show();
            return;
        }
        //String[] clist = paramString.toCharArray();
        String[] slist = new String[paramString.length()];
        for (int i = 0; i < slist.length; i++) {
            Utils.Log("1222=" + paramString.charAt(i));
            slist[i] = "0" + paramString.charAt(i);
        }
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append("26")
                .append("01")
                .append("06");
        //.append(slist.toString());
        for (int i = 0; i < slist.length; i++) {
            paramString1.append(slist[i]);
        }
        Utils.Log("12=" + slist.toString());
        byte rtc = AESUtil.getXOR(AESUtil.hexStringToBytes(paramString1.toString()));
        paramString1.append(BytetohexString(new byte[]{rtc}));
        paramString1.append("686868686868");
        Utils.Log("11=" + BytetohexString(new byte[]{rtc}));
        Utils.Log("paramString1=" + paramString1);
        try {
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), AESUtil.hexStringToBytes(paramString1.toString())));
        } catch (Exception e) {
            Utils.Log("1234546");
            e.printStackTrace();
        }

    }

    public void onLockLogin() {
        if (password == null || "".equals(password)) {
            return;
        }
        Utils.Log("ppa=" + password);
        if (password.length() != 6 || password == null || "".equals(password)) {
            Toast.makeText(this, "mimaweishubudui", Toast.LENGTH_SHORT).show();
            return;
        }
        //String[] clist = paramString.toCharArray();
        String[] slist = new String[password.length()];
        for (int i = 0; i < slist.length; i++) {
            Utils.Log("1222=" + password.charAt(i));
            slist[i] = "0" + password.charAt(i);
        }
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append("26")
                .append("01")
                .append("06");
        //.append(slist.toString());
        for (int i = 0; i < slist.length; i++) {
            paramString1.append(slist[i]);
        }
        Utils.Log("12=" + slist.toString());
        byte rtc = AESUtil.getXOR(AESUtil.hexStringToBytes(paramString1.toString()));
        paramString1.append(BytetohexString(new byte[]{rtc}));
        paramString1.append("686868686868");
        Utils.Log("11=" + BytetohexString(new byte[]{rtc}));
        Utils.Log("paramString1=" + paramString1);
        try {
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), AESUtil.hexStringToBytes(paramString1.toString())));
        } catch (Exception e) {
            Utils.Log("1234546");
            e.printStackTrace();
        }

    }


    public void onQueryLockBat() {
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append(token)
                .append("07")
                .append("00");
        byte rtc = AESUtil.getXOR(AESUtil.hexStringToBytes(paramString1.toString()));
        paramString1.append(BytetohexString(new byte[]{rtc}));
        paramString1.append("686868686868686868686868");
        Utils.Log("bat=" + paramString1);
        try {
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), AESUtil.hexStringToBytes(paramString1.toString())));
        } catch (Exception e) {
            Utils.Log("bat333");
            e.printStackTrace();
        }

    }


    public void onLockLocked() {
        String[] slist = new String[password.length()];
        for (int i = 0; i < slist.length; i++) {
            Utils.Log("1222=" + password.charAt(i));
            slist[i] = "0" + password.charAt(i);
        }
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append(token)
                .append("0B")
                .append("06");
        //.append(slist.toString());
        for (int i = 0; i < slist.length; i++) {
            paramString1.append(slist[i]);
        }
        byte rtc = AESUtil.getXOR(AESUtil.hexStringToBytes(paramString1.toString()));
        paramString1.append(BytetohexString(new byte[]{rtc}));
        paramString1.append("686868686868");
        try {
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), AESUtil.hexStringToBytes(paramString1.toString())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onSetLockName(String paramString) {
        if (paramString.length() <= 0 || paramString.length() > 8) {
            Toast.makeText(this, "lockname lenght is error", Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] pb = paramString.getBytes();
        Utils.Log("pb=" + pb.length);
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append(token)
                .append("05")
                .append("0" + pb.length);
        byte[] cmd = AESUtil.hexStringToBytes(paramString1.toString());
        StringBuffer fillString = new StringBuffer();
        for (int i = 0; i < 16 - 4 - pb.length; i++) {
            fillString.append("68");
        }
        byte[] fill = AESUtil.hexStringToBytes(fillString.toString());
        byte[] cmdpb = new byte[pb.length + cmd.length];
        for (int i = 0; i < cmd.length; i++) {
            cmdpb[i] = cmd[i];
        }
        for (int i = 0; i < pb.length; i++) {
            cmdpb[cmd.length + i] = pb[i];
        }
        byte orbyte = AESUtil.getXOR(cmdpb);
        byte[] pa = new byte[pb.length + cmd.length + fill.length + 1];
        Utils.Log("pa=" + pa.length);
        //System.arraycopy(cmd,0,pa,0,cmd.length);
        // System.arraycopy(pb,0,pa,cmd.length,pb.length);
        // System.arraycopy(fill,0,pa,pb.length+cmd.length,fill.length);
        for (int i = 0; i < cmd.length; i++) {
            pa[i] = cmd[i];
        }
        for (int i = 0; i < pb.length; i++) {
            pa[cmd.length + i] = pb[i];
        }
        pa[cmd.length + pb.length] = orbyte;
        for (int i = 0; i < fill.length; i++) {
            pa[cmd.length + pb.length + 1 + i] = fill[i];
        }

        StringBuffer sbbb = new StringBuffer();
        sbbb.append(BytetohexString(new byte[]{pa[0], pa[1], pa[2]}));
        int len = Integer.parseInt(BytetohexString(new byte[]{pa[2]}));
        byte[] eds = new byte[len];
        for (int i = 0; i < len; i++) {
            eds[i] = pa[i + 3];
        }
        sbbb.append(new String(eds));
        Log.i("liao1", "11===" + sbbb.toString());
        byte[] fff = new byte[16 - len - 3];
        for (int i = 0; i < 16 - len - 3; i++) {
            fff[i] = pa[i + 3 + len];
        }
        sbbb.append(BytetohexString(fff));
        Log.i("liao1", "222===" + sbbb.toString());

        try {
            Utils.Log("1111");
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), pa));
            Utils.Log("2222");
        } catch (Exception e) {
            Utils.Log("333");
            e.printStackTrace();
        }
    }


    public void onSetLockPassword(String paramString) {
        /*char[] clist = paramString.toCharArray();
        String[] slist = new String[clist.length];
        for (int i = 0; i < clist.length; i++) {
            System.out.println(clist[i]);
            slist[i] = "0" + clist[i];
        }*/
        Utils.Log("pa=" + paramString + "   ppp=" + password);
        String tmp = password + paramString;
        StringBuffer hextmp = new StringBuffer();
        byte[] passwordbytes = new byte[12];
        for (int i = 0; i < tmp.length(); i++) {
            //hextmp.append("0"+tmp.charAt(i));
            char value = tmp.charAt(i);
            if (Character.isDigit(value)) {
                Integer it = new Integer(value);
                Utils.Log("1234==" + it.byteValue());
                passwordbytes[i] = AESUtil.hexStringToBytes("0" + value)[0];
                Utils.Log("123411==" + passwordbytes[i]);
            } else {
                String sv = new String(value + "");
                passwordbytes[i] = sv.getBytes()[0];
            }
        }
        Utils.Log("aa=" + tmp);
        Utils.Log("bb=" + hextmp);
        //byte[] passwordbytes=tmp.getBytes();
        //byte[] passwordbytes = AESUtil.hexStringToBytes(hextmp.toString());
        Utils.Log("passw=" + passwordbytes.length);
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append(token)
                .append("10")
                .append("0C");
        // .append(slist.toString());
        //byte orbyte=AESUtil.getXOR(AESUtil.hexStringToBytes(paramString1.toString()));
        byte[] cmd = AESUtil.hexStringToBytes(paramString1.toString());
        byte[] cmdpassword = new byte[passwordbytes.length + cmd.length];
        for (int i = 0; i < cmd.length; i++) {
            cmdpassword[i] = cmd[i];
        }
        for (int i = 0; i < passwordbytes.length; i++) {
            cmdpassword[cmd.length + i] = passwordbytes[i];
        }
        byte orbyte = AESUtil.getXOR(cmdpassword);
        byte[] sendcmd = new byte[16];
        for (int i = 0; i < cmdpassword.length; i++) {
            sendcmd[i] = cmdpassword[i];
        }
        sendcmd[15] = orbyte;
        try {
            Utils.Log("1111");
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), sendcmd));
            Utils.Log("222");
        } catch (Exception e) {
            Utils.Log("333");
            e.printStackTrace();
        }
    }


    public void onSetLockUnlock() {
        String[] slist = new String[password.length()];
        for (int i = 0; i < slist.length; i++) {
            Utils.Log("1222=" + password.charAt(i));
            slist[i] = "0" + password.charAt(i);
        }
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append(token)
                .append("09")
                .append("06");
        //.append(slist.toString());
        for (int i = 0; i < slist.length; i++) {
            paramString1.append(slist[i]);
        }
        byte rtc = AESUtil.getXOR(AESUtil.hexStringToBytes(paramString1.toString()));
        paramString1.append(BytetohexString(new byte[]{rtc}));
        paramString1.append("686868686868");
        try {
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), AESUtil.hexStringToBytes(paramString1.toString())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        lockManagers = new LockManagers();
        LockManagetInit();
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Utils.Log("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Utils.Log("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Utils.Log("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Utils.Log("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;

                String intentAction;
                intentAction = ACTION_GATT_CONNECTED;

                broadcastUpdate(intentAction, "addressanddeivcename", new String[]{mBluetoothGatt.getDevice().getAddress(), mBluetoothGatt.getDevice().getName()});
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Utils.Log("Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Utils.Log("Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Utils.Log("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Utils.Log("BluetoothAdapter not initialized");
            return;
        }
        Utils.Log("xxxxxxxxxxxxxxxxxx readCharacteristic:" + characteristic);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            Utils.Log("xxxxxxxxxxxxxxxxxx readCharacteristic stringBuilder:" + stringBuilder);
        }

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Utils.Log("BluetoothAdapter not initialized");
            return;
        }
        Utils.Log("xxxxxxxxxxxxxxxxxx writeCharacteristic:" + characteristic);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            Utils.Log("xxxxxxxxxxxxxxxxxx writeCharacteristic stringBuilder:" + stringBuilder);
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Utils.Log("BluetoothAdapter not initialized");
            return;
        }
        Utils.Log("xxxxxxxxxxxxxxxxxx setCharacteristicNotification:" + characteristic + ",enabled:" + enabled);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }


    public BluetoothGattService getGattService(UUID uuid) {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getService(uuid);
    }

    private void LockManagetInit() {

    }

    private void sentMsgToBt(byte[] paramsbyte) {
        Utils.Log("sentMsgToBt");
        BluetoothGattService gattService = getGattService(UUID.fromString(Utils.BT_SEND_SERVICE_UUID));
        if (gattService == null) {
            Toast.makeText(this, "gattService=null", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattCharacteristic characteristic = gattService
                .getCharacteristic(UUID.fromString(Utils.BT_SEND_CHARACTERISTIC_UUID));
        //byte[] value = new byte[20];
        //value[0] = (byte) 0x00;
        //characteristic.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        try {
            byte[] value = new byte[20];
            value[0] = (byte) 0x00;
            characteristic.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            characteristic.setValue(paramsbyte);
            Utils.Log("sentMsgToBt11==" + BytetohexString(paramsbyte));
            mBluetoothGatt.writeCharacteristic(characteristic);
            Utils.Log("sentMsgToB22t");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getTokenCmd() {
        StringBuffer paramString1 = new StringBuffer();
        paramString1.append("26")
                .append("03")
                .append("00")
                .append("25");
        paramString1.append("686868686868686868686868");
        Utils.Log("paramString1=" + paramString1);
        try {
            sentMsgToBt(AESUtil.encrypt(AESUtil.hexStringToBytes(AESUtil.AES_KEYS), AESUtil.hexStringToBytes(paramString1.toString())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnLockStatusListener(OnLockStatusListener mOnLockStatusListener) {
        this.mOnLockStatusListener = mOnLockStatusListener;
    }

}
