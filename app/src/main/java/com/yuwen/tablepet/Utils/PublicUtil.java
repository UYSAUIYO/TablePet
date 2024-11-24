package com.yuwen.tablepet.Utils;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.yuwen.tablepet.FirstActivity;
import com.yuwen.tablepet.Service.FloatingService;

import java.util.ArrayList;
import java.util.List;

public class PublicUtil {
    public static final int REQUEST_FLOAT_CODE = 1001;

/**
 * 跳转到设置页面申请打开无障碍辅助功能
 */
private static void accessibilityToSettingPage(Context context) {
    try {
        // 开启辅助功能页面
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    } catch (Exception e) {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        e.printStackTrace();
    }
}

/**
 * 判断Service是否开启
 */
public static boolean isServiceRunning(Context context, String serviceName) {
    if (TextUtils.isEmpty(serviceName)) {
        return false;
    }
    ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ArrayList<ActivityManager.RunningServiceInfo> runningService =
            (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(1000);
    for (int i = 0; i < runningService.size(); i++) {
        if (runningService.get(i).service.getClassName().equals(serviceName)) {
            return true;
        }
    }
    return false;
}

/**
 * 判断悬浮窗权限权限
 */
private static boolean commonROMPermissionCheck(Context context) {
    boolean result = true;
    if (Build.VERSION.SDK_INT >= 23) {
        try {
            Class<?> clazz = Settings.class;
            java.lang.reflect.Method canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context.class);
            result = (Boolean) canDrawOverlays.invoke(null, context);
        } catch (Exception e) {
            Log.e("ServiceUtils", Log.getStackTraceString(e));
        }
    }
    return result;
}

/**
 * 检查悬浮窗权限是否开启
 */
public static void checkSuspendedWindowPermission(Activity context, Runnable block) {
    if (commonROMPermissionCheck(context)) {
        block.run();
    } else {
        Toast.makeText(context, "请开启悬浮窗权限", Toast.LENGTH_SHORT).show();
        context.startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData(Uri.parse("package:" + context.getPackageName())), REQUEST_FLOAT_CODE);
    }
}

/**
 * 检查无障碍服务权限是否开启
 */
public static void checkAccessibilityPermission(Activity context, Runnable block) {
    if (isServiceRunning(context, FloatingService.class.getCanonicalName())) {
        block.run();
    } else {
        accessibilityToSettingPage(context);
    }
}

/**
 * 判断是否为空
 */
public static boolean isNull(Object any) {
    return any == null;
}

}