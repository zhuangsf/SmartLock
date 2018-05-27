package com.oulu.lock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by liao on 2017/9/2.
 */

public class Utils {
    public static final boolean DEBUG=true;
    public static final String TAG ="LOCK";
    public static final String ACTION_LIGHTCONTROL="com.oulu.lock.action";
    //bluetooth
    public static final String BT_GET_SERVICE_UUID="0000f888-0000-1000-8000-00805f9b34fb";
    public static final String BT_GET_CHARACTERISTIC_UUID="00001288-0000-1000-8000-00805f9b34fb";
    public static final String BT_SEND_SERVICE_UUID="0000f888-0000-1000-8000-00805f9b34fb";
    public static final String BT_SEND_CHARACTERISTIC_UUID="00001266-0000-1000-8000-00805f9b34fb";
    public static final String SHARE_PREFERENCE_LOCK="LOCK";
    public static void Log(String info){
        if(DEBUG){
            Log.i(TAG,info);
        }
    }
    public static String getInternelStoragePath(Context context) {
        ArrayList storagges = new ArrayList();
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?>[] paramClasses = {};
            Method getVolumeList = StorageManager.class.getMethod("getVolumeList", paramClasses);
            getVolumeList.setAccessible(true);
            Object[] params = {};
            Object[] invokes = (Object[]) getVolumeList.invoke(storageManager, params);
            if (invokes != null) {
                StorageInfo info = null;
                for (int i = 0; i < invokes.length; i++) {
                    Object obj = invokes[i];
                    Method getPath = obj.getClass().getMethod("getPath", new Class[0]);
                    String path = (String) getPath.invoke(obj, new Object[0]);
                    info = new StorageInfo(path);
                    File file = new File(info.path);
                    if ((file.exists()) && (file.isDirectory()) && (file.canWrite())) {
                        Method isRemovable = obj.getClass().getMethod("isRemovable", new Class[0]);
                        String state = null;
                        try {
                            Method getVolumeState = StorageManager.class.getMethod("getVolumeState", String.class);
                            state = (String) getVolumeState.invoke(storageManager, info.path);
                            info.state = state;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        info.isRemoveable = ((Boolean) isRemovable.invoke(obj, new Object[0])).booleanValue();

                        Log.e("jockeyTrack", "info.isRemoveable = " + info.isRemoveable + " path = " + path + " info.isMounted() = " + info.isMounted());
                        if (info.isMounted() && !info.isRemoveable) {
                            return info.path + "/MateCup";
                        }
                    }
                }
            }
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        storagges.trimToSize();

        return null;
    }
    public static SharedPreferences getSharedPpreference(Context c){
        SharedPreferences p;
        p = c.getSharedPreferences(Utils.SHARE_PREFERENCE_LOCK,Context.MODE_PRIVATE);
        return p;
    }
    public static SharedPreferences.Editor getSharedPpreferenceEdit(Context c){
        SharedPreferences.Editor e;
        e = getSharedPpreference(c).edit();
        return e;
    }
}
