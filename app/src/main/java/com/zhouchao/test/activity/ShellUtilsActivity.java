package com.zhouchao.test.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.zhouchao.test.R;
import com.zhouchao.test.utils.ShellUtils;

public class ShellUtilsActivity extends AppCompatActivity {
    public static final String TAG = "ShellUtilsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shell_utils);

        //读取SN号
        String[] commands1 = new String[]{"cat /persist/.sc_serialno.bin"};
        ShellUtils.CommandResult result1 = ShellUtils.execCommand(commands1, false);
        Log.e(TAG, "SN: " + result1.toString());

        //读取WiFi地址
        String[] commands2 = new String[]{"cat /sys/class/net/wlan0/address"};//或者cat /persist/wlan_mac.bin
        ShellUtils.CommandResult result2 = ShellUtils.execCommand(commands2, false);
        Log.e(TAG, "WiFi地址: " + result2.toString());

        //读取BT地址
        String[] commands3 = new String[]{"cat /persist/bluetooth/.bt_nv.bin"};//或者BluetoothAdapter.getDefaultAdapter().getAddress()
        ShellUtils.CommandResult result3 = ShellUtils.execCommand(commands3, false);
        Log.e(TAG, "BT地址: " + result3.toString());
    }
}
