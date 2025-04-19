package com.arduino.sensormonitoringapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final int BATCH_SIZE = 144;
    private static final String CHANNEL_ID = "sync_notification_channel";


    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sync Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getApplicationContext()
                    .getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createSyncNotification(String content) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Data Sync")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_sync) // Use your app's sync icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }


    @NonNull
    @Override
    public Result doWork() {
        NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Show starting notification
        notificationManager.notify(1, createSyncNotification("Starting data sync..."));

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sensorDataRef = database.getReference("sensorData");
        DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());

        try {
            Log.d(TAG, "Starting sync process");
            Result result = syncData(sensorDataRef, databaseHelper, notificationManager);

            // Show completion notification
            String message = result == Result.success() ?
                    "Sync completed" : "Sync partially completed (more data available)";
            notificationManager.notify(1, createSyncNotification(message));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error syncing data", e);
            notificationManager.notify(1, createSyncNotification("Sync failed"));
            return Result.failure();
        } finally {
            databaseHelper.close();
        }
    }

    private Result syncData(DatabaseReference sensorDataRef,
                            DatabaseHelper databaseHelper,
                            NotificationManager notificationManager) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Result[] result = {Result.success()};
        final AtomicInteger processedCount = new AtomicInteger(0);

        sensorDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dateSnapshots) {
                int totalRecords = 0;
                int unsyncedRecords = 0;

                // First pass to count records
                for (DataSnapshot dateSnapshot : dateSnapshots.getChildren()) {
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        totalRecords++;
                        Boolean isSynced = timeSnapshot.child("sync").getValue(Boolean.class);
                        if (isSynced == null || !isSynced) {
                            unsyncedRecords++;
                        }
                    }
                }

                notificationManager.notify(1, createSyncNotification(
                        "Syncing " + unsyncedRecords + " of " + totalRecords + " records"));

                // Second pass to process data
                for (DataSnapshot dateSnapshot : dateSnapshots.getChildren()) {
                    String date = dateSnapshot.getKey();

                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        Boolean isSynced = timeSnapshot.child("sync").getValue(Boolean.class);
                        if (isSynced != null && isSynced) {
                            continue;
                        }

                        if (processRecord(date, timeSnapshot, databaseHelper)) {
                            int processed = processedCount.incrementAndGet();

                            // Update progress every 10 records
                            if (processed % 10 == 0) {
                                notificationManager.notify(1, createSyncNotification(
                                        "Synced " + processed + " of " + unsyncedRecords + " records"));
                            }

                            if (processed >= BATCH_SIZE) {
                                break;
                            }
                        }
                    }

                    if (processedCount.get() >= BATCH_SIZE) {
                        break;
                    }
                }

                result[0] = processedCount.get() > 0 ? Result.retry() : Result.success();
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Sync cancelled: " + error.getMessage());
                result[0] = Result.failure();
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
            return result[0];
        } catch (InterruptedException e) {
            return Result.retry();
        }
    }

    private boolean processRecord(String date, DataSnapshot timeSnapshot, DatabaseHelper databaseHelper) {
        String time = timeSnapshot.getKey();
        Double temp = timeSnapshot.child("temp").getValue(Double.class);
        Double moisture = timeSnapshot.child("moisture").getValue(Double.class);
        Double humidity = timeSnapshot.child("humidity").getValue(Double.class);

        if (temp == null || moisture == null || humidity == null) {
            Log.w(TAG, "Skipping incomplete record: " + date + " " + time);
            return false;
        }

        try {
            long rowId = databaseHelper.insertData(date, time, temp, moisture, humidity);
            if (rowId == -1) {
                Log.w(TAG, "Failed to insert record: " + date + " " + time);
                return false;
            }

            // Mark as synced
            timeSnapshot.getRef().child("sync").setValue(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Marked as synced: " + date + " " + time);
                        } else {
                            Log.e(TAG, "Failed to mark as synced: " + date + " " + time);
                        }
                    });

            return true;
        } finally {
            databaseHelper.close();
        }
    }
}
