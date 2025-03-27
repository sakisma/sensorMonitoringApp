package com.arduino.sensormonitoringapp.adapter;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

public class DateRangeSpinnerAdapter extends ArrayAdapter<String> {
    private Context context;
    private Calendar startCalendar;
    private Calendar endCalendar;
    private OnDateRangeSelectedListener listener;

    public interface OnDateRangeSelectedListener {
        void onDateRangeSelected(Date startDate, Date endDate);
    }

    public DateRangeSpinnerAdapter(@NonNull Context context, OnDateRangeSelectedListener listener) {
        super(context, android.R.layout.simple_spinner_item, new String[]{
                "Last 24 Hours",
                "Last 3 Days",
                "Last 7 Days",
                "Custom Range"
        });
        this.context = context;
        this.listener = listener;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        return textView;
    }

    public void handleDateRangeSelection(int position) {
        endCalendar = Calendar.getInstance();
        startCalendar = Calendar.getInstance();

        switch (position) {
            case 0: // Last 24 Hours
                startCalendar.add(Calendar.HOUR_OF_DAY, -24);
                break;
            case 1: // Last 3 Days
                startCalendar.add(Calendar.DAY_OF_YEAR, -3);
                break;
            case 2: // Last 7 Days
                startCalendar.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case 3: // Custom Range
                showCustomRangePicker();
                return;
        }

        listener.onDateRangeSelected(startCalendar.getTime(), endCalendar.getTime());
    }

    private void showCustomRangePicker() {
        // Start Date Picker
        DatePickerDialog startDatePicker = new DatePickerDialog(context,
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Set start date
                    startCalendar.set(year, monthOfYear, dayOfMonth);

                    // End Date Picker
                    DatePickerDialog endDatePicker = new DatePickerDialog(context,
                            (view2, year2, monthOfYear2, dayOfMonth2) -> {
                                // Set end date
                                endCalendar.set(year2, monthOfYear2, dayOfMonth2);

                                // Ensure end date is not before start date
                                if (endCalendar.before(startCalendar)) {
                                    endCalendar.setTime(startCalendar.getTime());
                                }

                                // Callback with selected dates
                                listener.onDateRangeSelected(startCalendar.getTime(), endCalendar.getTime());
                            },
                            endCalendar.get(Calendar.YEAR),
                            endCalendar.get(Calendar.MONTH),
                            endCalendar.get(Calendar.DAY_OF_MONTH)
                    );

                    endDatePicker.setTitle("Select End Date");
                    endDatePicker.show();
                },
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)
        );

        startDatePicker.setTitle("Select Start Date");
        startDatePicker.show();
    }
}