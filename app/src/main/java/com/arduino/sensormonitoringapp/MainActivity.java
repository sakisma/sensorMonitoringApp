package com.arduino.sensormonitoringapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private FirebaseDatabase database;
    private DatabaseHelper databaseHelper;
    private DatabaseReference sensorDataRef;
    private BottomNavigationView bottomNavigationView;
    private static final String TAG = "MainActivity";

    //Initializes UI, Firebase, SQlite DB, Sync and save the FCM token
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
    }

    //Schedules Sync with work manager
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

    //Takes FCM token and Saves it in Firebase
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

    @Override
    protected void onResume() {
        super.onResume();
//        fetchLatestData();
    }

    //Load the Fragments by user command
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

    //Replace the fragment with the users choice
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content_frame,fragment);
        transaction.commit();
    }
}

