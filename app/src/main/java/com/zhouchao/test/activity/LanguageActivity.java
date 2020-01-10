package com.zhouchao.test.activity;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.LocaleList;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhouchao.test.R;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 更改系统语言，仅system app可用
 * <uses-permission android:name="android.permission.CHANGE_CONFIGURATION" />
 */
public class LanguageActivity extends AppCompatActivity {
    private Context mContext = LanguageActivity.this;
    private TextView tv1;
    private Button btn1, btn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        tv1 = findViewById(R.id.tv_statusUpdate);
        btn1 = findViewById(R.id.btn_install);
        btn2 = findViewById(R.id.btn2);

        tv1.setText(tv1.getText() + "\n" + getLanguageType());

        //设置系统语言为中文
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguageType("zh_CN");
            }
        });

        //设置系统语言为英文
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguageType("en_US");
            }
        });
    }

    public String getLanguageType() {
        Locale locale;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            LocaleList localeList = mContext.getResources().getConfiguration().getLocales();
            locale = localeList.get(0);
        } else {
            locale = Locale.getDefault();
        }

        return locale == null ? null : locale.toString();
    }

    public void setLanguageType(String type) {
        if (type.equals("zh_CN")) {
            updateLanguage(Locale.SIMPLIFIED_CHINESE);
        } else {
            updateLanguage(Locale.US);
        }
    }

    public void updateLanguage(Locale locale) {
        try {
            Object objIActMag, objActMagNative;
            Class clzIActMag = Class.forName("android.app.IActivityManager");
            Class clzActMagNative = Class.forName("android.app.ActivityManagerNative");
            Method mtdActMagNative$getDefault = clzActMagNative.getDeclaredMethod("getDefault");
            objIActMag = mtdActMagNative$getDefault.invoke(clzActMagNative);
            // objIActMag = amn.getConfiguration();
            Method mtdIActMag$getConfiguration = clzIActMag.getDeclaredMethod("getConfiguration");
            Configuration config = (Configuration) mtdIActMag$getConfiguration.invoke(objIActMag);         // set the locale to the new value
            config.locale = locale;         //持久化  config.userSetLocale = true;
            Class clzConfig = Class.forName("android.content.res.Configuration");
            java.lang.reflect.Field userSetLocale = clzConfig.getField("userSetLocale");
            userSetLocale.set(config, true);
            // 此处需要声明权限:android.permission.CHANGE_CONFIGURATION       // 会重新调用 onCreate();
            Class[] clzParams = {Configuration.class};
            // objIActMag.updateConfiguration(config);
            Method mtdIActMag$updateConfiguration = clzIActMag.getDeclaredMethod("updateConfiguration", clzParams);
            mtdIActMag$updateConfiguration.invoke(objIActMag, config);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean is_zh_CN(String type) {
        return type != null && type.contains("zh_CN");
    }

}
