package cn.stkit.greenluan.util;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;

import java.nio.ByteBuffer;

/**
 * 截屏工具
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-8
 */
public class ScreenshotUtils {
    private static final String TAG = "GreenLuanScreenshotUtils";
    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private static MediaProjection mediaProjection;
    private static VirtualDisplay virtualDisplay;
    private static ImageReader imageReader;
    private static int width, height;
    private static ScreenshotCallback callback;

    public interface ScreenshotCallback {
        void onScreenshotTaken(Bitmap bitmap);
        void onError(String error);
    }

    @SuppressLint("WrongConstant")
    public static void takeScreenshot(Context context, ScreenshotCallback screenshotCallback) {
        callback = screenshotCallback;

        if (mediaProjection == null) {
            // 需要启动屏幕捕获权限请求
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                MediaProjectionManager mediaProjectionManager =
                        (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                activity.startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
            } else {
                callback.onError("无法启动屏幕捕获权限请求，上下文不是Activity");
            }
            return;
        }

        // 获取屏幕尺寸
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;

        // 创建ImageReader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        // 创建虚拟显示
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenshotDisplay",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null
        );

        // 延迟获取截图
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                captureImage();
            }
        }, 100);
    }

    public static void initMediaProjection(Context context, int resultCode, Intent data) {
        if (context == null || data == null) {
            if (callback != null) {
                callback.onError("初始化MediaProjection失败：上下文或数据为空");
            }
            return;
        }

        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

        if (mediaProjection == null) {
            if (callback != null) {
                callback.onError("无法获取MediaProjection实例");
            }
            return;
        }

        // 初始化成功后可以再次尝试截图
        takeScreenshot(context, callback);
    }

    private static void captureImage() {
        if (imageReader == null) {
            if (callback != null) {
                callback.onError("ImageReader未初始化");
            }
            return;
        }

        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            if (callback != null) {
                callback.onError("无法获取图像");
            }
            return;
        }

        // 处理图像
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        // 创建Bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);

        // 裁剪为实际屏幕大小
        bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                width,
                height
        );

        // 释放资源
        image.close();
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (imageReader != null) {
            imageReader.close();
        }

        // 返回截图
        if (callback != null) {
            callback.onScreenshotTaken(bitmap);
        }
    }

    public static void cleanUp() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
