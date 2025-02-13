package com.arduino.sensormonitoringapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TemperatureMonitorService extends Service {

    private static final String CHANNEL_ID = "TemperatureAlerts";
    private static final int NOTIFICATION_ID_HIGH = 1;
    private static final int NOTIFICATION_ID_LOW = 2;
    private DatabaseReference temperatureRef;
    private float thresholdHigh = 30;
    private float thresholdLow = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        loadThresholdsFromPreferences();
        startTemperatureListener();
    }

    private void startTemperatureListener() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        temperatureRef = database.getReference("temperature");

        temperatureRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    float currentTemperature = snapshot.getValue(Float.class);
                    Log.d("TemperatureService", "Current Temp: " + currentTemperature);

                    if (currentTemperature > thresholdHigh) {
                        sendNotification("High Temperature Alert!", "Temperature: " + currentTemperature + "°C", NOTIFICATION_ID_HIGH);
                    } else if (currentTemperature < thresholdLow) {
                        sendNotification("Low Temperature Alert!", "Temperature: " + currentTemperature + "°C", NOTIFICATION_ID_LOW);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TemperatureService", "Firebase Error: " + error.getMessage());
            }
        });
    }

    private void sendNotification(String title, String message, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_temperature)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Temperature Alerts", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void loadThresholdsFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        thresholdHigh = SettingsFragment.getHighThreshold(prefs);
        thresholdLow = SettingsFragment.getLowThreshold(prefs);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        loadThresholdsFromPreferences();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (temperatureRef != null) {
            temperatureRef.removeEventListener((ValueEventListener) this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
