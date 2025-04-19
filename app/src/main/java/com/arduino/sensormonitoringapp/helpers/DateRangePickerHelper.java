package com.arduino.sensormonitoringapp.helpers;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.Date;

public class DateRangePickerHelper extends ArrayAdapter<String> {
    private Context context;
    private OnDateRangeSelectedListener listener;

    public interface OnDateRangeSelectedListener {
        void onDateRangeSelected(Date startDate, Date endDate);
    }

    public DateRangePickerHelper(@NonNull Context context, OnDateRangeSelectedListener listener) {
        super(context, android.R.layout.simple_spinner_item, new String[]{
                "Last 24 Hours",
                "Last 3 Days",
                "Last 7 Days",
                "Custom Range"
        });
        this.context = context;
        this.listener = listener;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        return textView;
    }

    public void handleDateRangeSelection(int position) {
        Calendar endCalendar = Calendar.getInstance();
        Calendar startCalendar = Calendar.getInstance();

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
        // Create date validator to prevent selecting future dates
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now());

        // Create Material Date Range Picker
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> datePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .setCalendarConstraints(constraintsBuilder.build())
                        .build();

        datePicker.show(((androidx.fragment.app.FragmentActivity)context).getSupportFragmentManager(), "DATE_RANGE_PICKER");

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convert milliseconds to dates
            Date startDate = new Date(selection.first);
            Date endDate = new Date(selection.second);

            // Ensure end of day for end date
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(endDate);
            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);
            endCalendar.set(Calendar.MILLISECOND, 999);

            listener.onDateRangeSelected(startDate, endCalendar.getTime());
        });
    }
}
