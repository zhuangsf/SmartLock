package com.oulu.lock;

import android.app.Dialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lock.greendao.gen.LockBeanDao;
import com.oulu.lock.activities.DeviceScanActivity;
import com.oulu.lock.adapters.LockAdapter;
import com.oulu.lock.beans.LockBean;
import com.oulu.lock.bleservices.BluetoothUtils;
import com.oulu.lock.dialogs.CustomDialog;
import com.oulu.lock.dialogs.UnbandDialog;
import com.oulu.lock.utils.Utils;

import java.util.ArrayList;

/**
 * Created by liao on 2017/12/2.
 */
public class FragmentLockList extends Fragment {

    private ArrayList<LockBean> locklist = new ArrayList<LockBean>();
    private ListView locklistview;
    private LockBeanDao lockBeanDao;
    private LockAdapter mlockadapter;
    private LinearLayout addlist;
    private LinearLayout linear1;
    private UnbandDialog mUnbandDialog;

    private BluetoothUtils mBluetoothUtils;

    private boolean isSean = false;

    public static FragmentLockList newInstance(Bundle b) {
        FragmentLockList fd = new FragmentLockList();
        fd.setArguments(b);
        return fd;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_locklist, null);
        locklistview = (ListView) view.findViewById(R.id.locklist);
        linear1 = (LinearLayout) view.findViewById(R.id.linear1);
        addlist = (LinearLayout) view.findViewById(R.id.addlist);
        addlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.Log("1234");
                Intent i = new Intent(getActivity(), DeviceScanActivity.class);
                getActivity().startActivityForResult(i, DeviceScanActivity.REQUEST_SELECT_BT);
            }
        });
        mBluetoothUtils = new BluetoothUtils();
        mBluetoothUtils.setBlueInfoCallBcak(new BluetoothUtils.BlueInfoCallBcak() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                String address = device.getAddress();
                Utils.Log("address=" + address);
                for (LockBean lock : locklist) {
                    Utils.Log("111=" + lock.getAddress());
                    if (address.equals(lock.getAddress())) {
                        if (lock.getLockName() != null && (!"".equals(lock.getLockName()) && (!lock.getLockName().equals(device.getName())))) {
                            if (device.getName() != null && !"".equals(device.getName())) {
                                lock.setLockName(device.getName());
                                lockBeanDao.insertOrReplace(lock);
                            }
                        }
                        lock.setIsable(1);
                        Utils.Log("11132");
                    }
                }
                mlockadapter.notifyDataSetChanged();

            }

            @Override
            public void onstartLeScan(boolean flag) {

            }

            @Override
            public void onstopLeScan(boolean flag) {
                isSean = false;
            }

            @Override
            public void onScanTimeout() {
                isSean = false;
            }
        });

        locklistview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("liao", "onItemClick position=" + position);
                LockBean lock = locklist.get(position);
                if (lock.getIsable() == 1) {
                    Log.i("liao","getIsable");
                    ((MainActivity) getActivity()).connectToBle(lock.getLockName(), lock.getAddress());
                }
            }
        });
        locklistview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("liao", "onItemLongClick position=" + position);
                showUnbandDialog(position);
                return true;
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Utils.Log("onResumeonResumeonResumeonResume");
        if (mBluetoothUtils.getBluetoothAdapter().isEnabled()) {
            Utils.Log("startLeScan");
            mBluetoothUtils.startLeScan();
            isSean = true;
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 101);
            return;
        }
        lockBeanDao = MyApplication.getInstance().getDaoSession().getLockBeanDao();
        locklist = (ArrayList<LockBean>) lockBeanDao.loadAll();
        if (locklist != null) {
            for (LockBean l : locklist) {
                l.setIsable(0);
            }
        }
        Log.i("liao","123444");
        if (locklist != null && locklist.size() > 0) {
            Log.i("liao","222222");
            linear1.setBackgroundResource(R.drawable.cup_light_gray);
        }
        mlockadapter = new LockAdapter(getActivity(), locklist);
        locklistview.setAdapter(mlockadapter);

    }

    public void setScan(boolean flag) {
        if (flag && !isSean) {
            locklist = (ArrayList<LockBean>) lockBeanDao.loadAll();
            Log.i("liao","1111111");
            if (locklist != null) {
                for (LockBean l : locklist) {
                    l.setIsable(0);
                }
            }
            if (locklist != null && locklist.size() > 0) {
                Log.i("liao","222222");
                linear1.setBackgroundResource(R.drawable.cup_light_gray);
            }
            mlockadapter = new LockAdapter(getActivity(), locklist);
            locklistview.setAdapter(mlockadapter);
            if (mBluetoothUtils.getBluetoothAdapter().isEnabled()) {
                Utils.Log("lockLost,,,start");
                mBluetoothUtils.startLeScan();
            }
        } else {
            if (isSean) {
                if (mBluetoothUtils.getBluetoothAdapter().isEnabled()) {
                    Utils.Log("lockLost,,,stop");
                    mBluetoothUtils.stopLeScan();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("liao","onPause111");
        setScan(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Utils.Log("onActivityResult");
        if (requestCode == 101) {
            //mBluetoothUtils.startLeScan();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void showUnbandDialog(int unbandId) {
        if (mUnbandDialog == null) {
            UnbandDialog.Builder builder = new UnbandDialog.Builder(getActivity());
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    dialogInterface.dismiss();
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    LockBean lb = locklist.get(mUnbandDialog.getUnbandId());
                    lockBeanDao.delete(lb);
                    locklist.remove(lb);
                    mlockadapter = new LockAdapter(getActivity(), locklist);
                    locklistview.setAdapter(mlockadapter);
                    if (locklist.size() == 0) {
                        linear1.setBackgroundResource(R.drawable.background_no_device);
                    }
                    dialogInterface.dismiss();
                }
            });
            builder.setTitle(R.string.unband_device_warn);
            mUnbandDialog = builder.create();
        }
        mUnbandDialog.setCancelable(false);
        mUnbandDialog.setCanceledOnTouchOutside(false);
        mUnbandDialog.setUnbandId(unbandId);
        mUnbandDialog.getWindow().setDimAmount(0);
        if (!mUnbandDialog.isShowing()) {
            mUnbandDialog.show();
        }
    }
}
