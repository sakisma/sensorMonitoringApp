package com.arduino.sensormonitoringapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatisticsFragment extends Fragment {

    private TextView highestTempText, lowestTempText, avgTempText;
    private TextView highestMoistureText, lowestMoistureText, avgMoistureText;
    private TextView highestHumidityText, lowestHumidityText, avgHumidityText;
    private DatabaseHelper dbHelper;
    private String startDate, endDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private Chip dateRangeChip;

    //Initializes UI, shows current month stats and sets listener for DateRangeChip
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        highestTempText = view.findViewById(R.id.highestTemp);
        lowestTempText = view.findViewById(R.id.lowestTemp);
        avgTempText = view.findViewById(R.id.avgTemp);

        highestMoistureText = view.findViewById(R.id.highestMoisture);
        lowestMoistureText = view.findViewById(R.id.lowestMoisture);
        avgMoistureText = view.findViewById(R.id.avgMoisture);

        highestHumidityText = view.findViewById(R.id.highestHumidity);
        lowestHumidityText = view.findViewById(R.id.lowestHumidity);
        avgHumidityText = view.findViewById(R.id.avgHumidity);

        dateRangeChip = view.findViewById(R.id.dateRangeChip);

        dbHelper = new DatabaseHelper(getContext());
        displayCurrentMonthStatistics();

        dateRangeChip.setOnClickListener(v -> openDateRangePicker());

        return view;
    }

    //Calculates and displays stats for the current month
    private void displayCurrentMonthStatistics() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        startDate = dateFormat.format(calendar.getTime()).split(" ")[0] + " 00:00:00";
        endDate = dateFormat.format(Calendar.getInstance().getTime());
        displayStatistics();
    }

    //Allows date selection and refreshes statistics
    private void openDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTimeInMillis(selection.first);
            startDate = dateFormat.format(startCalendar.getTime()).split(" ")[0] + " 00:00:00";

            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTimeInMillis(selection.second);
            endDate = dateFormat.format(endCalendar.getTime()).split(" ")[0] + " 23:59:59";

            displayStatistics();
        });

        picker.show(getChildFragmentManager(), picker.toString());
    }

    //Fetches DataPoints from DB, calculates max min average and displays on UI
    private void displayStatistics() {
        List<DatabaseHelper.DataPoint> dataPoints = dbHelper.getDataPoints(startDate, endDate);

        Log.i("QUERY Start Date", startDate);
        Log.i("QUERY End Date", endDate);


        // If empty display N/A
        if (dataPoints.isEmpty()) {
            highestTempText.setText("Highest: N/A");
            lowestTempText.setText("Lowest: N/A");
            avgTempText.setText("Average: N/A");
            highestMoistureText.setText("Highest: N/A");
            lowestMoistureText.setText("Lowest: N/A");
            avgMoistureText.setText("Average: N/A");
            highestHumidityText.setText("Highest: N/A");
            lowestHumidityText.setText("Lowest: N/A");
            avgHumidityText.setText("Average: N/A");

            return;
        }

        double highestTemp = Double.MIN_VALUE;
        double lowestTemp = Double.MAX_VALUE;
        double sumTemp = 0;

        double highestMoisture = Double.MIN_VALUE;
        double lowestMoisture = Double.MAX_VALUE;
        double sumMoisture = 0;

        double highestHumidity = Double.MIN_VALUE;
        double lowestHumidity = Double.MAX_VALUE;
        double sumHumidity = 0;

        for (DatabaseHelper.DataPoint dp : dataPoints) {
            highestTemp = Math.max(highestTemp, dp.temperature);
            lowestTemp = Math.min(lowestTemp, dp.temperature);
            sumTemp += dp.temperature;

            highestMoisture = Math.max(highestMoisture, dp.moisture);
            lowestMoisture = Math.min(lowestMoisture, dp.moisture);
            sumMoisture += dp.moisture;

            highestHumidity = Math.max(highestHumidity, dp.humidity);
            lowestHumidity = Math.min(lowestHumidity, dp.humidity);
            sumHumidity += dp.humidity;
        }

        double avgTemp = sumTemp / dataPoints.size();
        double avgMoisture = sumMoisture / dataPoints.size();
        double avgHumidity = sumHumidity / dataPoints.size();

        highestTempText.setText(String.format(Locale.getDefault(), "Highest: %.2f °C", highestTemp));
        lowestTempText.setText(String.format(Locale.getDefault(), "Lowest: %.2f °C", lowestTemp));
        avgTempText.setText(String.format(Locale.getDefault(), "Average: %.2f °C", avgTemp));

        highestMoistureText.setText(String.format(Locale.getDefault(), "Highest: %.2f %%", highestMoisture));
        lowestMoistureText.setText(String.format(Locale.getDefault(), "Lowest: %.2f %%", lowestMoisture));
        avgMoistureText.setText(String.format(Locale.getDefault(), "Average: %.2f %%", avgMoisture));

        highestHumidityText.setText(String.format(Locale.getDefault(), "Highest: %.2f %%", highestHumidity));
        lowestHumidityText.setText(String.format(Locale.getDefault(), "Lowest: %.2f %%", lowestHumidity));
        avgHumidityText.setText(String.format(Locale.getDefault(), "Average: %.2f %%", avgHumidity));
    }
}