package com.example.multifeatureapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkActivity extends AppCompatActivity {
    private TextView tvConnectionStatus;
    private TextView tvConnectionType;
    private TextView tvTestResult;
    private MaterialButton btnCheckConnection;
    private MaterialButton btnTestConnection;
    private MaterialButton btnScanWifi;
    private ListView lvWifiNetworks;
    private WifiManager wifiManager;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ArrayAdapter<String> wifiListAdapter;
    private List<ScanResult> scanResults;
    private BroadcastReceiver wifiScanReceiver;
    private BroadcastReceiver wifiStateReceiver;
    private WifiHelper wifiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        // Initialize WifiHelper
        wifiHelper = new WifiHelper(this);

        // Request all necessary permissions
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE
        };
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 1);
                break;
            }
        }

        // Initialize views
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvConnectionType = findViewById(R.id.tvConnectionType);
        tvTestResult = findViewById(R.id.tvTestResult);
        btnCheckConnection = findViewById(R.id.btnCheckConnection);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnScanWifi = findViewById(R.id.btnScanWifi);
        lvWifiNetworks = findViewById(R.id.lvWifiNetworks);

        // Initialize WiFi
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Initialize executor and handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(getMainLooper());

        // Initialize WiFi list
        wifiListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvWifiNetworks.setAdapter(wifiListAdapter);

        // Set click listeners
        btnCheckConnection.setOnClickListener(v -> checkNetworkConnection());
        btnTestConnection.setOnClickListener(v -> testInternetConnection());
        btnScanWifi.setOnClickListener(v -> scanWifiNetworks());

        // Set WiFi network click listener
        lvWifiNetworks.setOnItemClickListener((parent, view, position, id) -> {
            if (scanResults != null && position < scanResults.size()) {
                showWifiPasswordDialog(scanResults.get(position));
            }
        });

        // Initialize WiFi scan receiver
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    scanSuccess(wifiManager.getScanResults());
                } else {
                    scanFailure();
                }
            }
        };

        // Initialize WiFi state receiver
        wifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.d("WiFiState", "WiFi is enabled");
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        Log.d("WiFiState", "WiFi is disabled");
                        break;
                }
            }
        };

        // Register receivers
        IntentFilter scanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        IntentFilter stateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiScanReceiver, scanFilter);
        registerReceiver(wifiStateReceiver, stateFilter);

        // Initial check
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();

        if (network == null) {
            updateConnectionStatus(false, "Not Connected");
            return;
        }

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            updateConnectionStatus(false, "Not Connected");
            return;
        }

        boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        StringBuilder connectionType = new StringBuilder();

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            connectionType.append("WiFi");
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            if (connectionType.length() > 0) connectionType.append(" & ");
            connectionType.append("Mobile Data");
        }

        updateConnectionStatus(hasInternet, connectionType.toString());
    }

    private void updateConnectionStatus(boolean isConnected, String connectionType) {
        String status = isConnected ? "Connected" : "Not Connected";
        tvConnectionStatus.setText("Status: " + status);
        tvConnectionType.setText("Connection Type: " + connectionType);
    }

    private void testInternetConnection() {
        tvTestResult.setText("Testing internet connection...");
        executorService.execute(() -> {
            try {
                // Test multiple reliable servers
                boolean googleTest = checkSingleConnection("https://www.google.com");
                boolean cloudflareTest = checkSingleConnection("https://1.1.1.1");

                mainHandler.post(() -> {
                    if (googleTest || cloudflareTest) {
                        tvTestResult.setText("✅ Internet connection is working well!");
                    } else {
                        tvTestResult.setText("❌ Internet connection test failed");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvTestResult.setText("❌ Connection test error: " + e.getMessage());
                });
            }
        });
    }

    private boolean checkSingleConnection(String urlString) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Android");
            urlConnection.setRequestProperty("Connection", "close");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } catch (IOException e) {
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Please enable WiFi", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return;
        }

        if (checkLocationPermission()) {
            wifiListAdapter.clear();
            wifiManager.startScan();
        }
    }

    private void scanSuccess(List<ScanResult> results) {
        scanResults = results;
        wifiListAdapter.clear();
        if (results != null) {
            for (ScanResult result : results) {
                String security = "";
                if (result.capabilities.contains("WPA2")) {
                    security = "WPA2";
                } else if (result.capabilities.contains("WPA")) {
                    security = "WPA";
                } else if (result.capabilities.contains("WEP")) {
                    security = "WEP";
                } else {
                    security = "Open";
                }
                wifiListAdapter.add(result.SSID + "\n" + security + " - Signal: " + result.level + " dBm");
                Log.d("WiFiScan", "SSID: " + result.SSID + ", BSSID: " + result.BSSID + 
                    ", Level: " + result.level + ", Capabilities: " + result.capabilities);
            }
            wifiListAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "No networks found", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanFailure() {
        Log.e("WiFiScan", "WiFi scan failed");
        Toast.makeText(this, "Failed to scan WiFi networks", Toast.LENGTH_SHORT).show();
    }

    private void showWifiPasswordDialog(ScanResult network) {
        // Check if already connected to this network
        if (isConnectedToWifi(network.SSID)) {
            return; // Silently return if already connected
        }

        // For connecting to a new network
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_wifi_password, null);
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);
        ImageButton btnShowPassword = dialogView.findViewById(R.id.btnShowPassword);

        // Set up show/hide password functionality
        btnShowPassword.setOnClickListener(v -> {
            int inputType = passwordInput.getInputType();
            if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) > 0) {
                passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                btnShowPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnShowPassword.setImageResource(android.R.drawable.ic_menu_view);
            }
            passwordInput.setSelection(passwordInput.getText().length());
        });

        builder.setView(dialogView)
                .setTitle("Connect to " + network.SSID)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    
                    // Check if network requires password
                    boolean requiresPassword = network.capabilities.contains("WPA") || 
                                            network.capabilities.contains("WPA2") || 
                                            network.capabilities.contains("WEP");
                    
                    if (requiresPassword && password.trim().isEmpty()) {
                        Toast.makeText(this, "Password is required for this network", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Try the newer network suggestion method first
                    wifiHelper.suggestWifiNetwork(network.SSID, password);
                    
                    // If that fails, fall back to the traditional method
                    new Handler().postDelayed(() -> {
                        if (!isConnectedToWifi(network.SSID)) {
                            wifiHelper.connectToWifi(network.SSID, password);
                            
                            // Check connection status multiple times
                            final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger(0);
                            Handler connectionCheckHandler = new Handler();
                            Runnable connectionCheck = new Runnable() {
                                @Override
                                public void run() {
                                    if (isConnectedToWifi(network.SSID)) {
                                        Toast.makeText(NetworkActivity.this, 
                                            "Connected to " + network.SSID, 
                                            Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    if (attempts.incrementAndGet() < 3) {
                                        connectionCheckHandler.postDelayed(this, 2000);
                                    } else {
                                        Toast.makeText(NetworkActivity.this, 
                                            "Failed to connect. Please check your password.", 
                                            Toast.LENGTH_LONG).show();
                                    }
                                }
                            };
                            connectionCheckHandler.postDelayed(connectionCheck, 2000);
                        }
                    }, 2000);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isConnectedToWifi(String ssid) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                String connectedSSID = wifiManager.getConnectionInfo().getSSID();
                return connectedSSID != null && connectedSSID.equals("\"" + ssid + "\"");
            }
        }
        return false;
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(wifiScanReceiver);
            unregisterReceiver(wifiStateReceiver);
        } catch (Exception e) {
            Log.e("NetworkActivity", "Error unregistering receivers: " + e.getMessage());
        }
        executorService.shutdown();
    }
}