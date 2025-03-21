package com.arduino.sensormonitoringapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private FirebaseDatabase database;
    private DatabaseHelper databaseHelper;
    private DatabaseReference sensorDataRef;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        // todo set this up to false.
        boolean isSetupCompleted = prefs.getBoolean("setup_completed", true);

        if (!isSetupCompleted) {
            startActivity(new Intent(this, SetupArduinoWifi.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);

            // Bottom navigation
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setOnItemSelectedListener(this);
            loadFragment(new HomeFragment());

//            // Initialize Firebase Database
            database = FirebaseDatabase.getInstance();
            sensorDataRef = database.getReference("sensorData");

            databaseHelper = new DatabaseHelper(this);

            syncDataFromFirebase();
        }
    }

//    private void syncDataFromFirebase() {
////        TODO: only sync records for the day up to previous of the current day of app execution and comment out deletion.
//        sensorDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
//                    String date = dateSnapshot.getKey(); // Get the date (e.g., "2024-09-26")
//                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
//                        String time = timeSnapshot.getKey(); // Get the time (e.g., "01:38:03")
//                        Double temp = timeSnapshot.child("temp").getValue(Double.class);
//                        Double moisture = timeSnapshot.child("moisture").getValue(Double.class);
//
//                        if (temp != null && moisture != null) {
//                            // Insert data into SQLite
//                            long rowId = databaseHelper.insertData(date, time, temp, moisture);
//                            if (rowId != -1) {
//                                // Data inserted successfully, mark as synced in Firebase
//                                timeSnapshot.getRef().child("sync").setValue(false)
//                                        .addOnCompleteListener(task -> {
//                                            if (task.isSuccessful()) {
//                                                // Delete synced record from Firebase
////                                                timeSnapshot.getRef().removeValue();
//                                                Log.i("firebase delete", "onDataChange: data deleted");
//                                            }
//                                        });
//                            }
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Handle Firebase error
//            }
//        });
//    }

    private void syncDataFromFirebase() {
        sensorDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey(); // Get the date (e.g., "2024-09-26")
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        String time = timeSnapshot.getKey(); // Get the time (e.g., "01:38:03")

                        // Check if the record has already been synced
                        Boolean isSynced = timeSnapshot.child("sync").getValue(Boolean.class);
                        if (isSynced != null && isSynced) {
                            // Skip this record because it has already been synced
                            continue;
                        }

                        Double temp = timeSnapshot.child("temp").getValue(Double.class);
                        Double moisture = timeSnapshot.child("moisture").getValue(Double.class);

                        if (temp != null && moisture != null) {
                            // Insert data into SQLite
                            long rowId = databaseHelper.insertData(date, time, temp, moisture);
                            if (rowId != -1) {
                                // Data inserted successfully, mark as synced in Firebase
                                timeSnapshot.getRef().child("sync")
                                        .setValue(true)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Log.i("FirebaseSync", "Data marked as synced: " + date + " " + time);
                                            } else {
                                                Log.e("FirebaseSync", "Failed to mark data as synced: " + date + " " + time);
                                            }
                                        });
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseSync", "Database error: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
//        fetchLatestData();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment;
        int id = item.getItemId();

        if (id == R.id.navigation_home) {
            fragment = new HomeFragment();
        } else if (id == R.id.navigation_historical_data) {
            fragment = new HistoricalDataFragment();
        } else if (id == R.id.navigation_heatmap) {
            fragment = new HeatmapFragment();
        } else if (id == R.id.navigation_settings) {
            fragment = new SettingsFragment();
        } else {
            return false;
        }
        loadFragment(fragment);
        return true;
    }

    private void fetchLatestData() {
        // Query to get the latest date entry from Firebase
        sensorDataRef.limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String latestDate = dateSnapshot.getKey();  // Get the latest date (e.g., "2024-09-26")

                    // Query to get the latest time entry under the latest date
                    DatabaseReference timeRef = sensorDataRef.child(latestDate);
                    timeRef.limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot timeSnapshot) {
                            for (DataSnapshot timeEntry : timeSnapshot.getChildren()) {
                                String latestTime = timeEntry.getKey();  // Get the latest time (e.g., "01:38:03")

                                // Fetch the latest temperature and moisture values
                                Object tempObject = timeEntry.child("temp").getValue();
                                Object moistureObject = timeEntry.child("moisture").getValue();

                                // Check if temp and moisture values are not null
                                if (tempObject != null && moistureObject != null) {
                                    String latestTemp = tempObject.toString();
                                    String latestMoisture = moistureObject.toString();

                                    // Update the UI with the latest values
                                    TextView tempValueText = findViewById(R.id.tempValue);
                                    TextView moistureValueText = findViewById(R.id.moistureValue);

                                    tempValueText.setText(latestTemp + " °C");
                                    moistureValueText.setText(latestMoisture + " %");
                                } else {
                                    // Handle missing or null values here (optional)
                                    TextView tempValueText = findViewById(R.id.tempValue);
                                    TextView moistureValueText = findViewById(R.id.moistureValue);

                                    tempValueText.setText("-- °C");
                                    moistureValueText.setText("-- %");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle database error
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
            }
        });
    }


    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_frame,fragment);
        transaction.commit();
    }
}

