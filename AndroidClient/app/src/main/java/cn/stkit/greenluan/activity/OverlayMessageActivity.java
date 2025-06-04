package cn.stkit.greenluan.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import cn.stkit.greenluan.R;

/**
 * 覆盖消息活动Activity
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class OverlayMessageActivity extends Activity {

    private TextView messageTextView;
    private Button closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置透明主题和布局
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(params);

        setContentView(R.layout.activity_overlay_message);

        // 初始化控件
        messageTextView = findViewById(R.id.message_text);
        closeButton = findViewById(R.id.close_button);

        // 获取传递的消息
        String message = getIntent().getStringExtra("message");
        if (message != null) {
            messageTextView.setText(message);
        }

        // 设置关闭按钮点击事件
        closeButton.setOnClickListener(v -> finish());
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键，防止意外关闭
    }
}
