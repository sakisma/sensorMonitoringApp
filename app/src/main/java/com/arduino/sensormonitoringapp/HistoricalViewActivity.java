package com.arduino.sensormonitoringapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistoricalViewActivity extends AppCompatActivity {
    private LineChart lineChart;
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;
    private Spinner dateRangeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_view);

        lineChart = findViewById(R.id.chart);
        dateRangeSpinner = findViewById(R.id.dateRangeSpinner);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        // Setup date range spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.date_ranges, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateRangeSpinner.setAdapter(adapter);

        // Listen for date range selection
        dateRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRange = dateRangeSpinner.getSelectedItem().toString();
                fetchHistoricalData(selectedRange);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void fetchHistoricalData(String dateRange) {
        // Fetch data based on date range and plot it on the chart
        Calendar calendar = Calendar.getInstance();
        List<Entry> tempEntries = new ArrayList<>();
        List<Entry> moistureEntries = new ArrayList<>();

        // Logic to query Firebase for the selected date range goes here
        // Example for last day:
        if (dateRange.equals("Daily")) {
            calendar.add(Calendar.DATE, -1);
        }

        // Plot the fetched data on the chart
        LineDataSet tempDataSet = new LineDataSet(tempEntries, "Temperature (°C)");
        LineDataSet moistureDataSet = new LineDataSet(moistureEntries, "Soil Moisture");

        LineData lineData = new LineData(tempDataSet, moistureDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }
}
