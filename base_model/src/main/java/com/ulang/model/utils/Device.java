package com.ulang.model.utils;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TimeZone;

import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

public class Device {
    protected static final String Log_TAG = Device.class.getName();
    protected static final String UNKNOW = "Unknown";
    private static final String MOBILE_NETWORK = "2G/3G";
    private static final String WIFI = "Wi-Fi";
    public static final int DEFAULT_TIMEZONE = 8;

    private static int mScreenWidth = 0;
    private static int mScreenHeight = 0;
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    public static void init(Context context) {
        sContext = context;
    }

    public static int getAppVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String getAppVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    public static boolean checkPermission(Context context, String permName) {
        return context.getPackageManager().checkPermission(permName, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getDeviceId(Context context) {
        String deviceId = getImei(context);

        if (TextUtils.isEmpty(deviceId)) {
            deviceId = getMac(context);
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }
        return deviceId;
    }

    public static String getImei(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (tm != null && checkPermission(context, "android.permission.READ_PHONE_STATE")) {
            return tm.getDeviceId();
        } else {
            return "";
        }
    }

    public static String getMac(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wm != null && checkPermission(context, "android.permission.ACCESS_WIFI_STATE")) {
            return wm.getConnectionInfo().getMacAddress();
        } else {
            return "";
        }
    }

    public static String getChannel(Context context) {
        String channel = "Unknown";
        try {
            PackageManager manager = context.getPackageManager();
            ApplicationInfo info = manager.getApplicationInfo(context.getPackageName(), 128);

            if ((info != null) && (info.metaData != null)) {
                Object idObject = info.metaData.get("UMENG_CHANNEL");
                if (idObject != null) {
                    String id = idObject.toString();
                    if (id != null)
                        channel = id;
                    else {
                        // Log.i(//Log_TAG,
                        // "Could not read UMENG_CHANNEL meta-data from AndroidManifest.xml.");
                    }
                }
            }
        } catch (Exception e) {
            // Log.i(//Log_TAG,
            // "Could not read UMENG_CHANNEL meta-data from AndroidManifest.xml.");

            e.printStackTrace();
        }
        return channel;
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {

        if (mScreenWidth != 0) {
            return mScreenWidth;
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mScreenWidth = outMetrics.widthPixels;

        return mScreenWidth;
    }

    /**
     * 获得屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {

        if (mScreenHeight != 0) {
            return mScreenHeight;
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mScreenHeight = outMetrics.heightPixels;
        return mScreenHeight;
    }

    /**
     * 获得状态栏的高度
     *
     * @param context
     * @return
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
     * dp转px
     *
     * @param context
     * @param dpVal
     * @return
     */
    public static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, context.getResources().getDisplayMetrics());
    }

    public static int dp2px(DisplayMetrics displayMetrics, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, displayMetrics);
    }

    /**
     * sp转px
     *
     * @param context
     * @param spVal
     * @return
     */
    public static int sp2px(Context context, float spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spVal, context.getResources().getDisplayMetrics());
    }

    /**
     * px转dp
     *
     * @param context
     * @param pxVal
     * @return
     */
    public static float px2dp(Context context, float pxVal) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (pxVal / scale);
    }

    /**
     * px转sp
     *
     * @param context
     * @param pxVal
     * @return
     */
    public static float px2sp(Context context, float pxVal) {
        return (pxVal / context.getResources().getDisplayMetrics().scaledDensity);
    }

    /**
     * 是否为魅族flyme系统
     */
    public static boolean isFlyme() {
        try {
            // Invoke Build.hasSmartBar()
            final Method method = Build.class.getMethod("hasSmartBar");
            return method != null;
        } catch (final Exception e) {
            return false;
        }
    }

    public static boolean isMIUI() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        return manufacturer.equalsIgnoreCase("Xiaomi");
