package com.kegszool.weather;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class GpsTracker {

    private static final String TAG = "GpsTracker";
    public static boolean isFromSetting = false;

    private final Context mContext;
    boolean canGetLocation = false;

    Location location;
    double latitude;
    double longitude;

    private static final long MIN_TIME_BW_UPDATES = 60000; // 1 min
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f; // 10 meters

    private final FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    @RequiresPermission(allOf = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    public GpsTracker(Context context) {
        this.mContext = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
        getLocation();
    }

    @RequiresPermission(allOf = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    })
    public Location getLocation() {
        if (!hasLocationPermission()) {
            requestPermissions();
            return location;
        }
        canGetLocation = true;
        if (locationCallback == null) {
            startLocationUpdates();
        }
        fusedClient.getLastLocation()
            .addOnSuccessListener(loc -> {
                if (loc != null) {
                    updateLocation(loc);
                }
            });
        return location;
    }

    @RequiresPermission(allOf = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
            .setInterval(MIN_TIME_BW_UPDATES)
            .setFastestInterval(MIN_TIME_BW_UPDATES / 2)
            .setSmallestDisplacement(MIN_DISTANCE_CHANGE_FOR_UPDATES)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    updateLocation(loc);
                }
            }
        };
        fusedClient.requestLocationUpdates(
                request, locationCallback, Looper.getMainLooper());
    }

    private void updateLocation(Location loc) {
        this.location = loc;
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
    }

    public void stopUsingGPS(){
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }
        return latitude;
    }

     public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public void showSettingsAlert() {
        try {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
            var permissionMsg ="GPS is not enabled. Do you want to go to settings menu?";
            alertDialog.setTitle("GPS settings");
            alertDialog.setMessage(permissionMsg);

            alertDialog.setPositiveButton(
                "Settings",
                (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    isFromSetting = true;
                    mContext.startActivity(intent);
                }
            );
            alertDialog.setNegativeButton(
                "Cancel",
                (dialog, which) -> dialog.cancel()
            );
            alertDialog.show();
        } catch(Exception e) {
            Log.e(TAG, "Failed to show settings alert", e);
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(
        		mContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
                mContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions((Activity) mContext, new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        }, 101);
    }
}
