package com.arduino.sensormonitoringapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HeatmapFragment extends Fragment {
    private GridLayout heatmapGrid;
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heatmap, container, false);

        heatmapGrid = view.findViewById(R.id.heatmap_grid);
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        fetchHeatmapData();

        return view;
    }

    private void fetchHeatmapData() {
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Float> temperatureValues = new ArrayList<>();

                // Λήψη του ορίου θερμοκρασίας από τις Ρυθμίσεις
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                float temperatureThreshold = Float.parseFloat(sharedPreferences.getString("temperature_threshold", "30"));

                boolean thresholdExceeded = false;

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        Object tempObject = timeSnapshot.child("temp").getValue();
                        if (tempObject != null) {
                            float tempValue = Float.parseFloat(tempObject.toString());
                            temperatureValues.add(tempValue);

                            // Έλεγχος αν ξεπερνά το όριο θερμοκρασίας
                            if (tempValue > temperatureThreshold) {
                                thresholdExceeded = true;
                            }
                        }
                    }
                }

                // Αν ξεπεραστεί το όριο, στείλε ειδοποίηση
                if (thresholdExceeded) {
                    sendNotification("Temperature Alert", "A temperature has exceeded " + temperatureThreshold + "°C!");
                }

                populateHeatmap(temperatureValues);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Διαχείριση σφαλμάτων
            }
        });
    }

    private void populateHeatmap(List<Float> temperatureValues) {
        heatmapGrid.removeAllViews();

        int totalCells = heatmapGrid.getColumnCount() * heatmapGrid.getRowCount();

        for (int i = 0; i < totalCells && i < temperatureValues.size(); i++) {
            View cell = new View(getContext());

            float temperature = temperatureValues.get(i);
            cell.setBackgroundColor(getColorForTemperature(temperature));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.width = 0;
            params.height = 0;
            params.setMargins(4, 4, 4, 4);
            cell.setLayoutParams(params);

            heatmapGrid.addView(cell);
        }
    }

    private int getColorForTemperature(float temperature) {
        if (temperature < 15) {
            return Color.BLUE;
        } else if (temperature < 25) {
            return Color.GREEN;
        } else if (temperature < 35) {
            return Color.YELLOW;
        } else {
            return Color.RED;
        }
    }
    private void checkTemperatureAndNotify(float currentTemperature) {
        // Πάρε τις ρυθμίσεις του χρήστη
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean notificationsEnabled = prefs.getBoolean("enable_notifications", true);
        String maxTemperatureString = prefs.getString("max_temperature", "30");
        float maxTemperature = Float.parseFloat(maxTemperatureString);

        // Έλεγχος: Αν είναι ενεργοποιημένες οι ειδοποιήσεις και ξεπεράστηκε η θερμοκρασία
        if (notificationsEnabled && currentTemperature > maxTemperature) {
            sendNotification("Temperature Alert", "The temperature exceeded " + maxTemperature + "°C!");
        }
    }

    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "temperature_alerts";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Temperature Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for temperature threshold alerts");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.ic_notification) // Βεβαιώσου ότι υπάρχει το εικονίδιο
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
