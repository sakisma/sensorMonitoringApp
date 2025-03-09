package com.arduino.sensormonitoringapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SetupArduinoWifi extends AppCompatActivity {
    private static final String AP_SSID = "GreenhouseConfig"; // Replace with your Arduino's AP SSID
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private EditText etPassword;
    private Spinner spinnerSSID;
    private Button btnScan, btnConfigure;
    private WifiManager wifiManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private List<String> availableNetworks = new ArrayList<>();
    private String selectedSSID = "";
    private List<Long> scanTimestamps = new ArrayList<>();
    private static final int MAX_SCANS = 4; // Maximum allowed scans in 2 minutes
    private static final long SCAN_WINDOW_MS = 120000; // 2 minutes in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_arduino_wifi); // Create a new layout file for this activity

        spinnerSSID = findViewById(R.id.spinnerSSID);
        etPassword = findViewById(R.id.etPassword);
        btnScan = findViewById(R.id.btnScan);
        btnConfigure = findViewById(R.id.btnConfigure);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        btnScan.setEnabled(true);

        checkPermissions();

        btnScan.setOnClickListener(v -> scanForNetworks());

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);

        spinnerSSID.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSSID = availableNetworks.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSSID = "";
            }
        });

        btnConfigure.setOnClickListener(v -> {
            String password = etPassword.getText().toString();

            if (TextUtils.isEmpty(selectedSSID) || password.isEmpty()) {
                Toast.makeText(SetupArduinoWifi.this, "Enter WiFi SSID and Password", Toast.LENGTH_SHORT).show();
            } else {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String currentSSID = wifiInfo.getSSID();
                String currentIP = Formatter.formatIpAddress(wifiInfo.getIpAddress());
                Log.i("WIFI IP", "CURRENT IP: " + currentIP);
                if (currentSSID.equals("\"GreenhouseConfig\"")) {
                    sendWiFiCredentials(selectedSSID, password);
                } else {
                    Toast.makeText(SetupArduinoWifi.this, "Wrong network!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
////        unregisterReceiver(wifiScanReceiver);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            connectToAP();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToAP();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scanSuccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        availableNetworks.clear();

        for (ScanResult result : results) {
            if (!availableNetworks.contains(result.SSID) && !TextUtils.isEmpty(result.SSID)) {
                availableNetworks.add(result.SSID);
            }
        }

        if (availableNetworks.isEmpty()) {
            availableNetworks.add("No networks found");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(SetupArduinoWifi.this, android.R.layout.simple_spinner_item, availableNetworks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSSID.setAdapter(adapter);
        btnScan.setEnabled(true);
    }

    private void scanFailure() {
        Toast.makeText(SetupArduinoWifi.this, "WiFi scan failed. Try again.", Toast.LENGTH_SHORT).show();
        btnScan.setEnabled(true);
    }

    private void scanForNetworks() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled. Please enable it first.", Toast.LENGTH_SHORT).show();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            scanTimestamps.removeIf(timestamp -> currentTime - timestamp > SCAN_WINDOW_MS);
        }

        if (scanTimestamps.size() >= MAX_SCANS) {
            long oldestScan = scanTimestamps.get(0);
            long waitTime = SCAN_WINDOW_MS - (currentTime - oldestScan);
            String message = "Wait " + (waitTime / 1000) + " seconds before scanning again";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            btnScan.setEnabled(false);
            new Handler().postDelayed(() -> btnScan.setEnabled(true), waitTime);
            return;
        }

        btnScan.setEnabled(false);
        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(this, "WiFi scan failed. Try again.", Toast.LENGTH_SHORT).show();
            btnScan.setEnabled(true);
        } else {
            scanTimestamps.add(currentTime);
        }
    }

    private void connectToAP() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(AP_SSID)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build();

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                    handler.post(() -> Toast.makeText(SetupArduinoWifi.this, "Connected to " + AP_SSID, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    handler.post(() -> Toast.makeText(SetupArduinoWifi.this, "Failed to connect to " + AP_SSID, Toast.LENGTH_SHORT).show());
                }
            });
            Toast.makeText(this, "Select '" + AP_SSID + "' in the WiFi prompt.", Toast.LENGTH_LONG).show();
        } else {
            new Thread(() -> {
                WifiConfiguration config = new WifiConfiguration();
                config.SSID = "\"" + AP_SSID + "\"";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                int netId = wifiManager.addNetwork(config);
                if (netId != -1) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                }

                boolean connected = false;
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    if (isAPConnected()) {
                        connected = true;
                        break;
                    }
                }

                boolean finalConnected = connected;
                handler.post(() -> {
                    if (finalConnected) {
                        Toast.makeText(SetupArduinoWifi.this, "Connected to " + AP_SSID, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SetupArduinoWifi.this, "Failed to connect automatically. Please connect manually.", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        }
    }

    private boolean isAPConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
                Log.i("IP ADDRESS", "CURRENT IP:" + ipAddress);
                String currentSSID = wifiInfo.getSSID().replace("\"", "");
                return currentSSID.equals(AP_SSID);
            }
        }
        return false;
    }

    private void sendWiFiCredentials(String ssid, String password) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);

                URL url = new URL("http://192.168.4.1/configure");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                String data = "ssid=" + ssid + "&password=" + password;
                connection.getOutputStream().write(data.getBytes());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(SetupArduinoWifi.this, "WiFi Configured!", Toast.LENGTH_SHORT).show();
                        // Mark setup as completed
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        prefs.edit().putBoolean("setup_completed", true).apply();
                        // Navigate back to MainActivity
                        startActivity(new Intent(SetupArduinoWifi.this, MainActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(SetupArduinoWifi.this, "Config Failed", Toast.LENGTH_SHORT).show());
                }

                connection.disconnect();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(SetupArduinoWifi.this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            runOnUiThread(() -> {
                if (success) {
                    scanSuccess();
                } else {
                    scanFailure();
                }
                btnScan.setEnabled(true);
            });
        }
    };
}
