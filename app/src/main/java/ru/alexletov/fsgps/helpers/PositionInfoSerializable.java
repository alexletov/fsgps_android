package ru.alexletov.fsgps.helpers;

import java.io.Serializable;

/**
 * Created by Alex on 01.10.2014.
 */
public class PositionInfoSerializable extends Object implements Serializable {
    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private double mSpeed;
    private float mBearing;

    public PositionInfoSerializable(double lat, double lon, double alt, double speed, float bear) {
        mLatitude = lat;
        mLongitude = lon;
        mAltitude = alt;
        mSpeed = speed;
        mBearing = bear;
    }

    public void setLatitude(double lat) {
        mLatitude = lat;
    }

    public void setLongitude(double lon) {
        mLongitude = lon;
    }

    public void setAltitude(double alt) {
        mAltitude = alt;
    }

    public void setSpeed(double speed) {
        mSpeed = speed;
    }

    public void setBearing(float bear) {
        mBearing = bear;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public double getAltitude() {
        return mAltitude;
    }

    public double getSpeed() {
        return mSpeed;
    }

    public float getBearing() {
        return mBearing;
    }
}