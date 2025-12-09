package com.kegszool.weather;

import java.util.Collections;
import java.util.List;

public record WeatherData(
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
    public WeatherData(
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
            this.dailyForecasts = List.copyOf(dailyForecasts);
        }
    }

    public record DailyForecast(
        String dayLabel,
        String temperature,
        int conditionId,
        String description
    ) {
        public DailyForecast(
            String dayLabel,
            String temperature,
            int conditionId,
            String description
        ) {
            this.dayLabel = dayLabel != null
                    ? dayLabel
                    : "";
            this.temperature = temperature != null
                    ? temperature
                    : "";
            this.description = description != null
                    ? description
                    : "";
            this.conditionId = conditionId;
        }
    }
}