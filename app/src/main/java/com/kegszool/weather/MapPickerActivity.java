package com.kegszool.weather;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

public class MapPickerActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_LABEL = "extra_label";

    private GoogleMap googleMap;
    private Marker currentMarker;
    private TextView selectionInfo;
    private View overlayContainer;
    private double selectedLat = Double.NaN;
    private double selectedLng = Double.NaN;
    private String selectedLabel = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        selectionInfo = findViewById(R.id.selectionInfo);
        overlayContainer = findViewById(R.id.mapOverlayContainer);
        Button confirmButton = findViewById(R.id.confirmSelection);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, R.string.map_no_selection, Toast.LENGTH_SHORT).show();
        }

        confirmButton.setOnClickListener(v -> returnSelection());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapClickListener(this);
        applyMapPaddingWhenReady();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (latLng == null) {
            return;
        }
        selectedLat = latLng.latitude;
        selectedLng = latLng.longitude;
        selectedLabel = formatLatLng(selectedLat, selectedLng);

        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.map_marker_title))
                    .snippet(selectedLabel));
        } else {
            currentMarker.setPosition(latLng);
            currentMarker.setSnippet(selectedLabel);
        }
        if (currentMarker != null) {
            currentMarker.showInfoWindow();
        }
        selectionInfo.setText(getString(R.string.map_selection_label, selectedLabel));
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void applyMapPaddingWhenReady() {
        if (overlayContainer == null) {
            return;
        }
        overlayContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (overlayContainer.getHeight() > 0) {
                    applyMapPadding();
                    overlayContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    private void applyMapPadding() {
        if (googleMap == null || overlayContainer == null) {
            return;
        }
        googleMap.setPadding(0, 0, 0, overlayContainer.getHeight());
    }

    private String formatLatLng(double latitude, double longitude) {
        return String.format(Locale.getDefault(), getString(R.string.map_selection_fallback), latitude, longitude);
    }

    private void returnSelection() {
        if (Double.isNaN(selectedLat) || Double.isNaN(selectedLng)) {
            Toast.makeText(this, R.string.map_no_selection, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_LATITUDE, selectedLat);
        result.putExtra(EXTRA_LONGITUDE, selectedLng);
        result.putExtra(EXTRA_LABEL, selectedLabel);
        setResult(RESULT_OK, result);
        finish();
    }
}
