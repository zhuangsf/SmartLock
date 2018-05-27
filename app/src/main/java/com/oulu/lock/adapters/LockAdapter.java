package com.oulu.lock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oulu.lock.R;
import com.oulu.lock.beans.LockBean;

import java.util.ArrayList;

/**
 * Created by liao on 2017/12/6.
 */

public class LockAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<LockBean> mLocklist;
    public LockAdapter(Context context,ArrayList<LockBean> locklist){
        mContext=context;
        mLocklist=locklist;
    }

    @Override
    public int getCount() {
        return mLocklist.size();
    }

    @Override
    public Object getItem(int position) {
        return mLocklist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder=null;
        if(convertView==null){
            convertView= LayoutInflater.from(mContext).inflate(R.layout.lock_list,null);
            holder=new ViewHolder();
            holder.customName=(TextView) convertView.findViewById(R.id.lock_name);
            holder.isable=(TextView)convertView.findViewById(R.id.lock_statue);
            convertView.setTag(holder);
        }else{
            holder=(ViewHolder)convertView.getTag();
        }
        LockBean lockbean=mLocklist.get(position);
        String lockName=lockbean.getCustomName();
        if(lockName==null ||"".equals(lockName)){
            lockName=lockbean.getLockName();
        }
        if(lockName==null ||"".equals(lockName)){
            lockName=mContext.getString(R.string.unknown_device);
        }
        holder.customName.setText(lockName);
        if(lockbean.getIsable ()==1){
            holder.isable.setText("(连接可用)");
        }else {
            holder.isable.setText("");
        }
        return convertView;
    }
    public class ViewHolder{
        public TextView customName;
        public TextView isable;
    }
}
