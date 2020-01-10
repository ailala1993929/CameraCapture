package com.zhouchao.test.activity;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zhouchao.test.R;
import com.zhouchao.test.utils.OKHttpUtils;
import com.zhouchao.test.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.widget.Toast.LENGTH_SHORT;

/**
 * UploadFileActivity
 */
public class UploadFileActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "UploadFileActivity";
    private Context mContext = UploadFileActivity.this;

    private Button btn1, btn2, btn3;
    private TextView tv1, tv2, tv3;

    private SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_file);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)) {

            }
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            init();
        }
    }

    private void init() {
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);

        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        tv3 = findViewById(R.id.tv3);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);

        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        tv1.setText(sp.getString("imgPath", ""));
        tv2.setText(sp.getString("modelPath", ""));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1://选择图片
                Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
                intent1.setType("image/*");
                intent1.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent1, 1);
                break;

            case R.id.btn2://选择文件
                Intent intent2 = new Intent(Intent.ACTION_GET_CONTENT);
                intent2.setType("*/*");
                intent2.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent2, 2);
                break;

            case R.id.btn3://上传
                if (TextUtils.isEmpty(tv1.getText()) || TextUtils.isEmpty(tv2.getText())) return;

                String url = "http://10.4.64.108:7002/api/v3/app/contents";
                String imgFullPath = tv1.getText().toString();
                String modelFullPath = tv2.getText().toString();
                String modelFullName = modelFullPath.substring(modelFullPath.lastIndexOf("\\") + 1);
                String modelName = modelFullName.substring(0, modelFullName.lastIndexOf("."));

                Map<String, String> headersMap = new HashMap<>();
                headersMap.put("service-param-developerId", "191");
                headersMap.put("service-param-appId", "691");

                Map<String, String> bodyMap = new HashMap<>();
                bodyMap.put("name", modelFullName);
                bodyMap.put("type", "MODEL_FBX");
                bodyMap.put("description", modelName + " model");
                bodyMap.put("label", "k:v,k:v");
                bodyMap.put("annotation", "k:v,k:v");

                Map<String, String> filePathMap = new HashMap<>();
                bodyMap.put("img", imgFullPath);
                bodyMap.put("dataFile", modelFullPath);

                OKHttpUtils.getInstance().uploadFile(url, headersMap, bodyMap, filePathMap, new OKHttpUtils.ResultCallBack<String>() {
                    @Override
                    public void onSuccess(String result) {
                        tv3.setText(result);
                    }

                    @Override
                    public void onFailure(Object object) {
                        tv3.setText(object.toString());
                    }
                });
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 1) {//是否选择，没选择就不会继续
            Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
            String path = Utils.getPath(this, uri);
            tv1.setText(path);
            sp.edit().putString("imgPath", path).apply();
        } else if (resultCode == Activity.RESULT_OK && requestCode == 2) {
            Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
            //String path = Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];
            String path = Utils.getPath(this, uri);
            tv2.setText(path);
            sp.edit().putString("modelPath", path).apply();
        }
    }

    // 请求存储权限结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                Toast.makeText(this, "该功能需要读写存储，请授予权限", LENGTH_SHORT).show();
                finish();
            }
        }
    }

}
