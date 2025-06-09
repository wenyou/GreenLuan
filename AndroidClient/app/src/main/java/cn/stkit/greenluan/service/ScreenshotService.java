package cn.stkit.greenluan.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import cn.stkit.greenluan.App;
import cn.stkit.greenluan.MainActivity;
import cn.stkit.greenluan.R;
import cn.stkit.greenluan.config.ConfigConstants;
import cn.stkit.greenluan.util.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 截图服务
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class ScreenshotService extends Service {
    private static final String TAG = "GreenLuanScreenshotService";
    //通知相关
    private static final int NOTIFICATION_ID = ConfigConstants.SCREENSHOT_NOTIFICATION_ID;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth, screenHeight, screenDensity;
    private Handler handler;
    private String screenshotPath;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        initScreenCapture();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("resultCode") && intent.hasExtra("data")) {
            int resultCode = intent.getIntExtra("resultCode", 0);
            Intent data = intent.getParcelableExtra("data");
            startProjection(resultCode, data);
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopProjection();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
    }

    private void initScreenCapture() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void startProjection(int resultCode, Intent data) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null");
            stopSelf();
            return;
        }

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler
        );

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * screenWidth;

                    // 创建位图
                    bitmap = Bitmap.createBitmap(
                            screenWidth + rowPadding / pixelStride,
                            screenHeight,
                            Bitmap.Config.ARGB_8888
                    );
                    bitmap.copyPixelsFromBuffer(buffer);

                    // 保存截图
                    saveScreenshot(bitmap);

                    // 发送截图到服务器
                    sendScreenshotToServer(bitmap);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error capturing screenshot: " + e.getMessage());
            } finally {
                if (image != null) {
                    image.close();
                }

                if (bitmap != null) {
                    bitmap.recycle();
                }

                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                stopProjection();
                stopSelf();
            }
        }, handler);
    }

    private void stopProjection() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }
    }

    private void saveScreenshot(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Screenshot_" + timeStamp + ".png";

            File storageDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "GreenLuanStudentMonitoring");
            } else {
                storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "GreenLuanStudentMonitoring");
            }

            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return;
            }

            File imageFile = new File(storageDir, fileName);
            screenshotPath = imageFile.getAbsolutePath();

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.d(TAG, "Screenshot saved: " + screenshotPath);
        } catch (IOException e) {
            Log.e(TAG, "Error saving screenshot: " + e.getMessage());
        }
    }

    private void sendScreenshotToServer(Bitmap bitmap) {
        new Thread(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] byteArray = stream.toByteArray();
                stream.close();

                // 将图片转换为Base64编码
                String encodedImage = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);

                // 构建JSON数据
                JSONObject screenshotData = new JSONObject();
                screenshotData.put("imageData", encodedImage);
                screenshotData.put("timestamp", System.currentTimeMillis());

                // 发送到服务器
                String serverUrl = "https://your-server-api.com/screenshot/upload";
                HttpUtils.sendPostRequest(serverUrl, screenshotData.toString());
                Log.d(TAG, "Screenshot sent to server");
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error sending screenshot: " + e.getMessage());
            }
        }).start();
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, App.SCREENSHOT_CHANNEL_ID)
                .setContentTitle("Taking Screenshot")
                .setContentText("Capturing current screen")
                .setSmallIcon(R.drawable.ic_screenshot)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
