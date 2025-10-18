package com.kegszool.weather;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Weather extends AsyncTask<String, Void, Weather.Result> {

    private static final String TAG = "WeatherTask";
    private final WeakReference<Callback> callbackRef;

    public interface Callback {
        void onWeatherLoaded(WeatherData data);
        void onError(String message);
    }

    public Weather(Callback callback) {
        this.callbackRef = new WeakReference<>(callback);
    }

    @Override
    protected Result doInBackground(String... urls) {

        if (urls == null ||
            urls.length == 0 ||
            urls[0] == null ||
            urls[0].isEmpty()
        ) {
            return Result.error("Missing endpoint");
        }

        HttpURLConnection connection = null;
        InputStream stream = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urls[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            stream = responseCode >= HttpURLConnection.HTTP_OK &&
                    responseCode < HttpURLConnection.HTTP_MULT_CHOICE
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (stream == null) {
                return Result.error("No response from server");
            }

            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder payloadBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                payloadBuilder.append(line);
            }

            String payload = payloadBuilder.toString();
            if (responseCode >= HttpURLConnection.HTTP_OK &&
                responseCode < HttpURLConnection.HTTP_MULT_CHOICE
            ) {
                WeatherData data = parseWeather(payload);
                return Result.success(data);
            } else {
                return Result.error(parseErrorMessage(payload));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to load weather data", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "Failed to load weather data");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(Result result) {

        Callback callback = callbackRef.get();
        if (callback == null) {
            return;
        }

        if (result == null) {
            callback.onError("No weather data");
        } else if (result.isSuccess()) {
            callback.onWeatherLoaded(result.getData());
        } else {
            callback.onError(result.getError());
        }
    }

    @Override
    protected void onCancelled(Result result) {
        notifyCancellation(result != null ? result.getError() : "Request cancelled");
    }

    @Override
    protected void onCancelled() {
        notifyCancellation("Request cancelled");
    }

    private void notifyCancellation(String message) {
        Callback callback = callbackRef.get();
        if (callback != null) {
            callback.onError(message);
        }
    }

    private WeatherData parseWeather(String payload) throws JSONException {

        JSONObject response = new JSONObject(payload);
        JSONObject cityObject = response.optJSONObject("city");
        String location = parseLocation(cityObject);

        JSONArray forecastList = response.optJSONArray("list");
        if (forecastList == null || forecastList.length() == 0) {
            throw new JSONException("Empty forecast data");
        }

        JSONObject firstForecast = forecastList.getJSONObject(0);
        JSONArray weatherArray = firstForecast.optJSONArray("weather");
        String description = "";
        int conditionId = 0;
        if (weatherArray != null && weatherArray.length() > 0) {
            JSONObject weatherDetails = weatherArray.getJSONObject(0);
            description = formatDescription(weatherDetails.optString("description", ""));
            conditionId = weatherDetails.optInt("id", 0);
        }

        String temperature = "";
        String humidity = "";
        String pressure = "";

        JSONObject mainObject = firstForecast.optJSONObject("main");
        if (mainObject != null) {
            double tempValue = mainObject.optDouble("temp", Double.NaN);
            if (!Double.isNaN(tempValue)) {
                temperature = String.valueOf(Math.round(tempValue));
            }
            if (mainObject.has("humidity")) {
                humidity = mainObject.optInt("humidity") + " %";
            }
            if (mainObject.has("pressure")) {
                pressure = mainObject.optInt("pressure") + " hPa";
            }
        }

        JSONObject windObject = firstForecast.optJSONObject("wind");
        String windSpeed = "";
        if (windObject != null) {
            double windMetersPerSecond = windObject.optDouble("speed", 0d);
            double windKilometersPerHour = windMetersPerSecond * 3.6d;
            windSpeed = new DecimalFormat("0.0").format(windKilometersPerHour) + " km/h";
        }

        int visibilityValue = firstForecast.optInt("visibility", 0);
        String visibility = formatVisibility(visibilityValue);

        List<WeatherData.DailyForecast> dailyForecasts = buildDailyForecasts(forecastList);

        return new WeatherData(
                location,
                description,
                conditionId,
                temperature,
                humidity,
                pressure,
                windSpeed,
                visibility,
                dailyForecasts
        );
    }

    private List<WeatherData.DailyForecast> buildDailyForecasts(JSONArray forecastList) {
        if (forecastList == null || forecastList.length() == 0) {
            return Collections.emptyList();
        }

        final int maxDays = 4;
        Map<String, JSONObject> dayToForecast = new LinkedHashMap<>();

        for (int i = 0; i < forecastList.length(); i++) {
            JSONObject item = forecastList.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String dtTxt = item.optString("dt_txt", "");
            if (dtTxt.length() < 10) {
                continue;
            }
            String dateKey = dtTxt.substring(0, 10);
            JSONObject existing = dayToForecast.get(dateKey);
            if (existing == null) {
                dayToForecast.put(dateKey, item);
                continue;
            }
            boolean newIsMidday = dtTxt.contains("12:00:00");
            if (!newIsMidday) {
                continue;
            }
            String existingDt = existing.optString("dt_txt", "");
            if (!existingDt.contains("12:00:00")) {
                dayToForecast.put(dateKey, item);
            }
        }

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        SimpleDateFormat fallbackInputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        List<WeatherData.DailyForecast> results = new ArrayList<>();

        for (Map.Entry<String, JSONObject> entry : dayToForecast.entrySet()) {
            if (results.size() >= maxDays) {
                break;
            }

            JSONObject forecastObject = entry.getValue();
            if (forecastObject == null) {
                continue;
            }

            String dayLabel = formatDayLabel(forecastObject.optString("dt_txt", ""),
                    entry.getKey(), inputFormat, fallbackInputFormat, dayFormat);

            JSONObject mainObject = forecastObject.optJSONObject("main");
            String temperature = "";
            if (mainObject != null) {
                double tempValue = mainObject.optDouble("temp", Double.NaN);
                if (!Double.isNaN(tempValue)) {
                    temperature = Math.round(tempValue) + "\u00B0";
                }
            }

            JSONArray dayWeatherArray = forecastObject.optJSONArray("weather");
            int dayConditionId = 0;
            String dayDescription = "";
            if (dayWeatherArray != null && dayWeatherArray.length() > 0) {
                JSONObject weatherDetails = dayWeatherArray.optJSONObject(0);
                if (weatherDetails != null) {
                    dayConditionId = weatherDetails.optInt("id", 0);
                    dayDescription = formatDescription(weatherDetails.optString("description", ""));
                }
            }

            if (temperature.isEmpty() && dayDescription.isEmpty()) {
                continue;
            }

            results.add(new WeatherData.DailyForecast(dayLabel, temperature, dayConditionId, dayDescription));
        }

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(results);
    }

    private String formatDayLabel(
            String dateTime,
            String fallbackDate,
            SimpleDateFormat inputFormat,
            SimpleDateFormat fallbackInputFormat,
            SimpleDateFormat outputFormat
    ) {
        Date parsedDate = null;
        if (dateTime != null && !dateTime.isEmpty()) {
            try {
                parsedDate = inputFormat.parse(dateTime);
            } catch (ParseException ignored) {
            }
        }

        if (parsedDate == null && fallbackDate != null && !fallbackDate.isEmpty()) {
            try {
                parsedDate = fallbackInputFormat.parse(fallbackDate);
            } catch (ParseException ignored) {
            }
        }

        if (parsedDate == null) {
            return fallbackDate != null ? fallbackDate : "";
        }

        return outputFormat.format(parsedDate);
    }

    private String parseLocation(JSONObject cityObject) {
        if (cityObject == null) {
            return "";
        }
        String name = cityObject.optString("name", "");
        String country = cityObject.optString("country", "");
        if (!name.isEmpty() && !country.isEmpty()) {
            return name + ", " + country;
        } else if (!name.isEmpty()) {
            return name;
        } else if (!country.isEmpty()) {
            return country;
        }
        return "";
    }

    private String formatVisibility(int visibilityMeters) {
        int visibilityKilometers = Math.max(0, visibilityMeters / 1000);
        return visibilityKilometers + " km";
    }

    private String formatDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isEmpty()) {
            return "";
        }
        String firstLetter = rawDescription.substring(0, 1).toUpperCase(Locale.getDefault());
        String remaining = rawDescription.length() > 1 ? rawDescription.substring(1) : "";
        return firstLetter + remaining;
    }

    private String parseErrorMessage(String payload) {
        if (payload == null || payload.isEmpty()) {
            return "Unable to load weather data";
        }
        try {
            JSONObject errorObject = new JSONObject(payload);
            if (errorObject.has("message")) {
                return errorObject.optString("message", "Unable to load weather data");
            }
        } catch (JSONException ignored) {
        }
        return "Unable to load weather data";
    }

    static final class WeatherData {
        private final String location;
        private final String description;
        private final int conditionId;
        private final String temperature;
        private final String humidity;
        private final String pressure;
        private final String windSpeed;
        private final String visibility;
        private final List<DailyForecast> dailyForecasts;

        WeatherData(
            String location,
            String description,
            int conditionId,
            String temperature,
            String humidity,
            String pressure,
            String windSpeed,
            String visibility,
            List<DailyForecast> dailyForecasts
        ) {
            this.location = location;
            this.description = description;
            this.conditionId = conditionId;
            this.temperature = temperature;
            this.humidity = humidity;
            this.pressure = pressure;
            this.windSpeed = windSpeed;
            this.visibility = visibility;
            if (dailyForecasts == null || dailyForecasts.isEmpty()) {
                this.dailyForecasts = Collections.emptyList();
            } else {
                this.dailyForecasts = Collections.unmodifiableList(new ArrayList<>(dailyForecasts));
            }
        }

        String getLocation() {
            return location;
        }

        String getDescription() {
            return description;
        }

        int getConditionId() {
            return conditionId;
        }

        String getTemperature() {
            return temperature;
        }

        String getHumidity() {
            return humidity;
        }

        String getPressure() {
            return pressure;
        }

        String getWindSpeed() {
            return windSpeed;
        }

        String getVisibility() {
            return visibility;
        }

        List<DailyForecast> getDailyForecasts() {
            return dailyForecasts;
        }

        static final class DailyForecast {
            private final String dayLabel;
            private final String temperature;
            private final int conditionId;
            private final String description;

            DailyForecast(String dayLabel, String temperature, int conditionId, String description) {
                this.dayLabel = dayLabel != null ? dayLabel : "";
                this.temperature = temperature != null ? temperature : "";
                this.conditionId = conditionId;
                this.description = description != null ? description : "";
            }

            String getDayLabel() {
                return dayLabel;
            }

            String getTemperature() {
                return temperature;
            }

            int getConditionId() {
                return conditionId;
            }

            String getDescription() {
                return description;
            }
        }
    }

    static final class Result {

        private final WeatherData data;
        private final String error;

        private Result(WeatherData data, String error) {
            this.data = data;
            this.error = error;
        }

        static Result success(WeatherData data) {
            return new Result(data, null);
        }

        static Result error(String message) {
            return new Result(null, message);
        }

        boolean isSuccess() {
            return data != null;
        }

        WeatherData getData() {
            return data;
        }

        String getError() {
            return error;
        }
    }
}
