package com.oulu.lock;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.lock.greendao.gen.DaoMaster;
import com.lock.greendao.gen.DaoSession;
import com.oulu.lock.bleservices.BluetoothUtils;

/**
 * Created by liao on 2017/9/4.
 */

public class MyApplication extends Application {
    private static MyApplication mInstance;
    private SQLiteDatabase db;
    private DaoMaster.DevOpenHelper mHelper;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothUtils.initialize(getApplicationContext());
        mInstance=this;
        BluetoothUtils.initialize(getApplicationContext());
        initDatabase();
    }
    public static MyApplication getInstance() {
        return mInstance;
    }
    private void initDatabase() {
        mHelper = new DaoMaster.DevOpenHelper(this, "lock.db", null);
        db = mHelper.getWritableDatabase();
        mDaoMaster = new DaoMaster(db);
        mDaoSession = mDaoMaster.newSession();
    }
    public DaoSession getDaoSession() {
        return mDaoSession;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

}
