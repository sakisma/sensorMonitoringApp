package com.arduino.sensormonitoringapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private FirebaseDatabase database;
    private DatabaseHelper databaseHelper;
    private DatabaseReference sensorDataRef;
    private BottomNavigationView bottomNavigationView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        setContentView(R.layout.activity_main);

        // Bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);
        loadFragment(new HomeFragment());

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        // Get Token and save it to realtime db.
        setupFirebaseMessaging();

        // Initialize local sqlite database
        databaseHelper = new DatabaseHelper(this);

        // Schedule sync work
        scheduleSync();

        // Sync Realtime db data with local sqlite
        // TODO: OPTIMIZE SYNC to hanlde the volume of 
//        syncDataFromFirebase();
    }

    private void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueue(syncRequest);

        // Optional: Observe work status
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(syncRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        Log.d("SyncWork", "Work state: " + workInfo.getState());
                        if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
                            Log.i("SyncWork", "Sync work enqueued");
                        }
                    }
                });
    }

    private void setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Save token to Firebase Database
                    FirebaseDatabase.getInstance().getReference("/userSettings/fcmToken")
                            .setValue(token)
                            .addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    Log.d(TAG, "Token saved to Firebase Database");
                                } else {
                                    Log.e(TAG, "Failed to save token to Firebase Database", dbTask.getException());
                                }
                            });

                    Log.d(TAG, "FCM Token: " + token);
                });
    }

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
                        Double humidity = timeSnapshot.child("humidity").getValue(Double.class);

                        if (temp != null && moisture != null) {
                            // Insert data into SQLite
                            long rowId = databaseHelper.insertData(date, time, temp, moisture, humidity);
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
        } else if (id == R.id.navigation_statistics) {
            fragment = new StatisticsFragment();
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

