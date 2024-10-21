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

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;
    private Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize the Drawer Layout and Toggle
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();  // This will sync the hamburger icon

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        // Fetch latest data and update the UI
        fetchLatestData();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Stay on the main screen
        } else if (id == R.id.nav_historical_view) {
            Intent intent = new Intent(MainActivity.this, HistoricalViewActivity.class);
            startActivity(intent);
            // Open Historical View activity
            // Example: Intent intent = new Intent(MainActivity.this, HistoricalViewActivity.class);
            // startActivity(intent);
        }
        drawerLayout.closeDrawers();  // Close the drawer after selection
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
}

//    private void fetchLatestData() {
//        // Query to get the latest entry from Firebase
//        sensorDataRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    String latestTemp = snapshot.child("temp").getValue().toString();
//                    String latestMoisture = snapshot.child("moisture").getValue().toString();
//
//                    // Update the UI with the latest values
//                    TextView tempValueText = findViewById(R.id.tempValue);
//                    TextView moistureValueText = findViewById(R.id.moistureValue);
//
//                    tempValueText.setText(latestTemp + " °C");
//                    moistureValueText.setText(latestMoisture + " %");
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Handle database error
//            }
//        });
//    }