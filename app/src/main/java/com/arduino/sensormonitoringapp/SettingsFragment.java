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

public class SettingsFragment extends Fragment {

    //private EditText tempThresholdEditText, moistureThresholdEditText;
    private EditText maxTempThresholdEditText, minTempThresholdEditText;
    private EditText maxMoistureThresholdEditText, minMoistureThresholdEditText;
    private SwitchMaterial notificationsSwitch;
    private SharedPreferences sharedPreferences;
    private DatabaseReference userSettingsRef;
    private MaterialButton saveThresholdsButton;
    private boolean thresholdsChanged = false;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        //tempThresholdEditText = view.findViewById(R.id.tempThresholdEditText);
        //moistureThresholdEditText = view.findViewById(R.id.moistureThresholdEditText);
        maxTempThresholdEditText = view.findViewById(R.id.maxTempThresholdEditText);
        minTempThresholdEditText = view.findViewById(R.id.minTempThresholdEditText);
        maxMoistureThresholdEditText = view.findViewById(R.id.maxMoistureThresholdEditText);
        minMoistureThresholdEditText = view.findViewById(R.id.minMoistureThresholdEditText);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        saveThresholdsButton = view.findViewById(R.id.saveThresholdsButton);

        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        userSettingsRef = FirebaseDatabase.getInstance().getReference("userSettings");

        loadSettings();

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());

        //tempThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        //moistureThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        maxTempThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        minTempThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        maxMoistureThresholdEditText.addTextChangedListener(thresholdTextWatcher);
        minMoistureThresholdEditText.addTextChangedListener(thresholdTextWatcher);

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
            //userSettingsRef.child("tempThreshold").setValue(30.0f);
            //userSettingsRef.child("moistureThreshold").setValue(70.0f);
            userSettingsRef.child("notificationsEnabled").setValue(true);
            userSettingsRef.child("maxTempThreshold").setValue(30.0f);
            userSettingsRef.child("maxMoistureThreshold").setValue(100.0f);
            userSettingsRef.child("minTempThreshold").setValue(10.0f);
            userSettingsRef.child("minMoistureThreshold").setValue(50.0f);

            // Update firstLaunch flag
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstLaunch", false);
            editor.apply();
        }
    }


    private void loadSettings() {
        notificationsSwitch.setChecked(sharedPreferences.getBoolean("notificationsEnabled", true));
        //tempThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("tempThreshold", 30.0f)));
        //moistureThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("moistureThreshold", 70.0f)));
        maxTempThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("maxTempThreshold", 30.0f)));
        minTempThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("minTempThreshold", 10.0f)));
        maxMoistureThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("maxMoistureThreshold", 100.0f)));
        minMoistureThresholdEditText.setText(String.valueOf(sharedPreferences.getFloat("minMoistureThreshold", 50.0f)));

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
            //float tempThreshold = Float.parseFloat(tempThresholdEditText.getText().toString());
            //float moistureThreshold = Float.parseFloat(moistureThresholdEditText.getText().toString());
            float maxTempThreshold = Float.parseFloat(maxTempThresholdEditText.getText().toString());
            float minTempThreshold = Float.parseFloat(minTempThresholdEditText.getText().toString());
            float maxMoistureThreshold = Float.parseFloat(maxMoistureThresholdEditText.getText().toString());
            float minMoistureThreshold = Float.parseFloat(minMoistureThresholdEditText.getText().toString());

            SharedPreferences.Editor editor = sharedPreferences.edit();
            //editor.putFloat("tempThreshold", tempThreshold);
            //editor.putFloat("moistureThreshold", moistureThreshold);
            editor.putFloat("maxTempThreshold", maxTempThreshold);
            editor.putFloat("minTempThreshold", minTempThreshold);
            editor.putFloat("maxMoistureThreshold", maxMoistureThreshold);
            editor.putFloat("minMoistureThreshold", minMoistureThreshold);
            editor.apply();

            // Save to Firebase
            //userSettingsRef.child("tempThreshold").setValue(tempThreshold);
            //userSettingsRef.child("moistureThreshold").setValue(moistureThreshold);
            userSettingsRef.child("maxTempThreshold").setValue(maxTempThreshold);
            userSettingsRef.child("minTempThreshold").setValue(minTempThreshold);
            userSettingsRef.child("maxMoistureThreshold").setValue(maxMoistureThreshold);
            userSettingsRef.child("minMoistureThreshold").setValue(minMoistureThreshold);

            Toast.makeText(getContext(), "Thresholds saved", Toast.LENGTH_SHORT).show();
            thresholdsChanged = false;
            saveThresholdsButton.setEnabled(false);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
        }
    }

//    private void saveSettings() {
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean("notificationsEnabled", notificationsSwitch.isChecked());
//        try {
//            float tempThreshold = Float.parseFloat(tempThresholdEditText.getText().toString());
//            float moistureThreshold = Float.parseFloat(moistureThresholdEditText.getText().toString());
//            editor.putFloat("tempThreshold", tempThreshold);
//            editor.putFloat("moistureThreshold", moistureThreshold);
//
//            // Save to Firebase
//            userLimitsRef.child("tempThreshold").setValue(tempThreshold);
//            userLimitsRef.child("moistureThreshold").setValue(moistureThreshold);
//
//            Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show();
//        } catch (NumberFormatException e) {
//            Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
//        }
//        editor.apply();
//    }


    public boolean isNotificationsEnabled() {
        return sharedPreferences.getBoolean("notificationsEnabled", true);
    }

    public float getTempThreshold() {
        return sharedPreferences.getFloat("tempThreshold", 30.0f);
    }

    public float getMoistureThreshold() {
        return sharedPreferences.getFloat("moistureThreshold", 70.0f);
    }
}