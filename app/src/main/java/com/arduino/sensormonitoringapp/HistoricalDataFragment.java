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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.charting.charts.HeatMapChart;
import com.charting.data.HeatMap;

public class HistoricalDataFragment extends Fragment {
    private LineChart lineChart;
    private BarChart barChart;
    private HeatMapChart heatMapChart;
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;
    private Spinner dateRangeSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);
        lineChart = view.findViewById(R.id.line_chart);
        barChart = view.findViewById(R.id.bar_chart);
        heatMapChart = view.findViewById(R.id.heatmap_chart);  // Initialize HeatMapChart
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        fetchHistoricalData();

        return view;
    }

    private void fetchHistoricalData() {
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Entry> lineChartEntries = new ArrayList<>();
                List<HeatMap.DataPoint> heatMapEntries = new ArrayList<>();  // To store heatmap data

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String date = dateSnapshot.getKey();
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        String time = timeSnapshot.getKey();
                        String timestampString = date + " " + time;
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                        try {
                            Date timestampDate = dateFormat.parse(timestampString);
                            if (timestampDate != null) {
                                long timestamp = timestampDate.getTime();

                                Object tempObject = timeSnapshot.child("temp").getValue();
                                if (tempObject != null) {
                                    float tempValue = Float.parseFloat(tempObject.toString());

                                    // Add data to line chart entries
                                    lineChartEntries.add(new Entry(timestamp, tempValue));

                                    // Process data for the heatmap
                                    int hour = Integer.parseInt(time.split(":")[0]);  // Get the hour of the day
                                    heatMapEntries.add(new HeatMap.DataPoint(hour, tempValue));
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Set data for the line chart
                LineDataSet dataSet = new LineDataSet(lineChartEntries, "Temperature");
                dataSet.setColor(Color.RED);
                dataSet.setValueTextColor(Color.BLACK);

                List<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(dataSet);
                LineData lineData = new LineData(dataSets);
                lineChart.setData(lineData);

                // Format the x-axis
                XAxis xAxis = lineChart.getXAxis();
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        Date date = new Date((long) value);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
                        return sdf.format(date);
                    }
                });

                lineChart.invalidate();  // Refresh line chart

                // Set data for the heatmap
                HeatMap heatMap = new HeatMap();
                heatMap.addAll(heatMapEntries);  // Add all data points to the heatmap
                heatMapChart.setHeatMap(heatMap);
                heatMapChart.invalidate();  // Refresh heatmap
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}
