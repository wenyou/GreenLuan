package cn.stkit.greenluan.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;

import cn.stkit.greenluan.App;
import cn.stkit.greenluan.MainActivity;
import cn.stkit.greenluan.R;
import cn.stkit.greenluan.activity.OverlayMessageActivity;
import cn.stkit.greenluan.network.ApiClient;
import cn.stkit.greenluan.util.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.stkit.greenluan.util.ScreenshotUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 命令执行服务
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class CommandProcessingService extends Service {

    private static final String TAG = "GreenLuanCommandService";
    private static final int NOTIFICATION_ID = 103;
    private static final String NOTIFICATION_CHANNEL_ID = "greenLuan's_command_service_channel";
    // 检查命令间隔（秒）
    private static final long COMMAND_CHECK_INTERVAL = 30;

    private WindowManager windowManager;
    private View overlayView;
    private ScheduledExecutorService executorService;
    private Gson gson;

    //old code
    private static final long CHECK_INTERVAL = 30 * 1000; // 30秒检查一次
    private Timer commandCheckTimer;
    // end old code


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GreenLuan's CommandService created");

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        gson = new Gson();

        // 启动定时检查命令任务
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::checkForCommands,
                0, COMMAND_CHECK_INTERVAL, TimeUnit.SECONDS);

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());

        //old code
        /*
        startForeground(3, createNotification());
        startCommandCheckTimer();
        */
        //end old code
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "GreenLuan's CommandService started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CommandService destroyed");
        if (executorService != null) {
            executorService.shutdown();
        }

        // 移除覆盖视图
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay view: " + e.getMessage());
            }
        }

        // 重启服务
        Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
    }

    /* old code
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commandCheckTimer != null) {
            commandCheckTimer.cancel();
        }
        // 重启服务
        Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartIntent);
        } else {
            getApplicationContext().startService(restartIntent);
        }
    }*/

    //检查命令
    private void checkForCommands() {
        Log.d(TAG, "Checking for new commands from GreenLuan's server");

        ApiClient.getInstance(this).fetchCommands(new ApiClient.Callback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Received commands: " + response);
                // 解析命令并执行
                processCommands(response);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch commands: " + error);
            }
        });
    }

    /**
     * 解析命令并执行
     * @param commandJson
     */
    private void processCommands(String commandJson) {
        // 解析命令（简化实现）
        // 实际应用中应根据服务器返回的JSON格式解析具体命令

        if (commandJson.contains("SHOW_MESSAGE")) {
            // 显示消息命令
            String message = extractMessageFromCommand(commandJson);
            showOverlayMessage(message);
        } else if (commandJson.contains("TAKE_SCREENSHOT")) {
            // 截屏命令
            takeScreenshotAndUpload();
        }
    }

    //从命令中提取消息
    private String extractMessageFromCommand(String commandJson) {
        // 解析消息内容（简化实现）
        return "收到新消息: " + commandJson;
    }

    //显示覆盖消息
    private void showOverlayMessage(String message) {
        // 移除之前的覆盖视图
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                Log.e(TAG, "Error removing previous overlay view: " + e.getMessage());
            }
        }

        // 创建新的覆盖视图
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_message, null);
        TextView messageTextView = overlayView.findViewById(R.id.message_text);
        messageTextView.setText(message);

        // 设置窗口参数
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 100; // 距离顶部的偏移量

        // 添加视图到窗口
        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            Log.e(TAG, "Error adding overlay view: " + e.getMessage());
        }

        // 设置自动消失
        overlayView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (overlayView != null) {
                    try {
                        windowManager.removeView(overlayView);
                        overlayView = null;
                    } catch (Exception e) {
                        Log.e(TAG, "Error removing overlay view: " + e.getMessage());
                    }
                }
            }
        }, 5000); // 5秒后消失
    }

    /**
     * 截图并上传
     */
    private void takeScreenshotAndUpload() {
        Log.d(TAG, "GreenLuan Taking screenshot...");

        ScreenshotUtils.takeScreenshot(this, new ScreenshotUtils.ScreenshotCallback() {
            @Override
            public void onScreenshotTaken(Bitmap bitmap) {
                Log.d(TAG, "GreenLuan's Screenshot taken successfully");
                // 上传截图到服务器
                uploadScreenshot(bitmap);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "GreenLuan Failed to take screenshot: " + error);
                // 通知服务器截图失败
                sendScreenshotError(error);
            }
        });
    }

    //上传截图到服务器
    private void uploadScreenshot(Bitmap bitmap) {
        ApiClient.getInstance(this).uploadScreenshot(bitmap, new ApiClient.Callback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "GreenLuan's Screenshot uploaded successfully");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "GreenLuan Failed to upload screenshot: " + error);
            }
        });
    }

    //通知服务器截图失败
    private void sendScreenshotError(String error) {
        // 通知服务器截图失败
    }

    //创建通知
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("GreenLuan's Screenshot Command receiving service")//命令接收服务
                .setContentText("Waiting for the GreenLuanServer's instructions")//正在等待服务器指令
                .setSmallIcon(R.drawable.ic_screenshot)//ic_notification
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "GreenLuan's Screenshot Command receiving service",//命令接收服务
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return builder.build();
    }



    //old code
    private void startCommandCheckTimer() {
        commandCheckTimer = new Timer();
        commandCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForCommands();
            }
        }, 0, CHECK_INTERVAL);
    }

    /*
    //old code
    private void checkForCommands() {
        String serverUrl = "https://greenluan.com/commands/check";

        HttpUtils.sendGetRequest(serverUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch commands: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject commandJson = new JSONObject(responseData);
                        processCommand(commandJson);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing command JSON: " + e.getMessage());
                    }
                }
            }
        });
    }*/

    //old code
    private void processCommand(JSONObject command) {
        try {
            String commandType = command.getString("type");
            Log.d(TAG, "Received command: " + commandType);

            switch (commandType) {
                case "SHOW_MESSAGE":
                    String message = command.getString("message");
                    showOverlayMessage(message);
                    break;

                case "TAKE_SCREENSHOT":
                    takeScreenshot();
                    break;

                case "BLOCK_APP":
                    String packageName = command.getString("packageName");
                    blockApp(packageName);
                    break;

                case "UNBLOCK_APP":
                    String packageToUnblock = command.getString("packageName");
                    unblockApp(packageToUnblock);
                    break;

                default:
                    Log.d(TAG, "Unknown command type: " + commandType);
            }

            // 标记命令已处理
            markCommandAsProcessed(command.getString("commandId"));

        } catch (JSONException e) {
            Log.e(TAG, "Error processing command: " + e.getMessage());
        }
    }

    /*
    //old code
    private void showOverlayMessage(String message) {
        Intent intent = new Intent(this, OverlayMessageActivity.class);
        intent.putExtra("message", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    */

    //old code
    private void takeScreenshot() {
        Intent screenshotIntent = new Intent(this, ScreenshotService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(screenshotIntent);
        } else {
            startService(screenshotIntent);
        }
    }

    //old code
    private void blockApp(String packageName) {
        // 实现应用屏蔽逻辑
        Log.d(TAG, "Blocking app: " + packageName);
        // 可以使用AccessibilityService实现
    }

    //old code
    private void unblockApp(String packageName) {
        // 实现应用解除屏蔽逻辑
        Log.d(TAG, "Unblocking app: " + packageName);
    }

    //old code
    private void markCommandAsProcessed(String commandId) {
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("commandId", commandId);
            requestData.put("status", "processed");

            String serverUrl = "https://greenluan.com/commands/markprocessed";
            HttpUtils.sendPostRequest(serverUrl, requestData.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error marking command as processed: " + e.getMessage());
        }
    }

    /*
    //old code
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, App.COMMAND_CHANNEL_ID)
                .setContentTitle("Command Processing")
                .setContentText("Listening for server commands")
                .setSmallIcon(R.drawable.ic_command)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }*/
}
