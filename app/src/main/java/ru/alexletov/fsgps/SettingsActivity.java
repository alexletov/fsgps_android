package ru.alexletov.fsgps;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import ru.alexletov.fsgps.helpers.Utils;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "FSGPS_PREFERENCE";
    private SharedPreferences mSharedPreferences;
    private EditTextPreference etpPort;
    private EditTextPreference etpHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.main_pref);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        etpPort  = (EditTextPreference) getPreferenceScreen().findPreference("pref_port");
        etpHost  = (EditTextPreference) getPreferenceScreen().findPreference("pref_host");
        etpPort.setOnPreferenceChangeListener(this);
        etpHost.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (etpPort != null)
            etpPort.setOnPreferenceChangeListener(null);

        if (etpHost != null)
            etpHost.setOnPreferenceChangeListener(null);

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        if (item.getItemId() == android.R.id.home) {
            finish();
            if (Utils.useTransitions(mSharedPreferences))
                overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        this.finish();
        if (Utils.useTransitions(mSharedPreferences))
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        Log.d(TAG, "onPreferenceChange");
        if (preference == null)
            return false;
        if (preference.getKey().equals("pref_port"))
            return checkPort(o);
        if (preference.getKey().equals("pref_host"))
            return checkHost(o);
        return true;
    }

    private boolean checkPort(Object o) {
        Log.d(TAG, "checkPort");
        int intPort = 0;
        try {
            intPort = Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            Log.d(TAG, "Port is not a number");
        }

        if (intPort < 1 || intPort > 65535) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.pref_incorrect_port_alert_title)
                    .setMessage(R.string.pref_incorrect_port_alert_message)
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(R.string.ok,
                            new DialogCancelListener());
            AlertDialog alert = builder.create();
            alert.show();
            Log.d(TAG, "Port is not a valid number");
            return false;
        }
        return true;
    }

    private boolean checkHost(Object o) {
        Log.d(TAG, "checkHost");
        String host = o.toString();
        if (!host.matches("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.pref_incorrect_host_alert_title)
                    .setMessage(R.string.pref_incorrect_host_alert_message)
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(R.string.ok,
                            new DialogCancelListener());
            AlertDialog alert = builder.create();
            alert.show();
            Log.d(TAG, "Host does not match IPv4 format");
            return false;
        }
        return true;
    }

    private static class DialogCancelListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
        }
    }
}
