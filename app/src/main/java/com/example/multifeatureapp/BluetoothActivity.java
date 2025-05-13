package com.example.multifeatureapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private TextView tvBluetoothStatus;
    private TextView tvConnectionStatus;
    private MaterialButton btnToggleBluetooth;
    private MaterialButton btnScanDevices;
    private ListView lvDevices;
    private ArrayAdapter<BluetoothDeviceWrapper> deviceArrayAdapter;
    private ArrayList<BluetoothDeviceWrapper> deviceList;
    private boolean isScanning = false;
    private boolean isHandlingStop = false;

    private class BluetoothDeviceWrapper {
        BluetoothDevice device;
        boolean isHeader;
        String displayName;

        BluetoothDeviceWrapper(BluetoothDevice device) {
            this.device = device;
            this.isHeader = false;
            this.displayName = (device.getName() != null ? device.getName() : "Unknown Device")
                    + "\n" + device.getAddress();
        }

        BluetoothDeviceWrapper(String header) {
            this.isHeader = true;
            this.displayName = header;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // Initialize Bluetooth adapter
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Initialize views
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        btnToggleBluetooth = findViewById(R.id.btnToggleBluetooth);
        btnScanDevices = findViewById(R.id.btnScanDevices);
        lvDevices = findViewById(R.id.lvDevices);

        // Initialize lists
        deviceList = new ArrayList<>();
        deviceArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, deviceList);
        lvDevices.setAdapter(deviceArrayAdapter);

        // Set click listeners
        btnToggleBluetooth.setOnClickListener(v -> toggleBluetooth());
        btnScanDevices.setOnClickListener(v -> toggleScanning());

        // Set device click listeners
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDeviceWrapper wrapper = deviceArrayAdapter.getItem(position);
            if (wrapper != null && !wrapper.isHeader) {
                connectToDevice(wrapper.device);
            }
        });

        lvDevices.setOnItemLongClickListener((parent, view, position, id) -> {
            BluetoothDeviceWrapper wrapper = deviceArrayAdapter.getItem(position);
            if (wrapper != null && !wrapper.isHeader) {
                showDeviceOptionsDialog(wrapper.device);
            }
            return true;
        });

        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);

        // Check initial Bluetooth state
        updateBluetoothStatus(bluetoothAdapter != null ? bluetoothAdapter.getState() :
                BluetoothAdapter.ERROR);
    }

    private void addDeviceToList(BluetoothDevice device) {
        BluetoothDeviceWrapper newDevice = new BluetoothDeviceWrapper(device);

        // Check if device is already in the list
        for (BluetoothDeviceWrapper wrapper : deviceList) {
            if (!wrapper.isHeader &&
                    wrapper.device.getAddress().equals(device.getAddress())) {
                return; // Device already in list
            }
        }

        deviceList.add(newDevice);
        deviceArrayAdapter.notifyDataSetChanged();
    }

    private void toggleScanning() {
        if (isScanning) {
            // Stop scanning
            isHandlingStop = true;  // Set flag before stopping
            isScanning = false;
            btnScanDevices.setText("Scan for Devices");
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        } else {
            // Start scanning
            isHandlingStop = false;  // Reset flag
            isScanning = true;
            btnScanDevices.setText("Stop Scanning");
            refreshDeviceList();
        }
    }

    // Update the BroadcastReceiver's onReceive method:
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    addDeviceToList(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                btnScanDevices.setText("Scan for Devices");
                if (isHandlingStop) {
                    // Only show message if this was triggered by user stopping the scan
                    Toast.makeText(context, "Scanning finished", Toast.LENGTH_SHORT).show();
                    isHandlingStop = false;  // Reset the flag
                }
                if (isScanning && !isHandlingStop) {
                    // If we're still supposed to be scanning, restart discovery
                    refreshDeviceList();
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                updateBluetoothStatus(state);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(context, "Successfully paired with " + device.getName(),
                                Toast.LENGTH_SHORT).show();
                        refreshDeviceList();
                    }
                }
            }
        }
    };

    private void updateBluetoothStatus(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                tvBluetoothStatus.setText("Status: Enabled");
                btnToggleBluetooth.setText("Turn Bluetooth Off");
                btnScanDevices.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_OFF:
                tvBluetoothStatus.setText("Status: Disabled");
                btnToggleBluetooth.setText("Turn Bluetooth On");
                btnScanDevices.setEnabled(false);
                deviceList.clear();
                deviceArrayAdapter.notifyDataSetChanged();
                break;
            default:
                tvBluetoothStatus.setText("Status: Unknown");
                break;
        }
    }

    private void toggleBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (checkBluetoothPermissions()) {
            try {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent);
                } else {
                    // Stop any ongoing discovery
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }

                    // Close any existing connection
                    if (bluetoothSocket != null) {
                        try {
                            bluetoothSocket.close();
                        } catch (IOException e) {
                            // Handle close exception
                        }
                        bluetoothSocket = null;
                    }

                    // Turn off Bluetooth using system settings
                    Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(intent);

                    // Clear UI
                    tvConnectionStatus.setText("");
                    deviceList.clear();
                    deviceArrayAdapter.notifyDataSetChanged();

                    Toast.makeText(this, "Please turn off Bluetooth in settings",
                            Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error toggling Bluetooth: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshDeviceList() {
        if (!checkBluetoothPermissions()) {
            return;
        }

        deviceList.clear();

        // Add paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            deviceList.add(new BluetoothDeviceWrapper("=== Paired Devices ==="));
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(new BluetoothDeviceWrapper(device));
            }
        }

        deviceList.add(new BluetoothDeviceWrapper("=== Available Devices ==="));
        deviceArrayAdapter.notifyDataSetChanged();

        // Start discovery only if we're in scanning mode
        if (isScanning && !isHandlingStop) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        }
    }

    private void showDeviceOptionsDialog(BluetoothDevice device) {
        String[] options;
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            options = new String[]{"Connect", "Unpair", "Cancel"};
        } else {
            options = new String[]{"Pair", "Cancel"};
        }

        new AlertDialog.Builder(this)
                .setTitle(device.getName() != null ? device.getName() : "Unknown Device")
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("Pair") || options[which].equals("Connect")) {
                        connectToDevice(device);
                    } else if (options[which].equals("Unpair")) {
                        unpairDevice(device);
                    }
                })
                .show();
    }

    private void connectToDevice(BluetoothDevice device) {
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            // If not paired, pair first
            pairDevice(device);
            return;
        }

        // If already paired, try to connect
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        new Thread(() -> {
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Connecting to: " + device.getName());
                });

                bluetoothSocket.connect();

                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Connected to: " + device.getName());
                    Toast.makeText(this, "Connected to " + device.getName(),
                            Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Connection failed");
                    Toast.makeText(this, "Connection failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    // Handle close exception
                }
            }
        }).start();
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                tvConnectionStatus.setText("Pairing with: " + device.getName());
                device.createBond();
                Toast.makeText(this, "Pairing with " + device.getName(),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Already paired with " + device.getName(),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            tvConnectionStatus.setText("Pairing failed");
            Toast.makeText(this, "Error pairing with device: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            // First disconnect if connected
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                bluetoothSocket.close();
                tvConnectionStatus.setText("");
            }

            // Then unpair
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

            Toast.makeText(this, "Unpairing " + device.getName(), Toast.LENGTH_SHORT).show();
            refreshDeviceList();
        } catch (Exception e) {
            Toast.makeText(this, "Error unpairing device: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkBluetoothPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScanning = false;
        isHandlingStop = false;
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // Handle close exception
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (isScanning) {
                    refreshDeviceList();
                }
            } else {
                Toast.makeText(this, "All permissions are required for Bluetooth scanning",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}