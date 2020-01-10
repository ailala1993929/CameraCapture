package com.zhouchao.test.activity;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.zhouchao.test.R;

/**
 * 播放提示音
 */
public class SoundPoolActivity extends AppCompatActivity {
    public static final String TAG = "SoundPoolActivity";

    private SoundPool mSoundPool;
    private boolean loadComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_pool);

        Button btn_play1 = findViewById(R.id.btn_play1);
        Button btn_play2 = findViewById(R.id.btn_play2);
        Button btn_play3 = findViewById(R.id.btn_play3);

        mSoundPool = new SoundPool(1, AudioManager.STREAM_ALARM, 0);

        //LoadComplete监听
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.e(TAG, "onLoadComplete: ");
                loadComplete = true;
            }
        });

        //加载项目raw下资源
        final int soundID1 = mSoundPool.load(this, R.raw.argon, 1);
        //加载SD卡下资源
        final int soundID2 = mSoundPool.load("/sdcard/Notifications/tweeters.ogg", 1);
        //加载系统内置资源
        final int soundID3 = mSoundPool.load("/system/media/audio/notifications/Argon.ogg", 1);

        //播放
        btn_play1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadComplete) {
                    mSoundPool.play(soundID1, 1, 1, 0, 0, 1);
                    mSoundPool.release();
                }
            }
        });
        btn_play2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadComplete) {
                    mSoundPool.play(soundID2, 1, 1, 0, 0, 1);
                    mSoundPool.release();
                }
            }
        });
        btn_play3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadComplete) {
                    mSoundPool.play(soundID3, 1, 1, 0, 0, 1);
                    mSoundPool.release();
                }
            }
        });
    }
}
