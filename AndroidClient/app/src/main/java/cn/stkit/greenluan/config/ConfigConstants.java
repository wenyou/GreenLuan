package cn.stkit.greenluan.config;

/**
 * 常量配置类
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-8
 */
public class ConfigConstants {
    // 服务器基础配置
    public static final String SERVER_BASE_URL = "https://api.greenluan.com/student-monitor";
    public static final int SERVER_TIMEOUT = 15; // 超时时间（秒）

    // 接口路径
    public static final String API_STUDENT_LOCATION = "/v1/location/upload";//实时位置，定位
    public static final String API_APP_USAGE = "/v1/app-usage/report";//应用使用情况
    public static final String API_COMMAND_POLL = "/v1/command/poll";//命令执行服务
    public static final String API_SCREENSHOT_UPLOAD = "/v1/screenshot/upload";//上传屏幕截图

    // 通知渠道 ID  ？？
    public static final String NOTIFICATION_CHANNEL_ID = "greenluan_monitoring_channel";
    private static final int NOTIFICATION_ID = 100;
    public static final String NOTIFICATION_CHANNEL_NAME = "青鸾信学生监控服务";

    // 权限请求码 ？？
    public static final int REQUEST_CODE_LOCATION = 1001;
    public static final int REQUEST_CODE_OVERLAY = 1002;
    public static final int REQUEST_CODE_ACCESSIBILITY = 1003;


    // 其他配置
    public static final boolean DEBUG_MODE = false; // 调试模式开关
    public static final long POLL_INTERVAL = 30 * 1000; // 命令轮询间隔（毫秒）
}
