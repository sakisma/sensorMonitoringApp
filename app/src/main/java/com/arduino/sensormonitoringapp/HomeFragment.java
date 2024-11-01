package com.arduino.sensormonitoringapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");
        fetchLatestData();

        return view;
    }

    private void fetchLatestData() {
        // Query to get the latest date entry from Firebase
        sensorDataRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String latestDate = dateSnapshot.getKey();  // Get the latest date (e.g., "2024-09-26")

                    // Query to get the latest time entry under the latest date
                    DatabaseReference timeRef = sensorDataRef.child(latestDate);
                    timeRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
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
                                    TextView tempValueText = requireView().findViewById(R.id.tempValue);
                                    TextView moistureValueText = requireView().findViewById(R.id.moistureValue);

                                    tempValueText.setText(latestTemp + " °C");
                                    moistureValueText.setText(latestMoisture + " %");
                                } else {
                                    // Handle missing or null values here (optional)
                                    TextView tempValueText = requireView().findViewById(R.id.tempValue);
                                    TextView moistureValueText = requireView().findViewById(R.id.moistureValue);

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
