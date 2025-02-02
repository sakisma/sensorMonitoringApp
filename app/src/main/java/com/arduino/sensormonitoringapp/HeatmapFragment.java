package com.arduino.sensormonitoringapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import com.google.gson.Gson;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeatmapFragment extends Fragment {

//    private HeatMapView heatMapView;

    private WebView webView;
    private FirebaseDatabase database;
    private DatabaseReference sensorDataRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heatmap, container, false);

        webView = view.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
//        webView.addJavascriptInterface(new WebAppInterface(getContext()), "android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("HeatmapFragment", "Page finished loading: " + url);
                fetchHeatmapData();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e("HeatmapFragment", "Error loading page: " + error.getDescription());
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.loadUrl("file:///android_asset/heatmap.html");
        database = FirebaseDatabase.getInstance();
        sensorDataRef = database.getReference("sensorData");

        return view;
    }

    private void fetchHeatmapData() {
        sensorDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Map<String, Object>> heatmapData = new ArrayList<>();

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String date = dateSnapshot.getKey(); // Get the date (e.g., "2024-09-26")
                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
                        String time = timeSnapshot.getKey(); // Get the time (e.g., "01:38:03")

                        String[] parts = date.split("-");
                        int year = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int day = Integer.parseInt(parts[2]);

                        // Combine date and time to create a timestamp
                        String timestamp = date + "T" + time + ".000Z";

                        // Fetch temperature value
                        Object tempObject = timeSnapshot.child("temp").getValue();
                        if (tempObject != null) {
                            float temperature = Float.parseFloat(tempObject.toString());
                            // Create month/year format for y-axis
                            String formattedMonthYear = month + "/" + year;

                            // Add data to the list
                            Map<String, Object> dataPoint = new HashMap<>();
                            dataPoint.put("date", timestamp);
                            dataPoint.put("monthYear", formattedMonthYear);
                            dataPoint.put("temperature", temperature);
                            heatmapData.add(dataPoint);
                        }
                    }
                }

                // Convert the data to JSON and pass it to JavaScript
                String jsonData = new Gson().toJson(heatmapData);
                Log.d("HeatmapFragment", "JSON Data: " + jsonData);
                webView.evaluateJavascript("updateHeatmapData(" + JSONObject.quote(jsonData) + ");", null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
                Log.e("HeatmapFragment", "Database error: " + error.getMessage());
            }
        });
    }

    public class WebAppInterface {
        Context context;

        WebAppInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public String getHeatmapData() {
            // This method can be called from JavaScript if needed
            return "[]"; // Return empty data by default
        }
    }
}

    //    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_heatmap, container, false);
//
//        heatMapView = view.findViewById(R.id.heatmap_view);
//        database = FirebaseDatabase.getInstance();
//        sensorDataRef = database.getReference("sensorData");
//
//        fetchHeatmapData();
//
//        return view;
//    }
//
//    private void fetchHeatmapData() {
//        sensorDataRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                Map<Long, List<Float>> heatmapData = new HashMap<>();
//
//                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
//                    String date = dateSnapshot.getKey(); // Get the date (e.g., "2024-09-26")
//                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
//                        String time = timeSnapshot.getKey(); // Get the time (e.g., "01:38:03")
//
//                        // Combine date and time to create a timestamp
//                        String timestampString = date + " " + time;
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//                        try {
//                            Date timestampDate = dateFormat.parse(timestampString);
//                            if (timestampDate != null) {
//                                long timestamp = timestampDate.getTime(); // Get the timestamp in milliseconds
//
//                                Object tempObject = timeSnapshot.child("temp").getValue();
//                                if (tempObject != null) {
//                                    float tempValue = Float.parseFloat(tempObject.toString());
//
//                                    // Add the temperature value to the corresponding timestamp
//                                    if (!heatmapData.containsKey(timestamp)) {
//                                        heatmapData.put(timestamp, new ArrayList<>());
//                                    }
//                                    heatmapData.get(timestamp).add(tempValue);
//                                }
//                            }
//                        } catch (ParseException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//                heatMapView.setHeatmapData(heatmapData);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
//    }

