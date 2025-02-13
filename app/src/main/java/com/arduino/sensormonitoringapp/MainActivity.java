package com.arduino.sensormonitoringapp;

import android.content.Intent;
import android.os.Bundle;
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
    private DatabaseReference sensorDataRef;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);
        loadFragment(new HomeFragment());

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        // Fetch latest data and update the UI
        fetchLatestData();

        // Start the Temperature Monitoring Service
        Intent serviceIntent = new Intent(this, TemperatureMonitorService.class);
        startService(serviceIntent);

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

