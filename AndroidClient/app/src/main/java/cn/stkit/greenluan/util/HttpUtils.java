package cn.stkit.greenluan.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
/**
 * Http操作助手
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class HttpUtils {
    private static final String TAG = "GreenLuanHttpUtils";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setLenient().create();

    public static void sendPostRequest(String url, String json) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "POST request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "POST request successful: " + response.body().string());
                } else {
                    Log.e(TAG, "POST request failed with code: " + response.code());
                }
            }
        });
    }

    public static void sendGetRequest(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public static <T> T parseJson(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            return null;
        }
    }

    public static String toJson(Object object) {
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            Log.e(TAG, "Error converting to JSON: " + e.getMessage());
            return null;
        }
    }
}
