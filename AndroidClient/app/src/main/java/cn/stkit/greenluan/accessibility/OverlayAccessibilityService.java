package cn.stkit.greenluan.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.graphics.Path;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.stkit.greenluan.R;

/**
 * 覆盖式无障碍服务
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class OverlayAccessibilityService extends AccessibilityService {

    private static final String TAG = "GreenLuanOverlayService";
    private WindowManager windowManager;
    private View overlayView;
    private LinearLayout controlPanel;
    private boolean isPanelExpanded = false;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;


    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlayView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 处理可访问性事件
    }

    @Override
    public void onInterrupt() {
        // 服务中断时调用
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createOverlayView() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

        // 设置LayoutParams
        WindowManager.LayoutParams params;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    android.graphics.PixelFormat.TRANSLUCENT
            );
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    android.graphics.PixelFormat.TRANSLUCENT
            );
        }

        // 设置重力和初始位置
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // 添加视图到窗口管理器
        windowManager.addView(overlayView, params);

        // 设置触摸监听器，使窗口可拖动
        overlayView.findViewById(R.id.drag_handle).setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_UP:
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(overlayView, params);
                    return true;
            }
            return false;
        });

        // 设置控制面板按钮
        Button expandButton = overlayView.findViewById(R.id.expand_button);
        expandButton.setOnClickListener(v -> toggleControlPanel());

        // 设置控制按钮
        setupControlButtons();
    }

    private void toggleControlPanel() {
        controlPanel = overlayView.findViewById(R.id.control_panel);
        isPanelExpanded = !isPanelExpanded;

        if (isPanelExpanded) {
            controlPanel.setVisibility(View.VISIBLE);
            ((Button) overlayView.findViewById(R.id.expand_button)).setText("收缩");
        } else {
            controlPanel.setVisibility(View.GONE);
            ((Button) overlayView.findViewById(R.id.expand_button)).setText("展开");
        }
    }

    private void setupControlButtons() {
        // 示例：添加一个锁屏按钮
        Button lockButton = overlayView.findViewById(R.id.lock_button);
        lockButton.setOnClickListener(v -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN));

        // 示例：添加一个主屏幕按钮
        Button homeButton = overlayView.findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> performGlobalAction(GLOBAL_ACTION_HOME));

        // 示例：添加一个返回按钮
        Button backButton = overlayView.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> performGlobalAction(GLOBAL_ACTION_BACK));

        // 示例：添加一个通知栏按钮
        Button notificationsButton = overlayView.findViewById(R.id.notifications_button);
        notificationsButton.setOnClickListener(v -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS));
    }

    public void showMessage(String message) {
        if (overlayView != null) {
            TextView messageView = overlayView.findViewById(R.id.overlay_message);
            messageView.setText(message);
            messageView.setVisibility(View.VISIBLE);

            // 5秒后隐藏消息
            overlayView.postDelayed(() -> messageView.setVisibility(View.GONE), 5000);
        }
    }

    public void simulateTap(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path tapPath = new Path();
            tapPath.moveTo(x, y);

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(tapPath, 0, 100));

            dispatchGesture(gestureBuilder.build(), null, null);
        }
    }

    public void simulateSwipe(int startX, int startY, int endX, int endY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path swipePath = new Path();
            swipePath.moveTo(startX, startY);
            swipePath.lineTo(endX, endY);

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 500, false));

            dispatchGesture(gestureBuilder.build(), null, null);
        }
    }

    public int getScreenWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    public int getScreenHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }
}
