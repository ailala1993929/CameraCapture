package com.zhouchao.test.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.zhouchao.test.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * SIM卡和TF卡
 * <uses-permission android:name="android.permission.READ_PHONE_STATE" />
 */
public class SIMAndTFActivity extends AppCompatActivity {
    public static final String TAG = "SIMAndTFActivity";
    private Context mContext = SIMAndTFActivity.this;

    private SIMCardReceiver simCardReceiver;
    private TFCardReceiver tfCardReceiver;
    private TelephonyManager mTelephonyManager;
    private MyPhoneStateListener myPhoneStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_and_tf);

        //SIM卡状态
        simCardReceiver = new SIMCardReceiver();
        IntentFilter simCardFilter = new IntentFilter();
        simCardFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        registerReceiver(simCardReceiver, simCardFilter);

        //SIM卡信号强度
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        myPhoneStateListener = new MyPhoneStateListener();
        mTelephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        //TF卡状态
        tfCardReceiver = new TFCardReceiver();
        IntentFilter tfCardFilter = new IntentFilter();
        tfCardFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        tfCardFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        tfCardFilter.addDataScheme("file");
        registerReceiver(tfCardReceiver, tfCardFilter);

        boolean b1 = isTFMounted();
        Log.e(TAG, "isTFMounted: " + b1);

        boolean b2 = isSIMCardInsert();
        Log.e(TAG, "isSIMCardInsert: " + b2);
    }


    //安装或卸载SIM卡的广播
    private class SIMCardReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "SIMCardReceiver: intent.getAction()=" + intent.getAction());

            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {//SIM卡状态广播，首次就有广播
                boolean isInsert = isSIMCardInsert();

                /*//G2上插拔是准的，开机不准，状态为ABSENT，所以不采用以下方法，采用isSIMCardInsert()主动去获取SIM卡状态
                String stateExtra = intent.getStringExtra("ss");
                Log.e(TAG, "SIM_STATE_CHANGED: state = " + stateExtra);
                if (stateExtra != null) {
                    switch (stateExtra) {
                        case "ABSENT":  //卡拔出状态
                            Log.e(TAG, "SIMCardReceiver: 卡拔出状态，stateExtra = " + stateExtra);
                            break;
                        case "READY": //卡正常状态
                        case "IMSI":
                        case "LOADED":
                            Log.e(TAG, "SIMCardReceiver: 卡正常状态，stateExtra = " + stateExtra);
                            break;
                        case "LOCKED": //卡被锁状态
                        case "NOT_READY":
                        case "PIN":
                        case "PUK":
                            Log.e(TAG, "SIMCardReceiver: 卡被锁状态，stateExtra = " + stateExtra);
                            break;
                    }
                }*/
            }
        }
    }

    //SIM卡信号强度
    private class MyPhoneStateListener extends PhoneStateListener {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            int level = signalStrength.getLevel();//[0, 4]
            Log.e(TAG, "onSignalStrengthsChanged: level = " + level);
        }
    }

    private class TFCardReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "TFCardReceiver: intent.getAction()=" + intent.getAction());//TF卡状态广播，需要插拔才有广播

            if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
                Log.e(TAG, "onReceive: ACTION_MEDIA_MOUNTED");
            } else if (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
                Log.e(TAG, "onReceive: ACTION_MEDIA_EJECT");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (simCardReceiver != null) {
            unregisterReceiver(simCardReceiver);
        }

        if (tfCardReceiver != null) {
            unregisterReceiver(tfCardReceiver);
        }

        if (mTelephonyManager != null && myPhoneStateListener != null) {
            mTelephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    //判断是否挂载外置TF卡
    private boolean isTFMounted() {
        boolean isMounted = false;
        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeList = StorageManager.class.getMethod("getVolumeList");
            getVolumeList.setAccessible(true);
            Object[] results = (Object[]) getVolumeList.invoke(sm);
            if (results != null) {
                for (Object result : results) {
                    Boolean isRemovable = (Boolean) result.getClass().getMethod("isRemovable").invoke(result);
                    if (isRemovable) {
                        String path = (String) result.getClass().getMethod("getPath").invoke(result);
                        String state = (String) result.getClass().getMethod("getState").invoke(result);
                        if (state.equals(Environment.MEDIA_MOUNTED)) {
                            isMounted = true;
                            Log.e(TAG, "TFMounted, path: " + path);
                            break;
                        }
                    }
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return isMounted;
    }

    //判断是否插入SIM卡
    public boolean isSIMCardInsert() {
        boolean isInsert = false;
        int simState = mTelephonyManager.getSimState();
        Log.e(TAG, "mTelephonyManager.getSimState(): " + simState);
        switch (simState) {
            case TelephonyManager.SIM_STATE_READY://卡正常状态
                isInsert = true;
                break;
        }
        return isInsert;
    }

    //获得移动数据的开关状态（只要有一张卡为开即可）
    public boolean getMobileNetworkEnable() {
        //卡1
        boolean isMobileNetworkEnable0 = false;
        try {
            int subid0 = SubscriptionManager.from(mContext).getActiveSubscriptionInfoForSimSlotIndex(0).getSubscriptionId();
            TelephonyManager telephonyService = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled", int.class);
            if (null != getDataEnabled) {
                isMobileNetworkEnable0 = (Boolean) getDataEnabled.invoke(telephonyService, subid0);
                Log.e(TAG, "chao getMobileNetworkEnable success, slotIndex = 0, subid = " + subid0 + ", enable = " + isMobileNetworkEnable0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "chao getMobileNetworkEnable failure, slotIndex = 0");
        }

        //卡2
        boolean isMobileNetworkEnable1 = false;
        try {
            int subid1 = SubscriptionManager.from(mContext).getActiveSubscriptionInfoForSimSlotIndex(1).getSubscriptionId();
            TelephonyManager telephonyService = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled", int.class);
            if (null != getDataEnabled) {
                isMobileNetworkEnable1 = (Boolean) getDataEnabled.invoke(telephonyService, subid1);
                Log.e(TAG, "chao getMobileNetworkEnable success, slotIndex = 1, subid = " + subid1 + ", enable = " + isMobileNetworkEnable1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "chao getMobileNetworkEnable failure, slotIndex = 1");
        }

        return isMobileNetworkEnable0 || isMobileNetworkEnable1;
    }

    //打开或关闭移动数据（只要有一张卡操作成功即可）
    public boolean setMobileNetworkEnable(boolean enable) {
        //卡1
        boolean success0 = false;
        try {
            int subid0 = SubscriptionManager.from(mContext).getActiveSubscriptionInfoForSimSlotIndex(0).getSubscriptionId();
            TelephonyManager telephonyService = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled", int.class, boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyService, subid0, enable);
                success0 = true;
                Log.e(TAG, "chao setMobileNetworkEnable success, slotIndex = 0, subid = " + subid0 + ", enable = " + enable);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "chao setMobileNetworkEnable failure, slotIndex = 0");
        }

        //卡2
        boolean success1 = false;
        try {
            int subid1 = SubscriptionManager.from(mContext).getActiveSubscriptionInfoForSimSlotIndex(1).getSubscriptionId();
            TelephonyManager telephonyService = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled", int.class, boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyService, subid1, enable);
                success1 = true;
                Log.e(TAG, "chao setMobileNetworkEnable success, slotIndex = 1, subid = " + subid1 + ", enable = " + enable);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "chao setMobileNetworkEnable failure, slotIndex = 1");
        }

        return success0 || success1;
    }

}
