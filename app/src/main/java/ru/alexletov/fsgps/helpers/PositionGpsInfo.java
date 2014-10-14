package ru.alexletov.fsgps.helpers;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Alex on 01.10.2014.
 */
public class PositionGpsInfo extends Object implements Serializable, Parcelable {
    private static final double KNOTS_TO_MPS_KOEFF = 0.51444444444;
    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private double mSpeed;
    private float mBearing;

    public PositionGpsInfo(double lat, double lon, double alt, double speed, float bear) {
        mLatitude = lat;
        mLongitude = lon;
        mAltitude = alt;
        mSpeed = speed;
        mBearing = bear;
    }

    private PositionGpsInfo(Parcel in) {
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mAltitude = in.readDouble();
        mSpeed = in.readDouble();
        mBearing = in.readFloat();
    }

    public static final Parcelable.Creator<PositionGpsInfo> CREATOR
            = new Parcelable.Creator<PositionGpsInfo>() {
        public PositionGpsInfo createFromParcel(Parcel in) {
            return new PositionGpsInfo(in);
        }

        public PositionGpsInfo[] newArray(int size) {
            return new PositionGpsInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(mLatitude);
        parcel.writeDouble(mLongitude);
        parcel.writeDouble(mAltitude);
        parcel.writeDouble(mSpeed);
        parcel.writeFloat(mBearing);
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

    public float getSpeedInMetersPerSecond() {
        return (float) (mSpeed * KNOTS_TO_MPS_KOEFF);
    }

    public String getLatitudeFormatted() {
        String result = getFormattedDegrees(mLatitude);
        result = ((mLongitude > 0) ? "N " : "S ") + result;
        return result;
    }

    public String getLongitudeFormatted() {
        String result = getFormattedDegrees(mLongitude);
        result = ((mLongitude > 0) ? "E " : "W ") + result;
        return result;
    }

    private String getFormattedDegrees (double degree) {
        degree = Math.abs(degree);
        Integer grad = (int) degree;
        double minTemp = (degree - grad) * 60;
        Integer min = (int) minTemp;
        Integer sec = (int) ((minTemp - min) * 60);
        return String.format("%03d° %02d' %02d''", grad, min, sec);
    }
}