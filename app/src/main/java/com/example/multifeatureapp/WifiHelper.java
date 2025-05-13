package com.example.multifeatureapp;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class WifiHelper {
    private WifiManager wifiManager;
    private Context context;

    public WifiHelper(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void connectToWifi(String ssid, String password) {
        // First check if we're already connected to this network
        if (isConnectedToWifi(ssid)) {
            return; // Silently return if already connected
        }

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // Remove any existing configuration for this network first
        removeExistingNetwork(ssid);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + ssid + "\"";
        
        // Set security type based on password
        if (password != null && !password.isEmpty()) {
            wifiConfig.preSharedKey = "\"" + password + "\"";
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        } else {
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        int networkId = wifiManager.addNetwork(wifiConfig);
        if (networkId == -1) {
            return; // Silently return if fails
        }

        wifiManager.disconnect();
        wifiManager.enableNetwork(networkId, true);
        wifiManager.reconnect();
    }

    private boolean isConnectedToWifi(String ssid) {
        String connectedSSID = wifiManager.getConnectionInfo().getSSID();
        return connectedSSID != null && connectedSSID.equals("\"" + ssid + "\"");
    }

    private void removeExistingNetwork(String ssid) {
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                if (config.SSID != null && config.SSID.equals("\"" + ssid + "\"")) {
                    wifiManager.removeNetwork(config.networkId);
                    wifiManager.saveConfiguration();
                }
            }
        }
    }

    public void suggestWifiNetwork(String ssid, String password) {
        // First check if we're already connected to this network
        if (isConnectedToWifi(ssid)) {
            return; // Silently return if already connected
        }

        // Ensure Wi-Fi is enabled
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // Create a Wi-Fi network suggestion
        WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setIsAppInteractionRequired(true);

        if (password != null && !password.isEmpty()) {
            builder.setWpa2Passphrase(password);
        }

        WifiNetworkSuggestion suggestion = builder.build();

        // Add suggestion to a list
        List<WifiNetworkSuggestion> suggestions = new ArrayList<>();
        suggestions.add(suggestion);

        // Remove any existing suggestions for this network
        wifiManager.removeNetworkSuggestions(suggestions);

        // Suggest the network
        int status = wifiManager.addNetworkSuggestions(suggestions);

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(context, "Connecting to " + ssid, Toast.LENGTH_SHORT).show();
        }
    }

    public void listConfiguredNetworks() {
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                String ssid = config.SSID != null ? config.SSID : "Unknown";
                android.util.Log.d("WifiConfig", "SSID: " + ssid + ", Network ID: " + config.networkId);
            }
        } else {
            Toast.makeText(context, "No configured networks found or permission denied", Toast.LENGTH_SHORT).show();
        }
    }
} 