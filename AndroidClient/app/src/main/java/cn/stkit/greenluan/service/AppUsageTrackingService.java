package cn.stkit.greenluan.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import cn.stkit.greenluan.App;
import cn.stkit.greenluan.MainActivity;
import cn.stkit.greenluan.R;
import cn.stkit.greenluan.util.HttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 应用使用跟踪服务
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class AppUsageTrackingService extends Service {

    private static final String TAG = "GreenLuanAppUsageService";
    private static final long CHECK_INTERVAL = 60 * 60 * 1000; // 每小时检查一次
    private static final long USAGE_STATS_PERIOD = 24 * 60 * 60 * 1000; // 24小时数据

    private UsageStatsManager usageStatsManager;
    private Timer usageCheckTimer;
    private PackageManager packageManager;

    @Override
    public void onCreate() {
        super.onCreate();
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        packageManager = getPackageManager();
        startForeground(2, createNotification());
        startUsageCheckTimer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (usageCheckTimer != null) {
            usageCheckTimer.cancel();
        }
        // 重启服务
        Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartIntent);
        } else {
            getApplicationContext().startService(restartIntent);
        }
    }

    private void startUsageCheckTimer() {
        usageCheckTimer = new Timer();
        usageCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAndSendAppUsageStats();
            }
        }, 0, CHECK_INTERVAL);
    }

    private void collectAndSendAppUsageStats() {
        try {
            Calendar calendar = Calendar.getInstance();
            long endTime = calendar.getTimeInMillis();
            long startTime = endTime - USAGE_STATS_PERIOD;

            // 获取应用使用统计
            Map<String, Long> appUsageTime = new HashMap<>();
            UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();
            String currentPackage = "";
            long currentTime = 0;

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    currentPackage = event.getPackageName();
                    currentTime = event.getTimeStamp();
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND && !currentPackage.isEmpty()) {
                    long usageTime = event.getTimeStamp() - currentTime;
                    appUsageTime.put(currentPackage, appUsageTime.getOrDefault(currentPackage, 0L) + usageTime);
                    currentPackage = "";
                }
            }

            // 处理仍在前台的应用
            if (!currentPackage.isEmpty()) {
                long usageTime = endTime - currentTime;
                appUsageTime.put(currentPackage, appUsageTime.getOrDefault(currentPackage, 0L) + usageTime);
            }

            // 转换为JSON并发送到服务器
            JSONArray usageArray = new JSONArray();
            for (Map.Entry<String, Long> entry : appUsageTime.entrySet()) {
                String packageName = entry.getKey();
                long usageTime = entry.getValue();

                if (usageTime > 0) {
                    JSONObject appUsage = new JSONObject();
                    appUsage.put("packageName", packageName);
                    appUsage.put("usageTime", usageTime);
                    appUsage.put("appName", getAppName(packageName));
                    usageArray.put(appUsage);
                }
            }

            JSONObject finalData = new JSONObject();
            finalData.put("appUsageStats", usageArray);
            finalData.put("timestamp", System.currentTimeMillis());

            // 发送数据到服务器
            String serverUrl = "https://your-server-api.com/appusage/upload";
            HttpUtils.sendPostRequest(serverUrl, finalData.toString());
            Log.d(TAG, "App usage data sent to server");

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error sending app usage data: " + e.getMessage());
        }
    }

    private String getAppName(String packageName) {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(applicationInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, App.APP_USAGE_CHANNEL_ID)
                .setContentTitle("App Usage Tracking")
                .setContentText("Monitoring app usage statistics")
                .setSmallIcon(R.drawable.ic_app_usage)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
