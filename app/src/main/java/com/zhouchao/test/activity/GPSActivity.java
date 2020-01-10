package com.zhouchao.test.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhouchao.test.R;

/**
 * 打开或关闭GPS
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
 * <uses-permission android:name="android.permission.INTERNET" />
 * <p>
 * Maybe need:
 * android:sharedUserId="android.uid.system"
 */
public class GPSActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "GPSActivity";
    private Context mContext = GPSActivity.this;
    private Button openGPS, closeGPS;
    private TextView tv_gps_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        init();
    }

    private void init() {
        openGPS = findViewById(R.id.openGPS);
        closeGPS = findViewById(R.id.closeGPS);
        tv_gps_state = findViewById(R.id.tv_gps_state);

        openGPS.setOnClickListener(this);
        closeGPS.setOnClickListener(this);

        tv_gps_state.setText(isGPSOpen(this) ? "当前GPS状态：开" : "当前GPS状态：关");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.openGPS:
                openOrCloseGPS(true);
                refreshGPSState();
                break;

            case R.id.closeGPS:
                openOrCloseGPS(false);
                refreshGPSState();
                break;
        }
    }

    // 打开或关闭gps（模式是Device only或Sensors only不会有弹框提示提示）
    public void openOrCloseGPS(boolean open) {
        if (Build.VERSION.SDK_INT < 19) {
            Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(), LocationManager.GPS_PROVIDER, open);
        } else {
            if (open) {
                Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
            } else {
                Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE, android.provider.Settings.Secure.LOCATION_MODE_OFF);
            }
        }
    }

    // 打开gps，强制打开，无弹框（很多情况下会失效）
    public void openGPS() {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(mContext, 0, GPSIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    //判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
    public boolean isGPSOpen(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

    private void refreshGPSState() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tv_gps_state.setText(isGPSOpen(GPSActivity.this) ? "当前GPS状态：开" : "当前GPS状态：关");
            }
        }, 500);
    }

}
