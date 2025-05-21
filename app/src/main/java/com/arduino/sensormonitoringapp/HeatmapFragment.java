package com.arduino.sensormonitoringapp;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.HeatDataEntry;
import com.anychart.charts.HeatMap;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeatmapFragment extends Fragment {

    private AnyChartView anyChartView;
    private HeatMap heatMap;
    private DatabaseHelper databaseHelper;
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private String selectedMetric = "temperature"; // Default to temperature
    private MaterialButton yearButton;

    // Track actual min/max values for legend
    private double actualMinValue = 0;
    private double actualMaxValue = 30;

    // Scale factor to maintain precision when storing double as int
    private static final int VALUE_SCALE = 100;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_heatmap, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseHelper = new DatabaseHelper(requireContext());
        anyChartView = view.findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(view.findViewById(R.id.progress_bar));

        // Initialize chart
        heatMap = AnyChart.heatMap();
        setupHeatmap();
        anyChartView.setChart(heatMap);

        yearButton = view.findViewById(R.id.year_button);
        yearButton.setText(String.valueOf(selectedYear));
        yearButton.setOnClickListener(v -> showYearPicker());

        // Metric selection
        ChipGroup metricChipGroup = view.findViewById(R.id.metric_chip_group);
        metricChipGroup.setSingleSelection(true);

        Chip temperatureChip = view.findViewById(R.id.chip_temperature);
        Chip moistureChip = view.findViewById(R.id.chip_moisture);
        Chip humidityChip = view.findViewById(R.id.chip_humidity);

        // Set initial state
        temperatureChip.setChecked(true);
        moistureChip.setChecked(false);
        humidityChip.setChecked(false);
        selectedMetric = "temperature";

        metricChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_temperature) {
                selectedMetric = "temperature";
            } else if (checkedId == R.id.chip_moisture) {
                selectedMetric = "moisture";
            } else if (checkedId == R.id.chip_humidity) {
                selectedMetric = "humidity";
            }
            updateHeatmap();
        });

        // Initial load
        updateHeatmap();
    }

    private void showYearPicker() {
        // Create a NumberPicker dialog for years
        final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.year_picker_dialog, null);
        final NumberPicker yearPicker = dialogView.findViewById(R.id.year_picker);

        // Set range - from 5 years ago to current year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 5);
        yearPicker.setMaxValue(currentYear);
        yearPicker.setValue(selectedYear);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Year")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedYear = yearPicker.getValue();
                    yearButton.setText(String.valueOf(selectedYear));
                    updateHeatmap();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupHeatmap() {
        heatMap.stroke("1 #fff");

        // Create labels for all days (1-31)
        String[] daysLabels = new String[31];
        for (int i = 0; i < 31; i++) {
            daysLabels[i] = String.valueOf(i + 1);
        }

        // Create labels for all months
        String[] monthLabels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        // Set X-axis labels (days 1-31)
        heatMap.xAxis(0)
                .title("Day of Month")
                .labels()
                .format("function() { return this.value; }");

        // Set Y-axis labels (months)
        heatMap.yAxis(0)
                .title("Month")
                .labels()
                .format("function() { " +
                        "var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']; " +
                        "return months[this.value]; }");

        // Configure cell labels to show actual values (with scaling conversion)
        heatMap.labels()
                .enabled(true)
                .minFontSize(8)
                .format("function() {" +
                        "  if (this.heat === 0) return '0.0';" +
                        "  return (this.heat / " + VALUE_SCALE + ").toFixed(1);" +
                        "}");

        // Default color scales
        String[] tempColors = {"#90caf9", "#42a5f5", "#1e88e5", "#0d47a1", "#ffb74d", "#ff9800", "#ef6c00", "#e65100"};
        String[] moistureColors = {"#d7ccc8", "#bcaaa4", "#a1887f", "#8d6e63", "#795548", "#6d4c41", "#5d4037", "#4e342e"};
        String[] humidityColors = {"#b3e2cd", "#81c784", "#4caf50", "#388e3c", "#2e7d32", "#1b5e20", "#33691e", "#1a3c34"};

        // Set color scale based on selected metric
        heatMap.colorScale().colors(
                selectedMetric.equals("temperature") ? tempColors :
                        selectedMetric.equals("moisture") ? moistureColors : humidityColors
        );

        // Tooltip
        heatMap.tooltip()
                .useHtml(true)
                .titleFormat("function() {" +
                        "var months = ['January','February','March','April','May','June','July','August','September','October','November','December'];" +
                        "return '<b>' + months[this.y] + ' ' + this.x + '</b>';}")
                .format("function() {" +
                        "return '<span style=\"color:#CECECE\">Value: </span>' + " +
                        "(this.heat === 0 ? '0.00' : (this.heat / " + VALUE_SCALE + ").toFixed(2)) + " +
                        "'<span style=\"color:#CECECE\">" + getMetricUnit() + "</span>';}");

        // Enable legend
        heatMap.legend().enabled(true);

        // Enable scroller and set point to 7 days initially
        heatMap.xScroller().enabled(true);
        heatMap.xZoom().setToPointsCount(7, false, null);
    }

    private String getMetricUnit() {
        switch (selectedMetric) {
            case "temperature":
                return "°C";
            case "moisture":
            case "humidity":
                return "%";
            default:
                return "";
        }
    }

    private void updateHeatmap() {
        List<DataEntry> data = new ArrayList<>();

        // Get data for the year
        Cursor cursor = databaseHelper.getYearlyData(selectedYear);

        // Create a map to store values for each day/month combination
        Map<String, Double> dataMap = new HashMap<>();
        // Reset min/max values
        actualMinValue = Double.MAX_VALUE;
        actualMaxValue = Double.MIN_VALUE;

        // Process data from the database
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));
                double value;
                switch (selectedMetric) {
                    case "temperature":
                        value = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEMPERATURE));
                        break;
                    case "moisture":
                        value = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MOISTURE));
                        break;
                    case "humidity":
                        value = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HUMIDITY));
                        break;
                    default:
                        value = 0;
                }

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = sdf.parse(timestamp.split(" ")[0]);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);

                    int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                    int month = cal.get(Calendar.MONTH);

                    // Create a key for this day/month combination
                    String key = month + ":" + dayOfMonth;

                    // Add or update value in map (for averaging if multiple values exist)
                    if (dataMap.containsKey(key)) {
                        double existingValue = dataMap.get(key);
                        dataMap.put(key, (existingValue + value) / 2); // Simple average
                    } else {
                        dataMap.put(key, value);
                    }

                    // Track min/max values
                    if (value < actualMinValue) actualMinValue = value;
                    if (value > actualMaxValue) actualMaxValue = value;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Set reasonable defaults if no data
        if (actualMinValue == Double.MAX_VALUE) {
            actualMinValue = selectedMetric.equals("temperature") ? 0 : 0;
            actualMaxValue = selectedMetric.equals("temperature") ? 30 : 100;
        }

        // Create entries for all possible day/month combinations
        for (int month = 0; month < 12; month++) {
            // Get days in month (considering leap years for February)
            int daysInMonth = 31;
            if (month == 1) { // February
                boolean isLeapYear = (selectedYear % 4 == 0 && selectedYear % 100 != 0) || (selectedYear % 400 == 0);
                daysInMonth = isLeapYear ? 29 : 28;
            } else if (month == 3 || month == 5 || month == 8 || month == 10) { // April, June, September, November
                daysInMonth = 30;
            }

            for (int day = 1; day <= daysInMonth; day++) {
                String key = month + ":" + day;

                // Get value from map or use 0 for empty cells
                Double value = dataMap.get(key);

                if (value != null) {
                    data.add(new CustomHeatDataEntry(
                            String.valueOf(day),
                            String.valueOf(month),
                            value,
                            getColorForValue((value - actualMinValue) / (actualMaxValue - actualMinValue))
                    ));
                } else {
                    // Add empty cell (transparent or very light color)
                    data.add(new CustomHeatDataEntry(
                            String.valueOf(day),
                            String.valueOf(month),
                            0.0,
                            "#F5F5F5"
                    ));
                }
            }
        }

        // Update chart title with correct range information
        String title = selectedMetric.equals("temperature") ?
                String.format("Temperature in %d (%.1f°C - %.1f°C)", selectedYear, actualMinValue, actualMaxValue) :
                selectedMetric.equals("moisture") ?
                        String.format("Soil Moisture in %d (%.1f%% - %.1f%%)", selectedYear, actualMinValue, actualMaxValue) :
                        String.format("Humidity in %d (%.1f%% - %.1f%%)", selectedYear, actualMinValue, actualMaxValue);

        // Update chart
        heatMap.title().text(title);

        // Update color scale
        heatMap.colorScale().colors(
                selectedMetric.equals("temperature") ?
                        new String[]{"#90caf9", "#42a5f5", "#1e88e5", "#0d47a1", "#ffb74d", "#ff9800", "#ef6c00", "#e65100"} :
                        selectedMetric.equals("moisture") ?
                                new String[]{"#d7ccc8", "#bcaaa4", "#a1887f", "#8d6e63", "#795548", "#6d4c41", "#5d4037", "#4e342e"} :
                                new String[]{"#b3e2cd", "#81c784", "#4caf50", "#388e3c", "#2e7d32", "#1b5e20", "#33691e", "#1a3c34"}
        );

        // Configure legend with proper min/max values
        heatMap.legend()
                .enabled(false)
                .itemsFormat("function() {" +
                        "var range = " + (actualMaxValue - actualMinValue) + ";" +
                        "var min = " + actualMinValue + ";" +
                        "var step = range / 7;" +
                        "var value = min + step * this.index;" +
                        "return (value / " + VALUE_SCALE + ").toFixed(1) + ('" + getMetricUnit() + "');" +
                        "}")
                .title(selectedMetric.equals("temperature") ? "Temperature (°C)" : selectedMetric.equals("moisture") ? "Moisture (%)" : "Humidity (%)");

        heatMap.data(data);
    }

    private String getColorForValue(double normalizedValue) {
        if (normalizedValue <= 0) return "#F5F5F5";

        String[] colors = selectedMetric.equals("temperature") ?
                new String[]{"#90caf9", "#42a5f5", "#1e88e5", "#0d47a1", "#ffb74d", "#ff9800", "#ef6c00", "#e65100"} :
                selectedMetric.equals("moisture") ?
                        new String[]{"#d7ccc8", "#bcaaa4", "#a1887f", "#8d6e63", "#795548", "#6d4c41", "#5d4037", "#4e342e"} :
                        new String[]{"#b3e2cd", "#81c784", "#4caf50", "#388e3c", "#2e7d32", "#1b5e20", "#33691e", "#1a3c34"};

        if (normalizedValue < 0.125) return colors[0];
        else if (normalizedValue < 0.25) return colors[1];
        else if (normalizedValue < 0.375) return colors[2];
        else if (normalizedValue < 0.5) return colors[3];
        else if (normalizedValue < 0.625) return colors[4];
        else if (normalizedValue < 0.75) return colors[5];
        else if (normalizedValue < 0.875) return colors[6];
        else return colors[7];
    }

    private static class CustomHeatDataEntry extends HeatDataEntry {
        CustomHeatDataEntry(String dayOfMonth, String month, double value, String fill) {
            super(dayOfMonth, month, value == 0 ? 0 : (int) (value * VALUE_SCALE));
            setValue("fill", fill);
        }
    }

    @Override
    public void onDestroyView() {
        databaseHelper.close();
        super.onDestroyView();
    }
}