package com.arduino.sensormonitoringapp;

import static com.arduino.sensormonitoringapp.DatabaseHelper.COLUMN_MOISTURE;
import static com.arduino.sensormonitoringapp.DatabaseHelper.COLUMN_TEMPERATURE;
import static com.arduino.sensormonitoringapp.DatabaseHelper.COLUMN_TIMESTAMP;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
//import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;

public class HistoricalDataFragment extends Fragment {

    private Spinner chartTypeSpinner;
    private Button btnLast24Hours, btnLast7Days, btnLast30Days, btnCustomRange;
    private FrameLayout chartContainer;
    private TextView textMinTemp, textMaxTemp, textAvgTemp;
    private DatabaseHelper dbHelper;
    private LineChart chart;
    private String selectedChartType = "Temperature";  // Default chart type

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_historical_data, container, false);

        // Initialize views
        chartTypeSpinner = rootView.findViewById(R.id.spinnerChartType);
        btnLast24Hours = rootView.findViewById(R.id.btnLast24Hours);
        btnLast7Days = rootView.findViewById(R.id.btnLast7Days);
        btnLast30Days = rootView.findViewById(R.id.btnLast30Days);
        btnCustomRange = rootView.findViewById(R.id.btnCustomRange);
        chartContainer = rootView.findViewById(R.id.chartContainer);
        textMinTemp = rootView.findViewById(R.id.textMinTemp);
        textMaxTemp = rootView.findViewById(R.id.textMaxTemp);
        textAvgTemp = rootView.findViewById(R.id.textAvgTemp);

        dbHelper = new DatabaseHelper(getContext());

        // Initialize chart
        chart = new LineChart(getContext());
        chartContainer.addView(chart);

        // Set up chart type spinner
        // Set up chart type spinner
        chartTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedChartType = parentView.getItemAtPosition(position).toString();
                updateChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        // Set up date range buttons
        btnLast24Hours.setOnClickListener(v -> filterData("24"));
        btnLast7Days.setOnClickListener(v -> filterData("7"));
        btnLast30Days.setOnClickListener(v -> filterData("30"));
        btnCustomRange.setOnClickListener(v -> showCustomDatePicker());

        return rootView;
    }

    private void filterData(String range) {
        long startMillis = System.currentTimeMillis();
        long endMillis = startMillis;

        switch (range) {
            case "24":
                startMillis -= 24 * 60 * 60 * 1000;
                break;
            case "7":
                startMillis -= 7 * 24 * 60 * 60 * 1000;
                break;
            case "30":
                startMillis -= 30 * 24 * 60 * 60 * 1000;
                break;
        }

        updateChart(startMillis, endMillis);
    }

    private void showCustomDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Handle date picker result
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show(getFragmentManager(), "DatePickerDialog");
    }


    private void showEndDatePicker(Calendar startDate) {
        DatePickerDialog endDatePickerDialog = DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Handle end date selection
                    Calendar endDate = Calendar.getInstance();
                    endDate.set(year, monthOfYear, dayOfMonth);
                    updateChart(startDate.getTimeInMillis(), endDate.getTimeInMillis());
                },
                startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH));
        endDatePickerDialog.setMinDate(startDate); // Ensure end date is after start date
        endDatePickerDialog.show(getFragmentManager(), "EndDatePickerDialog");
    }

    private void updateChart(long startMillis, long endMillis) {
        Cursor cursor = dbHelper.getDailyAverages(startMillis, endMillis);

        if (cursor != null) {
            ArrayList<Entry> entries = new ArrayList<>();
            ArrayList<String> xVals = new ArrayList<>();
            float minTemp = Float.MAX_VALUE;
            float maxTemp = Float.MIN_VALUE;
            float avgTemp = 0;
            int count = 0;

            // Check if the column exists
            int timestampIndex = cursor.getColumnIndex("day"); // In the query, the alias is "day"
            if (timestampIndex == -1) {
                Log.e("Cursor Error", "Day column not found");
                return; // Exit if the column is not found
            }

            // Column indices for calculated values
            int avgTempIndex = cursor.getColumnIndex("avg_temp"); // AVG temperature
            int avgMoistureIndex = cursor.getColumnIndex("avg_moisture"); // AVG moisture
            int minTempIndex = cursor.getColumnIndex("min_temp"); // MIN temperature
            int maxTempIndex = cursor.getColumnIndex("max_temp"); // MAX temperature

            while (cursor.moveToNext()) {
                // Get the date
                String date = cursor.getString(timestampIndex); // "day" column from query

                // Get the calculated temperature and moisture values
                float temp = cursor.getFloat(avgTempIndex);
                float moisture = cursor.getFloat(avgMoistureIndex);

                // Depending on the selected chart type, add the relevant data
                float valueToPlot = 0;

                if ("Temperature".equals(selectedChartType)) {
                    valueToPlot = temp;
                } else if ("Moisture".equals(selectedChartType)) {
                    valueToPlot = moisture;
                } else if ("Avg Temperature".equals(selectedChartType)) {
                    valueToPlot = temp;
                }

                // Add the data entry to the chart
                entries.add(new Entry(valueToPlot, entries.size()));
                xVals.add(date);

                // Update min, max, and average temperature values
                minTemp = Math.min(minTemp, temp);
                maxTemp = Math.max(maxTemp, temp);
                avgTemp += temp;
                count++;
            }

            if (count > 0) {
                avgTemp /= count;
            }

            // Update the summary values
            textMinTemp.setText("Min Temp: " + minTemp + "°C");
            textMaxTemp.setText("Max Temp: " + maxTemp + "°C");
            textAvgTemp.setText("Avg Temp: " + avgTemp + "°C");

            // Set up the chart with data
            LineDataSet dataSet = new LineDataSet(entries, selectedChartType);
            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.invalidate(); // Refresh the chart
        }
    }

    private void updateChart() {
        // Call updateChart with the last 24 hours data
        long startMillis = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        long endMillis = System.currentTimeMillis();
        updateChart(startMillis, endMillis);
    }
}


