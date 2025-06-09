package cn.stkit.greenluan;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;

import cn.stkit.greenluan.config.ConfigConstants;
import cn.stkit.greenluan.service.AppUsageTrackingService;
import cn.stkit.greenluan.service.CommandProcessingService;
import cn.stkit.greenluan.service.LocationTrackingService;

/**
 * 应用程序入口
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class App extends Application {
    //主服务通知通道
    public static final String CHANNEL_ID = ConfigConstants.NOTIFICATION_CHANNEL_ID;
    //位置服务通知通道
    public static final String LOCATION_CHANNEL_ID = ConfigConstants.LOCATION_NOTIFICATION_CHANNEL_ID;
    //应用使用统计通知通道
    public static final String APP_USAGE_CHANNEL_ID = ConfigConstants.APP_USAGE_NOTIFICATION_CHANNEL_ID;
    //命令处理通知通道
    public static final String COMMAND_CHANNEL_ID = ConfigConstants.COMMAND_NOTIFICATION_CHANNEL_ID;
    //截图服务通知通道
    public static final String SCREENSHOT_CHANNEL_ID = ConfigConstants.SCREENSHOT_NOTIFICATION_CHANNEL_ID;

    @Override
    public void onCreate() {
        super.onCreate();
        //创建通知渠道
        //createNotificationChannels();
        //开始监控服务
        //startMonitoringServices();
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannels() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 主服务通知通道
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "GreenLuan's Student Monitoring Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription("GreenLuan's Background service for student monitoring");

            // 位置服务通知通道
            NotificationChannel locationChannel = new NotificationChannel(
                    LOCATION_CHANNEL_ID,
                    "GreenLuan's Location Tracking Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            locationChannel.setDescription("GreenLuan's Service for tracking device location");

            // 应用使用统计通知通道
            NotificationChannel appUsageChannel = new NotificationChannel(
                    APP_USAGE_CHANNEL_ID,
                    "GreenLuan's App Usage Tracking Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            appUsageChannel.setDescription("GreenLuan's Service for tracking app usage");

            // 命令处理通知通道
            NotificationChannel commandChannel = new NotificationChannel(
                    COMMAND_CHANNEL_ID,
                    "GreenLuan's Command Processing Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            commandChannel.setDescription("GreenLuan's Service for processing server commands");

            // 截图服务通知通道
            NotificationChannel screenshotChannel = new NotificationChannel(
                    SCREENSHOT_CHANNEL_ID,
                    "GreenLuan's Screenshot Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            screenshotChannel.setDescription("GreenLuan's Service for taking screenshots");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(locationChannel);
            manager.createNotificationChannel(appUsageChannel);
            manager.createNotificationChannel(commandChannel);
            manager.createNotificationChannel(screenshotChannel);
        }
    }

    //开始监控服务
    private void startMonitoringServices() {
        // 启动位置跟踪服务
        Intent locationServiceIntent = new Intent(this, LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationServiceIntent);
        } else {
            startService(locationServiceIntent);
        }

        // 启动应用使用跟踪服务
        Intent appUsageServiceIntent = new Intent(this, AppUsageTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(appUsageServiceIntent);
        } else {
            startService(appUsageServiceIntent);
        }

        // 启动命令处理服务
        Intent commandServiceIntent = new Intent(this, CommandProcessingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(commandServiceIntent);
        } else {
            startService(commandServiceIntent);
        }
    }

}
