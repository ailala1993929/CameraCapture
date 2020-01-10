package com.zhouchao.test.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.zhouchao.test.R;

/**
 * Created by chao on 2018/5/3.
 * 8.0通知栏和应用角标适配
 */
public class NotificationActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "NotificationActivity";
    private Context mContext = NotificationActivity.this;

    public static final String NOTIFICATION_CLICKED_ACTION = "notification_clicked";
    public static final String NOTIFICATION_DELETE_ACTION = "notification_delete";

    private Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private PendingIntent pendingIntentClick, pendingIntentDelete;
    private int notificationId = 0;

    private Button mButton1, mButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        init();
        addListener();
    }

    private void init() {
        mButton1 = findViewById(R.id.mButton1);
        mButton2 = findViewById(R.id.mButton2);

        /*Channel只需创建一次，后续重复执行不会重复创建*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create Channel 1
            String channelId1 = "chat";
            String channelName1 = "聊天消息";
            int importance1 = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(channelId1, channelName1, importance1);

            //Create Channel 2
            String channelId2 = "subscribe";
            String channelName2 = "订阅消息";
            int importance2 = NotificationManager.IMPORTANCE_DEFAULT;
            createNotificationChannel(channelId2, channelName2, importance2);
        }
    }

    private void addListener() {
        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);
    }

    /*创建通知渠道*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, int importance) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(channelId, channelName, importance));
            Log.e(TAG, "createNotificationChannel: channelId = " + channelId + ", channelName = " + channelName);
        }
    }

    /*处理通知栏点击事件和移除事件*/
    public static class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NOTIFICATION_CLICKED_ACTION.equals(action)) {
                Log.e(TAG, "onReceive: 通知栏点击了");
            }

            if (NOTIFICATION_DELETE_ACTION.equals(action)) {
                Log.e(TAG, "onReceive: 通知栏移除了");
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mButton1:
                createPendingIntent();
                NotificationManager manager1 = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification notification1 = new NotificationCompat
                        .Builder(this, "chat")
                        .setContentTitle("收到一条聊天消息")
                        .setContentText("今天中午吃什么？")
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        //.setSound(defaultSoundUri)//声音
                        //.setVibrate(new long[]{0, 500})//震动，延迟0，持续0.5s
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentIntent(pendingIntentClick)//点击事件
                        .setDeleteIntent(pendingIntentDelete)//移除事件
                        .setOngoing(false)//是否常驻
                        .setAutoCancel(true)//点击后消失
                        .build();
                if (manager1 != null) manager1.notify(notificationId++, notification1);
                break;

            case R.id.mButton2:
                createPendingIntent();
                NotificationManager manager2 = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification notification2 = new NotificationCompat
                        .Builder(this, "subscribe")
                        .setContentTitle("收到一条订阅消息")
                        .setContentText("美味不用等，好品抢购中！")
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setSound(defaultSoundUri)
                        .setVibrate(new long[]{0, 500})
                        .setContentIntent(pendingIntentClick)
                        .setDeleteIntent(pendingIntentDelete)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .build();
                if (manager2 != null) manager2.notify(notificationId++, notification2);
                break;
        }
    }

    /*创建通知栏点击和移除事件Intent 移除是指滑动移除和点X移除*/
    private void createPendingIntent() {
        //点击事件Intent
        Intent intentClick = new Intent(this, NotificationBroadcastReceiver.class);
        intentClick.setAction(NOTIFICATION_CLICKED_ACTION);
        pendingIntentClick = PendingIntent.getBroadcast(this, 0, intentClick, PendingIntent.FLAG_UPDATE_CURRENT);

        //移除事件Intent
        Intent intentDelete = new Intent(this, NotificationBroadcastReceiver.class);
        intentDelete.setAction(NOTIFICATION_DELETE_ACTION);
        pendingIntentDelete = PendingIntent.getBroadcast(this, 0, intentDelete, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}