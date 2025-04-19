package com.arduino.sensormonitoringapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoricalDataFragment extends Fragment {

    private LineChart temperatureChart, moistureChart;
    private BarChart temperatureBarChart, moistureBarChart;
    private DatabaseHelper dbHelper;
    private AutoCompleteTextView chartTypeDropdown, dateRangeDropdown, aggregationDropdown;
    private String selectedChartType = "Line Chart";
    private String selectedDateRange = "Last 24 Hours";
    private String selectedAggregation = "Daily";

    private String startDate, endDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);

        temperatureChart = view.findViewById(R.id.temperature_chart);
        moistureChart = view.findViewById(R.id.moisture_chart);
        temperatureBarChart = view.findViewById(R.id.temperature_bar_chart);
        moistureBarChart = view.findViewById(R.id.moisture_bar_chart);
        dbHelper = new DatabaseHelper(getContext());
        chartTypeDropdown = view.findViewById(R.id.chart_type_dropdown);
        dateRangeDropdown = view.findViewById(R.id.date_range_dropdown);
        aggregationDropdown = view.findViewById(R.id.aggregation_dropdown); // Initialize aggregationDropdown

        setupDropdowns();
        setDateRange("Last 24 Hours"); // Default date range

        return view;
    }

    private void setupDropdowns() {
        String[] aggregations = {"Daily", "Weekly", "Total"};
        ArrayAdapter<String> aggregationAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, aggregations);
        aggregationDropdown.setAdapter(aggregationAdapter);

        String[] chartTypes = {"Line Chart", "Bar Chart"};
        ArrayAdapter<String> chartTypeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, chartTypes);
        chartTypeDropdown.setAdapter(chartTypeAdapter);

        String[] dateRanges = {"Last 24 Hours", "Last 3 Days", "Last 7 Days", "Last 30 Days", "Custom Date Range"};
        ArrayAdapter<String> dateRangeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, dateRanges);
        dateRangeDropdown.setAdapter(dateRangeAdapter);

        chartTypeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedChartType = (String) parent.getItemAtPosition(position);
            updateCharts();
        });

        dateRangeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedDateRange = (String) parent.getItemAtPosition(position);
            setDateRange(selectedDateRange);
        });

        aggregationDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedAggregation = (String) parent.getItemAtPosition(position);
            updateCharts();
        });
    }

    private void setDateRange(String range) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        endDate = dateFormat.format(now);

        switch (range) {
            case "Last 24 Hours":
                calendar.add(Calendar.HOUR, -24);
                break;
            case "Last 3 Days":
                calendar.add(Calendar.DAY_OF_YEAR, -3);
                break;
            case "Last 7 Days":
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case "Last 30 Days":
                calendar.add(Calendar.DAY_OF_YEAR, -30);
                break;
            case "Custom Date Range":
                openDateRangePicker();
                return; // Prevent immediate chart update
        }

        startDate = dateFormat.format(calendar.getTime());
        updateCharts();
    }

    private void openDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = dateFormat.format(new Date(selection.first));
            endDate = dateFormat.format(new Date(selection.second));
            updateCharts();
        });

        picker.show(getChildFragmentManager(), picker.toString());
    }

    private void updateCharts() {
        List<DatabaseHelper.DataPoint> dataPoints = dbHelper.getDataPoints(startDate, endDate);
        if (selectedChartType.equals("Line Chart")) {
            showLineCharts(dataPoints);
        } else {
            showBarCharts(dataPoints);
        }
    }

    private void showLineCharts(List<DatabaseHelper.DataPoint> dataPoints) {
        temperatureChart.setVisibility(View.VISIBLE);
        moistureChart.setVisibility(View.VISIBLE);
        temperatureBarChart.setVisibility(View.GONE);
        moistureBarChart.setVisibility(View.GONE);

        List<Entry> temperatureEntries = new ArrayList<>();
        List<Entry> moistureEntries = new ArrayList<>();

        for (int i = 0; i < dataPoints.size(); i++) {
            DatabaseHelper.DataPoint dp = dataPoints.get(i);
            temperatureEntries.add(new Entry(i, (float) dp.temperature));
            moistureEntries.add(new Entry(i, dp.moisture));
        }

        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "Temperature");
        LineDataSet moistureDataSet = new LineDataSet(moistureEntries, "Moisture");

        LineData temperatureLineData = new LineData(temperatureDataSet);
        LineData moistureLineData = new LineData(moistureDataSet);

        temperatureChart.setData(temperatureLineData);
        moistureChart.setData(moistureLineData);
        temperatureChart.invalidate();
        moistureChart.invalidate();
    }
    private void showBarCharts(List<DatabaseHelper.DataPoint> dataPoints) {
        temperatureChart.setVisibility(View.GONE);
        moistureChart.setVisibility(View.GONE);
        temperatureBarChart.setVisibility(View.VISIBLE);
        moistureBarChart.setVisibility(View.VISIBLE);

        List<BarEntry> temperatureBarEntries = new ArrayList<>();
        List<BarEntry> moistureBarEntries = new ArrayList<>();

        List<DailyAverage> temperatureAverages;
        List<DailyAverage> moistureAverages;

        if (selectedAggregation.equals("Daily")) {
            temperatureAverages = calculateDailyAverages(dataPoints, true);
            moistureAverages = calculateDailyAverages(dataPoints, false);
        } else if (selectedAggregation.equals("Weekly")) {
            temperatureAverages = calculateWeeklyAverages(dataPoints, true);
            moistureAverages = calculateWeeklyAverages(dataPoints, false);
        } else {
            temperatureAverages = calculateTotalAverages(dataPoints, true);
            moistureAverages = calculateTotalAverages(dataPoints, false);
        }

        for (int i = 0; i < temperatureAverages.size(); i++) {
            temperatureBarEntries.add(new BarEntry(i, (float) temperatureAverages.get(i).average));
            moistureBarEntries.add(new BarEntry(i, (float) moistureAverages.get(i).average));
        }

        BarDataSet temperatureBarDataSet = new BarDataSet(temperatureBarEntries, "Temperature Average");
        BarDataSet moistureBarDataSet = new BarDataSet(moistureBarEntries, "Moisture Average");

        BarData temperatureBarData = new BarData(temperatureBarDataSet);
        BarData moistureBarData = new BarData(moistureBarDataSet);

        temperatureBarChart.setData(temperatureBarData);
        moistureBarChart.setData(moistureBarData);
        temperatureBarChart.invalidate();
        moistureBarChart.invalidate();
    }

    private List<DailyAverage> calculateDailyAverages(List<DatabaseHelper.DataPoint> dataPoints, boolean isTemperature) {
        List<DailyAverage> dailyAverages = new ArrayList<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDay = "";
        double sum = 0;
        int count = 0;

        for (DatabaseHelper.DataPoint dp : dataPoints) {
            String day = dayFormat.format(parseDate(dp.timestamp));
            if (!day.equals(currentDay) && !currentDay.isEmpty()) {
                dailyAverages.add(new DailyAverage(currentDay, sum / count));
                sum = 0;
                count = 0;
            }
            currentDay = day;
            if (isTemperature) {
                sum += dp.temperature;
            } else {
                sum += dp.moisture;
            }
            count++;
        }

        if (count > 0) {
            dailyAverages.add(new DailyAverage(currentDay, sum / count));
        }

        return dailyAverages;
    }

    private List<DailyAverage> calculateWeeklyAverages(List<DatabaseHelper.DataPoint> dataPoints, boolean isTemperature) {
        List<DailyAverage> weeklyAverages = new ArrayList<>();
        SimpleDateFormat weekFormat = new SimpleDateFormat("yyyy-ww", Locale.getDefault()); // year and week of year
        String currentWeek = "";
        double sum = 0;
        int count = 0;

        for (DatabaseHelper.DataPoint dp : dataPoints) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parseDate(dp.timestamp));
            String week = weekFormat.format(calendar.getTime());

            if (!week.equals(currentWeek) && !currentWeek.isEmpty()) {
                weeklyAverages.add(new DailyAverage(currentWeek, sum / count));
                sum = 0;
                count = 0;
            }
            currentWeek = week;
            if (isTemperature) {
                sum += dp.temperature;
            } else {
                sum += dp.moisture;
            }
            count++;
        }

        if (count > 0) {
            weeklyAverages.add(new DailyAverage(currentWeek, sum / count));
        }

        return weeklyAverages;
    }

    private List<DailyAverage> calculateTotalAverages(List<DatabaseHelper.DataPoint> dataPoints, boolean isTemperature) {
        List<DailyAverage> totalAverages = new ArrayList<>();
        double sum = 0;
        int count = 0;

        for (DatabaseHelper.DataPoint dp : dataPoints) {
            if (isTemperature) {
                sum += dp.temperature;
            } else {
                sum += dp.moisture;
            }
            count++;
        }

        if (count > 0) {
            totalAverages.add(new DailyAverage("Total Average", sum / count));
        }

        return totalAverages;
    }

    private Date parseDate(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date(); // Return current date as fallback
        }
    }

    private static class DailyAverage {
        String day;
        double average;

        DailyAverage(String day, double average) {
            this.day = day;
            this.average = average;
        }
    }
}