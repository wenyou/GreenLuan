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

    //OkHttpClient 配置
    public static final int OK_HTTP_CLIENT_CONNECT_TIMEOUT = 30;//OkHttpClient connectTimeout  连接超时，单位：秒。
    public static final int OK_HTTP_CLIENT_READ_TIMEOUT = 30;//readTimeout 读取超时，单位：秒。
    public static final int OK_HTTP_CLIENT_WRITE_TIMEOUT = 30;//writeTimeout 写入超时，单位：秒。

    // 接口路径
    public static final String API_STUDENT_LOCATION = "/v1/location/upload";//实时位置，定位
    public static final String API_APP_USAGE = "/v1/app-usage/report";//应用使用情况
    public static final String API_COMMAND_POLL = "/v1/command/poll";//命令执行服务
    public static final String API_SCREENSHOT_UPLOAD = "/v1/screenshot/upload";//上传屏幕截图

    // 通知ID, 通知渠道ID (通知相关)
    public static final int NOTIFICATION_ID = 100;
    public static final int LOCATION_NOTIFICATION_ID = 101;//Location，位置服务通知ID
    public static final int APP_USAGE_NOTIFICATION_ID = 102;//应用使用监控通知ID
    public static final int COMMAND_NOTIFICATION_ID = 103;//命令执行服务通知ID
    public static final int SCREENSHOT_NOTIFICATION_ID = 104;//截图服务通知ID
    public static final String NOTIFICATION_CHANNEL_ID = "greenluan_monitoring_channel";//主服务通知通道
    public static final String LOCATION_NOTIFICATION_CHANNEL_ID = "greenluan_location_service_channel";//位置服务通知通道，位置通知渠道ID
    public static final String APP_USAGE_NOTIFICATION_CHANNEL_ID = "greenluan_app_usage_channel";//应用使用统计通知通道，应用使用监控通知渠道ID
    public static final String COMMAND_NOTIFICATION_CHANNEL_ID = "greenluan_command_service_channel";//命令处理通知通道，命令执行服务渠道ID
    public static final String SCREENSHOT_NOTIFICATION_CHANNEL_ID = "greenluan_screenshot_service_channel";//截图服务通知通道
    //
    public static final String NOTIFICATION_CHANNEL_ID_NAME = "青鸾信学生监控服务";

    //请求码相关
    //权限请求码 （用于标识权限请求的类型）
    public static final int PERMISSION_REQUEST_CODE = 1001;//权限请求的自定义请求码（整数常量），用于标识不同的权限请求场景
    public static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 1002;//使用情况统计权限（Usage Stats Permission） 的请求码
    public static final int OVERLAY_PERMISSION_REQUEST_CODE = 1003;// 悬浮窗权限（SYSTEM_ALERT_WINDOW） 的自定义请求码
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1004;// 位置权限的自定义请求码
    public static final int SCREENSHOT_PERMISSION_REQUEST_CODE = 1005;//屏幕截图权限的自定义请求码
    public static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 1006;//无障碍服务权限的自定义请求码

    // 位置更新参数 配置
    public static final int LOCATION_UPDATE_INTERVAL = 1 * 60 * 1000; // 1分钟
    public static final int LOCATION_FASTEST_INTERVAL = 30 * 1000; // 30秒
    // 应用使用统计间隔（分钟）  配置
    public static final long APP_USAGE_STATISTICS_INTERVAL = 15;//单位：分钟
    // 检查服务器下达命令的时间间隔（秒）
    public static final long COMMAND_CHECK_INTERVAL = 60;//单位：秒

    // 其他配置
    public static final boolean DEBUG_MODE = false; // 调试模式开关
    public static final long POLL_INTERVAL = 30 * 1000; // 命令轮询间隔（毫秒）
}