package cn.stkit.greenluan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import cn.stkit.greenluan.service.AppUsageTrackingService;
import cn.stkit.greenluan.service.CommandProcessingService;
import cn.stkit.greenluan.service.LocationTrackingService;


/**
 * App主界面Activity
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GreenLuanMainActivity";
    //请求码相关
    private static final int PERMISSION_REQUEST_CODE = 1001;//权限请求的自定义请求码（整数常量），用于标识不同的权限请求场景
    //用于标识权限请求的类型
    private static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 1002;//使用情况统计权限（Usage Stats Permission） 的请求码
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1003;// 悬浮窗权限（SYSTEM_ALERT_WINDOW） 的自定义请求码
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1004;// 位置权限的自定义请求码
    // 通知相关
    private static final String NOTIFICATION_CHANNEL_ID = "greenluan_monitoring_channel";
    private static final int NOTIFICATION_ID = 100;

    // 存储偏好
    private SharedPreferences sharedPreferences;
    private Gson gson;

    //old code
    private static final int REQUEST_CODE_USAGE_STATS = 102;
    private static final int REQUEST_CODE_OVERLAY = 103;
    private static final int REQUEST_CODE_SCREENSHOT = 104;
    private static final int REQUEST_CODE_BACKGROUND_LOCATION = 105;

    private TextView statusTextView;
    //end old code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //设置MainActivity标题栏标题
        setTitle(R.string.main_activity_title);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 初始化组件
        initComponents();

        // 初始化存储和工具
        sharedPreferences = getSharedPreferences("greenluan_monitoring", MODE_PRIVATE);
        gson = new Gson();

        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();
        // 检查并请求权限
        checkAndRequestPermissions();

        // 启动服务
        startMonitoringServices();

        // 隐藏主界面（仅用于初始设置）
        finish();

        //old code
        //statusTextView = findViewById(R.id.status_text);
        //Button checkPermissionsButton = findViewById(R.id.check_permissions_button);
        //checkPermissionsButton.setOnClickListener(v -> checkAndRequestPermissions());
        //启动服务
        //startServices();
        //end old code

        // 显示状态  old code
        //updateStatus();
    }

    //初始化组件
    private void initComponents() {
        // 设置按钮点击事件（如果需要UI交互）
        Button exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRestrictedDialog();
            }
        });
    }

    //创建通知渠道（Android 8.0+）
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "青鸾信学生监控服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("青鸾信监控学生手机使用情况和位置");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    //检查并请求权限
    private void checkAndRequestPermissions() {
        //所需权限列表
        List<String> permissionsNeeded = new ArrayList<>();
        //boolean allPermissionsGranted = true;//old code

        // 检查位置权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);

            //old code
            /*ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            allPermissionsGranted = false;
            */
            //end old code
        }

        // 检查系统警告窗口权限（用于覆盖UI） 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);

            //showOverlayPermissionDialog();
            //allPermissionsGranted = false;
        }

        // 检查应用使用情况权限 检查应用使用统计权限
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_STATS_PERMISSION_REQUEST_CODE);
            //showUsageStatsPermissionDialog();
            //allPermissionsGranted = false;
        }

        // 检查存储权限（用于截图）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Android 13（API 33）及以上
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            //Android 12（API 31）及以下
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        /*
        //old code
        // 检查后台位置权限 (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_CODE_BACKGROUND_LOCATION
            );
            allPermissionsGranted = false;
        }

        // 检查无障碍服务权限
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog();
            allPermissionsGranted = false;
        }
        // 检查截图权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !hasScreenshotPermission()) {
            showScreenshotPermissionDialog();
            allPermissionsGranted = false;
        }

        //end old
        */

        // 显示 请求权限 检查提示
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }

        //updateStatus();

        /* old code
        if (allPermissionsGranted) {
            Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show();
        }*/
    }

    //检查应用使用情况权限 检查应用使用统计权限
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    //根据请求获取权限结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                showRestrictedDialog();
            }
        }
    }

    //在“活动结果”中
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USAGE_STATS_PERMISSION_REQUEST_CODE) {
            if (!hasUsageStatsPermission()) {
                showRestrictedDialog();
            }
        } else if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                showRestrictedDialog();
            }
        }
    }

    /**
     * 启动监控服务
     */
    private void startMonitoringServices() {
        // 启动位置服务
        Intent locationIntent = new Intent(this, LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationIntent);
        } else {
            startService(locationIntent);
        }

        // 启动应用使用监控服务
        Intent appUsageIntent = new Intent(this, AppUsageTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(appUsageIntent);
        } else {
            startService(appUsageIntent);
        }

        // 启动命令接收服务
        Intent commandIntent = new Intent(this, CommandProcessingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(commandIntent);
        } else {
            startService(commandIntent);
        }
    }

    //显示受限制提示框
    private void showRestrictedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("功能限制")
                .setMessage("为了保证青鸾信学生监控功能正常运行，需要授予所有请求的权限。应用无法退出。")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkAndRequestPermissions();
                    }
                })
                .setCancelable(false)
                .show();
    }

    //返回键按下事件
    @Override
    public void onBackPressed() {
        // 禁用返回键
        showRestrictedDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保应用不被轻易终止
        if (!isChangingConfigurations()) {
            startMonitoringServices();
        }
    }

    /**
     * 启动服务
     * old code
    private void startServices() {
        // 启动位置跟踪服务
        Intent locationServiceIntent = new Intent(this, LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationServiceIntent);
        } else {
            startService(locationServiceIntent);
        }
    }*/

    /*
    private boolean isAccessibilityServiceEnabled() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_ACCESSIBILITY,
                android.os.Process.myUid(),
                getPackageName()
        );

        return mode == AppOpsManager.MODE_ALLOWED;
    }*/

    //old code
    private boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null) return false;

        String expectedComponentName = getPackageName() + "/.accessibility.OverlayAccessibilityService";
        return enabledServices.contains(expectedComponentName);
    }

    //old code
    private boolean hasScreenshotPermission() {
        // 截图权限需要通过MediaProjection API获取
        return false;
    }

    //old code
    private void showUsageStatsPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("应用使用统计权限")
                .setMessage("为了监控应用使用情况，需要授予应用使用统计权限")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_USAGE_STATS);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    //old code
    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("悬浮窗权限")
                .setMessage("为了显示控制界面，需要授予悬浮窗权限")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    //old code
    private void showAccessibilityServiceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("无障碍服务")
                .setMessage("为了实现远程控制功能，需要启用无障碍服务")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /* old code
    private void showScreenshotPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("截图权限")
                .setMessage("为了实现远程截图功能，需要授予截图权限")
                .setPositiveButton("去设置", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        MediaProjectionManager mediaProjectionManager =
                                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                        startActivityForResult(intent, REQUEST_CODE_SCREENSHOT);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }*/

    /*
    //old code
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "位置权限已授予", Toast.LENGTH_SHORT).show();
                    // 如果需要后台位置权限，继续请求
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                REQUEST_CODE_BACKGROUND_LOCATION
                        );
                    }
                } else {
                    Toast.makeText(this, "位置权限被拒绝，应用可能无法正常工作", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE_BACKGROUND_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "后台位置权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "后台位置权限被拒绝，应用可能无法在后台获取位置", Toast.LENGTH_SHORT).show();
                }
                break;
        }

        updateStatus();
    }*/

    /*
    //old code
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_USAGE_STATS:
                if (hasUsageStatsPermission()) {
                    Toast.makeText(this, "应用使用统计权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "应用使用统计权限被拒绝，应用可能无法监控应用使用情况", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE_OVERLAY:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "悬浮窗权限被拒绝，应用可能无法显示控制界面", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE_SCREENSHOT:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // 保存截图权限结果
                    Intent screenshotServiceIntent = new Intent(this, ScreenshotService.class);
                    screenshotServiceIntent.putExtra("resultCode", resultCode);
                    screenshotServiceIntent.putExtra("data", data);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(screenshotServiceIntent);
                    } else {
                        startService(screenshotServiceIntent);
                    }
                    Toast.makeText(this, "截图权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "截图权限被拒绝，应用可能无法进行远程截图", Toast.LENGTH_SHORT).show();
                }
                break;
        }

        updateStatus();
    }*/

    //old code
    //更新显示状态
    private void updateStatus() {
        StringBuilder status = new StringBuilder();

        // 检查位置权限
        boolean hasLocationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        // 检查后台位置权限
        boolean hasBackgroundLocationPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasBackgroundLocationPermission = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
        }

        // 检查应用使用统计权限
        boolean hasUsageStatsPermission = hasUsageStatsPermission();

        // 检查悬浮窗权限
        boolean hasOverlayPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlayPermission = Settings.canDrawOverlays(this);
        }

        // 检查无障碍服务权限
        boolean hasAccessibilityPermission = isAccessibilityServiceEnabled();

        // 检查截图权限
        boolean hasScreenshotPermission = hasScreenshotPermission();

        status.append("位置权限: ").append(hasLocationPermission ? "已授予" : "未授予").append("\n");
        status.append("后台位置权限: ").append(hasBackgroundLocationPermission ? "已授予" : "未授予").append("\n");
        status.append("应用使用统计权限: ").append(hasUsageStatsPermission ? "已授予" : "未授予").append("\n");
        status.append("悬浮窗权限: ").append(hasOverlayPermission ? "已授予" : "未授予").append("\n");
        status.append("无障碍服务权限: ").append(hasAccessibilityPermission ? "已授予" : "未授予").append("\n");
        status.append("截图权限: ").append(hasScreenshotPermission ? "已授予" : "未授予").append("\n");

        statusTextView.setText(status.toString());
    }
}