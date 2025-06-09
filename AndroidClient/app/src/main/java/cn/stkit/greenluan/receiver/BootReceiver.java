package cn.stkit.greenluan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import cn.stkit.greenluan.service.AppUsageTrackingService;
import cn.stkit.greenluan.service.CommandProcessingService;
import cn.stkit.greenluan.service.LocationTrackingService;

/**
 * 启动接收器 (开机启动)
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-8
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "GreenLuanBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals("android.intent.action.QUICKBOOT_POWERON") ||
                action.equals("com.htc.intent.action.QUICKBOOT_POWERON"))) {
        //if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) { // 使用上面条件判断代替此行 2025-6-9

            Log.d(TAG, "Device booted, GreenLuan starting monitoring services...");

            // 启动位置服务(启动位置跟踪服务)
            Intent locationIntent = new Intent(context, LocationTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(locationIntent);
            } else {
                context.startService(locationIntent);
            }

            // 启动应用使用监控服务(启动应用使用跟踪服务)
            Intent appUsageIntent = new Intent(context, AppUsageTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(appUsageIntent);
            } else {
                context.startService(appUsageIntent);
            }

            // 启动命令服务（启动命令处理服务）
            Intent commandIntent = new Intent(context, CommandProcessingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(commandIntent);
            } else {
                context.startService(commandIntent);
            }
        }
    }
}
