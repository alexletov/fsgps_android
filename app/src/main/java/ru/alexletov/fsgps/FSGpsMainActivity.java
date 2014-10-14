package ru.alexletov.fsgps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import ru.alexletov.fsgps.api.IPositionUpdate;
import ru.alexletov.fsgps.api.IStatusUpdate;
import ru.alexletov.fsgps.helpers.PositionGpsInfo;
import ru.alexletov.fsgps.helpers.StatusInfo;
import ru.alexletov.fsgps.helpers.Utils;

public class FSGpsMainActivity extends ActionBarActivity implements View.OnClickListener {
    private static final String TAG = "FSGPS_ACTIVITY";
    private static WeakReference<FSGpsMainActivity> mActivityRef;

    private ServiceConnectionImpl mConnection = new ServiceConnectionImpl(this);

    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAltitude;
    private TextView tvSpeed;
    private TextView tvStatus;
    private Button bStartStop;

    private SharedPreferences mSharedPreferences;

    private StatusInfo mCurrentStatus;
    private PositionGpsInfo mCurrentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fsgps_main);

        findViewById(R.id.bStart).setOnClickListener(this);
        mActivityRef = new WeakReference<FSGpsMainActivity>(this);

        tvLatitude = (TextView) findViewById(R.id.tvLatitudeVal);
        tvLongitude = (TextView) findViewById(R.id.tvLongitudeVal);
        tvAltitude = (TextView) findViewById(R.id.tvAltitudeVal);
        tvSpeed = (TextView) findViewById(R.id.tvSpeedVal);
        tvStatus = (TextView) findViewById(R.id.tvStartedValue);
        bStartStop = (Button) findViewById(R.id.bStart);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        Log.d(TAG, mConnection.toString());
        Intent intent = new Intent(this, GpsService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

        if (mConnection.canUnbind()) {
            unbindService(mConnection);
            mConnection.setUnbinded();
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        boolean canResume = true;

        try {
            if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION) != 1) {
                canResume = false;
            }
        } catch (Settings.SettingNotFoundException e) {
            canResume = false;
            Log.d(TAG, Settings.Secure.ALLOW_MOCK_LOCATION + " not found in Settings.Secure");
        }

        if (!canResume) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.allow_mock_alert_title)
                    .setMessage(R.string.allow_mock_alert_message)
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                    finish();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
            Log.w(TAG, "Allow Mock Locations setting is disabled!");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        Intent serviceIntent = new Intent(this, GpsService.class);

        StatusInfo info = mConnection == null ? null : mConnection.getStatusImmediate();
        if (info == null || info.getStatus() == StatusInfo.Status.STATUS_DISCONNECTED) {
            Log.d(TAG, "Stopping service");
            stopService(serviceIntent);
        }

        if (mActivityRef != null) {
            mActivityRef.clear();
            mActivityRef = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fsgps_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            if (Utils.useTransitions(mSharedPreferences))
                overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
            return true;
        }

        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            if (Utils.useTransitions(mSharedPreferences))
                overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static boolean checkActivityForExist(Activity activity) {
        return activity != null && !activity.isDestroyed() && !activity.isFinishing();
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick");
        Log.d(TAG, "Current status: " + mCurrentStatus);
        if (mCurrentStatus == null || mCurrentStatus.getStatus() == StatusInfo.Status.STATUS_DISCONNECTED) {
            mConnection.startEmulate();
        } else {
            mConnection.stopEmulate();
        }

        bStartStop.setEnabled(false);
    }

    private void updateStatus(StatusInfo info) {
        Log.d(TAG, "updateStatus");
        mCurrentStatus = info;
        Log.d(TAG, "Status updated to: " + (mCurrentStatus == null ? "<null>" : mCurrentStatus.getStatus()));
    }

    private void updatePosition(PositionGpsInfo info) {
        Log.d(TAG, "updatePosition");
        mCurrentPosition = info;
        Log.d(TAG, "Position updated to: " + (mCurrentPosition == null ? "<null>" : mCurrentPosition.toString()));
    }

    private class ServiceConnectionImpl implements ServiceConnection {
        private WeakReference<FSGpsMainActivity> mActivityRef;
        private boolean mBound;
        private IGpsServiceApi mService;

        private StatusUpdateCallback mStatusUpdateCallback;
        private PositionUpdateCallback mPositionUpdateCallback;

        public ServiceConnectionImpl(FSGpsMainActivity activity) {
            mActivityRef = new WeakReference<FSGpsMainActivity>(activity);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected");
            mBound = true;

            mService = IGpsServiceApi.Stub.asInterface(iBinder);

            FSGpsMainActivity fsActivity = mActivityRef.get();
            if (!FSGpsMainActivity.checkActivityForExist(fsActivity)) {
                Log.e(TAG, "Main Activity does not exists!");
                return;
            }

            mStatusUpdateCallback = new StatusUpdateCallback(fsActivity);
            mPositionUpdateCallback = new PositionUpdateCallback(fsActivity);

            try {
                mService.registerStatusCallback(mStatusUpdateCallback);
                updateStatus(mService.getStatus());
            } catch (RemoteException e) {
                Log.e(TAG, "Could not register status update callback");
            }

            try {
                mService.registerPositionCallback(mPositionUpdateCallback);
                updatePosition(mService.getPosition());
            } catch (RemoteException e) {
                Log.e(TAG, "Could not register status update callback");
            }

            if (fsActivity.bStartStop != null) {
                fsActivity.bStartStop.setEnabled(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            mBound = false;
            mService = null;

            FSGpsMainActivity fsActivity = mActivityRef.get();
            if (!FSGpsMainActivity.checkActivityForExist(fsActivity)) {
                Log.e(TAG, "Main Activity does not exists!");
                return;
            }

            if (fsActivity.bStartStop != null) {
                fsActivity.bStartStop.setEnabled(false);
            }
        }

        private void startEmulate() {
            Log.d(TAG, "startEmulate");
            if (!mBound || mService == null) {
                return;
            }

            try {
                mService.startEmulate();
            } catch (RemoteException e) {
                Log.d(TAG, "RemoteException");
            }
        }

        private void stopEmulate() {
            Log.d(TAG, "stopEmulate");
            if (!mBound || mService == null) {
                return;
            }

            try {
                mService.stopEmulate();
            } catch (RemoteException e) {
                Log.d(TAG, "RemoteException");
            }
        }

        private void registerPositionCallback(IPositionUpdate callback) {
            Log.d(TAG, "registerPositionCallback");
            if (!mBound || mService == null) {
                return;
            }

            try {
                mService.registerPositionCallback(callback);
            } catch (RemoteException e) {
                Log.d(TAG, "RemoteException");
            }
        }

        private void unregisterPositionCallback() {
            Log.d(TAG, "unregisterPositionCallback");
            if (!mBound || mService == null) {
                return;
            }

            try {
                mService.unregisterPositionCallback();
            } catch (RemoteException e) {
                Log.d(TAG, "RemoteException");
            }
        }

        private void registerStatusCallback(IStatusUpdate callback) {
            Log.d(TAG, "registerStatusCallback");
            if (!mBound || mService == null) {
                return;
            }

            try {
                mService.registerStatusCallback(callback);
            } catch (RemoteException e) {
                Log.d(TAG, "RemoteException");
            }
        }

        private void unregisterStatusCallback() {
            Log.d(TAG, "unregisterStatusCallback");
            if (!mBound || mService == null) {
                return;
            }

            try {
                mService.unregisterStatusCallback();
            } catch (RemoteException e) {
                Log.d(TAG, "RemoteException");
            }
        }

        private boolean canUnbind() {
            return mBound;
        }

        private void setUnbinded() {
            mBound = false;
        }

        private StatusInfo getStatusImmediate() {
            Log.d(TAG, "getStatusImmediate");
            if (!mBound || mService == null) {
                return null;
            }
            try {
                return mService.getStatus();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void renderPosition() {
        if (tvLatitude != null
                && tvLongitude != null
                && tvAltitude != null
                && tvSpeed != null) {
            if (mCurrentPosition != null) {
                tvLatitude.setText(mCurrentPosition.getLatitudeFormatted());
                tvLongitude.setText(mCurrentPosition.getLongitudeFormatted());
                tvAltitude.setText(Integer.toString((int) mCurrentPosition.getAltitude()) + " m");
                tvSpeed.setText(Integer.toString((int) mCurrentPosition.getSpeed()) + " kt");
            } else {
                tvLatitude.setText("-");
                tvLongitude.setText("-");
                tvAltitude.setText("- m");
                tvSpeed.setText("- kt");
            }
        }
    }

    public void renderStatus() {
        if (mCurrentStatus != null && tvStatus != null) {
            switch (mCurrentStatus.getStatus()) {
                case STATUS_DISCONNECTED:
                    tvStatus.setText(R.string.status_disconnected);
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    bStartStop.setText(R.string.string_start);
                    bStartStop.setEnabled(true);

                    updatePosition(null);
                    renderPosition();

                    break;
                case STATUS_CONNECTED:
                    tvStatus.setText(R.string.status_connected);
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    bStartStop.setText(R.string.string_stop);
                    bStartStop.setEnabled(true);
                    break;
                case STATUS_CONNECTING:
                    tvStatus.setText(R.string.status_connecting);
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    bStartStop.setText(R.string.string_stop);
                    bStartStop.setEnabled(true);
                    break;
            }
        }
    }

    private static class StatusUpdateCallback extends IStatusUpdate.Stub {
        private WeakReference<FSGpsMainActivity> mActivityRef;

        public StatusUpdateCallback(FSGpsMainActivity activity) {
            mActivityRef = new WeakReference<FSGpsMainActivity>(activity);
        }

        @Override
        public boolean update(StatusInfo status) throws RemoteException {
            Log.d(TAG, "UpdateStatus callback");
            FSGpsMainActivity activity = mActivityRef.get();
            if (!Utils.checkActivityLive(activity)) {
                Log.e(TAG, "Activity object already removed.");
                return false;
            }
            activity.updateStatus(status);
            UpdateStatus updateStatus = new UpdateStatus(activity);
            activity.runOnUiThread(updateStatus);
            return true;
        }
    }

    private static class PositionUpdateCallback extends IPositionUpdate.Stub {
        private WeakReference<FSGpsMainActivity> mActivityRef;

        public PositionUpdateCallback(FSGpsMainActivity activity) {
            mActivityRef = new WeakReference<FSGpsMainActivity>(activity);
        }

        @Override
        public boolean update(PositionGpsInfo info) throws RemoteException {
            Log.d(TAG, "UpdatePosition callback");
            FSGpsMainActivity activity = mActivityRef.get();
            if (!Utils.checkActivityLive(activity)) {
                Log.e(TAG, "Activity object already removed.");
                return false;
            };
            activity.updatePosition(info);
            UpdatePosition updatePosition = new UpdatePosition(activity);
            activity.runOnUiThread(updatePosition);
            return true;
        }
    }

    private static class UpdateStatus implements Runnable {
        private WeakReference<FSGpsMainActivity> mActivityRef;

        public UpdateStatus(FSGpsMainActivity activity) {
            mActivityRef = new WeakReference<FSGpsMainActivity>(activity);
        }

        @Override
        public void run() {
            Log.d(TAG, "UpdateStatus UI thread");
            FSGpsMainActivity activity = mActivityRef.get();
            if (!Utils.checkActivityLive(activity)) {
                Log.e(TAG, "Activity object already removed.");
                return;
            }

            activity.renderStatus();
        }
    }

    private static class UpdatePosition implements Runnable {
        private WeakReference<FSGpsMainActivity> mActivityRef;

        public UpdatePosition(FSGpsMainActivity activity) {
            mActivityRef = new WeakReference<FSGpsMainActivity>(activity);
        }

        @Override
        public void run() {
            Log.d(TAG, "UpdatePosition UI thread");
            FSGpsMainActivity activity = mActivityRef.get();
            if (!Utils.checkActivityLive(activity)) {
                Log.e(TAG, "Activity object already removed.");
                return;
            }

            activity.renderPosition();
        }
    }
}