//        return true;
    }

    public static String getProcessName(Context cxt, int pid) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        /*returns your application package only
            http://stackoverflow.com/questions/30619349/android-5-1-1-and-above-getrunningappprocesses-returns-my-application-packag
        */
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    /**
     * Check if APP is installed
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean checkIfAppInstalled(Context context, String packageName, boolean shouldStart) {
        if (packageName == null || "".equals(packageName)) return false;

        // Try to get packageInfo & ApplicationInfo
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        if (shouldStart) {
            Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            context.startActivity(LaunchIntent);
        }
        return true;
    }

    public static final String SCALE_HIGH = "/resize_720x720_90";
    public static final String SCALE_MEDIUM = "/resize_640x640_90";
    public static final String SCALE_SMALL = "/resize_480x480_90";
    public static final String SCALE_TINY = "/resize_150x150_90";

    public static String getCover(Context context, String big_cover, boolean... isList) {
        if (TextUtils.isEmpty(big_cover) || big_cover.length() < 5) {
            return "";
        }
        String pattern = ".com";
        StringBuilder temp = new StringBuilder(big_cover);
        if (isList.length != 0 && isList[0]) {
            return temp.insert(temp.indexOf(pattern) + pattern.length(), SCALE_TINY).toString();
        }

        int screenW = getScreenWidth(context);
        if (screenW <= 720 && screenW > 480) {
            temp.insert(temp.indexOf(pattern) + pattern.length(), SCALE_MEDIUM);
        } else if (screenW <= 480) {
            temp.insert(temp.indexOf(pattern) + pattern.length(), SCALE_SMALL);
        } else {
            temp.insert(temp.indexOf(pattern) + pattern.length(), SCALE_HIGH);
        }
        return temp.toString();
    }

    public static final int SIZE_HIGH = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_SMALL = 2;

    @IntDef({SIZE_HIGH, SIZE_MEDIUM, SIZE_SMALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SizeRes {

    }

    public static String getCover(String big_cover, @SizeRes int size) {
        if (big_cover == null) return null;
        if (big_cover.length() < 5) return "";

        String pattern = ".com";
        StringBuilder temp = new StringBuilder(big_cover);
        String resizePattern = "/resize_%dx%d_90";

        int screenW = getScreenWidth(sContext);
        if (screenW <= 720 && screenW > 480) {
            switch (size) {
                case SIZE_HIGH:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 640, 640));
                    break;
                case SIZE_MEDIUM:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 640 / 2, 640 / 2));
                    break;
                case SIZE_SMALL:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 640 / 4, 640 / 4));
                    break;
            }
        } else if (screenW <= 480) {
            switch (size) {
                case SIZE_HIGH:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 480, 480));
                    break;
                case SIZE_MEDIUM:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 480 / 2, 480 / 2));
                    break;
                case SIZE_SMALL:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 480 / 4, 480 / 4));
                    break;
            }
        } else {
            switch (size) {
                case SIZE_HIGH:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 720, 720));
                    break;
                case SIZE_MEDIUM:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 720 / 2, 720 / 2));
                    break;
                case SIZE_SMALL:
                    temp.insert(temp.indexOf(pattern) + pattern.length(), String.format(resizePattern, 720 / 4, 720 / 4));
                    break;
            }
        }
        return temp.toString();
    }

    public static int getMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && SDK_INT >= HONEYCOMB) {
            memoryClass = am.getLargeMemoryClass();
        }
        // Target ~15% of the available heap.
        return 1024 * 1024 * memoryClass / 5;
    }

    public static final int SECONDS_IN_DAY = 60 * 60 * 24;
    public static final long MILLIS_IN_DAY = 1000L * SECONDS_IN_DAY;

    public static boolean isSameDayOfMillis(final long ms1, final long ms2) {
        final long interval = ms1 - ms2;
        return interval < MILLIS_IN_DAY
                && interval > -1L * MILLIS_IN_DAY
                && toDay(ms1) == toDay(ms2);
    }

    private static long toDay(long millis) {
        return (millis + TimeZone.getDefault().getOffset(millis)) / MILLIS_IN_DAY;
    }


    private static final double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

/*    public static void bd_encrypt(double gg_lat, double gg_lon, double &bd_lat, double &bd_lon)
    {
        double x = gg_lon, y = gg_lat;
        double z = sqrt(x * x + y * y) + 0.00002 * sin(y * x_pi);
        double theta = atan2(y, x) + 0.000003 * cos(x * x_pi);
        bd_lon = z * cos(theta) + 0.0065;
        bd_lat = z * sin(theta) + 0.006;
    }*/



}
