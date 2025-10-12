package com.kegszool.weather;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Weather.Callback {

    private static final String TAG = "MainActivity";
    private static final String DEFAULT_CITY = "Moscow";
    private static final String NOT_FOUND_MSG_FALLBACK = "city not found";
    private static final long VIBRATION_DURATION_MS = 15L;

    private GpsTracker gpsTracker;

    private ConstraintLayout rootLayout;
    private TextView locationView;
    private TextView descriptionView;
    private TextView humidityView;
    private TextView pressureView;
    private TextView mainTempView;
    private TextView windSpeedView;
    private TextView visibilityView;
    private EditText searchView;

    private Weather currentTask;
    private String lastSearchedCity = DEFAULT_CITY;
    private String apiKey;
    private int currentBackgroundResId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        rootLayout = findViewById(R.id.main);
        if (rootLayout != null) {
            currentBackgroundResId = R.drawable.main_bg;
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }
        apiKey = BuildConfig.OPEN_WEATHER_API_KEY;

        bindViews();
        setupSearch();
        fetchWeatherForCurrentLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (GpsTracker.isFromSetting) {
            GpsTracker.isFromSetting = false;
            fetchWeatherForCurrentLocation();
        }
    }

    @Override
    protected void onDestroy() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        if (gpsTracker != null) {
            gpsTracker.stopUsingGPS();
        }
        super.onDestroy();
    }

    @Override
    public void onWeatherLoaded(Weather.WeatherData data) {
        if (data == null) {
            onError("No weather data");
            return;
        }
        if (!TextUtils.isEmpty(data.getLocation())) {
            locationView.setText(data.getLocation());
        }
        descriptionView.setText(data.getDescription());
        humidityView.setText(data.getHumidity());
        pressureView.setText(data.getPressure());
        mainTempView.setText(data.getTemperature());
        windSpeedView.setText(data.getWindSpeed());
        visibilityView.setText(data.getVisibility());
        updateBackground(data);
    }

    @Override
    public void onError(String message) {
        String displayMessage = TextUtils.isEmpty(message) ? "UNABLE TO LOAD WEATHER DATA" : message;
        if (displayMessage.equals(NOT_FOUND_MSG_FALLBACK)) {
            displayMessage = NOT_FOUND_MSG_FALLBACK.toUpperCase();
            vibrate();
        }
        Toast.makeText(getApplicationContext(), displayMessage, Toast.LENGTH_SHORT).show();
        Log.w(TAG, displayMessage);
    }

    private void bindViews() {
        searchView = findViewById(R.id.search);
        locationView = findViewById(R.id.city);
        descriptionView = findViewById(R.id.description);
        mainTempView = findViewById(R.id.tempMain);
        humidityView = findViewById(R.id.humidity);
        pressureView = findViewById(R.id.pressure);
        windSpeedView = findViewById(R.id.windSpeed);
        visibilityView = findViewById(R.id.visibility);

        searchView.clearFocus();
    }

    private void setupSearch() {
        searchView.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleCitySearch();
                return true;
            }
            return false;
        });
    }

    private void handleCitySearch() {
        String inputCity = searchView.getText().toString().trim();
        if (inputCity.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter a city!", Toast.LENGTH_SHORT).show();
            vibrate();
            if (!TextUtils.isEmpty(lastSearchedCity)) {
                requestWeatherByCity(lastSearchedCity);
            }
            return;
        }
        lastSearchedCity = inputCity;
        requestWeatherByCity(inputCity);
    }

    private void fetchWeatherForCurrentLocation() {
        gpsTracker = new GpsTracker(this);
        if (gpsTracker.canGetLocation()) {
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();

            if (latitude == 0.0 && longitude == 0.0) {
                Location lastKnownLocation = gpsTracker.getLocation();
                if (lastKnownLocation != null) {
                    latitude = lastKnownLocation.getLatitude();
                    longitude = lastKnownLocation.getLongitude();
                }
            }
            requestWeatherByCoordinates(latitude, longitude);
        } else {
            gpsTracker.showSettingsAlert();
        }
    }

    private void requestWeatherByCity(String cityName) {
        if (TextUtils.isEmpty(apiKey)) {
            notifyMissingApiKey();
            return;
        }
        try {
            String encodedCity = URLEncoder.encode(cityName, "UTF-8");
            String endpoint = String.format(
                    Locale.US,
                    "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric",
                    encodedCity,
                    apiKey
            );
            executeWeatherRequest(endpoint);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to encode city name", e);
            onError("Unable to search for city");
        }
    }

    private void requestWeatherByCoordinates(double latitude, double longitude) {
        if (TextUtils.isEmpty(apiKey)) {
            notifyMissingApiKey();
            return;
        }
        String endpoint = String.format(
                Locale.US,
                "https://api.openweathermap.org/data/2.5/forecast?lat=%.6f&lon=%.6f&appid=%s&units=metric",
                latitude,
                longitude,
                apiKey
        );
        executeWeatherRequest(endpoint);
    }

    private void executeWeatherRequest(String endpoint) {
        if (TextUtils.isEmpty(endpoint)) {
            onError("Invalid request");
            return;
        }
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentTask = new Weather(this);
        currentTask.execute(endpoint);
    }

    private void updateBackground(Weather.WeatherData data) {
        if (rootLayout == null) {
            return;
        }
        int resolvedResId = resolveBackgroundResource(data.getConditionId());
        if (resolvedResId == 0 || resolvedResId == currentBackgroundResId) {
            return;
        }
        rootLayout.setBackgroundResource(resolvedResId);
        currentBackgroundResId = resolvedResId;
        startBackgroundAnimation();
    }

    private int resolveBackgroundResource(int conditionId) {
        if (conditionId >= 200 && conditionId < 300) {
            return R.drawable.anim_bg_thunderstorm;
        } else if (conditionId >= 300 && conditionId < 400) {
            return R.drawable.anim_bg_drizzle;
        } else if (conditionId >= 500 && conditionId < 600) {
            return R.drawable.anim_bg_rain;
        } else if (conditionId >= 600 && conditionId < 700) {
            return R.drawable.anim_bg_snow;
        } else if (conditionId >= 700 && conditionId < 800) {
            return R.drawable.anim_bg_mist;
        } else if (conditionId == 800) {
            return R.drawable.anim_bg_clear;
        } else if (conditionId > 800 && conditionId < 900) {
            return R.drawable.anim_bg_clouds;
        }
        return R.drawable.main_bg;
    }

    private void startBackgroundAnimation() {
        Drawable background = rootLayout != null ? rootLayout.getBackground() : null;
        if (!(background instanceof AnimationDrawable)) {
            return;
        }
        AnimationDrawable animationDrawable = (AnimationDrawable) background;
        animationDrawable.setEnterFadeDuration(40);
        animationDrawable.setExitFadeDuration(4200);
        animationDrawable.stop();
        animationDrawable.setVisible(true, true);
        animationDrawable.start();
    }

    private void notifyMissingApiKey() {
        onError("API key is missing. Please check configuration.");
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(VIBRATION_DURATION_MS);
        }
    }
}
