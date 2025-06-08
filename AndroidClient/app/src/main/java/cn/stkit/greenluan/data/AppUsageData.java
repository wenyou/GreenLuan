package cn.stkit.greenluan.data;

import java.io.Serializable;

/**
 * App应用使用情况数据对象
 * @author Zeeny  zhwenyou@gmail.com
 * @date 2025-6-8
 */
public class AppUsageData implements Serializable {
    private String packageName;//app名称
    private long usageTime; // 毫秒，使用时长
    private long startTime; //开始使用时间
    private long endTime; //结束使用时间

    public AppUsageData(String packageName, long usageTime, long startTime, long endTime) {
        this.packageName = packageName;
        this.usageTime = usageTime;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getUsageTime() {
        return usageTime;
    }

    public void setUsageTime(long usageTime) {
        this.usageTime = usageTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
