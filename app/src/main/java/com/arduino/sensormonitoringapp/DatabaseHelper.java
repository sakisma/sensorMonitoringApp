package com.arduino.sensormonitoringapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SensorData.db";
    private static final int DATABASE_VERSION = 10;
    private static final String TABLE_NAME = "sensor_data";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TIME = "time";
    static final String COLUMN_TEMPERATURE = "temperature";
    static final String COLUMN_MOISTURE = "moisture";
    static final String COLUMN_HUMIDITY = "humidity";
    static final String COLUMN_TIMESTAMP = "timestamp"; // New column

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_TIME + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT, " +
                COLUMN_TEMPERATURE + " REAL, " +
                COLUMN_HUMIDITY + " REAL, " +
                COLUMN_MOISTURE + " REAL)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    public long insertData(String date, String time, double temp, double moisture, double humidity) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_DATE, date);
            values.put(COLUMN_TIME, time);
            values.put(COLUMN_TIMESTAMP, date + " " + time);
            values.put(COLUMN_TEMPERATURE, temp);
            values.put(COLUMN_MOISTURE, moisture);
            values.put(COLUMN_HUMIDITY, humidity);
            return db.insert(TABLE_NAME, null, values);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public Cursor getYearlyData(int year) {
        // Get all data points for the year
        String query = "SELECT " +
                COLUMN_TIMESTAMP + ", " +
                COLUMN_TEMPERATURE + ", " +
                COLUMN_MOISTURE + " " +
                "FROM " + TABLE_NAME + " " +
                "WHERE strftime('%Y', " + COLUMN_TIMESTAMP + ") = ? " +
                "ORDER BY " + COLUMN_TIMESTAMP + " ASC";

        return getReadableDatabase().rawQuery(query, new String[]{String.valueOf(year)});
    }

    public Cursor getYearlyAverages(int year) {
        // Get daily averages for the year
        String query = "SELECT " +
                "date(" + COLUMN_TIMESTAMP + ") as day, " +
                "AVG(" + COLUMN_TEMPERATURE + ") as avg_temp, " +
                "AVG(" + COLUMN_MOISTURE + ") as avg_moisture " +
                "FROM " + TABLE_NAME + " " +
                "WHERE strftime('%Y', " + COLUMN_TIMESTAMP + ") = ? " +
                "GROUP BY day " +
                "ORDER BY day ASC";

        return getReadableDatabase().rawQuery(query, new String[]{String.valueOf(year)});
    }

    public Cursor getDataForDateRange(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME +
                " WHERE " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?" +
                " ORDER BY " + COLUMN_TIMESTAMP + " ASC";

        Log.d("DB_QUERY", query + " [" + startDate + " -> " + endDate + "]");
        return db.rawQuery(query, new String[]{startDate, endDate});
    }


    // Get hourly averages (alternative)
    public Cursor getHourlyAverages(long startMillis, long endMillis) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " +
                "strftime('%Y-%m-%d %H:00:00', " + COLUMN_TIMESTAMP + ") as hour, " +
                "AVG(" + COLUMN_TEMPERATURE + ") as avg_temp, " +
                "AVG(" + COLUMN_MOISTURE + ") as avg_moisture " +
                "FROM " + TABLE_NAME + " " +
                "WHERE datetime(" + COLUMN_TIMESTAMP + ") BETWEEN datetime(?, 'unixepoch') AND datetime(?, 'unixepoch') " +
                "GROUP BY strftime('%Y-%m-%d %H', " + COLUMN_TIMESTAMP + ") " +
                "ORDER BY hour ASC";

        return db.rawQuery(query, new String[]{
                String.valueOf(startMillis/1000),
                String.valueOf(endMillis/1000)
        });
    }

    // For daily averages with min/max
    public Cursor getDailyAverages(long startMillis, long endMillis) {
        String query = "SELECT " +
                "date(" + COLUMN_TIMESTAMP + ") as day, " +
                "AVG(" + COLUMN_TEMPERATURE + ") as avg_temp, " +
                "AVG(" + COLUMN_MOISTURE + ") as avg_moisture, " +
                "MIN(" + COLUMN_TEMPERATURE + ") as min_temp, " +
                "MAX(" + COLUMN_TEMPERATURE + ") as max_temp, " +
                "MIN(" + COLUMN_MOISTURE + ") as min_moisture, " +
                "MAX(" + COLUMN_MOISTURE + ") as max_moisture " +
                "FROM " + TABLE_NAME + " " +
                "WHERE datetime(" + COLUMN_TIMESTAMP + ") BETWEEN datetime(?, 'unixepoch') AND datetime(?, 'unixepoch') " +
                "GROUP BY day " +
                "ORDER BY day ASC";

        return getReadableDatabase().rawQuery(query, new String[]{
                String.valueOf(startMillis/1000),
                String.valueOf(endMillis/1000)
        });
    }

    // For raw data
    public Cursor getRawData(long startMillis, long endMillis) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM " + TABLE_NAME + " " +
                        "WHERE datetime(" + COLUMN_TIMESTAMP + ") BETWEEN datetime(?, 'unixepoch') AND datetime(?, 'unixepoch') " +
                        "ORDER BY " + COLUMN_TIMESTAMP + " ASC",
                new String[]{String.valueOf(startMillis/1000), String.valueOf(endMillis/1000)}
        );
    }

    public List<DataPoint> getDataPoints(String startDate, String endDate) {
        List<DataPoint> dataPoints = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT timestamp, temperature, moisture FROM " + TABLE_NAME + " WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";
        Cursor cursor = db.rawQuery(query, new String[]{startDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                String timestamp = cursor.getString(0);
                double temperature = cursor.getDouble(1);
                int moisture = cursor.getInt(2);
                dataPoints.add(new DataPoint(timestamp, temperature, moisture));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return dataPoints;
    }

    @Override
    public synchronized void close() {
        super.close(); // Ensures all connections are released
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public static class DataPoint {
        public String timestamp;
        public double temperature;
        public int moisture;

        public DataPoint(String timestamp, double temperature, int moisture) {
            this.timestamp = timestamp;
            this.temperature = temperature;
            this.moisture = moisture;
        }
    }
}
