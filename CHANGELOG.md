[0.0.2]

- Switching to FusedLocationProviderClient in GPS tracking logic
- Refactored weather loading to WeatherService/WeatherRouter and split ForecastViewHolder class.

[0.0.1]

- City search with ru/en suggestions via AutoCompleteTextView.
- Weather fetching optimized (gzip, buffered UTF-8 reader, reused formatters).
- RU/EN localization for UI and tooltips on metrics.
- Map picker to select location and load forecast by coordinates.
- Home screen widget showing multi-day forecast with icons.
- Styled dropdowns, search/city text outlines, and map confirm button tweaks