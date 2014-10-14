package ru.alexletov.fsgps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;

import ru.alexletov.fsgps.api.IPositionUpdate;
import ru.alexletov.fsgps.api.IStatusUpdate;
import ru.alexletov.fsgps.helpers.PositionGpsInfo;
import ru.alexletov.fsgps.helpers.PositionInfoSerializable;
import ru.alexletov.fsgps.helpers.StatusInfo;
import ru.alexletov.fsgps.helpers.Utils;

/**
 * Created by Alex on 04.10.2014.
 */
public class GpsService extends Service {
    public static final String TAG = "FS_GPS_SERVICE";
    private IGpsServiceApi.Stub api;

    @Override
    public void onCreate() {
        api = new GpsServiceApiImpl(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return api;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        try {
            api.stopEmulate();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not stop Gps Emulator thread");
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    private static class GpsServiceApiImpl extends IGpsServiceApi.Stub {
        private IPositionUpdate mPositionUpdateCb;
        private IStatusUpdate mStatusUpdateCb;

        private GpsEmulatorTask mMainThread;

        private Context mContext;

        private GpsServiceApiImpl(Context ctx) {
            mContext = ctx;
        }

        @Override
        public void startEmulate() throws RemoteException {
            Log.d(TAG, "startEmulate");
            if (mMainThread != null) {
                throw new RemoteException();
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String host = prefs.getString("pref_host", "");
            String portString = prefs.getString("pref_port", "0");

            int port;

            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException ex) {
                port = 0;
                Log.d(TAG, "Port is invalid");
            }

            mMainThread = new GpsEmulatorTask(this, host, port);
            mMainThread.start();
        }

        @Override
        public void stopEmulate() throws RemoteException {
            Log.d(TAG, "stopEmulate");
            if (mMainThread != null) {
                Log.d(TAG, "Thread " + mMainThread + " interrupted!");
                mMainThread.interrupt();
                mMainThread = null;
            }
        }

        @Override
        public void registerPositionCallback(IPositionUpdate callback) throws RemoteException {
            Log.d(TAG, "registerPositionCallback");
            Utils.logStackTrace(TAG);
            unregisterPositionCallback();
            mPositionUpdateCb = callback;
            Log.d(TAG, "New Position Update Callback is set to " + callback);
        }

        @Override
        public void registerStatusCallback(IStatusUpdate callback) throws RemoteException {
            Log.d(TAG, "registerStatusCallback");
            Utils.logStackTrace(TAG);
            unregisterStatusCallback();
            mStatusUpdateCb = callback;
            Log.d(TAG, "New Status Update Callback is set to " + callback);
        }

        @Override
        public void unregisterPositionCallback() throws RemoteException {
            Log.d(TAG, "unregisterPositionCallback");
            Utils.logStackTrace(TAG);
            if (mPositionUpdateCb != null) {
                Log.d(TAG, "Clearing previous Position Update Callback" + mPositionUpdateCb);
                mPositionUpdateCb = null;
            }
        }

        @Override
        public void unregisterStatusCallback() throws RemoteException {
            Log.d(TAG, "unregisterStatusCallback");
            Utils.logStackTrace(TAG);
            if (mStatusUpdateCb != null) {
                Log.d(TAG, "Clearing previous Status Update Callback" + mStatusUpdateCb);
                mStatusUpdateCb = null;
            }
        }

        @Override
        public PositionGpsInfo getPosition() throws RemoteException {
            Log.d(TAG, "getPosition");
            return mMainThread != null ? mMainThread.getPosition() : null;
        }

        @Override
        public StatusInfo getStatus() throws RemoteException {
            Log.d(TAG, "getStatus");
            return mMainThread != null ? mMainThread.getStatus() : null;
        }

        private static class GpsEmulatorTask extends Thread {
            private WeakReference<GpsServiceApiImpl> mApiImplRef;
            private String mHost;
            private int mPort;

            private StatusInfo mStatus;
            private PositionGpsInfo mPosition;



            public GpsEmulatorTask(GpsServiceApiImpl apiImpl, String host, int port) {
                mApiImplRef = new WeakReference<GpsServiceApiImpl>(apiImpl);
                mHost = host;
                mPort = port;
                Log.d(TAG, "Gps Emulator Task created. >>" + Utils.getStackTrace());
            }

            @Override
            public void run() {
                Log.d(TAG, "run");

                StatusInfo result = new StatusInfo();

                GpsServiceApiImpl api = mApiImplRef.get();
                if (api == null) {
                    Log.w(TAG, "Could not get service object");
                    result.setStatus(StatusInfo.Status.STATUS_DISCONNECTED);
                    updateStatus(result);
                    return;
                }

                result.setStatus(StatusInfo.Status.STATUS_CONNECTING);
                updateStatus(result);

                Socket socket;
                try {
                    socket = new Socket(mHost, mPort);
                } catch (IOException e) {
                    Log.w(TAG, "Could not connect to socket (" + mHost + ":" + mPort + ")");
                    result.setStatus(StatusInfo.Status.STATUS_DISCONNECTED);
                    updateStatus(result);
                    return;
                }

                ObjectInputStream inSocketStream;
                try {
                    inSocketStream = new ObjectInputStream(socket.getInputStream());
                } catch (IOException e) {
                    Log.d(TAG, "Can't get InputStream for socket!");
                    result.setStatus(StatusInfo.Status.STATUS_DISCONNECTED);
                    updateStatus(result);
                    return;
                }

                /**
                 * Prepare Mock Locations
                 */
                LocationManager locationManager = (LocationManager) api.mContext
                        .getSystemService(Context.LOCATION_SERVICE);

                try {
                    locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch (IllegalArgumentException ex) {
                    Log.d(TAG, "Could not clear test provider (GPS)");
                }

                locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false,
                        false, true, true, true, android.location.Criteria.POWER_LOW,
                        android.location.Criteria.ACCURACY_FINE);
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

                result.setStatus(StatusInfo.Status.STATUS_CONNECTED);
                updateStatus(result);

                try {
                    while (!isInterrupted()) {
                        PositionGpsInfo posInfo = Utils
                                .convertToPositionGpsInfo((PositionInfoSerializable) inSocketStream
                                        .readObject());
                        updatePosition(posInfo);

                        /**
                         * Update location
                         */

                        Location newLocation = new Location(LocationManager.GPS_PROVIDER);
                        newLocation.setLatitude(posInfo.getLatitude());
                        newLocation.setLongitude(posInfo.getLongitude());
                        newLocation.setAltitude(posInfo.getAltitude());
                        newLocation.setBearing(posInfo.getBearing());
                        newLocation.setSpeed(posInfo.getSpeedInMetersPerSecond());
                        newLocation.setAccuracy(0);

                        locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER,
                                LocationProvider.AVAILABLE,
                                null, System.currentTimeMillis());

                        try {
                            newLocation.setTime(System.currentTimeMillis());
                            newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER,
                                    newLocation);
                        } catch (IllegalArgumentException ex) {
                            Log.e(TAG, "Could not set location: " + ex.getMessage());
                        }
                    }
                    Log.d(TAG, "Thread is interrupted!");
                    Utils.logStackTrace(TAG);
                } catch (IOException e) {
                    Log.e(TAG, "Socket disconnected");
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "Class not found");
                }
                result.setStatus(StatusInfo.Status.STATUS_DISCONNECTED);
                updateStatus(result);

                locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER,
                        LocationProvider.OUT_OF_SERVICE,
                        null, System.currentTimeMillis());

                try {
                    locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch (IllegalArgumentException ex) {
                    Log.d(TAG, "Could not clear test provider (GPS)");
                }

                try {
                    inSocketStream.close();
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't close socket");
                }
                Log.e(TAG, "Thread end");
            }

            private void updateStatus(StatusInfo status) {
                Log.d(TAG, "updateStatus");
                mStatus = status;
                GpsServiceApiImpl api = mApiImplRef.get();
                if (api != null && api.mStatusUpdateCb != null) {
                    try {
                        api.mStatusUpdateCb.update(status);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Could not notify Status Update Callback!");
                    }
                }
            }

            private void updatePosition(PositionGpsInfo pos) {
                Log.d(TAG, "updatePosition");
                mPosition = pos;
                GpsServiceApiImpl api = mApiImplRef.get();
                if (api != null && api.mPositionUpdateCb != null) {
                    try {
                        api.mPositionUpdateCb.update(pos);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Could not notify Position Update Callback!");
                    }
                }
            }

            private StatusInfo getStatus() {
                return mStatus;
            }

            private PositionGpsInfo getPosition() {
                return mPosition;
            }
        }
    }
}
