package com.zhouchao.test.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhouchao.test.R;
import com.zhouchao.test.utils.ShellUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Android A/B升级
 * <uses-permission android:name="android.permission.ACCESS_CACHE_FILESYSTEM" />
 * 普通APP需修改过的android.jar，以便调用UpdateEngine
 * 从服务器上下载部分略过
 */
public class OTAActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MainActivity";
    private Context mContext = OTAActivity.this;

    private TextView tv_onStatusUpdate, tv_onPayloadApplicationComplete;
    private Button btn_start, btn_paused, btn_cancel, btn_reboot;

    private UpdateEngine engine;
    private String isUpdating_tag = "isUpdating";
    private static final String PAYLOAD_BINARY_FILE_NAME = "payload.bin";
    private static final String PAYLOAD_PROPERTIES_FILE_NAME = "payload_properties.txt";

    private float updatePercent;// 更新进度[0.0, 100.0]
    private static final String SAVE_FILE_NAME_KEY = "SaveFileName";// 下载时保存的文件名SharedPreferences key值
    private String saveFileName = "a.a.a-1.04.12-G2-full-ota-override-timestamp.zip";// 下载时保存的文件名
    private String saveFilePath = "/data/ota_package/";// 下载时保存的路径
    private SharedPreferences sp;

    private static final SparseArray<String> ERROR_STATE_MAP = new SparseArray<>();

    static {
        ERROR_STATE_MAP.put(0, "SUCCESS");
        ERROR_STATE_MAP.put(1, "ERROR");
        ERROR_STATE_MAP.put(4, "FILESYSTEM_COPIER_ERROR");
        ERROR_STATE_MAP.put(5, "POST_INSTALL_RUNNER_ERROR");
        ERROR_STATE_MAP.put(6, "PAYLOAD_MISMATCHED_TYPE_ERROR");
        ERROR_STATE_MAP.put(7, "INSTALL_DEVICE_OPEN_ERROR");
        ERROR_STATE_MAP.put(8, "KERNEL_DEVICE_OPEN_ERROR");
        ERROR_STATE_MAP.put(9, "DOWNLOAD_TRANSFER_ERROR");
        ERROR_STATE_MAP.put(10, "PAYLOAD_HASH_MISMATCH_ERROR");
        ERROR_STATE_MAP.put(11, "PAYLOAD_SIZE_MISMATCH_ERROR");
        ERROR_STATE_MAP.put(12, "DOWNLOAD_PAYLOAD_VERIFICATION_ERROR");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);

        tv_onStatusUpdate = findViewById(R.id.tv_onStatusUpdate);
        tv_onPayloadApplicationComplete = findViewById(R.id.tv_onPayloadApplicationComplete);

        btn_start = findViewById(R.id.btn_start);
        btn_paused = findViewById(R.id.btn_paused);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_reboot = findViewById(R.id.btn_reboot);

        btn_start.setOnClickListener(this);
        btn_paused.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        btn_reboot.setOnClickListener(this);

        bindUpdateEngine();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                beginUpgrade();
                break;

            case R.id.btn_paused:
                pausedUpgrade();
                break;

            case R.id.btn_cancel:
                cancelUpgrade();
                break;

            case R.id.btn_reboot:
                reboot();
                break;
        }
    }

    //绑定UpdateEngine，持续获取A/B升级状态
    private void bindUpdateEngine() {
        updatePercent = 0.0f;
        engine = new UpdateEngine();
        engine.bind(new UpdateEngineCallback() {
            @Override
            public void onStatusUpdate(int status, float percent) {
                switch (status) {
                    case UpdateEngine.UpdateStatusConstants.IDLE:
                        boolean isUpdating = sp.getBoolean(isUpdating_tag, false);
                        if (isUpdating) {//更新暂停了
                            btn_start.setEnabled(true);
                            btn_paused.setEnabled(false);
                            btn_cancel.setEnabled(true);
                            btn_reboot.setEnabled(false);
                            btn_start.setText("继续更新");
                        } else {//空闲状态
                            btn_start.setEnabled(true);
                            btn_paused.setEnabled(false);
                            btn_cancel.setEnabled(false);
                            btn_reboot.setEnabled(false);
                            btn_start.setText("开始更新");
                        }
                        break;
                    case UpdateEngine.UpdateStatusConstants.CHECKING_FOR_UPDATE:
                        break;
                    case UpdateEngine.UpdateStatusConstants.UPDATE_AVAILABLE:
                        break;
                    case UpdateEngine.UpdateStatusConstants.DOWNLOADING://流式更新中或从zip包解压安装中。（暂停后回调的还是DOWNLOADING，所以先显示暂停的UI，待真正更新后再显示更新UI，这样能cover到暂停的情况）                        NotifyUnityUtils.notifyUnityUpdatePaused();
                        sp.edit().putBoolean(isUpdating_tag, true).apply();
                        if (updatePercent == 0.0f) {
                            btn_start.setEnabled(true);
                            btn_paused.setEnabled(false);
                            btn_cancel.setEnabled(true);
                            btn_reboot.setEnabled(false);
                            btn_start.setText("继续更新");
                        } else {
                            btn_start.setEnabled(false);
                            btn_paused.setEnabled(false);
                            btn_cancel.setEnabled(true);
                            btn_reboot.setEnabled(false);
                        }
                        updatePercent = percent;
                        break;
                    case UpdateEngine.UpdateStatusConstants.VERIFYING:
                        break;
                    case UpdateEngine.UpdateStatusConstants.FINALIZING:
                        break;
                    case UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT://更新完成
                        btn_start.setEnabled(false);
                        btn_paused.setEnabled(false);
                        btn_cancel.setEnabled(true);
                        btn_reboot.setEnabled(true);
                        break;
                    case UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT:
                        break;
                    case UpdateEngine.UpdateStatusConstants.ATTEMPTING_ROLLBACK:
                        break;
                    case UpdateEngine.UpdateStatusConstants.DISABLED:
                        break;
                }
                tv_onStatusUpdate.setText(("status = " + status + ", percent = " + percent));
                Log.e(TAG, "onStatusUpdate: status = " + status + ", percent = " + percent);
            }

            @Override
            public void onPayloadApplicationComplete(int errorCode) {
                sp.edit().putBoolean(isUpdating_tag, false).apply();
                switch (errorCode) {
                    case UpdateEngine.ErrorCodeConstants.SUCCESS:
                        //sp.edit().putBoolean(isFistUpdate_tag, false).apply();
                        break;
                    case UpdateEngine.ErrorCodeConstants.ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.FILESYSTEM_COPIER_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.POST_INSTALL_RUNNER_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.PAYLOAD_MISMATCHED_TYPE_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.INSTALL_DEVICE_OPEN_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.KERNEL_DEVICE_OPEN_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.DOWNLOAD_TRANSFER_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.PAYLOAD_HASH_MISMATCH_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.PAYLOAD_SIZE_MISMATCH_ERROR:
                        break;
                    case UpdateEngine.ErrorCodeConstants.DOWNLOAD_PAYLOAD_VERIFICATION_ERROR:
                        break;
                }
                if (errorCode != UpdateEngine.ErrorCodeConstants.SUCCESS) {//出错了
                    //NotifyUnityUtils.notifyUnityUpdateErrorDialogShow(ERROR_STATE_MAP.get(errorCode));
                }
                //deleteOTAFiles();
                tv_onPayloadApplicationComplete.setText(("errorCode = " + errorCode));
                Log.e(TAG, "onPayloadApplicationComplete: errorCode = " + errorCode);
            }
        });
    }

    /**
     * 开始|继续A/B升级
     */
    public void beginUpgrade() {
        //进页面时，UpdateEngine判断需继续上次更新，此时没有走checkVersion的流程，saveFileName为null
        if (saveFileName == null) {
            saveFileName = sp.getString(SAVE_FILE_NAME_KEY, null);
        }
        if (saveFileName == null) {
            //NotifyUnityUtils.notifyUnityUpdateErrorDialogShow(mContext.getString(R.string.update_file_name_lost));
            return;
        }

        try {
            PayloadSpec payloadInfo = forNonStreaming(new File(saveFilePath + saveFileName));
            engine.applyPayload(payloadInfo.getmUrl(), payloadInfo.getmOffset(), payloadInfo.getmSize(), payloadInfo.getmProperties().toArray(new String[0]));
            Log.e(TAG, "engine.applyPayload() start");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Already processing an update")) {//Already processing an update，so continue update
                engine.resume();
                Log.e(TAG, "engine.applyPayload Exception, Already processing an update, so continue update, engine.resume()");
            } else {
                e.printStackTrace();
                Log.e(TAG, "engine.applyPayload Exception: " + e.getMessage());
                //NotifyUnityUtils.notifyUnityUpdateErrorDialogShow(e.getMessage());
            }
        }
    }

    /**
     * 暂停A/B升级
     */
    public void pausedUpgrade() {
        try {
            engine.suspend();
            //NotifyUnityUtils.notifyUnityUpdatePaused();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 继续A/B升级（只有engine.suspend()的状态下才能engine.resume()，所以建议调用{@link #beginUpgrade()}方法）
     */
    public void continueUpgrade() {
        try {
            engine.resume();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消A/B升级
     */
    public void cancelUpgrade() {
        try {
            engine.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从.zip包中获取PayloadInfo
     *
     * @param packageFile zip file
     * @return PayloadInfo
     * @throws IOException
     */
    private PayloadSpec forNonStreaming(File packageFile) throws IOException {
        boolean payloadFound = false;
        long payloadOffset = 0;
        long payloadSize = 0;

        List<String> properties = new ArrayList<>();
        try (ZipFile zip = new ZipFile(packageFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            long offset = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                // Zip local file header has 30 bytes + filename + sizeof extra field.
                // https://en.wikipedia.org/wiki/Zip_(file_format)
                long extraSize = entry.getExtra() == null ? 0 : entry.getExtra().length;
                offset += 30 + name.length() + extraSize;

                if (entry.isDirectory()) {
                    continue;
                }

                long length = entry.getCompressedSize();
                if (PAYLOAD_BINARY_FILE_NAME.equals(name)) {
                    if (entry.getMethod() != ZipEntry.STORED) {
                        throw new IOException("Invalid compression method.");
                    }
                    payloadFound = true;
                    payloadOffset = offset;
                    payloadSize = length;
                } else if (PAYLOAD_PROPERTIES_FILE_NAME.equals(name)) {
                    InputStream inputStream = zip.getInputStream(entry);
                    if (inputStream != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = br.readLine()) != null) {
                            properties.add(line);
                        }
                    }
                }
                offset += length;
            }
        }

        if (!payloadFound) {
            throw new IOException("Failed to find payload entry in the given package.");
        }

        return new PayloadSpec("file://" + packageFile.getAbsolutePath(), payloadOffset, payloadSize, properties);
    }

    /**
     * PayloadSpec实体类
     */
    class PayloadSpec {
        private String mUrl;
        private long mOffset;
        private long mSize;
        private List<String> mProperties;

        PayloadSpec(String url, long offset, long size, List<String> properties) {
            this.mUrl = url;
            this.mOffset = offset;
            this.mSize = size;
            this.mProperties = properties;
        }

        public String getmUrl() {
            return mUrl;
        }

        public long getmOffset() {
            return mOffset;
        }

        public long getmSize() {
            return mSize;
        }

        public List<String> getmProperties() {
            return mProperties;
        }
    }

    /**
     * 删除OTA包
     */
    public void deleteOTAFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File[] files = new File(saveFilePath).listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".zip")) {
                            file.delete();
                            Log.e(TAG, "deleteOTAFiles: file.getName() = " + file.getName());
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 重启
     */
    public void reboot() {
        ShellUtils.CommandResult result = ShellUtils.execCommand(new String[]{"reboot"}, false);
        Log.e(TAG, "execCommand reboot, commandResult: " + result.toString());
    }

    /**
     * 返回退出应用时，如正在安装更新，App退到后台运行，否则退出App
     */
    public void appExitOrRunInBackground(String exitMode) {
        boolean isUpdating = sp.getBoolean(isUpdating_tag, false);
        if (isUpdating) {
            Log.e(TAG, "App exit by " + exitMode + ", because is downloading or updating..., so running in the background!");

            //回到Home桌面
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            mContext.startActivity(home);
        } else {
            Log.e(TAG, "App exit by " + exitMode);
            ((MainActivity) mContext).finish();
        }
    }


    public void getFingerprintInfo() {
        try {
            FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
            Method method = FingerprintManager.class.getDeclaredMethod("getEnrolledFingerprints");
            Object obj = method.invoke(fingerprintManager);//List<Fingerprint>

            if (obj != null) {
                Method fingerId = Fingerprint.class.getDeclaredMethod("getFingerId");
                Method fingerName = Fingerprint.class.getDeclaredMethod("getName");

                for (int i = 0; i < ((List) obj).size(); i++) {
                    Object item = ((List) obj).get(i);//Fingerprint
                    if (null == item) {
                        continue;
                    }

                    Log.d(TAG, "fingerId: " + fingerId.invoke(item));
                    Log.d(TAG, "fingerName: " + fingerName.invoke(item));
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
