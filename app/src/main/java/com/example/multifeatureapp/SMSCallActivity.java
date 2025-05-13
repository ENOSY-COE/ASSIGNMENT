package com.example.multifeatureapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SMSCallActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextInputEditText phoneNumberInput, messageInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smscall);

        // Initialize views
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        messageInput = findViewById(R.id.messageInput);
        MaterialButton btnCall = findViewById(R.id.btnCall);
        MaterialButton btnSendSMS = findViewById(R.id.btnSendSMS);
        statusText = findViewById(R.id.statusText);

        // Set click listeners
        btnCall.setOnClickListener(v -> makePhoneCall());
        btnSendSMS.setOnClickListener(v -> sendSMS());

        // Request permissions
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS
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
        }
    }

    private void makePhoneCall() {
        String phoneNumber = phoneNumberInput.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
            statusText.setText("Calling: " + phoneNumber);
        } else {
            Toast.makeText(this, "Call permission required", Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
        }
    }

    private void sendSMS() {
        String phoneNumber = phoneNumberInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please enter both phone number and message",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                statusText.setText("SMS sent successfully to: " + phoneNumber);
                Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                statusText.setText("Failed to send SMS: " + e.getMessage());
                Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Permissions are required for full functionality",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}