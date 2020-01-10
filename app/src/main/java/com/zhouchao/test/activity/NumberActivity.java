package com.zhouchao.test.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;
import com.zhouchao.test.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

public class NumberActivity extends AppCompatActivity {
    public static final String TAG = "chao";
    private RelativeLayout rl_number;
    private TextView tv_time;
    private TickerView tv_number;
    private TextView tv_number_;

    private int time = 15;
    private int start = 100000000;
    private int end = 200000000;

    private int countdownIntervals = 1000;//Millis
    private int increaseIntervals = 500;//Millis

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number);

        // Hide navigationBar and statusBar
        int uiFlags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);

        rl_number = findViewById(R.id.rl_number);
        tv_time = findViewById(R.id.tv_time);
        tv_number = findViewById(R.id.tv_number);
        tv_number_ = findViewById(R.id.tv_number_);

        if (time < 10) {
            tv_time.setText(("00:0" + time));
        } else {
            tv_time.setText(("00:" + time));
        }

        tv_number.setCharacterLists(TickerUtils.provideNumberList());
        tv_number.setText(getFormatMoney(String.valueOf(start)));

        setTextViewStyles(tv_number_);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                countdown();
                increaseNumber();
            }
        }, 3000);
    }

    private void countdown() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;

                int currentTime = Integer.parseInt(tv_time.getText().toString().replace("00:", ""));
                if (currentTime == 0) {
                    tv_number.setText((String.valueOf(end)));
                    startAnim();
                    return;
                }

                if (currentTime - 1 < 10) {
                    tv_time.setText(("00:0" + (currentTime - 1)));
                } else {
                    tv_time.setText(("00:" + (currentTime - 1)));
                }

                countdown();
            }
        }, countdownIntervals);
    }

    private void increaseNumber() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;

                int currentNumber = Integer.parseInt(tv_number.getText().replace(",", ""));
                if (currentNumber >= end) return;
                int increase = new Random().nextInt((end - start) / time * 2 / (1000 / increaseIntervals));
                if (currentNumber + increase >= end) {
                    tv_number.setText(getFormatMoney(String.valueOf(end)));
                    return;
                } else {
                    tv_number.setText(getFormatMoney(String.valueOf(currentNumber + increase)));
                }

                increaseNumber();
            }
        }, increaseIntervals);
    }

    //放大缩小动画
    private void startAnim() {
        tv_number.setVisibility(View.INVISIBLE);
        tv_number_.setVisibility(View.VISIBLE);
        tv_number_.setText(getFormatMoney(String.valueOf(end)));

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(rl_number, "scaleX", rl_number.getScaleX(), 1.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(rl_number, "scaleY", rl_number.getScaleY(), 1.3f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(scaleX).with(scaleY);
        animatorSet.setDuration(1000);
        animatorSet.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator scaleX_ = ObjectAnimator.ofFloat(rl_number, "scaleX", rl_number.getScaleX(), 1f);
                ObjectAnimator scaleY_ = ObjectAnimator.ofFloat(rl_number, "scaleY", rl_number.getScaleY(), 1f);
                AnimatorSet animatorSet_ = new AnimatorSet();
                animatorSet_.play(scaleX_).with(scaleY_);
                animatorSet_.setDuration(1000);
                animatorSet_.start();
            }
        }, 1000);
    }

    //颜色渐变
    public void setTextViewStyles(TextView text) {
        LinearGradient mLinearGradient = new LinearGradient(0, 0, 0, text.getPaint().getTextSize(), Color.parseColor("#E0CCA8"), Color.parseColor("#FFD700"), Shader.TileMode.CLAMP);
        text.getPaint().setShader(mLinearGradient);
        text.invalidate();
    }

    //金钱格式化。千分位格式化+最多保留2位小数+不四舍五入（如：65562.1578 → 65,562.15）
    //","代表分隔符，"."后面的##代表保留位数，如果换成0，效果就是位数不足0补齐
    public static String getFormatMoney(String money) {
        BigDecimal b = new BigDecimal(money);
        //DecimalFormat d = new DecimalFormat("#,###.##");//四舍五入
        DecimalFormat d = new DecimalFormat("#,##0.##");//不四舍五入，要设置舍入模式
        d.setRoundingMode(RoundingMode.FLOOR);
        return d.format(b);
    }
}
