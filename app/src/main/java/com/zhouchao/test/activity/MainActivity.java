package com.zhouchao.test.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.zhouchao.test.R;

/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private Context mContext = MainActivity.this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startActivity(new Intent(this, CameraActivity.class));
        finish();
    }

}
