package cn.stkit.greenluan.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.stkit.greenluan.MainActivity;
import cn.stkit.greenluan.R;
import cn.stkit.greenluan.config.ConfigConstants;
import cn.stkit.greenluan.data.LocationData;
import cn.stkit.greenluan.network.ApiClient;
import cn.stkit.greenluan.util.HttpUtils;

/**
 * 位置跟踪服务(实时定位)
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class LocationTrackingService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GreenLuanLocationService";
    //通知相关
    private static final int NOTIFICATION_ID = ConfigConstants.LOCATION_NOTIFICATION_ID;//101， 位置通知ID
    private static final String NOTIFICATION_CHANNEL_ID = ConfigConstants.LOCATION_NOTIFICATION_CHANNEL_ID;//位置通知渠道ID,greenLuan's_location_service_channel
    // 位置更新参数
    private static final int UPDATE_INTERVAL = ConfigConstants.LOCATION_UPDATE_INTERVAL; // 1分钟
    private static final int FASTEST_INTERVAL = ConfigConstants.LOCATION_FASTEST_INTERVAL; // 30秒

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private GoogleApiClient googleApiClient;
    private SharedPreferences sharedPreferences;
    private Location lastLocation;
    private Timer uploadTimer;

    private Gson gson;
    private ScheduledExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GreenLuan's LocationService created");
        sharedPreferences = getSharedPreferences("greenluan_monitoring", MODE_PRIVATE);
        gson = new Gson();
        // 初始化Google API客户端
        buildGoogleApiClient();
        // 初始化位置请求，更新位置
        createLocationRequest();
        // 初始化位置回调
        createLocationCallback();
        // 初始化位置客户端
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 启动定时上传任务
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::uploadPendingLocations,
                5, 5, TimeUnit.MINUTES);

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());

        //old code
        /*fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        startForeground(1, createNotification());
        startLocationUpdates();
        startUploadTimer();*/
        //en old code
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "GreenLuan's LocationService started");
        connectLocationClient();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "GreenLuan's LocationService destroyed");
        disconnectLocationClient();
        if (executorService != null) {
            executorService.shutdown();
        }
        // 重启服务
        Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }

        //old code
        /*
        stopLocationUpdates();
        if (uploadTimer != null) {
            uploadTimer.cancel();
        }
        // 重启服务
        Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartIntent);
        } else {
            getApplicationContext().startService(restartIntent);
        }*/
        //end old code
    }

    //初始化Google API客户端
    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    //初始化位置请求
    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);//n秒更新一次
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * 初始化位置回调
     * 1: 本地保存位置信息
     * 2: 上传位置信息到服务器
     */
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " +
                                location.getLongitude());
                        saveLocation(location);//本地保存位置信息
                        uploadLocation(location);//上传位置信息到服务器
                    }
                }

                //old code
                /*lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    Log.d(TAG, "Location updated: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                    //sendLocationToServer(lastLocation);
                    broadcastLocation(lastLocation);
                }*/
                //end old code
            }
        };
    }

    //连接位置客户端
    private void connectLocationClient() {
        if (googleApiClient != null && !googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    //断开位置客户端连接
    private void disconnectLocationClient() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            stopLocationUpdates();
            googleApiClient.disconnect();
        }
    }

    //开启位置更新
    private void startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied: " + e.getMessage());
        }
    }

    //old code
    /* old code
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted: " + e.getMessage());
        }
    }*/

    //停止位置更新
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    //实现位置信息本地存储，以防上传失败
    private void saveLocation(Location location) {
        // 这里可以实现本地存储，以防上传失败
        // 简化实现，实际应用中可以使用数据库
    }

    //上传位置信息到服务器
    private void uploadLocation(Location location) {
        // 创建位置数据对象
        LocationData locationData = new LocationData(
                System.currentTimeMillis(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy()
        );

        // 上传到服务器
        ApiClient.getInstance(this).uploadLocation(locationData, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Location uploaded successfully");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to upload location: " + error);
                // 上传失败，保存到本地待重试
                saveLocationForRetry(locationData);
            }
        });
    }

    //实现本地存储待上传的位置数据
    private void saveLocationForRetry(LocationData locationData) {
        // 实现本地存储待上传的位置数据
    }

    //实现上传所有待上传的位置数据
    private void uploadPendingLocations() {
        // 实现上传所有待上传的位置数据
    }

    /**
     * 创建位置服务通知通道
     * @return
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("GreenLuan's LocationTrackingService")
                .setContentText("GreenLuan is Monitoring your location")
                .setSmallIcon(R.drawable.ic_add_location)//R.drawable.ic_notification
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        /* old code
        return new NotificationCompat.Builder(this, App.LOCATION_CHANNEL_ID)
                .setContentTitle("GreenLuan's Location Tracking")
                .setContentText("GreenLuan's Monitoring your location")
                .setSmallIcon(R.drawable.ic_add_location)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 位置服务通知通道
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "GreenLuan's Location Tracking Service",//位置监控服务
                    NotificationManager.IMPORTANCE_LOW //NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("GreenLuan's Service for tracking device location");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return builder.build();
    }

    //old code
    private void startUploadTimer() {
        uploadTimer = new Timer();
        uploadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (lastLocation != null) {
                    sendLocationToServer(lastLocation);
                }
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    /**
     * old code
     * 发送位置信息到服务器
     * @param location
     */
    private void sendLocationToServer(Location location) {
        new Thread(() -> {
            try {
                JSONObject locationData = new JSONObject();
                locationData.put("latitude", location.getLatitude());
                locationData.put("longitude", location.getLongitude());
                locationData.put("accuracy", location.getAccuracy());
                locationData.put("timestamp", System.currentTimeMillis());

                // 发送数据到服务器
                String serverUrl = "https://greenluan.com/location/upload";
                HttpUtils.sendPostRequest(serverUrl, locationData.toString());
                Log.d(TAG, "Location data sent to server");
            } catch (JSONException e) {
                Log.e(TAG, "Error creating location JSON: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error sending location data: " + e.getMessage());
            }
        }).start();
    }

    //广播位置  old code
    private void broadcastLocation(Location location) {
        Intent intent = new Intent("location_update");
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    //
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google API client connected");
        startLocationUpdates();
    }

    //Google API client 连接已暂停
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google API client connection suspended");
    }

    //Google API client 连接失败
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Google API client connection failed: " + connectionResult.getErrorMessage());
    }
}
