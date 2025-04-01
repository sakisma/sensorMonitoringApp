package com.arduino.sensormonitoringapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private EditText maxTempThresholdEditText, minTempThresholdEditText;
    private EditText maxMoistureThresholdEditText, minMoistureThresholdEditText;
    private EditText maxHumidityThresholdEditText, minHumidityThresholdEditText;
    private SwitchMaterial notificationsSwitch;
    private SharedPreferences sharedPreferences;
    private DatabaseReference userSettingsRef;
    private MaterialButton saveThresholdsButton;
    private boolean thresholdsChanged = false;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        maxTempThresholdEditText = view.findViewById(R.id.maxTempThresholdEditText);
        minTempThresholdEditText = view.findViewById(R.id.minTempThresholdEditText);
        maxMoistureThresholdEditText = view.findViewById(R.id.maxMoistureThresholdEditText);
        minMoistureThresholdEditText = view.findViewById(R.id.minMoistureThresholdEditText);
        maxHumidityThresholdEditText = view.findViewById(R.id.maxHumidityThresholdEditText);
        minHumidityThresholdEditText = view.findViewById(R.id.minHumidityThresholdEditText);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        saveThresholdsButton = view.findViewById(R.id.saveThresholdsButton);

        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        userSettingsRef = FirebaseDatabase.getInstance().getReference("userSettings");

        loadSettings();

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());

        maxTempThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        minTempThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        maxMoistureThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        minMoistureThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        maxHumidityThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        minHumidityThresholdEditText.addTextChangedListener(thresholdTextWatcher);

        saveThresholdsButton.setOnClickListener(v -> saveThresholds());

        // Send default values only once at the very first application execution
        sendDefaultValuesToFirebaseIfNeeded();

        return view;
    }

    private void sendDefaultValuesToFirebaseIfNeeded() {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        boolean firstLaunch = prefs.getBoolean("firstLaunch", true);

        if (firstLaunch) {
            // Send default values to Firebase
            userSettingsRef.child("notificationsEnabled").setValue(true);
            userSettingsRef.child("maxTempThreshold").setValue(30.0f);
            userSettingsRef.child("maxMoistureThreshold").setValue(100.0f);
            userSettingsRef.child("minTempThreshold").setValue(10.0f);
            userSettingsRef.child("minMoistureThreshold").setValue(50.0f);
            userSettingsRef.child("maxHumidityThreshold").setValue(45.0f);
            userSettingsRef.child("minHumidityThreshold").setValue(15.0f);

            // Update firstLaunch flag
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstLaunch", false);
            editor.apply();
        }
    }

    private void loadSettings() {
        notificationsSwitch.setChecked(sharedPreferences.getBoolean("notificationsEnabled", true));
        maxTempThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("maxTempThreshold", 30.0f)));
        minTempThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("minTempThreshold", 10.0f)));
        maxMoistureThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("maxMoistureThreshold", 100.0f)));
        minMoistureThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("minMoistureThreshold", 50.0f)));
        maxHumidityThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("maxHumidityThreshold", 45.0f)));
        minHumidityThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("minHumidityThreshold", 15.0f)));
    }

    private TextWatcher thresholdTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            thresholdsChanged = true;
            saveThresholdsButton.setEnabled(true);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("notificationsEnabled", notificationsSwitch.isChecked());
        editor.apply();

        // Save to Firebase
        userSettingsRef.child("notificationsEnabled").setValue(notificationsSwitch.isChecked());

        Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void saveThresholds() {
        try {
            float maxTempThreshold = Float.parseFloat(maxTempThresholdEditText.getText().toString());
            float minTempThreshold = Float.parseFloat(minTempThresholdEditText.getText().toString());
            float maxMoistureThreshold = Float.parseFloat(maxMoistureThresholdEditText.getText().toString());
            float minMoistureThreshold = Float.parseFloat(minMoistureThresholdEditText.getText().toString());
            float maxHumidityThreshold = Float.parseFloat(maxHumidityThresholdEditText.getText().toString());
            float minHumidityThreshold = Float.parseFloat(minHumidityThresholdEditText.getText().toString());


            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat("maxTempThreshold", maxTempThreshold);
            editor.putFloat("minTempThreshold", minTempThreshold);
            editor.putFloat("maxMoistureThreshold", maxMoistureThreshold);
            editor.putFloat("minMoistureThreshold", minMoistureThreshold);
            editor.putFloat("maxHumidityThreshold", maxHumidityThreshold);
            editor.putFloat("minHumidityThreshold", minHumidityThreshold);
            editor.apply();

            // Save to Firebase
            userSettingsRef.child("maxTempThreshold").setValue(maxTempThreshold);
            userSettingsRef.child("minTempThreshold").setValue(minTempThreshold);
            userSettingsRef.child("maxMoistureThreshold").setValue(maxMoistureThreshold);
            userSettingsRef.child("minMoistureThreshold").setValue(minMoistureThreshold);
            userSettingsRef.child("maxHumidityThreshold").setValue(maxHumidityThreshold);
            userSettingsRef.child("minHumidityThreshold").setValue(minHumidityThreshold);

            Toast.makeText(getContext(), "Thresholds saved", Toast.LENGTH_SHORT).show();
            thresholdsChanged = false;
            saveThresholdsButton.setEnabled(false);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isNotificationsEnabled() {
        return sharedPreferences.getBoolean("notificationsEnabled", true);
    }

    public Map<String, Float> getMinMaxTempThreshold() {
        Map<String, Float> result = new LinkedHashMap<>();
        result.put("maxTempThreshold", sharedPreferences.getFloat("maxTempThreshold", 30.0f));
        result.put("minTempThreshold", sharedPreferences.getFloat("minTempThreshold", 10.0f));
        return result;
    }

    public Map<String, Float> getMinMaxMoistureThreshold() {
        Map<String, Float> result = new LinkedHashMap<>();
        result.put("maxMoistureThreshold", sharedPreferences.getFloat("maxMoistureThreshold", 100.0f));
        result.put("minMoistureThreshold", sharedPreferences.getFloat("minMoistureThreshold", 50.0f));
        return result;
    }

    public Map<String, Float> getMinMaxHumidityThreshold() {
        Map<String, Float> result = new LinkedHashMap<>();
        result.put("maxHumidityThreshold", sharedPreferences.getFloat("maxHumidityThreshold", 45.0f));
        result.put("minHumidityThreshold", sharedPreferences.getFloat("minHumidityThreshold", 15.0f));
        return result;
    }
}