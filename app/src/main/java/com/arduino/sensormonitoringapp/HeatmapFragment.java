package com.arduino.sensormonitoringapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

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

public class HeatmapFragment extends Fragment {
    private GridLayout heatmapGrid;
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heatmap, container, false);

        heatmapGrid = view.findViewById(R.id.heatmap_grid);
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        fetchHeatmapData();

        return view;
    }

    private void fetchHeatmapData() {
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Float> temperatureValues = new ArrayList<>();

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        Object tempObject = timeSnapshot.child("temp").getValue();
                        if (tempObject != null) {
                            float tempValue = Float.parseFloat(tempObject.toString());
                            temperatureValues.add(tempValue);
                        }
                    }
                }

                populateHeatmap(temperatureValues);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void populateHeatmap(List<Float> temperatureValues) {
        // Clear any existing views in the grid
        heatmapGrid.removeAllViews();

        // Calculate number of rows and columns for grid layout
        int totalCells = heatmapGrid.getColumnCount() * heatmapGrid.getRowCount();

        for (int i = 0; i < totalCells && i < temperatureValues.size(); i++) {
            // Create a new View for each cell in the heatmap
            View cell = new View(getContext());

            // Set cell background color based on temperature
            float temperature = temperatureValues.get(i);
            cell.setBackgroundColor(getColorForTemperature(temperature));

            // Set layout parameters for each cell
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.width = 0;
            params.height = 0;
            params.setMargins(4, 4, 4, 4);
            cell.setLayoutParams(params);

            // Add cell to the grid
            heatmapGrid.addView(cell);
        }
    }

    private int getColorForTemperature(float temperature) {
        // Customize these ranges based on your needs
        if (temperature < 15) {
            return Color.BLUE;   // Cold
        } else if (temperature < 25) {
            return Color.GREEN;  // Mild
        } else if (temperature < 35) {
            return Color.YELLOW; // Warm
        } else {
            return Color.RED;    // Hot
        }
    }
}