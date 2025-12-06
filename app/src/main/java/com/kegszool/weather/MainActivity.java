package com.kegszool.weather;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Weather.Callback {

    private static final String TAG = "MainActivity";
    private static final String DEFAULT_CITY = "Moscow";
    private static final String NOT_FOUND_MSG_FALLBACK = "city not found";
    private static final long VIBRATION_DURATION_MS = 15L;
    private static final String CITY_SUGGESTION_SEPARATOR = " / ";
    private static final Map<String, String> POPULAR_CITIES = createPopularCities();

    private GpsTracker gpsTracker;

    private ConstraintLayout rootLayout;
    private TextView locationView;
    private TextView descriptionView;
    private TextView humidityView;
    private TextView pressureView;
    private TextView mainTempView;
    private TextView windSpeedView;
    private TextView visibilityView;
    private AutoCompleteTextView searchView;
    private ForecastViewHolder[] forecastHolders;
    private ArrayAdapter<String> citySuggestionsAdapter;

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
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
        renderDailyForecasts(data.getDailyForecasts());
        updateBackground(data);
        WeatherNotificationManager.showWeatherNotification(this, data);
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
        WeatherNotificationManager.cancel(this);
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

        TextView day1Label = findViewById(R.id.day1);
        ImageView day1Icon = findViewById(R.id.day1img);
        TextView day1Temp = findViewById(R.id.day1temp);

        TextView day2Label = findViewById(R.id.day2);
        ImageView day2Icon = findViewById(R.id.day2img);
        TextView day2Temp = findViewById(R.id.day2temp);

        TextView day3Label = findViewById(R.id.day3);
        ImageView day3Icon = findViewById(R.id.day3img);
        TextView day3Temp = findViewById(R.id.day3temp);

        TextView day4Label = findViewById(R.id.day4);
        ImageView day4Icon = findViewById(R.id.day4img);
        TextView day4Temp = findViewById(R.id.day4temp);

        forecastHolders = new ForecastViewHolder[]{
                new ForecastViewHolder(getParentOrSelf(day1Label), day1Label, day1Icon, day1Temp),
                new ForecastViewHolder(getParentOrSelf(day2Label), day2Label, day2Icon, day2Temp),
                new ForecastViewHolder(getParentOrSelf(day3Label), day3Label, day3Icon, day3Temp),
                new ForecastViewHolder(getParentOrSelf(day4Label), day4Label, day4Icon, day4Temp)
        };

        renderDailyForecasts(null);

        searchView.clearFocus();
    }

    private void setupSearch() {
        setupCitySuggestions();
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
        String normalizedCity = normalizeCityInput(inputCity);
        lastSearchedCity = normalizedCity;
        searchView.dismissDropDown();
        requestWeatherByCity(normalizedCity);
    }

    private void setupCitySuggestions() {
        citySuggestionsAdapter = new ArrayAdapter<>(this, R.layout.item_city_suggestion, R.id.citySuggestionText, new ArrayList<>());
        searchView.setAdapter(citySuggestionsAdapter);
        searchView.setDropDownBackgroundResource(R.drawable.autocomplete_dropdown_bg);
        searchView.setThreshold(0);
        searchView.setOnItemClickListener((parent, view, position, id) -> {
            if (parent == null || position < 0 || position >= parent.getCount()) {
                return;
            }
            Object item = parent.getItemAtPosition(position);
            String suggestion = item != null ? item.toString() : null;
            if (suggestion == null) {
                return;
            }
            String englishName = extractEnglishCityName(suggestion);
            if (!TextUtils.isEmpty(englishName)) {
                searchView.setText(englishName);
                searchView.setSelection(englishName.length());
            }
            handleCitySearch();
        });
        searchView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && citySuggestionsAdapter.getCount() > 0) {
                searchView.showDropDown();
            }
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterCitySuggestions(s != null ? s.toString() : "");
            }
        });
        filterCitySuggestions("");
    }

    private void filterCitySuggestions(String query) {
        if (citySuggestionsAdapter == null) {
            return;
        }
        String normalized = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        citySuggestionsAdapter.clear();
        for (Map.Entry<String, String> entry : POPULAR_CITIES.entrySet()) {
            String englishName = entry.getKey();
            String russianName = entry.getValue();
            if (TextUtils.isEmpty(normalized)
                    || startsWithLocalized(englishName, normalized)
                    || startsWithLocalized(russianName, normalized)) {
                citySuggestionsAdapter.add(buildSuggestionLabel(englishName, russianName));
            }
        }
        citySuggestionsAdapter.notifyDataSetChanged();
        if (!TextUtils.isEmpty(query) && citySuggestionsAdapter.getCount() > 0 && searchView.hasFocus()) {
            searchView.showDropDown();
        }
    }

    private String normalizeCityInput(String cityInput) {
        if (TextUtils.isEmpty(cityInput)) {
            return cityInput;
        }
        String englishFromLabel = extractEnglishCityName(cityInput);
        if (!TextUtils.isEmpty(englishFromLabel) && POPULAR_CITIES.containsKey(englishFromLabel)) {
            return englishFromLabel;
        }
        for (Map.Entry<String, String> entry : POPULAR_CITIES.entrySet()) {
            if (cityInput.equalsIgnoreCase(entry.getKey()) || cityInput.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }
        return !TextUtils.isEmpty(englishFromLabel) ? englishFromLabel : cityInput;
    }

    private String extractEnglishCityName(String suggestion) {
        if (TextUtils.isEmpty(suggestion)) {
            return suggestion;
        }
        int separatorIndex = suggestion.indexOf(CITY_SUGGESTION_SEPARATOR);
        if (separatorIndex > 0) {
            return suggestion.substring(0, separatorIndex).trim();
        }
        return suggestion.trim();
    }

    private String buildSuggestionLabel(String englishName, String russianName) {
        return englishName + CITY_SUGGESTION_SEPARATOR + russianName;
    }

    private boolean startsWithLocalized(String value, String prefix) {
        if (TextUtils.isEmpty(value) || TextUtils.isEmpty(prefix)) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    private static Map<String, String> createPopularCities() {
        Map<String, String> cities = new LinkedHashMap<>();
        cities.put("Moscow", "Москва");
        cities.put("Saint Petersburg", "Санкт-Петербург");
        cities.put("Novosibirsk", "Новосибирск");
        cities.put("Yekaterinburg", "Екатеринбург");
        cities.put("Kazan", "Казань");
        cities.put("Nizhny Novgorod", "Нижний Новгород");
        cities.put("Chelyabinsk", "Челябинск");
        cities.put("Samara", "Самара");
        cities.put("Omsk", "Омск");
        cities.put("Rostov-on-Don", "Ростов-на-Дону");
        cities.put("Ufa", "Уфа");
        cities.put("Krasnoyarsk", "Красноярск");
        cities.put("Perm", "Пермь");
        cities.put("Voronezh", "Воронеж");
        cities.put("Volgograd", "Волгоград");
        cities.put("Krasnodar", "Краснодар");
        cities.put("Sochi", "Сочи");
        cities.put("Kaliningrad", "Калининград");
        cities.put("Vladivostok", "Владивосток");
        cities.put("Murmansk", "Мурманск");
        cities.put("Khabarovsk", "Хабаровск");
        cities.put("Irkutsk", "Иркутск");
        cities.put("Yakutsk", "Якутск");
        cities.put("Astrakhan", "Астрахань");
        cities.put("Saratov", "Саратов");
        cities.put("Petropavlovsk-Kamchatsky", "Петропавловск-Камчатский");
        return Collections.unmodifiableMap(cities);
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
                    "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric%s",
                    encodedCity,
                    apiKey,
                    getApiLanguageQuery()
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
                "https://api.openweathermap.org/data/2.5/forecast?lat=%.6f&lon=%.6f&appid=%s&units=metric%s",
                latitude,
                longitude,
                apiKey,
                getApiLanguageQuery()
        );
        executeWeatherRequest(endpoint);
    }

    private String getApiLanguageQuery() {
        String language = Locale.getDefault().getLanguage();
        if ("ru".equalsIgnoreCase(language)) {
            return "&lang=ru";
        }
        return "";
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

    private void renderDailyForecasts(List<Weather.WeatherData.DailyForecast> forecasts) {
        if (forecastHolders == null || forecastHolders.length == 0) {
            return;
        }

        int itemsCount = forecasts != null ? Math.min(forecasts.size(), forecastHolders.length) : 0;

        for (int i = 0; i < forecastHolders.length; i++) {
            ForecastViewHolder holder = forecastHolders[i];
            if (holder == null) {
                continue;
            }
            if (i < itemsCount) {
                Weather.WeatherData.DailyForecast forecast = forecasts.get(i);
                if (holder.root != null) {
                    holder.root.setVisibility(View.VISIBLE);
                }
                holder.dayLabel.setText(forecast.getDayLabel());
                holder.temperatureView.setText(forecast.getTemperature());

                String contentDescription = forecast.getDescription();
                if (TextUtils.isEmpty(contentDescription)) {
                    contentDescription = forecast.getDayLabel();
                }
                holder.iconView.setContentDescription(contentDescription);

                int iconRes = resolveForecastIcon(forecast.getConditionId());
                holder.iconView.setImageResource(iconRes);

            } else {
                if (holder.root != null) {
                    holder.root.setVisibility(View.INVISIBLE);
                }
                holder.dayLabel.setText("");
                holder.temperatureView.setText("");
                holder.iconView.setImageDrawable(null);
                holder.iconView.setContentDescription(null);
            }
        }
    }

 	@DrawableRes
  	private int resolveForecastIcon(int conditionId) {
      	if (conditionId >= 200 && conditionId < 300) {
          	return R.drawable.wth_thunderstorm;
      	} else if (conditionId >= 300 && conditionId < 400) {
          	return R.drawable.wth_drizzle;
      	} else if (conditionId >= 500 && conditionId < 600) {
          	return R.drawable.wth_rainy;
      	} else if (conditionId >= 600 && conditionId < 700) {
          	return R.drawable.wth_snowy;
      	} else if (conditionId >= 700 && conditionId < 800) {
          	return R.drawable.wth_mist;
      	} else if (conditionId == 800) {
          	return R.drawable.wth_clear;
      	} else if (conditionId > 800 && conditionId < 900) {
          	return R.drawable.wth_clouds;
      	}
        return R.drawable.wth_clouds;
      }

    private View getParentOrSelf(View view) {
        if (view == null) {
            return null;
        }
        ViewParent parent = view.getParent();
        if (parent instanceof View) {
            return (View) parent;
        }
        return view;
    }

    private static final class ForecastViewHolder {
        final View root;
        final TextView dayLabel;
        final ImageView iconView;
        final TextView temperatureView;

        ForecastViewHolder(View root, TextView dayLabel, ImageView iconView, TextView temperatureView) {
            this.root = root != null ? root : dayLabel;
            this.dayLabel = dayLabel;
            this.iconView = iconView;
            this.temperatureView = temperatureView;
        }
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
