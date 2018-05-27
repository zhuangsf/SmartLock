package com.oulu.lock.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.oulu.lock.R;

/**
 * Created by admin on 2018/5/21.
 */

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
