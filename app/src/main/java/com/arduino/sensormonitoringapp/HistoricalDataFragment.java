package com.arduino.sensormonitoringapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoricalDataFragment extends Fragment {
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;
    private Spinner dateRangeSpinner;
    private GridLayout heatmapGrid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);

        heatmapGrid = view.findViewById(R.id.heatmap_grid);
        dateRangeSpinner = view.findViewById(R.id.dateRangeSpinner);
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        fetchDataAndPopulateHeatmap();

        return view;
    }

    private void fetchDataAndPopulateHeatmap() {
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Integer> temperatureValues = new ArrayList<>();

                // Extract temperature data from Firebase
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        Object tempObject = timeSnapshot.child("temp").getValue();
                        if (tempObject != null) {
                            int tempValue = Integer.parseInt(tempObject.toString());
                            temperatureValues.add(tempValue);
                        }
                    }
                }

                // Populate the heatmap with data
                populateHeatmap(temperatureValues);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if needed
            }
        });
    }

    private void populateHeatmap(List<Integer> temperatureValues) {
        heatmapGrid.removeAllViews();

        int gridSize = 6; // Προσαρμόσιμο μέγεθος grid (6x6 για αυτό το παράδειγμα)
        int index = 0;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                View cell = new View(getContext());
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(row, 1, 1f);
                params.columnSpec = GridLayout.spec(col, 1, 1f);
                params.setMargins(4, 4, 4, 4);
                cell.setLayoutParams(params);

                // Ελέγχει αν υπάρχουν αρκετές τιμές θερμοκρασίας
                if (index < temperatureValues.size()) {
                    int temperature = temperatureValues.get(index);
                    cell.setBackgroundColor(getColorForTemperature(temperature));
                    index++;
                } else {
                    cell.setBackgroundColor(Color.GRAY); // Χρώμα για κενά κελιά
                }

                heatmapGrid.addView(cell);
            }
        }
    }

    private int getColorForTemperature(int temperature) {
        if (temperature <= 20) {
            return Color.BLUE;
        } else if (temperature <= 25) {
            return Color.GREEN;
        } else if (temperature <= 30) {
            return Color.YELLOW;
        } else {
            return Color.RED;
        }
    }
}
