package com.oulu.lock.managers;

import android.util.Log;

import com.oulu.lock.BluetoothLeService;
import com.oulu.lock.listener.OnLockStatusListener;

/**
 * Created by liao on 2017/11/8.
 */

public class LockManagers {
    private BluetoothLeService mBluetoothLeService;

    private final int ACTIONBAR = 1;
    private final int ACTIONLOCK = 2;
    private final int ACTIONUNLOCK = 3;
    private final int ACTIONMODIFYNAME = 4;
    private final int ACTIONMODIFYPASSWORD = 5;
    private final int ACTIONGETLOCKSTATUS = 6;

    public int action = -1;

    private OnLockStatusListener onLockStatusListener = new OnLockStatusListener() {
        @Override
        public void getLogin(boolean flag) {
            Log.i("liao", "getLogin=" + flag);
            Log.i("liao", "1233");
            if (action == -1) {
                return;
            }
            Log.i("liao", "AAAAAS");
        }

        @Override
        public void getToken(String token) {
            Log.i("liao", "getTokenC=" + token);
            Log.i("liao", "action=" + action);

            switch (action) {
                case ACTIONBAR:
                    if (hasBluetoothLeService()) {
                        mBluetoothLeService.onQueryLockBat();
                    }
                    break;
                case ACTIONUNLOCK:
                    Log.i("liao", "ACTIONUNLOCK");
                    if (hasBluetoothLeService()) {
                        Log.i("liao", "ACTIONUNLOC1111111K");
                        mBluetoothLeService.onSetLockUnlock();
                    }
                    break;
                case ACTIONLOCK:

                    if (hasBluetoothLeService()) {
                        mBluetoothLeService.onLockLocked();
                    }
                    break;
                case ACTIONGETLOCKSTATUS:

                    if (hasBluetoothLeService()) {
                        mBluetoothLeService.onGetLockStatus();
                    }
                    break;
                case ACTIONMODIFYNAME:
                    break;
                case ACTIONMODIFYPASSWORD:
                    break;
            }
        }

        @Override
        public void getBar(String power) {
            Log.i("liao", "getBar=" + power);
        }

        @Override
        public void getUnlock(boolean flag) {
            Log.i("liao", "getUnlock=" + flag);
        }

        @Override
        public void getLock(boolean flag) {
            Log.i("liao", "getLock=" + flag);
        }

        @Override
        public void getModifyName(boolean flag) {
            Log.i("liao", "getModifyName=" + flag);
        }

        @Override
        public void getPassword(boolean flag) {
            Log.i("liao", "getPassword=" + flag);
        }

        @Override
        public void getLockstatus(boolean flag) {
            Log.i("liao", "getLockstatus=" + flag);
        }
    };

    public void setBluetoothLeService(BluetoothLeService mBluetoothLeService) {
        this.mBluetoothLeService = mBluetoothLeService;
        this.mBluetoothLeService.setOnLockStatusListener(onLockStatusListener);
    }

    public boolean hasBluetoothLeService() {
        return mBluetoothLeService != null;
    }

    public boolean connectBle(String address) {
        if (hasBluetoothLeService()) {
            mBluetoothLeService.connect(address);
            return true;
        }
        return false;
    }

    public boolean onLockLogin(String paramString) {
        if (hasBluetoothLeService()) {
            mBluetoothLeService.onLockLogin(paramString);
            return true;
        }
        return false;
    }

    public boolean onQueryLockBat() {
        action = ACTIONBAR;
        if (hasBluetoothLeService()) {
            mBluetoothLeService.onQueryLockBat();
        }
        return true;
    }

    public boolean onGetLockStatus() {
        action = ACTIONGETLOCKSTATUS;
        if (hasBluetoothLeService()) {
            mBluetoothLeService.onGetLockStatus();
        }
        return true;
    }

    public boolean onSetLockUnlock() {
        action = ACTIONUNLOCK;
        if (hasBluetoothLeService()) {
            mBluetoothLeService.onSetLockUnlock();
        }
        return true;
    }

    public boolean onLockLocked() {
        action = ACTIONLOCK;
        if (hasBluetoothLeService()) {
            mBluetoothLeService.onLockLocked();
        }
        return true;
    }

    public boolean onSetLockName(String paramString) {
        action = ACTIONMODIFYNAME;
        if (hasBluetoothLeService()) {
            mBluetoothLeService.onSetLockName(paramString);
            return true;
        }
        return false;
    }

    public boolean onSetLockPassword(String paramString) {
        action = ACTIONMODIFYPASSWORD;
        if (hasBluetoothLeService()) {
            mBluetoothLeService.onSetLockPassword(paramString);
            return true;
        }
        return false;
    }

    public boolean getTokenCmd() {
        if (hasBluetoothLeService()) {
            mBluetoothLeService.getTokenCmd();
            return true;
        }
        return false;
    }
}
