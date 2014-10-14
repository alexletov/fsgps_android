package ru.alexletov.fsgps.helpers;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Alex on 05.10.2014.
 */
public class StatusInfo implements Parcelable {
    public enum Status {
        STATUS_CONNECTED,
        STATUS_CONNECTING,
        STATUS_DISCONNECTED,
        STATUS_ERROR,
        STATUS_UNKNOWN
    }

    private Status mStatus = Status.STATUS_UNKNOWN;
    private String mStatusString;

    public StatusInfo() {
        mStatus = Status.STATUS_UNKNOWN;
    }

    private StatusInfo(Parcel in) {
        int val = in.readInt();
        mStatus =  (val > 0 && val < Status.values().length) ?
            Status.values()[val] : Status.STATUS_UNKNOWN;
        mStatusString = in.readString();
    }

    public static final Parcelable.Creator<StatusInfo> CREATOR
            = new Parcelable.Creator<StatusInfo>() {
        public StatusInfo createFromParcel(Parcel in) {
            return new StatusInfo(in);
        }

        public StatusInfo[] newArray(int size) {
            return new StatusInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mStatus.ordinal());
        parcel.writeString(mStatusString);
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setStatus(Status status) {
        mStatus = status;
    }

    public void setStatusString(String statusString) {
        mStatusString = statusString;
    }

    public String getStatusString() {
        return mStatusString;
    }
}
