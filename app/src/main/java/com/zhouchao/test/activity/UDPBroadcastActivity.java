package com.zhouchao.test.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhouchao.test.R;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class UDPBroadcastActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "UDPBroadcastActivity";
    private Context mContext = UDPBroadcastActivity.this;

    private Button btn_datagramSend1, btn_datagramSend2, btn_multicastSend;
    private TextView tv_datagramReceive, tv_multicastReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_broadcast);

        initView();
        addListener();

        //组播接收端
        multicastReceive("234.5.6.7", 9876);
        //群播接收端
        datagramReceive(9876);
    }

    private void initView() {
        btn_datagramSend1 = findViewById(R.id.btn_datagramSend1);
        btn_datagramSend2 = findViewById(R.id.btn_datagramSend2);
        btn_multicastSend = findViewById(R.id.btn_multicastSend);
        tv_datagramReceive = findViewById(R.id.tv_datagramReceive);
        tv_multicastReceive = findViewById(R.id.tv_multicastReceive);
    }

    private void addListener() {
        btn_datagramSend1.setOnClickListener(this);
        btn_datagramSend2.setOnClickListener(this);
        btn_multicastSend.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_datagramSend1://发送群播
                datagramSend("255.255.255.255", 2893, "发送了一个群播");
                break;
            case R.id.btn_datagramSend2://发送单播
                datagramSend("192.168.137.1", 2893, "发送了一个单播");
                break;
            case R.id.btn_multicastSend://发送组播
                multicastSend("234.5.6.7", 2893, "发送了一个组播");
                break;
        }
    }

    //组播接收端，接收组播和单播
    private void multicastReceive(final String ipGroup, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[1024];
                    MulticastSocket ms = new MulticastSocket(port);
                    InetAddress groupAddress = InetAddress.getByName(ipGroup);
                    ms.joinGroup(groupAddress);
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

                    while (true) {
                        ms.receive(dp);
                        String msg = new String(dp.getData(), 0, dp.getLength());
                        tv_multicastReceive.setText(msg);
                        Log.d(TAG, "multicastReceive msg = " + msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "multicastReceive Exception = " + e.getMessage());
                }
            }
        }).start();
    }

    //群播接收端，接收群播和单播
    private void datagramReceive(final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramSocket socket = new DatagramSocket(port);
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

                    while (true) {
                        socket.receive(dp);
                        String msg = new String(dp.getData(), 0, dp.getLength());
                        tv_datagramReceive.setText(msg);
                        Log.d(TAG, "datagramReceive msg = " + msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "datagramReceive Exception = " + e.getMessage());
                }
            }
        }).start();
    }

    //群播发送端，发送群波或单播。如果ip为255.255.255.255，则向局域网内所有主机发送消息，此时发送和接收可能较慢；若ip为指定ip，则发送接收较快
    private void datagramSend(String ip, int port, String msg) {
        try {
            byte[] msgByte = msg.getBytes();
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket dp = new DatagramPacket(msgByte, msgByte.length, InetAddress.getByName(ip), port);
            socket.send(dp);
            socket.close();
            Log.d(TAG, "chao Send datagram success! ip = " + ip + ", port = " + port + ", msg = " + msg);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "chao Send datagram fail! ip = " + ip + ", port = " + port + ", msg = " + msg);
        }
    }

    //组播发送端，发送组波，不能发送单播。向某个ip组如："234.5.6.7"发送广播，速度和接收都较快。
    private void multicastSend(String ipGroup, int port, String msg) {
        try {
            byte[] msgByte = msg.getBytes();
            MulticastSocket socket = new MulticastSocket(port);
            InetAddress groupAddress = InetAddress.getByName(ipGroup);
            socket.joinGroup(groupAddress);
            DatagramPacket dp = new DatagramPacket(msgByte, msgByte.length);
            socket.send(dp);
            socket.close();
            Log.d(TAG, "chao Send multicast success! ip = " + ipGroup + ", port = " + port + ", msg = " + msg);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "chao Send multicast fail! ip = " + ipGroup + ", port = " + port + ", msg = " + msg);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
