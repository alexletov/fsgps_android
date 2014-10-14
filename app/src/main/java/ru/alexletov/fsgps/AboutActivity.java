package ru.alexletov.fsgps;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import ru.alexletov.fsgps.helpers.Utils;


public class AboutActivity extends Activity {

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_about);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        this.finish();
        if (Utils.useTransitions(mSharedPreferences))
            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
    }
}
