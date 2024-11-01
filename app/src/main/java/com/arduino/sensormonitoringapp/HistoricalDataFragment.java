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

public class HistoricalDataFragment extends Fragment {
    private LineChart lineChart;
//    private HeatMapChart heatMapChart;
    private BarChart barChart;
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;
    private Spinner dateRangeSpinner;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);
        lineChart = view.findViewById(R.id.line_chart);
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        fetchHistoricalData();

        return view;
    }

    private void fetchHistoricalData() {
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Entry> entries = new ArrayList<>();

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String date = dateSnapshot.getKey(); // Get the date (e.g., "2024-09-26")
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        String time = timeSnapshot.getKey(); // Get the time (e.g., "01:38:03")

                        // Combine date and time to create a timestamp string
                        String timestampString = date + " " + time;

                        // Parse the timestamp string into a Date object
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        try {
                            Date timestampDate = dateFormat.parse(timestampString);
                            if (timestampDate != null) {
                                long timestamp = timestampDate.getTime(); // Get the timestamp in milliseconds

                                Object tempObject = timeSnapshot.child("temp").getValue();
                                if (tempObject != null) {
                                    float tempValue = Float.parseFloat(tempObject.toString());

                                    entries.add(new Entry(timestamp, tempValue));
                                }
                            }
                        } catch (ParseException e) {
                            // Handle parsing exception
                            e.printStackTrace();
                        }
                    }
                }

                LineDataSet dataSet = new LineDataSet(entries, "Temperature");
                dataSet.setColor(Color.RED);
                dataSet.setValueTextColor(Color.BLACK);

                List<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(dataSet);

                LineData lineData = new LineData(dataSets);

                lineChart.setData(lineData);


                // Formatting the x-axis to show dates
                XAxis xAxis = lineChart.getXAxis();
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value)
                    {
                        Date date = new Date((long) value);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
                        return sdf.format(date);
                    }
                });

                lineChart.invalidate(); // Refresh the chart
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}
