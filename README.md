# WeatherApp

## Overview
Android weather application that fetches forecasts from OpenWeather, supports ru/en UI, and offers both in-app search and map-based location picking. Provides a home screen widget with multi-day forecast.

## Features
- Current weather and 4-day forecast with dynamic icons and background.
- City search with ru/en suggestions; ru/en localization across UI and tooltips.
- Map picker for selecting location and loading weather by coordinates.
- Home screen widget showing city and multi-day forecast.
- Notifications for current weather; caching last weather for widget resilience.

## Dependencies
- AndroidX AppCompat/ConstraintLayout/Material.
- Google Play Services: Location, Maps.
- OpenWeather API (requires `OPEN_WEATHER_API_KEY`).
- Google Maps API (requires `GOOGLE_MAPS_API_KEY`).
- Gradle/AGP; Java 17.

## Architecture
- Single-activity app with `MainActivity` handling UI, search, and location.
- Weather fetching via `Weather` `AsyncTask` hitting OpenWeather endpoints.
- Widget updates via `WeatherWidgetProvider` using cached data in `LastWeatherStorage`.
- Map selection handled by `MapPickerActivity` using Google Maps SDK.

## Structure
- `app/src/main/java/com/kegszool/weather/` — core code (activities, widget provider, storage, weather fetcher).
- `app/src/main/res/layout/` — UI layouts including main screen, map picker, and widget.
- `app/src/main/res/values*/` — strings, colors, and localized resources.
- `app/src/main/res/xml/` — widget provider config.
- `app/build.gradle` — dependencies and API key wiring via `.env` (`OPEN_WEATHER_API_KEY`, `GOOGLE_MAPS_API_KEY`).