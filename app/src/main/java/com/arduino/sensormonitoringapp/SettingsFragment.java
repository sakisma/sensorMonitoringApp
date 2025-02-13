package com.arduino.sensormonitoringapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.user_settings, rootKey);

        EditTextPreference tempThresholdHigh = findPreference("temp_threshold_high");
        EditTextPreference tempThresholdLow = findPreference("temp_threshold_low");

        if (tempThresholdHigh != null) {
            tempThresholdHigh.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    Float.parseFloat(newValue.toString());
                    return true;
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }

        if (tempThresholdLow != null) {
            tempThresholdLow.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    Float.parseFloat(newValue.toString());
                    return true;
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    }

    public static float getHighThreshold(@NonNull SharedPreferences sharedPreferences) {
        String thresholdString = sharedPreferences.getString("temp_threshold_high", "30");
        try {
            return Float.parseFloat(thresholdString);
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public static float getLowThreshold(@NonNull SharedPreferences sharedPreferences) {
        String thresholdString = sharedPreferences.getString("temp_threshold_low", "10");
        try {
            return Float.parseFloat(thresholdString);
        } catch (NumberFormatException e) {
            return 10;
        }
    }
}
