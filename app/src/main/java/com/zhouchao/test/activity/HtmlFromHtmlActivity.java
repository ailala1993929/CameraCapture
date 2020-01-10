package com.zhouchao.test.activity;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import com.zhouchao.test.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Android HTML.fromHtml解析图片标签
 */
public class HtmlFromHtmlActivity extends AppCompatActivity {
    public static final String TAG = "HtmlFromHtmlActivity";
    private TextView mTextView1, mTextView2, mTextView3;

    private NetworkImageGetter mImageGetter;
    private String htmlThree;
    private String picName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html_from_html);

        mTextView1 = findViewById(R.id.mTextView1);
        mTextView2 = findViewById(R.id.mTextView2);
        mTextView3 = findViewById(R.id.mTextView3);

        String htmlOne = "本地图片测试：" + "<img src='/sdcard/DCIM/P70126-115217.jpg'>" + "本地";
        mTextView1.setText(Html.fromHtml(htmlOne, new LocalImageGetter(), null));

        String htmlTwo = "项目图片测试：" + "<img src=\"" + R.mipmap.ic_launcher + "\">" + "项目";
        mTextView2.setText(Html.fromHtml(htmlTwo, new ProImageGetter(), null));

        htmlThree = "网络图片测试：" + "<img src='http://img.my.csdn.net/uploads/201307/14/1373780364_7576.jpg'>" + "网络";
        picName = "networkPic.jpg";
        mImageGetter = new NetworkImageGetter();
        mTextView3.setText(Html.fromHtml(htmlThree, mImageGetter, null));
    }

    /**
     * 本地图片
     *
     * @author Susie
     */
    private final class LocalImageGetter implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            // 获取本地图片
            Drawable drawable = Drawable.createFromPath(source);
            // 必须设为图片的边际,不然TextView显示不出图片
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
            // 将其返回
            return drawable;
        }
    }

    /**
     * 项目资源图片
     *
     * @author Susie
     */
    private final class ProImageGetter implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            // 获取到资源id
            int id = Integer.parseInt(source);
            Drawable drawable = getResources().getDrawable(id);
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
            return drawable;
        }
    }

    /**
     * 网络图片
     *
     * @author Susie
     */
    private final class NetworkImageGetter implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            Drawable drawable = null;
            // 封装路径
            File file = new File(Environment.getExternalStorageDirectory(), picName);
            // 判断是否以http开头
            if (source.startsWith("http")) {
                // 判断路径是否存在
                if (file.exists()) {
                    // 存在即获取drawable
                    drawable = Drawable.createFromPath(file.getAbsolutePath());
                    if (drawable != null) {
                        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                    }
                } else {
                    // 不存在即开启异步任务加载网络图片
                    AsyncLoadNetworkPic networkPic = new AsyncLoadNetworkPic();
                    networkPic.execute(source);
                }
            }
            return drawable;
        }
    }

    /**
     * 加载网络图片异步类
     *
     * @author Susie
     */

    private final class AsyncLoadNetworkPic extends AsyncTask<String, Integer, Void> {
        @Override
        protected Void doInBackground(String... params) {
            // 加载网络图片
            loadNetPic(params);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // 当执行完成后再次为其设置一次
            mTextView3.setText(Html.fromHtml(htmlThree, mImageGetter, null));
        }

        /**
         * 加载网络图片
         */
        private void loadNetPic(String... params) {
            String path = params[0];
            File file = new File(Environment.getExternalStorageDirectory(), picName);
            InputStream in = null;
            FileOutputStream out = null;

            try {
                URL url = new URL(path);
                HttpURLConnection connUrl = (HttpURLConnection) url.openConnection();
                connUrl.setConnectTimeout(5000);
                connUrl.setRequestMethod("GET");

                if (connUrl.getResponseCode() == 200) {
                    in = connUrl.getInputStream();
                    out = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int len;

                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                } else {
                    Log.i(TAG, connUrl.getResponseCode() + "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
