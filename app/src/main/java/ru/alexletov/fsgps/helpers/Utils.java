package ru.alexletov.fsgps.helpers;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Alex on 05.10.2014.
 */
public class Utils {
    public static String getStackTrace() {
        return Thread.currentThread().getStackTrace().toString();
    }

    public static  void logStackTrace(String tag) {
        Log.d(tag, getStackTrace());
    }

    public static boolean useTransitions(SharedPreferences prefs) {
        return prefs == null ? false : prefs.getBoolean("pref_use_transition", false);
    }

    public static boolean checkActivityLive(Activity activity) {
        return !(activity == null || activity.isFinishing() || activity.isDestroyed());
    }

    public static PositionGpsInfo convertToPositionGpsInfo(PositionInfoSerializable info) {
        PositionGpsInfo pos = new PositionGpsInfo(0, 0, 0, 0, 0);
        pos.setAltitude(info.getAltitude());
        pos.setBearing(info.getBearing());
        pos.setLatitude(info.getLatitude());
        pos.setLongitude(info.getLongitude());
        pos.setSpeed(info.getSpeed());
        return pos;
    }
}
