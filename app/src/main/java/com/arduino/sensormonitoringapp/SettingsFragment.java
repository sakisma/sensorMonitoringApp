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
        // Συνδέει τις προτιμήσεις με το αρχείο XML
        setPreferencesFromResource(R.xml.user_settings, rootKey);

        // Πρόσβαση στην προτίμηση θερμοκρασίας
        EditTextPreference tempThresholdPreference = findPreference("temp_threshold");

        if (tempThresholdPreference != null) {
            // Έλεγχος κατά την αποθήκευση της τιμής
            tempThresholdPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    // Ελέγχει αν η τιμή είναι έγκυρος αριθμός
                    float threshold = Float.parseFloat(newValue.toString());

                    // Μήνυμα επιτυχούς αποθήκευσης
                    Toast.makeText(getContext(), "Threshold set to: " + threshold, Toast.LENGTH_SHORT).show();
                    return true; // Αποδέχεται την αλλαγή
                } catch (NumberFormatException e) {
                    // Εμφανίζει μήνυμα σφάλματος αν η τιμή δεν είναι έγκυρη
                    Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    }

    /**
     * Μέθοδος για ανάγνωση της αποθηκευμένης θερμοκρασίας από τις προτιμήσεις
     */
    public static float getTemperatureThreshold(@NonNull SharedPreferences sharedPreferences) {
        String thresholdString = sharedPreferences.getString("temp_threshold", "30"); // Προεπιλεγμένη τιμή: 30
        try {
            return Float.parseFloat(thresholdString);
        } catch (NumberFormatException e) {
            return 30; // Επιστρέφει την προεπιλεγμένη τιμή αν υπάρχει σφάλμα
        }
    }
}
