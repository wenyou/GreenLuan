package cn.stkit.greenluan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import cn.stkit.greenluan.service.LocationTrackingService;
import cn.stkit.greenluan.service.AppUsageTrackingService;
import cn.stkit.greenluan.service.CommandProcessingService;

/**
 * 启动接收器
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "GreenLuanBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals("android.intent.action.QUICKBOOT_POWERON") ||
                action.equals("com.htc.intent.action.QUICKBOOT_POWERON"))) {

            Log.d(TAG, "Device booted, starting monitoring services");

            // 启动位置跟踪服务
            Intent locationServiceIntent = new Intent(context, LocationTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(locationServiceIntent);
            } else {
                context.startService(locationServiceIntent);
            }

            // 启动应用使用跟踪服务
            Intent appUsageServiceIntent = new Intent(context, AppUsageTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(appUsageServiceIntent);
            } else {
                context.startService(appUsageServiceIntent);
            }

            // 启动命令处理服务
            Intent commandServiceIntent = new Intent(context, CommandProcessingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(commandServiceIntent);
            } else {
                context.startService(commandServiceIntent);
            }
        }
    }
}
