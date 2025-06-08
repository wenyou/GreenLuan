package cn.stkit.greenluan.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;

import cn.stkit.greenluan.config.ConfigConstants;
import cn.stkit.greenluan.data.AppUsageData;
import cn.stkit.greenluan.data.LocationData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * API客户端
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-8
 */
public class ApiClient {
    private static final String TAG = "GreenLuanApiClient";
    // 服务器配置
    private static final String BASE_URL = ConfigConstants.SERVER_BASE_URL;;
    //private static final String SERVER_URL = "https://greenluan.com/api";

    private static ApiClient instance;
    private OkHttpClient client;
    private Gson gson;
    private Context context;

    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();

        // 配置 OkHttpClient
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    //上传手机位置信息到服务器
    public void uploadLocation(LocationData locationData, final Callback callback) {
        String json = gson.toJson(locationData);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(BASE_URL + ConfigConstants.API_STUDENT_LOCATION)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Location upload failed: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Location upload success: " + responseData);
                    callback.onSuccess(responseData);
                } else {
                    Log.e(TAG, "Location upload failed: " + response.code());
                    callback.onFailure("HTTP error " + response.code());
                }
            }
        });
    }

    //上传手机APP使用情况
    public void uploadAppUsage(List<AppUsageData> usageDataList, final Callback callback) {
        String json = gson.toJson(usageDataList);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(BASE_URL + ConfigConstants.API_APP_USAGE)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "App usage upload failed: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "App usage upload success: " + responseData);
                    callback.onSuccess(responseData);
                } else {
                    Log.e(TAG, "App usage upload failed: " + response.code());
                    callback.onFailure("HTTP error " + response.code());
                }
            }
        });
    }

    //从服务器端获取要执行的命令
    public void fetchCommands(final Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + ConfigConstants.API_COMMAND_POLL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Fetch commands failed: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Fetch commands success: " + responseData);
                    callback.onSuccess(responseData);
                } else {
                    Log.e(TAG, "Fetch commands failed: " + response.code());
                    callback.onFailure("HTTP error " + response.code());
                }
            }
        });
    }

    //上传手机屏幕截图
    public void uploadScreenshot(Bitmap bitmap, final Callback callback) {
        // 将 Bitmap 转换为 Base64 字符串
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

        // 构建请求体
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("screenshot", encodedImage)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + ConfigConstants.API_SCREENSHOT_UPLOAD)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Screenshot upload failed: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Screenshot upload success: " + responseData);
                    callback.onSuccess(responseData);
                } else {
                    Log.e(TAG, "Screenshot upload failed: " + response.code());
                    callback.onFailure("HTTP error " + response.code());
                }
            }
        });
    }

    //回调
    public interface Callback {
        void onSuccess(String response);
        void onFailure(String error);
        //void onFailure(Call call,IOException e);
        //void onResponse(Call call, Response response);
    }
}
