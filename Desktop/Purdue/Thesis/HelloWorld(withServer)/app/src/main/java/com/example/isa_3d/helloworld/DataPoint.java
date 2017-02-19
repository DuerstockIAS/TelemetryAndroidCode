package com.example.isa_3d.helloworld;

import com.microsoft.band.sensors.HeartRateQuality;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by ISA-3D on 2/4/2016.
 */
public class DataPoint {
    private long time;
    private float heartRate;
    private int GSR;
    private float skinTemp;
    private HeartRateQuality quality;
    private boolean AD;
    public int day;
    public int minute;
    public int hour;

    public DataPoint(long time, float HR, HeartRateQuality quality, int GSR, float skinTemp, boolean AD) {
        this.time = time;
        this.heartRate = HR;
        this.GSR = GSR;
        this.quality = quality;
        this.skinTemp = skinTemp;
        this.AD = AD; // Autonomic Dysreflexia

        Date d = new Date(time);
        String[] dateArray = d.toString().split(" ");
        String[] timeArray = dateArray[3].toString().split(":");

        this.day = Integer.parseInt(dateArray[2]);
        this.hour = Integer.parseInt(timeArray[0]);
        this.minute = Integer.parseInt(timeArray[1]);
    }

    public float getHeartRate() {
        return heartRate;
    }

    public float getSkinTemp() {
        return skinTemp;
    }

    public int getGSR () {
        return GSR;
    }

    public HeartRateQuality getHRQ() {
        return quality;
    }

    public boolean getAD() {
        return AD;
    }

    public long getTime() {
        return time;
    }

    public void setHeartRate(float HR) {
        this.heartRate = HR;
    }

    public void setGSR(int GSR) {
        this.GSR = GSR;
    }

    public void setSkinTemp(float skinTemp) {
        this.skinTemp = skinTemp;
    }

    public void setQuality(HeartRateQuality quality) {
        this.quality = quality;
    }

    public void setAD(boolean AD) {
        this.AD = AD;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String toString() {

        String q;
        if (quality == HeartRateQuality.ACQUIRING)
            q = "0";
        else
            q = "1";

        String a;
        if (AD)
            a = "1";
        else
            a = "0";

        return String.format("%.2f\t%s\t%d\t%.2f\t%s",heartRate,q,GSR,skinTemp,a);

    }

    public JSONObject toJSON() {

        String q;
        if (quality == HeartRateQuality.ACQUIRING)
            q = "0";
        else
            q = "1";

        String a;
        if (AD)
            a = "1";
        else
            a = "0";

        JSONObject obj = new JSONObject();

        try {
            obj.put("HR", heartRate);
            obj.put("GSR", GSR);
            obj.put("ST", skinTemp);
            obj.put("ConnState", q);
            obj.put("AD",a);
            obj.put("Day",day);
            obj.put("Time",hour+":"+minute);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
