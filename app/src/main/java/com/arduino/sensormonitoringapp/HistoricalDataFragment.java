package com.arduino.sensormonitoringapp;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arduino.sensormonitoringapp.DatabaseHelper;
import com.arduino.sensormonitoringapp.adapter.DateRangeSpinnerAdapter;
import com.arduino.sensormonitoringapp.helpers.DateRangePickerHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoricalDataFragment extends Fragment {
    private DatabaseHelper databaseHelper;
    private LineChart lineChart;
    private ScatterChart scatterChart;
    private Spinner dateRangeSpinner, graphTypeSpinner;
    private TextView tvStatsSummary;

    private DateRangePickerHelper dateRangeAdapter;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);

        // Initialize UI components
        lineChart = view.findViewById(R.id.lineChart);
        scatterChart = view.findViewById(R.id.scatterChart);
        dateRangeSpinner = view.findViewById(R.id.dateRangeSpinner);
        graphTypeSpinner = view.findViewById(R.id.graphTypeSpinner);
        tvStatsSummary = view.findViewById(R.id.tvStatsSummary);

        databaseHelper = new DatabaseHelper(requireContext());

        setupDateRangeSpinner();
        setupGraphTypeSpinner();
        setupCharts();

        return view;
    }

    private void setupDateRangeSpinner() {
        dateRangeAdapter = new DateRangePickerHelper(requireContext(), (startDate, endDate) -> {
            // Update charts when date range is selected
            updateCharts(startDate, endDate);
        });

        dateRangeSpinner.setAdapter(dateRangeAdapter);
        dateRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dateRangeAdapter.handleDateRangeSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Trigger initial date range selection (last 7 days)
        dateRangeSpinner.setSelection(2);
    }


    private void setupGraphTypeSpinner() {
        String[] graphTypes = {"Line Chart", "Scatter Plot"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, graphTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        graphTypeSpinner.setAdapter(adapter);

        graphTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int dateRangePosition = dateRangeSpinner.getSelectedItemPosition();
                dateRangeAdapter.handleDateRangeSelection(dateRangePosition);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCharts() {
        // Line Chart Configuration
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        // Scatter Chart Configuration
        scatterChart.getDescription().setEnabled(false);
        scatterChart.setTouchEnabled(true);
        scatterChart.setDragEnabled(true);
        scatterChart.setScaleEnabled(true);
    }

    private void updateCharts(Date startDate, Date endDate) {
        try {
            long startMillis = startDate.getTime();
            long endMillis = endDate.getTime();

            // Fetch data
            Cursor rawDataCursor = databaseHelper.getRawData(startMillis, endMillis);

            if (rawDataCursor == null || rawDataCursor.getCount() == 0) {
                // Clear charts and summary if no data
                clearChartsAndSummary();
                return;
            }

            List<Entry> temperatureEntries = new ArrayList<>();
            List<Entry> moistureEntries = new ArrayList<>();

            // Process data
            int timestampIndex = rawDataCursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP);
            int tempIndex = rawDataCursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE);
            int moistureIndex = rawDataCursor.getColumnIndex(DatabaseHelper.COLUMN_MOISTURE);

            while (rawDataCursor.moveToNext()) {
                try {
                    Date timestamp = dateFormat.parse(rawDataCursor.getString(timestampIndex));
                    float temperature = rawDataCursor.getFloat(tempIndex);
                    float moisture = rawDataCursor.getFloat(moistureIndex);

                    float xValue = timestamp.getTime();
                    temperatureEntries.add(new Entry(xValue, temperature));
                    moistureEntries.add(new Entry(xValue, moisture));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            // Update charts based on selected type
            String selectedGraphType = graphTypeSpinner.getSelectedItem().toString();
            lineChart.setVisibility(selectedGraphType.equals("Line Chart") ? View.VISIBLE : View.GONE);
            scatterChart.setVisibility(selectedGraphType.equals("Scatter Plot") ? View.VISIBLE : View.GONE);

            if (selectedGraphType.equals("Line Chart")) {
                setupLineChart(temperatureEntries, moistureEntries);
            } else {
                setupScatterChart(temperatureEntries, moistureEntries);
            }

            // Update statistics summary
            updateStatisticsSummary(rawDataCursor);

            rawDataCursor.close();

        } catch (Exception e) {
            e.printStackTrace();
            clearChartsAndSummary();
        }
    }

    private void clearChartsAndSummary() {
        lineChart.clear();
        scatterChart.clear();
        tvStatsSummary.setText("No data available for selected date range.");
    }

    private void setupLineChart(List<Entry> temperatureEntries, List<Entry> moistureEntries) {
        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "Temperature (°C)");
        temperatureDataSet.setColor(Color.RED);
        temperatureDataSet.setCircleColor(Color.RED);

        LineDataSet moistureDataSet = new LineDataSet(moistureEntries, "Moisture");
        moistureDataSet.setColor(Color.BLUE);
        moistureDataSet.setCircleColor(Color.BLUE);

        LineData lineData = new LineData(temperatureDataSet, moistureDataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new DateValueFormatter());
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        lineChart.invalidate();
    }

    private void setupScatterChart(List<Entry> temperatureEntries, List<Entry> moistureEntries) {
        ScatterDataSet temperatureDataSet = new ScatterDataSet(temperatureEntries, "Temperature (°C)");
        temperatureDataSet.setColor(Color.RED);
        temperatureDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);

        ScatterDataSet moistureDataSet = new ScatterDataSet(moistureEntries, "Moisture");
        moistureDataSet.setColor(Color.BLUE);
        moistureDataSet.setScatterShape(ScatterChart.ScatterShape.SQUARE);

        ScatterData scatterData = new ScatterData(temperatureDataSet, moistureDataSet);
        scatterChart.setData(scatterData);

        XAxis xAxis = scatterChart.getXAxis();
        xAxis.setValueFormatter(new DateValueFormatter());
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        scatterChart.invalidate();
    }

    private void updateStatisticsSummary(Cursor rawDataCursor) {
        // Reset cursor to first position
        if (rawDataCursor != null && rawDataCursor.moveToFirst()) {
            int tempIndex = rawDataCursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE);
            int moistureIndex = rawDataCursor.getColumnIndex(DatabaseHelper.COLUMN_MOISTURE);

            double minTemp = Double.MAX_VALUE, maxTemp = Double.MIN_VALUE;
            double minMoisture = Double.MAX_VALUE, maxMoisture = Double.MIN_VALUE;
            double totalTemp = 0, totalMoisture = 0;
            int count = 0;

            do {
                double temp = rawDataCursor.getDouble(tempIndex);
                double moisture = rawDataCursor.getDouble(moistureIndex);

                minTemp = Math.min(minTemp, temp);
                maxTemp = Math.max(maxTemp, temp);
                minMoisture = Math.min(minMoisture, moisture);
                maxMoisture = Math.max(maxMoisture, moisture);

                totalTemp += temp;
                totalMoisture += moisture;
                count++;
            } while (rawDataCursor.moveToNext());

            String summaryText = String.format(Locale.getDefault(),
                    "Temperature: Min = %.1f°C, Max = %.1f°C, Avg = %.1f°C\n" +
                            "Moisture: Min = %.1f, Max = %.1f, Avg = %.1f",
                    minTemp, maxTemp, totalTemp/count,
                    minMoisture, maxMoisture, totalMoisture/count);

            tvStatsSummary.setText(summaryText);
        } else {
            tvStatsSummary.setText("No data available for selected date range.");
        }
    }
    private static class DateValueFormatter extends ValueFormatter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        @Override
        public String getFormattedValue(float value) {
            return sdf.format(new Date((long) value));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}