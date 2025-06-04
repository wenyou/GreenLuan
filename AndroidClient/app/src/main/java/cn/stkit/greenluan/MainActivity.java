package cn.stkit.greenluan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cn.stkit.greenluan.service.LocationTrackingService;
import cn.stkit.greenluan.service.ScreenshotService;

/**
 * App主界面Activity
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GreenLuanMainActivity";
    private static final int REQUEST_CODE_LOCATION = 101;
    private static final int REQUEST_CODE_USAGE_STATS = 102;
    private static final int REQUEST_CODE_OVERLAY = 103;
    private static final int REQUEST_CODE_SCREENSHOT = 104;
    private static final int REQUEST_CODE_BACKGROUND_LOCATION = 105;

    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //设置MainActivity标题栏标题
        setTitle(R.string.main_activity_title);

        statusTextView = findViewById(R.id.status_text);
        Button checkPermissionsButton = findViewById(R.id.check_permissions_button);

        checkPermissionsButton.setOnClickListener(v -> checkAndRequestPermissions());

        // 启动服务
        startServices();

        // 检查权限
        checkAndRequestPermissions();

        // 显示状态
        updateStatus();
    }

    /**
     * 启动服务
     */
    private void startServices() {
        // 启动位置跟踪服务
        Intent locationServiceIntent = new Intent(this, LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationServiceIntent);
        } else {
            startService(locationServiceIntent);
        }
    }

    private void checkAndRequestPermissions() {
        boolean allPermissionsGranted = true;

        // 检查位置权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION
            );
            allPermissionsGranted = false;
        }

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

        // 检查应用使用统计权限
        if (!hasUsageStatsPermission()) {
            showUsageStatsPermissionDialog();
            allPermissionsGranted = false;
        }

        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();
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

        if (allPermissionsGranted) {
            Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show();
        }

        updateStatus();
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName()
        );

        return mode == AppOpsManager.MODE_ALLOWED;
    }

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

    private boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null) return false;

        String expectedComponentName = getPackageName() + "/.accessibility.OverlayAccessibilityService";
        return enabledServices.contains(expectedComponentName);
    }

    private boolean hasScreenshotPermission() {
        // 截图权限需要通过MediaProjection API获取
        return false;
    }

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
    }

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
    }

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
    }

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