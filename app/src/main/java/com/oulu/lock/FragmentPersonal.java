package com.oulu.lock;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.oulu.lock.activities.LoginActivity;
import com.oulu.lock.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liao on 2017/12/2.
 */
public class FragmentPersonal extends Fragment{

    private TextView logout;
    private static final String IMAGE_FILE_NAME = "avatarImage.jpg";// 头像文件名称
    AlertDialog ad;
    public static FragmentPersonal newInstance(Bundle b) {
        FragmentPersonal fd = new FragmentPersonal();
        fd.setArguments(b);
        return fd;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_personal, null);

        logout = (TextView) view.findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // todo 退出的操作

                LayoutInflater inflater = getActivity().getLayoutInflater();
                final View layout = inflater.inflate(R.layout.red_title_dialog, (ViewGroup) v.findViewById(R.id.dialog));

                TextView title = (TextView)layout.findViewById(R.id.title);
                title.setText("个人中心");
                TextView summary = (TextView)layout.findViewById(R.id.summary);
                summary.setText("确定要退出当前账号?");
                TextView ok = (TextView) layout.findViewById(R.id.ok);
                ok.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        SharedPreferences.Editor e = Utils.getSharedPpreferenceEdit(getActivity());
                        e.putString(Utils.SHARE_PREFERENCE_LOCK, null);
                        e.commit();

                        //Intent i = new Intent(getActivity(), LoginActivity.class);
                        //startActivity(i);
                        // getActivity().finish();
                        ad.dismiss();

                    }
                });

                TextView cancel = (TextView) layout.findViewById(R.id.cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ad.dismiss();
                    }
                });
                AlertDialog.Builder alertBuiler = new AlertDialog.Builder(getActivity());
                ad = alertBuiler.create();
                ad.setView(layout);
                //ad.show();
                Intent intent= new Intent(getActivity() , LoginActivity.class);
                getActivity().startActivity(intent);

                return;
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}
