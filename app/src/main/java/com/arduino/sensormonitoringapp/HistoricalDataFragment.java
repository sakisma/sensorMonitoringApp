package com.arduino.sensormonitoringapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoricalDataFragment extends Fragment {
    private BarChart heatmapChart;
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;
    private Spinner dateRangeSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);

        heatmapChart = view.findViewById(R.id.bar_chart);  // Reusing bar_chart for heatmap simulation
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        fetchHeatmapData();

        return view;
    }

    private void fetchHeatmapData() {
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<BarEntry> entries = new ArrayList<>();
                int index = 0;

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        Object tempObject = timeSnapshot.child("temp").getValue();
                        if (tempObject != null) {
                            float tempValue = Float.parseFloat(tempObject.toString());

                            // Add entry with x-index and temperature as y-value
                            entries.add(new BarEntry(index++, tempValue));
                        }
                    }
                }

                BarDataSet dataSet = new BarDataSet(entries, "Temperature Heatmap");
                dataSet.setColors(generateHeatmapColors(entries));  // Custom method for colors
                BarData barData = new BarData(dataSet);

                heatmapChart.setData(barData);
                heatmapChart.invalidate(); // Refresh chart
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private int[] generateHeatmapColors(List<BarEntry> entries) {
        int[] colors = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            float value = entries.get(i).getY();
            if (value < 15) {
                colors[i] = Color.BLUE;   // Cold
            } else if (value < 25) {
                colors[i] = Color.GREEN;  // Mild
            } else if (value < 35) {
                colors[i] = Color.YELLOW; // Warm
            } else {
                colors[i] = Color.RED;    // Hot
            }
        }
        return colors;
    }
}
