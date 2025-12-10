package com.kegszool.weather;

import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public final class WeatherRouter {

    private static final String TAG = "WeatherRouter";

    private static final String ENDPOINT_FORMAT_FOR_CITY =
            "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric%s";

    private static final String ENDPOINT_FORMAT_FOR_COORDINATES =
            "https://api.openweathermap.org/data/2.5/forecast?lat=%.6f&lon=%.6f&appid=%s&units=metric%s";

    private final String apiKey;
    private final WeatherService service;

    public WeatherRouter(String apiKey, WeatherService.Callback callback) {
        this.apiKey = apiKey;
        this.service = new WeatherService(callback);
    }

    public void requestWeatherByCity(String cityName) {
        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(cityName)) {
            return;
        }
        try {
            String encodedCity = URLEncoder.encode(cityName, "UTF-8");
            String endpoint = String.format(
                    Locale.US,
                    ENDPOINT_FORMAT_FOR_CITY,
                    encodedCity,
                    apiKey,
                    getApiLanguageQuery()
            );
            service.execute(endpoint);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to encode city name", e);
        }
    }

    public void requestWeatherByCoordinates(double latitude, double longitude) {
        if (TextUtils.isEmpty(apiKey)) {
            return;
        }
        String endpoint = String.format(
                Locale.US,
                ENDPOINT_FORMAT_FOR_COORDINATES,
                latitude,
                longitude,
                apiKey,
                getApiLanguageQuery()
        );
        service.execute(endpoint);
    }

    public void cancel() {
        service.cancel();
    }

    private String getApiLanguageQuery() {
        String language = Locale.getDefault().getLanguage();
        if ("ru".equalsIgnoreCase(language)) {
            return "&lang=ru";
        }
        return "";
    }
}
