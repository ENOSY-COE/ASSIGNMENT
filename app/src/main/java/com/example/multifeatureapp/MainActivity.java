package com.example.multifeatureapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import android.Manifest;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    private String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.INTERNET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons
        MaterialButton btnCamera = findViewById(R.id.btnCamera);
        MaterialButton btnGPS = findViewById(R.id.btnGPS);
        MaterialButton btnSMS = findViewById(R.id.btnSMS);
        MaterialButton btnNetwork = findViewById(R.id.btnNetwork);
        MaterialButton btnBluetooth = findViewById(R.id.btnBluetooth);

        // Request permissions
        checkAndRequestPermissions();

        // Set click listeners
            btnCamera.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            });

        btnGPS.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GPSActivity.class);
            startActivity(intent);
        });

        btnSMS.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SMSCallActivity.class);
            startActivity(intent);
        });

        btnNetwork.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NetworkActivity.class);
            startActivity(intent);
        });

        btnBluetooth.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
            startActivity(intent);
        });
    }

    private void checkAndRequestPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE);
                break;
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
            if (!allGranted) {
                Toast.makeText(this, "Some permissions are required for all features",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}