package com.oulu.lock;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

public class WelcomeActivity extends AppCompatActivity {

    private Handler mHander=new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            Window window = getWindow();
            //window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        mHander.postDelayed(new Runnable() {
            @Override
            public void run() {
                WelcomeActivity.this.startActivity(new Intent(WelcomeActivity.this,MainActivity.class));
                finish();
                WelcomeActivity.this.finish();
            }
        },2000L);

    }
}
