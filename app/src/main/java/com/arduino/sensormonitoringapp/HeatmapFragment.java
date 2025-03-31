package com.arduino.sensormonitoringapp;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.HeatDataEntry;
import com.anychart.charts.HeatMap;
import com.anychart.enums.SelectionMode;
import com.anychart.graphics.vector.SolidFill;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HeatmapFragment extends Fragment {

    private AnyChartView anyChartView;
    private HeatMap heatMap;
    private DatabaseHelper databaseHelper;
    private long startDate = 0;
    private long endDate = 0;
    private boolean showTemperature = true;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_heatmap, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseHelper = new DatabaseHelper(requireContext());

        // Initialize chart view
        anyChartView = view.findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(view.findViewById(R.id.progress_bar));

        // Create heatmap instance
        heatMap = AnyChart.heatMap();
        setupHeatmap();
        anyChartView.setChart(heatMap);

        // Initialize UI components
        TextInputLayout dateRangeInputLayout = view.findViewById(R.id.date_range_input);
        TextInputEditText dateRangeInput = (TextInputEditText) dateRangeInputLayout.getEditText();

        MaterialButton applyFilterButton = view.findViewById(R.id.btn_apply_filter);
        ChipGroup metricChipGroup = view.findViewById(R.id.metric_chip_group);
        Chip chipTemperature = view.findViewById(R.id.chip_temperature);
        Chip chipMoisture = view.findViewById(R.id.chip_moisture);

        // Set default date range (last 7 days)
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        startDate = calendar.getTimeInMillis();
        updateDateRangeText(startDate, endDate, dateRangeInput);

        // Date range picker
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select date range")
                        .build();

        dateRangeInput.setOnClickListener(v -> dateRangePicker.show(getChildFragmentManager(), "DATE_RANGE_PICKER"));

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            startDate = selection.first;
            endDate = selection.second;
            updateDateRangeText(startDate, endDate, dateRangeInput);
        });

        // Metric selection
        metricChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_temperature) {
                showTemperature = true;
                updateHeatmap();
            } else if (checkedId == R.id.chip_moisture) {
                showTemperature = false;
                updateHeatmap();
            }
        });

        // Apply filter button
        applyFilterButton.setOnClickListener(v -> updateHeatmap());

        // Initial data load
        updateHeatmap();
    }

    private void updateDateRangeText(long start, long end, TextInputEditText input) {
        String startStr = displayDateFormat.format(new Date(start));
        String endStr = displayDateFormat.format(new Date(end));
        input.setText(String.format("%s - %s", startStr, endStr));
    }

    private void setupHeatmap() {
        heatMap.stroke("1 #fff");
        heatMap.hovered()
                .stroke("6 #fff")
                .fill(new SolidFill("#545f69", 1d))
                .labels("{ fontColor: '#fff' }");

        heatMap.interactivity().selectionMode(SelectionMode.NONE);

        heatMap.title().enabled(true);
        heatMap.title().padding(0d, 0d, 20d, 0d);

        heatMap.labels().enabled(true);
        heatMap.labels().minFontSize(12d);

        heatMap.yAxis(0).stroke(null);
        heatMap.yAxis(0).labels().padding(0d, 15d, 0d, 0d);
        heatMap.yAxis(0).ticks(false);
        heatMap.xAxis(0).stroke(null);
        heatMap.xAxis(0).ticks(false);

        heatMap.tooltip().title().useHtml(true);
        heatMap.tooltip().useHtml(true);
    }

    private void updateHeatmap() {
        // Get data from database
        Cursor cursor = databaseHelper.getDailyAverages(startDate, endDate);

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(requireContext(), "No data available for selected date range", Toast.LENGTH_SHORT).show();
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        List<DataEntry> data = new ArrayList<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        // Find min and max values for color scaling
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;

        // First pass to find min/max
        while (cursor.moveToNext()) {
            double value = showTemperature ?
                    cursor.getDouble(cursor.getColumnIndexOrThrow("avg_temp")) :
                    cursor.getDouble(cursor.getColumnIndexOrThrow("avg_moisture"));

            if (value < minValue) minValue = value;
            if (value > maxValue) maxValue = value;
        }

        // Reset cursor to beginning
        cursor.moveToFirst();

        // Second pass to create data entries
        while (cursor.moveToNext()) {
            try {
                String day = cursor.getString(cursor.getColumnIndexOrThrow("day"));
                Date date = this.dateFormat.parse(day);

                String dayOfWeek = dayFormat.format(date);
                String shortDate = dateFormat.format(date);

                double value = showTemperature ?
                        cursor.getDouble(cursor.getColumnIndexOrThrow("avg_temp")) :
                        cursor.getDouble(cursor.getColumnIndexOrThrow("avg_moisture"));

                // Normalize value for color (0-1 range)
                double normalizedValue = (value - minValue) / (maxValue - minValue);

                data.add(new CustomHeatDataEntry(
                        dayOfWeek,
                        shortDate,
                        normalizedValue,
                        getColorForValue(normalizedValue, showTemperature)
                ));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error processing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        cursor.close();

        // Update chart
        heatMap.title().text(showTemperature ? "Temperature Heatmap (°C)" : "Moisture Heatmap (%)");

        heatMap.tooltip().titleFormat("function() {\n" +
                        "      return '<b>' + this.y + '</b>';\n" +
                        "    }")
                .format("function () {\n" +
                        "       return '<span style=\"color: #CECECE\">Date: </span>' + this.y + '<br/>' +\n" +
                        "           '<span style=\"color: #CECECE\">Day: </span>' + this.x + '<br/>' +\n" +
                        "           '<span style=\"color: #CECECE\">" + (showTemperature ? "Temperature" : "Moisture") + ": </span>' + " +
                        "(this.heat / 1000 * (" + maxValue + " - " + minValue + ") + " + minValue + ").toFixed(2);\n" +
                        "   }");

        heatMap.data(data);
    }

    private String getColorForValue(double normalizedValue, boolean isTemperature) {
        if (isTemperature) {
            // Blue (cool) to Red (hot) gradient for temperature
            if (normalizedValue < 0.25) return "#90caf9"; // Light blue
            else if (normalizedValue < 0.5) return "#42a5f5"; // Medium blue
            else if (normalizedValue < 0.75) return "#ffb74d"; // Orange
            else return "#ef6c00"; // Dark orange/red
        } else {
            // Brown gradient for moisture (dry to wet)
            if (normalizedValue < 0.25) return "#d7ccc8"; // Light brown (dry)
            else if (normalizedValue < 0.5) return "#a1887f"; // Medium brown
            else if (normalizedValue < 0.75) return "#8d6e63"; // Dark brown
            else return "#5d4037"; // Very dark brown (wet)
        }
    }

    private static class CustomHeatDataEntry extends HeatDataEntry {
        private static final int HEAT_SCALE_FACTOR = 1000;
        CustomHeatDataEntry(String x, String y, double normalizedHeat, String fill) {
            super(x, y, (int)(normalizedHeat * HEAT_SCALE_FACTOR));
            setValue("fill", fill);
        }
    }

    @Override
    public void onDestroyView() {
        databaseHelper.close();
        super.onDestroyView();
    }
}