package cn.stkit.greenluan.data;

import java.io.Serializable;

/**
 * 位置数据对象
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-8
 */
public class LocationData implements Serializable {
    private long timestamp;
    private double latitude;//纬度
    private double longitude;//经度
    private float accuracy;//准确性，精确性

    public LocationData(long timestamp, double latitude, double longitude, float accuracy) {
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
}