//    private void fetchHeatmapData() {
//        sensorDataRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                List<Float> temperatureValue = new ArrayList<>();
//
//                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
//                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
//                        Object tempObject = timeSnapshot.child("temp").getValue();
//                        if (tempObject != null) {
//                            float tempValue = Float.parseFloat(tempObject.toString());
//                            temperatureValue.add(tempValue);
//                        }
//                    }
//                }
//
//                heatMapView.setTemperatureValues(temperatureValue);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
//    }



    //    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_heatmap, container, false);
//
//        heatmapGrid = view.findViewById(R.id.heatmap_grid);
//        database = FirebaseDatabase.getInstance();
//        sensorDataRef = database.getReference("sensorData");
//
//        fetchHeatmapData();
//
//        return view;
//    }

//    private void fetchHeatmapData() {
//        sensorDataRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                List<Float> temperatureValues = new ArrayList<>();
//
//                // Λήψη του ορίου θερμοκρασίας από τις Ρυθμίσεις
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
//                float temperatureThreshold = Float.parseFloat(sharedPreferences.getString("temperature_threshold", "30"));
//
//                boolean thresholdExceeded = false;
//
//                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
//                    for (DataSnapshot timeSnapshot : dateSnapshot.getChildren()) {
//                        Object tempObject = timeSnapshot.child("temp").getValue();
//                        if (tempObject != null) {
//                            float tempValue = Float.parseFloat(tempObject.toString());
//                            temperatureValues.add(tempValue);
//
//                            // Έλεγχος αν ξεπερνά το όριο θερμοκρασίας
//                            if (tempValue > temperatureThreshold) {
//                                thresholdExceeded = true;
//                            }
//                        }
//                    }
//                }
//
//                // Αν ξεπεραστεί το όριο, στείλε ειδοποίηση
//                if (thresholdExceeded) {
//                    sendNotification("Temperature Alert", "A temperature has exceeded " + temperatureThreshold + "°C!");
//                }
//
//                populateHeatmap(temperatureValues);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Διαχείριση σφαλμάτων
//            }
//        });
//    }

//    private void populateHeatmap(List<Float> temperatureValues) {
//        heatmapGrid.removeAllViews();
//
//        int totalCells = heatmapGrid.getColumnCount() * heatmapGrid.getRowCount();
//
//        for (int i = 0; i < totalCells && i < temperatureValues.size(); i++) {
//            View cell = new View(getContext());
//
//            float temperature = temperatureValues.get(i);
//            cell.setBackgroundColor(getColorForTemperature(temperature));
//
//            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
//            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
//            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
//            params.width = 0;
//            params.height = 0;
//            params.setMargins(4, 4, 4, 4);
//            cell.setLayoutParams(params);
//
//            heatmapGrid.addView(cell);
//        }
//    }
//
//    private int getColorForTemperature(float temperature) {
//        if (temperature < 15) {
//            return Color.BLUE;
//        } else if (temperature < 25) {
//            return Color.GREEN;
//        } else if (temperature < 35) {
//            return Color.YELLOW;
//        } else {
//            return Color.RED;
//        }
//    }
//    private void checkTemperatureAndNotify(float currentTemperature) {
//        // Πάρε τις ρυθμίσεις του χρήστη
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//        boolean notificationsEnabled = prefs.getBoolean("enable_notifications", true);
//        String maxTemperatureString = prefs.getString("max_temperature", "30");
//        float maxTemperature = Float.parseFloat(maxTemperatureString);
//
//        // Έλεγχος: Αν είναι ενεργοποιημένες οι ειδοποιήσεις και ξεπεράστηκε η θερμοκρασία
//        if (notificationsEnabled && currentTemperature > maxTemperature) {
//            sendNotification("Temperature Alert", "The temperature exceeded " + maxTemperature + "°C!");
//        }
//    }
//
//    private void sendNotification(String title, String message) {
//        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
//        String channelId = "temperature_alerts";
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(channelId, "Temperature Alerts", NotificationManager.IMPORTANCE_HIGH);
//            channel.setDescription("Notifications for temperature threshold alerts");
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), channelId)
//                .setSmallIcon(R.drawable.ic_notification) // Βεβαιώσου ότι υπάρχει το εικονίδιο
//                .setContentTitle(title)
//                .setContentText(message)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true);
//
//        notificationManager.notify(1, builder.build());
//    }

//}
