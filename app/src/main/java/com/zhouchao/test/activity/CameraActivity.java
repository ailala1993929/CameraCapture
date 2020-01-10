package com.zhouchao.test.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zhouchao.test.R;
import com.zhouchao.test.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener, MediaRecorder.OnInfoListener, SurfaceHolder.Callback {
    public static final String TAG = "chao";
    private Context mContext = CameraActivity.this;

    private FrameLayout mFrameLayout;
    private Button btn_start_preview, btn_stop_preview, btn_preview_test, btn_take_photo, btn_recorder, btn_pause_recording, btn_resume_recording;
    private TextView tv_time;
    private EditText et_cyclic_time;
    private boolean cyclicTest = false;

    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private MediaRecorder mMediaRecorder;

    private String outputFile = "/sdcard/DCIM/Test/";
    private boolean isRecording = false;
    private String videoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initView();
        addListener();
    }

    private void initView() {
        btn_start_preview = findViewById(R.id.btn_start_preview);
        btn_stop_preview = findViewById(R.id.btn_stop_preview);
        btn_preview_test = findViewById(R.id.btn_preview_test);
        btn_take_photo = findViewById(R.id.btn_take_photo);
        btn_recorder = findViewById(R.id.btn_recorder);
        btn_pause_recording = findViewById(R.id.btn_pause_recording);
        btn_resume_recording = findViewById(R.id.btn_resume_recording);
        tv_time = findViewById(R.id.tv_time);
        et_cyclic_time = findViewById(R.id.et_cyclic_time);

        mFrameLayout = findViewById(R.id.mFrameLayout);
        mFrameLayout.setLayoutParams(new RelativeLayout.LayoutParams(1080, 1080 * 1080 / 1920));
        //1像素，即无预览
        //mFrameLayout.setLayoutParams(new RelativeLayout.LayoutParams(1, 1));

        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(this);

        mFrameLayout.addView(mSurfaceView);

        File file = new File(outputFile);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    private void addListener() {
        mFrameLayout.setOnClickListener(this);
        btn_start_preview.setOnClickListener(this);
        btn_stop_preview.setOnClickListener(this);
        btn_preview_test.setOnClickListener(this);
        btn_take_photo.setOnClickListener(this);
        btn_recorder.setOnClickListener(this);
        btn_pause_recording.setOnClickListener(this);
        btn_resume_recording.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mFrameLayout://对焦
                if (mCamera != null) {
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean b, Camera camera) {
                            Log.e(TAG, "onAutoFocus: " + b);
                        }
                    });
                }
                break;

            case R.id.btn_start_preview://开始预览
                startPreview();
                startPreviewUI();
                break;

            case R.id.btn_stop_preview://停止预览
                stopPreview();
                stopPreviewUI();
                break;

            case R.id.btn_preview_test://循环开关预览测试
                cyclicTest = !cyclicTest;
                if (btn_preview_test.getText().toString().contains("循环开关预览测试")) {//开启测试
                    btn_preview_test.setText("关闭测试");
                    startOrStopTest(true);
                } else {//关闭测试
                    btn_preview_test.setText("循环开关预览测试");
                }
                break;

            case R.id.btn_take_photo://拍照
                if (mCamera != null) {
                    mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] bytes, Camera camera) {
                            try {
                                Bitmap a = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                String PictureName = Utils.getCurrentTimeString() + ".png";
                                String fileAddress = outputFile + PictureName;
                                FileOutputStream out = new FileOutputStream(new File(fileAddress));
                                a.compress(Bitmap.CompressFormat.PNG, 100, out);//压缩
                                Log.e(TAG, "onPictureTaken: picture have saved, Address = " + fileAddress);
                                scanFile(outputFile, PictureName);
                                mCamera.startPreview();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "onPictureTaken: " + e.getMessage());
                            }
                        }
                    });
                }
                break;

            case R.id.btn_recorder://录制
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
                break;

            case R.id.btn_pause_recording://暂停录制
                pauseRecording();
                break;

            case R.id.btn_resume_recording://继续录制
                resumeRecording();
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.e(TAG, "surfaceCreated()");
        if (mCamera != null) {
            try {
                Log.e(TAG, "mCamera.setPreviewDisplay(surfaceHolder)");
                mCamera.setPreviewDisplay(surfaceHolder);
            } catch (Exception e) {
                Log.e(TAG, "setPreviewDisplay() catch Exception: " + e);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.i(TAG, "surfaceChanged()");
        if (mCamera != null) {
            try {
                stopPreview();
                startPreview();
            } catch (Exception e) {
                Log.e(TAG, "setPreviewDisplay() catch Exception: " + e);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (isRecording) {
            stopRecording();
        }
        stopPreview();
    }

    //开启或关闭循环预览测试
    private void startOrStopTest(boolean b) {
        if (!b) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (cyclicTest) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (btn_start_preview.isEnabled()) {
                                btn_start_preview.performClick();
                            } else if (btn_stop_preview.isEnabled()) {
                                btn_stop_preview.performClick();
                            }
                        }
                    });

                    try {
                        Thread.sleep(TextUtils.isEmpty(et_cyclic_time.getText().toString()) ? 3 * 1000 : Integer.parseInt(et_cyclic_time.getText().toString()) * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "btn_start_preview -> performClick");
                btn_start_preview.performClick();
            }
        }, 150);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cyclicTest = false;
        btn_preview_test.setText("循环开关预览测试");
        btn_stop_preview.performClick();
    }

    private void startPreviewUI() {
        btn_start_preview.setEnabled(false);
        btn_stop_preview.setEnabled(true);
        btn_take_photo.setEnabled(true);
        btn_recorder.setEnabled(!cyclicTest);
        btn_pause_recording.setEnabled(false);
        btn_resume_recording.setEnabled(false);
        tv_time.setVisibility(View.INVISIBLE);
    }

    private void stopPreviewUI() {
        btn_start_preview.setEnabled(true);
        btn_stop_preview.setEnabled(false);
        btn_take_photo.setEnabled(false);
        btn_recorder.setEnabled(false);
        btn_pause_recording.setEnabled(false);
        btn_resume_recording.setEnabled(false);
        tv_time.setVisibility(View.INVISIBLE);

    }

    private void startRecordingUI() {
        btn_start_preview.setEnabled(false);
        btn_stop_preview.setEnabled(false);
        btn_preview_test.setEnabled(false);
        btn_recorder.setText("停止录制");
        btn_pause_recording.setEnabled(true);
        btn_resume_recording.setEnabled(false);
        tv_time.setVisibility(View.VISIBLE);
        startTiming();
    }

    private void stopRecordingUI() {
        btn_start_preview.setEnabled(false);
        btn_stop_preview.setEnabled(true);
        btn_preview_test.setEnabled(true);
        btn_recorder.setText("开始录制");
        btn_pause_recording.setEnabled(false);
        btn_resume_recording.setEnabled(false);
        tv_time.setVisibility(View.INVISIBLE);
        tv_time.setText("00:00:00");
    }

    private void pauseRecordingUI() {
        btn_start_preview.setEnabled(false);
        btn_stop_preview.setEnabled(false);
        btn_preview_test.setEnabled(false);
        btn_recorder.setText("停止录制");
        btn_pause_recording.setEnabled(false);
        btn_resume_recording.setEnabled(true);
        tv_time.setVisibility(View.VISIBLE);
    }

    private void errorRecordingUI() {
        btn_start_preview.setEnabled(false);
        btn_stop_preview.setEnabled(false);
        btn_preview_test.setEnabled(false);
        btn_take_photo.setEnabled(false);
        btn_recorder.setEnabled(false);
        btn_pause_recording.setEnabled(false);
        btn_resume_recording.setEnabled(false);
        tv_time.setVisibility(View.INVISIBLE);
    }

    //计时方法
    private int timeCount;

    private void startTiming() {
        timeCount = 0;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || !isRecording) return;

                timeCount++;

                String[] existingTime = tv_time.getText().toString().split(":");
                int existingHour = Integer.parseInt(existingTime[0]) * 3600;
                int existingMinute = Integer.parseInt(existingTime[1]) * 60;
                int existingSecond = Integer.parseInt(existingTime[2]);

                int hour = (existingHour + timeCount) / 3600;
                int minute = (existingHour + existingMinute + existingSecond + timeCount - hour * 3600) / 60;
                int second = existingHour + existingMinute + existingSecond + timeCount - hour * 3600 - minute * 60;

                tv_time.setText(String.format("%02d:%02d:%02d", hour, minute, second));

                startTiming();
            }
        }, 1000);
    }

    //最大文件大小和最长持续时间的监听
    @Override
    public void onInfo(MediaRecorder mediaRecorder, int i, int i1) {
        if (i == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {//最大文件大小
            Log.e(TAG, "onInfo: MediaRecorder max filesize reached!");
            stopRecordingUI();
            scanFile(outputFile, videoName);
        } else if (i == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {//最长持续时间
            Log.e(TAG, "onInfo: MediaRecorder max duration reached!");
            stopRecordingUI();
            scanFile(outputFile, videoName);
        }
    }

    public void startPreview() {
        try {
            Log.e(TAG, "Camera.open()");
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            Log.e(TAG, "Camera.open() catch Exception: " + e);
        }

        if (mCamera != null) {
            try {
                Log.e(TAG, "mCamera.getParameters()");
                Camera.Parameters parameter = mCamera.getParameters();
                Log.e(TAG, "setFocusMode()");
                parameter.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);//连续自动对焦视频
                Log.e(TAG, "setPreviewSize(): " + 1920 + "x" + 1080);
                parameter.setPreviewSize(1920, 1080);
                Log.e(TAG, "setPictureSize(): " + 1920 + "x" + 1080);
                parameter.setPictureSize(1920, 1080);
                parameter.setJpegThumbnailSize(0, 0);
                Log.e(TAG, "mCamera.setParameters()");
                mCamera.setParameters(parameter);
            } catch (Exception e) {
                Log.e(TAG, "mCamera.setParameters() catch Exception: " + e);
            }
        }

        if (mCamera != null) {
            try {
                Log.e(TAG, "mCamera.setPreviewDisplay()");
                mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            } catch (Exception e) {
                Log.e(TAG, "setPreviewDisplay() catch Exception: " + e);
            }
        }

        if (mCamera != null) {
            try {
                Log.e(TAG, "mCamera.startPreview()");
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "mCamera.startPreview() catch Exception: " + e);
            }
        }
    }

    public void stopPreview() {
        try {
            if (mCamera != null) {
                Log.e(TAG, "mCamera.stopPreview()");
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "mCamera.stopPreview() catch Exception, e: " + e.getMessage());
        }
    }

    public void startRecording() {
        if (mCamera == null) return;

        // initialize video camera
        if (prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared, now you can start recording
            mMediaRecorder.start();
            isRecording = true;

            // inform the user that recording has started
            startRecordingUI();
        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // inform user
            errorRecordingUI();
        }
    }

    public void stopRecording() {
        // stop recording and release camera
        mMediaRecorder.stop();  // stop the recording
        releaseMediaRecorder(); // release the MediaRecorder object
        mCamera.lock();         // take camera access back from MediaRecorder
        isRecording = false;

        // inform the user that recording has stopped
        stopRecordingUI();
        scanFile(outputFile, videoName);
    }

    public void pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.pause();
            isRecording = false;
            pauseRecordingUI();
        }
    }

    public void resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.resume();
            isRecording = true;
            startRecordingUI();
        }
    }

    private boolean prepareVideoRecorder() {
        if (mMediaRecorder == null) mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        //方式一：统一设置
        //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        //使用CamcorderProfile做配置的话，输出格式，音频编码，视频编码 不需要写
        //方式二：自定义设置。设置输出格式和编码格式(针对低于API Level 8版本)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //设置输出格式，.THREE_GPP为3gp，.MPEG_4为mp4
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);//设置声音编码类型
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//设置视频编码类型，一般h263，h264
        mMediaRecorder.setVideoSize(1920, 1080);//设置视频分辨率，设置错误调用start()时会报错，可注释掉再运行程序测试，有时注释掉可以运行
        mMediaRecorder.setVideoFrameRate(30);//设置视频帧率，需要Camera支持，不然报错
        mMediaRecorder.setVideoEncodingBitRate(20 * 1024 * 1024);//设置录制的视频编码比特率，录像模糊，花屏，绿屏可写上调试
        //mMediaRecorder.setMaxFileSize(500 * 1024 * 1024);//最大文件大小 500MB 需要实现MediaRecorder.OnInfoListener来监听
        mMediaRecorder.setMaxDuration(30 * 60 * 1000);//最长持续时间 30分钟 需要实现MediaRecorder.OnInfoListener来监听
        mMediaRecorder.setOnInfoListener(this);

        // Step 4: Set output file
        videoName = Utils.getCurrentTimeString() + ".mp4";
        mMediaRecorder.setOutputFile(outputFile + videoName);

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    public void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    //更新相册
    public void scanFile(String filePath, String fileName) {
        //把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(mContext.getContentResolver(), filePath, fileName, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //通知图库更新
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath + "/" + fileName))));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cyclicTest = false;
    }

}
