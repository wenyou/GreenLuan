package cn.stkit.greenluan.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;

import cn.stkit.greenluan.App;
import cn.stkit.greenluan.MainActivity;
import cn.stkit.greenluan.R;
import cn.stkit.greenluan.config.ConfigConstants;
import cn.stkit.greenluan.data.AppUsageData;
import cn.stkit.greenluan.network.ApiClient;
import cn.stkit.greenluan.util.HttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 应用使用跟踪服务 (使用监控)
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class AppUsageTrackingService extends Service {

    private static final String TAG = "GreenLuanAppUsageService";
    //通知相关
    private static final int NOTIFICATION_ID = ConfigConstants.APP_USAGE_NOTIFICATION_ID;//102, 通知ID
    private static final String NOTIFICATION_CHANNEL_ID = ConfigConstants.APP_USAGE_NOTIFICATION_CHANNEL_ID;//应用使用统计通知通道，渠道ID，"greenLuan's_app_usage_channel";
    // 应用使用统计间隔（分钟）
    private static final long STATISTICS_INTERVAL = ConfigConstants.APP_USAGE_STATISTICS_INTERVAL;

    private UsageStatsManager usageStatsManager;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private ScheduledExecutorService executorService;
    private Map<String, Long> appUsageTimes;
    private long lastStatsTime;

    //old code
    private static final long CHECK_INTERVAL = 60 * 60 * 1000; // 每小时检查一次
    private static final long USAGE_STATS_PERIOD = 24 * 60 * 60 * 1000; // 24小时数据
    private Timer usageCheckTimer;
    private PackageManager packageManager;
    //end code

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GreenLuan's AppUsageService created");
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        sharedPreferences = getSharedPreferences("greenluan_monitoring", MODE_PRIVATE);
        gson = new Gson();
        appUsageTimes = new HashMap<>();
        lastStatsTime = System.currentTimeMillis();

        // 启动定时统计任务
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::collectAndUploadAppUsage,
                STATISTICS_INTERVAL, STATISTICS_INTERVAL, TimeUnit.MINUTES);

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());

        //old code
        /*usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        packageManager = getPackageManager();
        startForeground(2, createNotification());
        startUsageCheckTimer();*/
        //end old
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "GreenLuan's AppUsageService started");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "GreenLuan's AppUsageService destroyed");
        if (executorService != null) {
            executorService.shutdown();
        }

        // 重启服务
        Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
    }

    /*old code
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
    }*/

    //收集并上传应用程序使用情况
    private void collectAndUploadAppUsage() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (STATISTICS_INTERVAL * 60 * 1000);

        Log.d(TAG, "Collecting app usage data from " + startTime + " to " + endTime);

        // 获取应用使用事件
        UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();

        // 重置应用使用时间
        appUsageTimes.clear();

        String currentPackage = "";
        long currentTime = 0;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentPackage = event.getPackageName();
                currentTime = event.getTimeStamp();
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND &&
                    !currentPackage.isEmpty()) {
                long duration = event.getTimeStamp() - currentTime;
                if (duration > 0) {
                    appUsageTimes.put(currentPackage,
                            appUsageTimes.getOrDefault(currentPackage, 0L) + duration);
                }
                currentPackage = "";
            }
        }

        // 上传应用使用数据
        uploadAppUsageData(appUsageTimes, startTime, endTime);

        // 更新上次统计时间
        lastStatsTime = endTime;
    }

    //上传应用程序使用数据
    private void uploadAppUsageData(Map<String, Long> usageTimes, long startTime, long endTime) {
        // 转换为可上传的数据格式
        List<AppUsageData> usageDataList = new ArrayList<>();

        for (Map.Entry<String, Long> entry : usageTimes.entrySet()) {
            AppUsageData data = new AppUsageData(
                    entry.getKey(),
                    entry.getValue(),
                    startTime,
                    endTime
            );
            usageDataList.add(data);
        }

        // 上传到服务器
        ApiClient.getInstance(this).uploadAppUsage(usageDataList, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "App usage data uploaded successfully");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to upload app usage data: " + error);
                // 上传失败，保存到本地待重试
                saveAppUsageForRetry(usageDataList);
            }
        });
    }

    //实现本地存储待上传的应用使用数据
    private void saveAppUsageForRetry(List<AppUsageData> usageDataList) {
        // 实现本地存储待上传的应用使用数据
    }

    /**
     * 创建通知
     * 应用使用统计通知通道
     * @return
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("GreenLuan's Application usage monitoring service")//应用使用监控服务
                .setContentText("GreenLuan is monitoring the usage of the application")//正在监控应用使用情况
                .setSmallIcon(R.drawable.ic_app_usage)//R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 应用使用统计通知通道
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "GreenLuan's Application usage monitoring service",//应用使用监控服务
                    NotificationManager.IMPORTANCE_LOW //NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("GreenLuan's Service for tracking app usage"); //Service for GreenLuan's tracking app usage

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return builder.build();
    }


    //old code
    private void startUsageCheckTimer() {
        usageCheckTimer = new Timer();
        usageCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAndSendAppUsageStats();
            }
        }, 0, CHECK_INTERVAL);
    }

    //old code
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
            String serverUrl = "https://greenluan.com/appusage/upload";
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

    /* old code
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
    }*/
}
