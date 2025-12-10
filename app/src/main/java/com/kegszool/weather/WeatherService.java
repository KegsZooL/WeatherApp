package com.kegszool.weather;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

public class WeatherService {

    private static final String TAG = "WeatherTask";
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_FORECAST_DAYS = 4;

    private static final DecimalFormat WIND_SPEED_FORMAT =
            new DecimalFormat("0.0");

    private static final SimpleDateFormat INPUT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final SimpleDateFormat FALLBACK_INPUT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("EEE", Locale.getDefault());

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onWeatherLoaded(WeatherData data);
        void onError(String message);
    }

    private final Callback callback;
    private Future<?> runningTask;

    public WeatherService(Callback callback) {
        this.callback = callback;
    }

    public void execute(String endpoint) {
        if (TextUtils.isEmpty(endpoint)) {
            deliverError("Invalid request");
            return;
        }
        cancel();
        runningTask = EXECUTOR.submit(() -> {
            WeatherData.Result result = load(endpoint);
            MAIN_HANDLER.post(() -> deliverResult(result));
        });
    }

    public void cancel() {
        if (runningTask != null) {
            runningTask.cancel(true);
            runningTask = null;
        }
    }

    private WeatherData.Result load(String endpoint) {

        HttpURLConnection connection = null;
        InputStream stream = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept-Encoding", "gzip");

            int responseCode = connection.getResponseCode();
            stream = responseCode >= HttpURLConnection.HTTP_OK &&
                    responseCode < HttpURLConnection.HTTP_MULT_CHOICE
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (stream == null) {
                return WeatherData.Result.error("No response from server");
            }
            stream = maybeUnzip(connection, stream);
            reader = new BufferedReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8), IO_BUFFER_SIZE);

            StringBuilder payloadBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                payloadBuilder.append(line);
            }

            String payload = payloadBuilder.toString();
            if (responseCode >= HttpURLConnection.HTTP_OK &&
                    responseCode < HttpURLConnection.HTTP_MULT_CHOICE
            ) {
                return WeatherData.Result.success(parseWeather(payload));
            } else {
                return WeatherData.Result.error(parseErrorMessage(payload));
            }
        } catch (IOException | JSONException e) {
            var msg = "Failed to load weather data";
            Log.e(TAG, msg, e);
            return WeatherData.Result.error(e.getMessage() != null
                    ? e.getMessage()
                    : msg);
        } finally {
            closeQuietly(reader);
            closeQuietly(stream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void deliverResult(WeatherData.Result result) {
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

    private void deliverError(String message) {
        if (callback != null) {
            callback.onError(message);
        }
    }

    private InputStream maybeUnzip(
        HttpURLConnection connection,
        InputStream source
    ) throws IOException {
        String encoding = connection != null
                ? connection.getContentEncoding()
                : null;

        if ("gzip".equalsIgnoreCase(encoding)) {
            return new GZIPInputStream(source, IO_BUFFER_SIZE);
        }
        return source;
    }

    private void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                 Log.e(TAG, "Failed to close InputStream");
            }
        }
    }

    private void closeQuietly(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                Log.e(TAG, "Failed to close BufferedReader");
            }
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
            description = formatDescription(
                    weatherDetails.optString("description", ""));
            conditionId = weatherDetails.optInt("id", 0);
        }

        String temperature = "";
        String humidity = "";
        String pressure = "";
        boolean isRussian = isRussianLocale();

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
                pressure = mainObject.optInt("pressure") + (isRussian ? " гПа" : " hPa");
            }
        }

        JSONObject windObject = firstForecast.optJSONObject("wind");
        String windSpeed = "";
        if (windObject != null) {
            double windMetersPerSecond = windObject.optDouble("speed", 0d);
            double windKilometersPerHour = windMetersPerSecond * 3.6d;
            windSpeed = WIND_SPEED_FORMAT.format(windKilometersPerHour) + (isRussian ? " км/ч" : " km/h");
        }

        int visibilityValue = firstForecast.optInt("visibility", 0);
        String visibility = formatVisibility(visibilityValue, isRussian);
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
            return new ArrayList<>();
        }

        Map<String, JSONObject> dayToForecast = new LinkedHashMap<>();
        for (int i = 0; i < forecastList.length(); i++) {

            JSONObject item = forecastList.optJSONObject(i);
            if (item == null) { continue; }

            String dtTxt = item.optString("dt_txt", "");
            if (dtTxt.length() < 10) {
                continue;
            }

            String dateKey = dtTxt.substring(0, 10);
            JSONObject existing = dayToForecast.get(dateKey);
            if (existing == null || dtTxt.contains("12:00:00")) {
                dayToForecast.put(dateKey, item);
            }
        }

        List<WeatherData.DailyForecast> results = new ArrayList<>();
        for (Map.Entry<String, JSONObject> entry : dayToForecast.entrySet()) {
            if (results.size() >= MAX_FORECAST_DAYS) {
                break;
            }
            JSONObject forecastObject = entry.getValue();
            if (forecastObject == null) {
                continue;
            }
            String dayLabel = formatDayLabel(
                    forecastObject.optString("dt_txt", ""),
                    entry.getKey()
            );

            JSONObject mainObject = forecastObject.optJSONObject("main");
            String temperature = "";
            if (mainObject != null) {
                double tempValue = mainObject.optDouble(
                        "temp", Double.NaN);
                if (!Double.isNaN(tempValue)) {
                    temperature = Math.round(tempValue) + "°";
                }
            }

            JSONArray dayWeatherArray = forecastObject.optJSONArray("weather");
            int dayConditionId = 0;
            String dayDescription = "";
            if (dayWeatherArray != null && dayWeatherArray.length() > 0) {
                JSONObject weatherDetails = dayWeatherArray.optJSONObject(0);
                if (weatherDetails != null) {
                    dayConditionId = weatherDetails.optInt("id", 0);
                    dayDescription = formatDescription(
                            weatherDetails.optString("description", ""));
                }
            }
            results.add(new WeatherData.DailyForecast(dayLabel, temperature, dayConditionId, dayDescription));
        }
        return results;
    }

    private String formatDayLabel(
        String dateTime,
        String fallbackDate
    ) {
        Date parsedDate = null;
        if (!TextUtils.isEmpty(dateTime)) {
            try {
                parsedDate = WeatherService.INPUT_FORMAT.parse(dateTime);
            } catch (ParseException ignored) {
            }
        }

        if (parsedDate == null && !TextUtils.isEmpty(fallbackDate)) {
            try {
                parsedDate = WeatherService.FALLBACK_INPUT_FORMAT.parse(fallbackDate);
            } catch (ParseException ignored) {
            }
        }

        if (parsedDate == null) {
            return fallbackDate != null ? fallbackDate : "";
        }
        return WeatherService.DAY_FORMAT.format(parsedDate);
    }

    private String parseLocation(JSONObject cityObject) {

        if (cityObject == null) { return ""; }

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

    private String formatVisibility(int visibilityMeters, boolean isRussian) {
        int visibilityKilometers = Math.max(0, visibilityMeters / 1000);
        	return visibilityKilometers + (isRussian ? " км" : " km");
    }

    private String formatDescription(String rawDescription) {

        if (rawDescription == null || rawDescription.isEmpty()) {
            return "";
        }
        String firstLetter = rawDescription
                .substring(0, 1)
                .toUpperCase(Locale.getDefault());

        String remaining = rawDescription.length() > 1
                ? rawDescription.substring(1)
                : "";

        return firstLetter + remaining;
    }

    private String parseErrorMessage(String payload) {
        if (TextUtils.isEmpty(payload)) {
            return "Unable to load weather data";
        }
        try {
            JSONObject errorObject = new JSONObject(payload);
            if (errorObject.has("message")) {
                return errorObject.optString("message", "Unable to load weather data");
            }
        } catch (JSONException ignored) {}
        return "Unable to load weather data";
    }

    private boolean isRussianLocale() {
        return "ru".equalsIgnoreCase(Locale.getDefault().getLanguage());
    }
}
