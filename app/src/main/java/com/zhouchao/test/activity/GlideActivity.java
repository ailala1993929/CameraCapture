package com.zhouchao.test.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.zhouchao.test.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class GlideActivity extends AppCompatActivity {
    public static final String TAG = "GlideActivity";
    private Context mContext = GlideActivity.this;
    private String url = "http://cn.bing.com/az/hprichbg/rb/Dongdaemun_ZH-CN10736487148_1920x1080.jpg";
    private String gifUrl = "http://p1.pstatp.com/large/166200019850062839d3";

    private Button mButton;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glide);

        mButton = findViewById(R.id.mButton);
        mImageView = findViewById(R.id.mImageView);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Glide.with(mContext)
                        .load(url)
                        .placeholder(R.mipmap.default_image)//从开始加载到加载成功之前的占位图
                        .error(R.mipmap.load_failed)//错误时的图片
                        .into(mImageView);
            }
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d, hh:mm a", Locale.ENGLISH);//年月日时分 AM/PM
        String time = sdf.format(System.currentTimeMillis());
        mButton.setText(time);
    }
}
