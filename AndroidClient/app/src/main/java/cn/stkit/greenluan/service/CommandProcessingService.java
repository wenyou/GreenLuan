package cn.stkit.greenluan.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import cn.stkit.greenluan.App;
import cn.stkit.greenluan.MainActivity;
import cn.stkit.greenluan.R;
import cn.stkit.greenluan.activity.OverlayMessageActivity;
import cn.stkit.greenluan.util.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

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
    private static final long CHECK_INTERVAL = 30 * 1000; // 30秒检查一次
    private Timer commandCheckTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(3, createNotification());
        startCommandCheckTimer();
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
    }

    private void startCommandCheckTimer() {
        commandCheckTimer = new Timer();
        commandCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForCommands();
            }
        }, 0, CHECK_INTERVAL);
    }

    private void checkForCommands() {
        String serverUrl = "https://your-server-api.com/commands/check";

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
    }

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

    private void showOverlayMessage(String message) {
        Intent intent = new Intent(this, OverlayMessageActivity.class);
        intent.putExtra("message", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void takeScreenshot() {
        Intent screenshotIntent = new Intent(this, ScreenshotService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(screenshotIntent);
        } else {
            startService(screenshotIntent);
        }
    }

    private void blockApp(String packageName) {
        // 实现应用屏蔽逻辑
        Log.d(TAG, "Blocking app: " + packageName);
        // 可以使用AccessibilityService实现
    }

    private void unblockApp(String packageName) {
        // 实现应用解除屏蔽逻辑
        Log.d(TAG, "Unblocking app: " + packageName);
    }

    private void markCommandAsProcessed(String commandId) {
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("commandId", commandId);
            requestData.put("status", "processed");

            String serverUrl = "https://your-server-api.com/commands/markprocessed";
            HttpUtils.sendPostRequest(serverUrl, requestData.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error marking command as processed: " + e.getMessage());
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

        return new NotificationCompat.Builder(this, App.COMMAND_CHANNEL_ID)
                .setContentTitle("Command Processing")
                .setContentText("Listening for server commands")
                .setSmallIcon(R.drawable.ic_command)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
