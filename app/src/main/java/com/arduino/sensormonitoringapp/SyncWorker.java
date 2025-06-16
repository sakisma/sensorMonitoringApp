package com.arduino.sensormonitoringapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
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
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final int BATCH_SIZE = 144;
    private static final String CHANNEL_ID = "sync_notification_channel";
    private static final long TWO_HOURS_IN_MILLIS = 2 * 60 * 60 * 1000; // 2 hours in milliseconds

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        createNotificationChannel();
    }

    //Creates a notification channel for sync notifications
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

    //Returns a Notification object with the given content also displays sync progress
    private Notification createSyncNotification(String content) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Data Sync")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_sync) // Use your app's sync icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    //Main function called by the Worker
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

    //Performs actual sync from Firebase to SQLite
    private Result syncData(DatabaseReference sensorDataRef,
                            DatabaseHelper databaseHelper,
                            NotificationManager notificationManager) {
        final CountDownLatch latch = new CountDownLatch(1);
        final Result[] result = {Result.success()};
        final AtomicInteger processedCount = new AtomicInteger(0);

        // Step 1: Retry Failed Deletions
        retryFailedDeletions(sensorDataRef, databaseHelper, notificationManager);

        sensorDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dateSnapshots) {
                int totalRecords = 0;
                int syncableRecords = 0;

                // Count records (excluding recent ones)
                for (DataSnapshot dateSnapshot : dateSnapshots.getChildren()) {
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        totalRecords++;
                        if (isRecordOlderThanTwoHours(dateSnapshot.getKey(), timeSnapshot.getKey())) {
                            syncableRecords++;
                        }
                    }
                }

                notificationManager.notify(1, createSyncNotification(
                        "Syncing " + syncableRecords + " of " + totalRecords + " records"));

                // Process data
                for (DataSnapshot dateSnapshot : dateSnapshots.getChildren()) {
                    String date = dateSnapshot.getKey();

                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        // Skip records newer than 2 hours
                        if (!isRecordOlderThanTwoHours(date, timeSnapshot.getKey())) {
                            continue;
                        }

                        if (processRecord(date, timeSnapshot, databaseHelper)) {
                            int processed = processedCount.incrementAndGet();

                            if (processed % 10 == 0) {
                                notificationManager.notify(1, createSyncNotification(
                                        "Synced " + processed + " of " + syncableRecords + " records"));
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

    //Attempts to delete previously failed deletions from Firebase
    private void retryFailedDeletions(DatabaseReference sensorDataRef,
                                      DatabaseHelper databaseHelper,
                                      NotificationManager notificationManager) {
        Cursor cursor = databaseHelper.getFailedDeletions();
        int retryCount = cursor.getCount();
        AtomicInteger deletedCount = new AtomicInteger(0);

        if (retryCount > 0) {
            notificationManager.notify(1, createSyncNotification(
                    "Retrying " + retryCount + " failed deletions..."));
        }

        while (cursor.moveToNext()) {
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));

            // Only retry if older than 2 hours
            if (!isRecordOlderThanTwoHours(date, time)) {
                continue;
            }

            DatabaseReference recordRef = sensorDataRef.child(date).child(time);
            CountDownLatch deleteLatch = new CountDownLatch(1);
            recordRef.removeValue((error, ref) -> {
                if (error == null) {
                    databaseHelper.removeFailedDeletion(date, time);
                    Log.d(TAG, "Retry succeeded: Deleted " + date + " " + time);
                    deletedCount.incrementAndGet();
                } else {
                    Log.e(TAG, "Retry failed: " + date + " " + time + ", error: " + error.getMessage());
                }
                deleteLatch.countDown();
            });

            try {
                deleteLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted during retry: " + date + " " + time);
            }
        }
        cursor.close();

        if (retryCount > 0) {
            notificationManager.notify(1, createSyncNotification(
                    "Retried " + retryCount + " failed deletions, " + deletedCount + " succeeded"));
        }
    }

    //Checks if a record is older than 2 hours
    private boolean isRecordOlderThanTwoHours(String date, String time) {
        try {
            // Assuming date format is "yyyy-MM-dd" and time is "HH:mm:ss"
            String dateTimeStr = date + " " + time;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date recordDate = sdf.parse(dateTimeStr);
            long recordTime = recordDate.getTime();
            long currentTime = System.currentTimeMillis();
            return (currentTime - recordTime) > TWO_HOURS_IN_MILLIS;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date/time: " + date + " " + time, e);
            return true; // Assume old if parsing fails to avoid skipping valid records
        }
    }

    //Processes a specific Firebase entry
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
            long rowId = databaseHelper.insertDataWithCheck(date, time, temp, moisture, humidity);
            if (rowId == -1) {
                Log.w(TAG, "Failed to insert record: " + date + " " + time);
                return false;
            }

            if (rowId >= 0) {
                // Delete from firebase
                timeSnapshot.getRef().removeValue((error, ref) -> {
                    if (error == null) {
                        Log.d(TAG, "Deleted record: " + date + " " + time);
                    } else {
                        Log.e(TAG, "Failed to delete record: " + date + " " + time + ", error: " + error.getMessage());
                        databaseHelper.addFailedDeletion(date, time);
                    }
                });
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error processing record: " + date + " " + time, e);
            return false;
        } finally {
            databaseHelper.close();
        }
    }
}
