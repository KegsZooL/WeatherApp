package com.kegszool.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class LastWeatherStorage {

    private static final String PREF_NAME = "last_weather_storage";
    private static final String KEY_CITY = "city";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_CONDITION_ID = "conditionId";
    private static final String KEY_FORECASTS = "forecasts";

    private LastWeatherStorage() {
    }

    static void save(Context context, WeatherData data) {
        if (context == null || data == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CITY, data.location())
                .putString(KEY_TEMPERATURE, data.temperature())
                .putInt(KEY_CONDITION_ID, data.conditionId())
                .putString(KEY_FORECASTS, serializeForecasts(data))
                .apply();
    }

    static WeatherSnapshot read(Context context) {
        if (context == null) {
            return WeatherSnapshot.empty();
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String city = prefs.getString(KEY_CITY, "");
        String temperature = prefs.getString(KEY_TEMPERATURE, "");
        int conditionId = prefs.getInt(KEY_CONDITION_ID, 0);
        String forecastsRaw = prefs.getString(KEY_FORECASTS, "");
        return new WeatherSnapshot(city, temperature, conditionId, parseForecasts(forecastsRaw));
    }

    static final class WeatherSnapshot {
        final String city;
        final String temperature;
        final int conditionId;
        final ForecastSnapshot[] forecasts;

        WeatherSnapshot(String city, String temperature, int conditionId, ForecastSnapshot[] forecasts) {
            this.city = city != null ? city : "";
            this.temperature = temperature != null ? temperature : "";
            this.conditionId = conditionId;
            this.forecasts = forecasts != null ? forecasts : new ForecastSnapshot[0];
        }

        static WeatherSnapshot empty() {
            return new WeatherSnapshot("", "", 0, new ForecastSnapshot[0]);
        }

        boolean hasData() {
            return !TextUtils.isEmpty(city) || !TextUtils.isEmpty(temperature);
        }
    }

    static final class ForecastSnapshot {
        final String dayLabel;
        final String temperature;
        final int conditionId;

        ForecastSnapshot(String dayLabel, String temperature, int conditionId) {
            this.dayLabel = dayLabel != null ? dayLabel : "";
            this.temperature = temperature != null ? temperature : "";
            this.conditionId = conditionId;
        }
    }

    private static String serializeForecasts(WeatherData data) {
        if (data == null || data.dailyForecasts() == null) {
            return "";
        }
        JSONArray array = new JSONArray();
        final int max = Math.min(4, data.dailyForecasts().size());
        for (int i = 0; i < max; i++) {
            WeatherData.DailyForecast forecast = data.dailyForecasts().get(i);
            if (forecast == null) {
                continue;
            }
            JSONObject obj = new JSONObject();
            try {
                obj.put("day", forecast.dayLabel());
                obj.put("temp", forecast.temperature());
                obj.put("cond", forecast.conditionId());
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        return array.toString();
    }

    private static ForecastSnapshot[] parseForecasts(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return new ForecastSnapshot[0];
        }
        try {
            JSONArray array = new JSONArray(raw);
            int count = Math.min(4, array.length());
            ForecastSnapshot[] snapshots = new ForecastSnapshot[count];
            for (int i = 0; i < count; i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    snapshots[i] = new ForecastSnapshot("", "", 0);
                    continue;
                }
                String day = obj.optString("day", "");
                String temp = obj.optString("temp", "");
                int cond = obj.optInt("cond", 0);
                snapshots[i] = new ForecastSnapshot(day, temp, cond);
            }
            return snapshots;
        } catch (JSONException e) {
            return new ForecastSnapshot[0];
        }
    }
}
