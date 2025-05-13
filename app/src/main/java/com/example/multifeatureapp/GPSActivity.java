package com.example.multifeatureapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

public class GPSActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int REQUEST_CHECK_SETTINGS = 102;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        tvLocation = findViewById(R.id.tvLocation);
        MaterialButton btnGetLocation = findViewById(R.id.btnGetLocation);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        createLocationRequest();
        createLocationCallback();

        btnGetLocation.setOnClickListener(v -> checkLocationSettingsAndStartUpdates());
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                }
            }
        };
    }

    private void checkLocationSettingsAndStartUpdates() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(this, (LocationSettingsResponse response) -> {
                    // All location settings are satisfied. Start location updates
                    startLocationUpdates();
                })
                .addOnFailureListener(this, e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(GPSActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            checkLocationSettingsAndStartUpdates();
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    private void startLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    private void updateLocationUI(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Update TextView
        String locationText = String.format("Location:\nLatitude: %f\nLongitude: %f",
                latitude, longitude);
        tvLocation.setText(locationText);

        // Update Map
        LatLng currentLocation = new LatLng(latitude, longitude);
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettingsAndStartUpdates();
            } else {
                Toast.makeText(this, "Location permission is required for this feature",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // User agreed to enable location
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location settings must be enabled to use this feature",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}