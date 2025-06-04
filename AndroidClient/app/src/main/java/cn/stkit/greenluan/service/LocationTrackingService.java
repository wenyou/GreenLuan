package cn.stkit.greenluan.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cn.stkit.greenluan.App;
import cn.stkit.greenluan.MainActivity;
import cn.stkit.greenluan.R;
import cn.stkit.greenluan.util.HttpUtils;

/**
 * 位置跟踪服务
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-4
 */
public class LocationTrackingService extends Service {
    private static final String TAG = "GreenLuanLocationService";
    private static final int UPDATE_INTERVAL = 5 * 60 * 1000; // 5分钟
    private static final int FASTEST_INTERVAL = 1 * 60 * 1000; // 1分钟

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private Timer uploadTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        startForeground(1, createNotification());
        startLocationUpdates();
        startUploadTimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
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
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    Log.d(TAG, "Location updated: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                    sendLocationToServer(lastLocation);
                    broadcastLocation(lastLocation);
                }
            }
        };
    }

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
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


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

    private void sendLocationToServer(Location location) {
        new Thread(() -> {
            try {
                JSONObject locationData = new JSONObject();
                locationData.put("latitude", location.getLatitude());
                locationData.put("longitude", location.getLongitude());
                locationData.put("accuracy", location.getAccuracy());
                locationData.put("timestamp", System.currentTimeMillis());

                // 发送数据到服务器
                String serverUrl = "https://your-server-api.com/location/upload";
                HttpUtils.sendPostRequest(serverUrl, locationData.toString());
                Log.d(TAG, "Location data sent to server");
            } catch (JSONException e) {
                Log.e(TAG, "Error creating location JSON: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error sending location data: " + e.getMessage());
            }
        }).start();
    }

    private void broadcastLocation(Location location) {
        Intent intent = new Intent("location_update");
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, App.LOCATION_CHANNEL_ID)
                .setContentTitle("GreenLuan's Location Tracking")
                .setContentText("GreenLuan's  Monitoring your location")
                .setSmallIcon(R.drawable.ic_add_location)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }



}
