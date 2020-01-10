package com.zhouchao.test.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 公共工具类
 * Created by 周超 on 2017/8/25.
 */
public class Utils {
    private static Toast toast;//Toast单例模式

    //短吐司
    public static void toast(final Context context, final CharSequence text) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (toast == null) {
                        toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                    } else {
                        toast.setText(text);
                        toast.setDuration(Toast.LENGTH_SHORT);
                    }
                    toast.show();
                }
            });
        }
    }

    //长吐司
    public static void l_toast(final Context context, final CharSequence text) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (toast == null) {
                        toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                    } else {
                        toast.setText(text);
                        toast.setDuration(Toast.LENGTH_LONG);
                    }
                    toast.show();
                }
            });
        }
    }

    /**
     * 系统自带默认的确定|取消对话框
     *
     * @param context               context
     * @param title                 标题
     * @param message               内容
     * @param cancelable            点击屏幕外是否可取消
     * @param withCancelButton      是否带取消按钮
     * @param negativeButton        消极按钮文字
     * @param positiveButtonMessage 积极按钮文字
     * @param showDialogInterface   回调接口
     * @return AlertDialog
     */
    public static AlertDialog showAlertDialog(Context context, String title, String message, boolean cancelable, boolean withCancelButton, String negativeButton, String positiveButtonMessage, final ShowDialogInterface showDialogInterface) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Html.fromHtml(title));
        builder.setMessage(Html.fromHtml(message));
        builder.setCancelable(cancelable);
        builder.setPositiveButton(Html.fromHtml(positiveButtonMessage), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialogInterface.setEnsureButton();
            }
        });
        if (withCancelButton) {
            builder.setNegativeButton(Html.fromHtml(negativeButton), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showDialogInterface.setCancelButton();
                }
            });
        }
        return builder.show();
    }

    /**
     * 系统对话框回调接口
     */
    public interface ShowDialogInterface {
        //确定按钮
        void setEnsureButton();

        //取消按钮
        void setCancelButton();
    }

    /**
     * DP转像素
     */
    public static int dp_to_px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) (dpValue * (scale / 160) + 0.5f);
    }

    /**
     * 像素转DP
     */
    public static int px_to_dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) ((pxValue * 160) / scale + 0.5f);
    }

    /**
     * 获取屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 获取屏幕高度
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    /**
     * 获得状态栏（通知栏）的高度
     */
    public static int getStatusHeight(Context context) {
        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    /**
     * 屏幕背景透明度，0.0-1.0之间就是黑色的透明值，值越大透明度越小
     */
    public static void bgAlpha(Context context, float bgAlpha) {
        WindowManager.LayoutParams lp = ((Activity) context).getWindow().getAttributes();
        lp.alpha = bgAlpha;
        ((Activity) context).getWindow().setAttributes(lp);
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    /**
     * 获得当前时间的字符串（如："20161206142603000"，HH大写表示24小时制）
     */
    public static String getCurrentTimeString() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * 根据毫秒值，获得年-月-日字符串（如："2016-06-28"）
     */
    public static String getYearMonthDayString(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(time);
    }

    /**
     * 金钱格式化。千分位格式化+最多保留2位小数+不四舍五入（如：65562.1578 → 65,562.15）
     * ","代表分隔符，"."后面的##代表保留位数，如果换成0，效果就是位数不足0补齐
     */
    public static String getFormatMoney1(String money) {
        BigDecimal b = new BigDecimal(money);
        //DecimalFormat d = new DecimalFormat("#,###.##");//四舍五入
        DecimalFormat d = new DecimalFormat("#,##0.##");//不四舍五入，要设置舍入模式
        d.setRoundingMode(RoundingMode.FLOOR);
        return d.format(b);
    }

    /**
     * 金钱格式化。最多保留2位小数+不四舍五入（如：5.6872 → 5.68）
     */
    public static String getFormatMoney2(String money) {
        BigDecimal b = new BigDecimal(money);
        DecimalFormat d = new DecimalFormat("#0.##");
        d.setRoundingMode(RoundingMode.FLOOR);
        return d.format(b);
    }

    /**
     * 指定字符串中的某些字符变色
     *
     * @param content 原字符窜
     * @param change  需要变色的字符串
     * @param color   颜色值（String类型，如："\"#2693FF\""   注：双重引号，最外层引号也需要，内层引号需要转义）
     * @return 返回变色后的字符窜或null
     */
    public static String getColorString(String content, String change, String color) {
        if (content.contains(change)) {
            String colorChar = "<font color=" + color + ">" + change + "</font>";
            return content.replace(change, colorChar);
        }
        return null;
    }

    // 复制文件
    public static boolean fileCopy(String oldFilePath, String newFilePath) throws IOException {
        //如果原文件不存在
        if (!fileExists(oldFilePath)) {
            Log.e("MainActivityLenovo", "fileCopy 原文件不存在");
            return false;
        }
        //获得原文件流
        FileInputStream inputStream = new FileInputStream(new File(oldFilePath));
        byte[] data = new byte[1024];
        //输出流
        FileOutputStream outputStream = new FileOutputStream(new File(newFilePath));
        //开始处理流
        while (inputStream.read(data) != -1) {
            outputStream.write(data);
        }
        inputStream.close();
        outputStream.close();
        return true;
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * 删除后缀名为.zip的文件
     */
    private void delete(File f) {
        File[] fi = f.listFiles();
        if (fi == null) return;
        for (File file : fi) {
            if (!file.isDirectory() && file.getName().endsWith(".zip")) {
                file.delete();
                Log.e("MainActivity", "deleteFile: " + file.getName());
            }
        }
    }

    /**
     * 通过Uri获得bitmap
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try {
            // 读取uri所在的图片
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取当前运行栈中栈顶的activity，然后获取该activity的包名
    public static String getTopActivity(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            //获取正在运行的task列表，其中1表示最近运行的task，通过该数字限制列表中task数目，最近运行的靠前
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);

            if (runningTaskInfos != null && runningTaskInfos.size() != 0) {
                return (runningTaskInfos.get(0).baseActivity).getPackageName();
            }
        } catch (Exception e) {
            Log.e("MainActivityBeijing", "获取栈顶Activity异常：" + e.toString());
        }
        return "";
    }

    /**
     * 针对Android4.4以上从Uri获取文件绝对路径
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            //ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    //亦或加密与解密
    public static String encrypt(String str, int key) {
        char[] charArray = str.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = (char) (charArray[i] ^ key);
        }
        return String.valueOf(charArray);
    }

    //检测网络是否连接（包括 WiFi、2G、3G、4G、5G）
    private static boolean isNetworkConnected(Context mContext) {
        ConnectivityManager conMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr == null) return false;

        NetworkInfo activeInfo = conMgr.getActiveNetworkInfo();
        if(activeInfo != null){
            Log.e("chao", "getType: " + activeInfo.getType());
        }

        return activeInfo != null && activeInfo.getType() == ConnectivityManager.TYPE_WIFI && activeInfo.isConnected();
    }

    //获取Android可用运行内存大小
    public static int getAndroidRemianMemory(Context context) {
        int remainMemory = -1;
        try {
            Activity activity = (Activity) context;
            if (activity != null) {
                ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memoryInfo);
                    remainMemory = (int) (memoryInfo.availMem / 1024 / 1024);
                    Log.e("chao", "memoryInfo.totalMem: " + memoryInfo.totalMem + ", memoryInfo.availMem: " + memoryInfo.availMem + ", memoryInfo.lowMemory: " + memoryInfo.lowMemory);
                }
            }
        } catch (Exception e) {
            Log.e("chao", "GetAndroidRemianMemory catch Exception: " + e.toString());
        }
        return remainMemory;
    }
}