//
//public class HistoricalDataFragment extends Fragment {
//
//    private enum AggregationLevel {
//        RAW, HOURLY, DAILY
//    }
//
//    private AggregationLevel currentAggregation = AggregationLevel.DAILY; // Default to daily
//
//    private LineChart lineChart;
//    private Spinner dateRangeSpinner;
//    private Button btnCustomRange;
//    private DatabaseHelper dbHelper;
//
//    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        dbHelper = new DatabaseHelper(requireContext());
////        validateDatabaseContents(); // Debug check
//    }
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);
//
//        lineChart = view.findViewById(R.id.line_chart);
//        dateRangeSpinner = view.findViewById(R.id.dateRangeSpinner);
//        btnCustomRange = view.findViewById(R.id.btnCustomRange);
//
//        setupChart();
//        setupDateRangeSelector();
//        loadDefaultData();
//
//        return view;
//    }
//
//    private void setupChart() {
//        lineChart.getDescription().setEnabled(false);
//        lineChart.setTouchEnabled(true);
//        lineChart.setDragEnabled(true);
//        lineChart.setScaleEnabled(true);
//
//        XAxis xAxis = lineChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return displayFormat.format(new Date((long) value));
//            }
//        });
//
//        YAxis leftAxis = lineChart.getAxisLeft();
//        leftAxis.setTextColor(Color.RED);
//        leftAxis.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return String.format(Locale.getDefault(), "%.1f°C", value);
//            }
//        });
//
//        YAxis rightAxis = lineChart.getAxisRight();
//        rightAxis.setTextColor(Color.BLUE);
//        rightAxis.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return String.format(Locale.getDefault(), "%.1f%%", value);
//            }
//        });
//    }
//
//    private void setupDateRangeSelector() {
//        dateRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                Calendar cal = Calendar.getInstance();
//                Date endDate = cal.getTime();
//
//                switch (pos) {
//                    case 0: cal.add(Calendar.HOUR, -24); break; // Last 24h
//                    case 1: cal.add(Calendar.DAY_OF_YEAR, -7); break; // Last 7d
//                    case 2: cal.add(Calendar.DAY_OF_YEAR, -30); break; // Last 30d
//                    case 3: return; // Custom handled by button
//                }
//
//                Date startDate = cal.getTime();
//                loadChartData(startDate, endDate);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {}
//        });
//
//        btnCustomRange.setOnClickListener(v -> showDateRangePicker());
//    }
//
//    private void showDateRangePicker() {
//        Calendar calendar = Calendar.getInstance();
//        long endDate = calendar.getTimeInMillis();
//        calendar.add(Calendar.DAY_OF_YEAR, -7);
//        long startDate = calendar.getTimeInMillis();
//
//        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
//        builder.setSelection(Pair.create(startDate, endDate));
//
//        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
//        picker.addOnPositiveButtonClickListener(selection -> {
//            loadChartData(new Date(selection.first), new Date(selection.second));
//        });
//        picker.show(getParentFragmentManager(), "DATE_PICKER");
//    }
//
//    private void loadDefaultData() {
//        Calendar cal = Calendar.getInstance();
//        Date endDate = cal.getTime();
//        cal.add(Calendar.DAY_OF_YEAR, -7);
//        loadChartData(cal.getTime(), endDate);
//    }
//
//    private void loadChartData(Date startDate, Date endDate) {
//        String startDateStr = dbDateFormat.format(startDate);
//        String endDateStr = dbDateFormat.format(endDate);
//
//        startDateStr += " 00:00:00";
//        endDateStr += " 23:59:59";
//
//        Log.d("DATE_RANGE", "Loading between: " + startDateStr + " and " + endDateStr);
//
//        String finalStartDateStr = startDateStr;
//        String finalEndDateStr = endDateStr;
//        new Thread(() -> {
//            try (Cursor cursor = dbHelper.getDataForDateRange(finalStartDateStr, finalEndDateStr)) {
//                List<Entry> tempEntries = new ArrayList<>();
//                List<Entry> moistureEntries = new ArrayList<>();
//
//                if (cursor != null) {
//                    int timestampIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP);
//                    int tempIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE);
//                    int moistureIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_MOISTURE);
//
//                    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//
//                    while (cursor.moveToNext()) {
//                        try {
//                            Date timestamp = parser.parse(cursor.getString(timestampIndex));
//                            float temp = cursor.getFloat(tempIndex);
//                            float moisture = cursor.getFloat(moistureIndex);
//
//                            if (timestamp != null) {
//                                tempEntries.add(new Entry(timestamp.getTime(), temp));
//                                moistureEntries.add(new Entry(timestamp.getTime(), moisture));
//                            }
//                        } catch (ParseException e) {
//                            Log.e("PARSE_ERROR", "Failed to parse timestamp", e);
//                        }
//                    }
//                    Log.d("DATA_LOAD", "Found " + tempEntries.size() + " records");
//                }
//
//                requireActivity().runOnUiThread(() -> updateChart(tempEntries, moistureEntries));
//
//            } catch (Exception e) {
//                Log.e("DB_ERROR", "Query failed", e);
//            }
//        }).start();
//    }
//
//    private void updateChart(List<Entry> tempEntries, List<Entry> moistureEntries) {
//        if (tempEntries.isEmpty() && moistureEntries.isEmpty()) {
//            lineChart.clear();
//            lineChart.setNoDataText("No data for selected range");
//            lineChart.invalidate();
//            return;
//        }
//
//        LineDataSet tempDataSet = new LineDataSet(tempEntries, "Temperature (°C)");
//        tempDataSet.setColor(Color.RED);
//        tempDataSet.setCircleColor(Color.RED);
//        tempDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
//
//        LineDataSet moistureDataSet = new LineDataSet(moistureEntries, "Moisture (%)");
//        moistureDataSet.setColor(Color.BLUE);
//        moistureDataSet.setCircleColor(Color.BLUE);
//        moistureDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
//
//        LineData lineData = new LineData(tempDataSet, moistureDataSet);
//        lineChart.setData(lineData);
//        lineChart.invalidate();
//        lineChart.animateY(500);
//    }
//
//    @Override
//    public void onDestroy() {
//        if (dbHelper != null) {
//            dbHelper.close();
//        }
//        super.onDestroy();
//    }
//}