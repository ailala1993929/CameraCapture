package com.zhouchao.test.utils;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.$Gson$Types;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.os.Looper.getMainLooper;

/**
 * OKHttp3.10.0简单封装工具类
 * Created by 周超 on 2017/12/1.
 */
public class OKHttpUtils {
    private static OKHttpUtils mOKHttpUtils;
    private OkHttpClient mOkHttpClient;
    private Handler mHandler;

    //OKHttpUtils单例模式（双重锁锁定）
    public static OKHttpUtils getInstance() {
        if (mOKHttpUtils == null) {
            synchronized (OKHttpUtils.class) {
                if (mOKHttpUtils == null) {
                    mOKHttpUtils = new OKHttpUtils();
                }
            }
        }
        return mOKHttpUtils;
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public Handler getHandler() {
        return mHandler;
    }

    //构造方法，设置连接超时、读取超时、写入超时
    private OKHttpUtils() {
        mOkHttpClient = new OkHttpClient.Builder()//
                .connectTimeout(10, TimeUnit.SECONDS)//连接超时
                .readTimeout(10, TimeUnit.SECONDS)//读取超时
                .writeTimeout(10, TimeUnit.SECONDS)//写入超时
                //.cookieJar(new CookiesManager())//实现对Cookie的自动管理
                .sslSocketFactory(createSSLSocketFactory())//设置用于保护HTTPS连接的套接字工厂。如果没有设置，系统将使用默认。
                .hostnameVerifier(new TrustAllHostnameVerifier())//设置验证用于确认响应证书申请要求的HTTPS连接主机名。
                .build();

        mHandler = new Handler(getMainLooper());
    }

    /**
     * 默认信任所有的证书
     * TODO 最好加上证书认证，主流App都有自己的证书
     */
    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory sSLSocketFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            sSLSocketFactory = sc.getSocketFactory();
        } catch (Exception e) {
            Log.e("OKHttpUtils", "createSSLSocketFactory error", e);
        }
        return sSLSocketFactory;
    }

    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    //设置请求头的方法，返回Headers
    private Headers setHeaders(Map<String, String> headersMap) {
        Headers.Builder headersBuilder = new Headers.Builder();
        Set<Map.Entry<String, String>> set = headersMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            headersBuilder.add(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return headersBuilder.build();
    }

    //发送异步GET请求(UI线程回调)，无Headers
    public void sendGet(String url, final ResultCallBack callBack) {
        sendGet(url, null, callBack);
    }

    //发送异步GET请求(UI线程回调)，headersMap可设置Headers
    public void sendGet(String url, Map<String, String> headersMap, final ResultCallBack callBack) {
        final Request request = headersMap == null ? new Request.Builder().url(url).get().build() : new Request.Builder().url(url).headers(setHeaders(headersMap)).get().build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String result = response.body().string();
                Log.d("chao", "onResponse: " + result);
                mHandler.post(new Runnable() {
                    public void run() {
                        if (callBack.getType() == String.class || callBack.getType() == null) {
                            callBack.onSuccess(result);
                        } else {
                            Object o = new Gson().fromJson(result, callBack.getType());
                            callBack.onSuccess(o);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, final IOException e) {
                Log.d("chao", "onFailure: " + e.getMessage());
                mHandler.post(new Runnable() {
                    public void run() {
                        callBack.onFailure(e);
                    }
                });
            }
        });
    }

    //发送异步POST请求(UI线程回调)，无Headers
    public void sendPost(String url, Map<String, String> bodyMap, final ResultCallBack callBack) {
        sendPost(url, null, bodyMap, callBack);
    }

    //发送异步POST请求(UI线程回调)，headersMap可设置Headers
    public void sendPost(String url, Map<String, String> headersMap, Map<String, String> bodyMap, final ResultCallBack callBack) {
        FormBody.Builder builder = new FormBody.Builder();
        Set<Map.Entry<String, String>> set = bodyMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            builder.add(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        RequestBody body = builder.build();
        Request request = headersMap == null ? new Request.Builder().url(url).post(body).build() : new Request.Builder().headers(setHeaders(headersMap)).url(url).post(body).build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String result = response.body().string();
                Log.d("chao", "onResponse: " + result);
                mHandler.post(new Runnable() {
                    public void run() {
                        if (callBack.getType() == String.class || callBack.getType() == null) {
                            callBack.onSuccess(result);
                        } else {
                            Object o = new Gson().fromJson(result, callBack.getType());
                            callBack.onSuccess(o);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, final IOException e) {
                Log.d("chao", "onFailure: " + e.getMessage());
                mHandler.post(new Runnable() {
                    public void run() {
                        callBack.onFailure(e);
                    }
                });
            }
        });
    }

    //异步上传文件，无Headers
    public void uploadFile(String url, Map<String, String> bodyMap, Map<String, String> filePathMap, final ResultCallBack callBack) {
        uploadFile(url, null, bodyMap, filePathMap, callBack);
    }

    //异步上传文件，headersMap可设置Headers
    public void uploadFile(String url, Map<String, String> headersMap, Map<String, String> bodyMap, Map<String, String> filePathMap, final ResultCallBack callBack) {
        MultipartBody.Builder builder = new MultipartBody.Builder();//创建MultipartBody.Builder，用于添加请求的数据

        Set<Map.Entry<String, String>> set = bodyMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            builder.addFormDataPart(entry.getKey(), entry.getValue());
        }

        Set<Map.Entry<String, String>> set1 = filePathMap.entrySet();
        for (Map.Entry<String, String> entry : set1) {
            File file = new File(entry.getValue());
            String fileType = getMimeType(file.getName());
            builder.addFormDataPart(entry.getKey(), file.getName(), RequestBody.create(MediaType.parse(fileType), file));
        }
        MultipartBody multipartBody = builder.build();

        Request request = headersMap == null ? new Request.Builder().url(url).post(multipartBody).build() : new Request.Builder().headers(setHeaders(headersMap)).url(url).post(multipartBody).build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String result = response.body() != null ? response.body().string() : null;
                Log.d("chao", "onResponse: " + result);
                mHandler.post(new Runnable() {
                    public void run() {
                        if (callBack.getType() == String.class || callBack.getType() == null) {
                            callBack.onSuccess(result);
                        } else {
                            Object o = new Gson().fromJson(result, callBack.getType());
                            callBack.onSuccess(o);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, final IOException e) {
                Log.d("chao", "onFailure: " + e.getMessage());
                mHandler.post(new Runnable() {
                    public void run() {
                        callBack.onFailure(e);
                    }
                });
            }
        });
    }

    private static String getMimeType(String filename) {
        FileNameMap filenameMap = URLConnection.getFileNameMap();
        String contentType = filenameMap.getContentTypeFor(filename);
        if (contentType == null) {
            contentType = "application/octet-stream"; //* exe,所有的可执行程序
        }
        return contentType;
    }

    //数据回调接口
    public abstract static class ResultCallBack<T> {
        private Type getType() {
            Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof Class) {
                //throw new RuntimeException("Missing type parameter.");
                return null;
            }
            ParameterizedType type = (ParameterizedType) superclass;
            return $Gson$Types.canonicalize(type.getActualTypeArguments()[0]);
        }

        //请求成功，回调结果
        public abstract void onSuccess(T result);

        //请求失败，回调Response或IOException
        public abstract void onFailure(Object object);
    }
}
