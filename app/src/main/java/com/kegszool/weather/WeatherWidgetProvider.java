package com.kegszool.weather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;

public class WeatherWidgetProvider extends AppWidgetProvider {

    private static final int[] DAY_LABEL_IDS = {
        R.id.widgetDay1Label, R.id.widgetDay2Label, R.id.widgetDay3Label, R.id.widgetDay4Label
    };

    private static final int[] DAY_TEMP_IDS = {
        R.id.widgetDay1Temp, R.id.widgetDay2Temp, R.id.widgetDay3Temp, R.id.widgetDay4Temp
    };

    private static final int[] DAY_ICON_IDS = {
        R.id.widgetDay1Icon, R.id.widgetDay2Icon, R.id.widgetDay3Icon, R.id.widgetDay4Icon
    };

    @Override
    public void onUpdate(
        Context context,
        AppWidgetManager appWidgetManager,
        int[] appWidgetIds
    ) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void requestUpdate(Context context) {
        if (context == null) { return; }

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, WeatherWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(componentName);
        if (widgetIds == null) { return; }

        for (int widgetId : widgetIds) {
            updateAppWidget(context, manager, widgetId);
        }
    }

    private static void updateAppWidget(
        Context context,
        AppWidgetManager appWidgetManager,
        int appWidgetId
    ) {
        LastWeatherStorage.WeatherSnapshot snapshot = LastWeatherStorage.read(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        String cityLabel = !TextUtils.isEmpty(snapshot.city()) ? snapshot.city() : context.getString(R.string.city_placeholder);
        views.setTextViewText(R.id.widgetCity, cityLabel);

        bindForecasts(views, snapshot);

        PendingIntent openApp = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.widgetContainer, openApp);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void bindForecasts(
        RemoteViews views,
        LastWeatherStorage.WeatherSnapshot snapshot
    ) {
        if (views == null) { return; }

        LastWeatherStorage.ForecastSnapshot[] forecasts = snapshot.forecasts();
        for (int i = 0; i < DAY_LABEL_IDS.length; i++) {

            String label = "—";
            String temp = "—";
            int icon = resolveIcon(0);

            if (forecasts != null && i < forecasts.length && forecasts[i] != null) {

                label = !TextUtils.isEmpty(forecasts[i].dayLabel())
                        ? forecasts[i].dayLabel()
                        : label;
                temp = !TextUtils.isEmpty(forecasts[i].temperature())
                        ? formatTemperature(forecasts[i].temperature())
                        : temp;

                icon = resolveIcon(forecasts[i].conditionId());
            } else if (i == 0) {
                temp = formatTemperature(snapshot.temperature());
                icon = resolveIcon(snapshot.conditionId());
            }
            views.setTextViewText(DAY_LABEL_IDS[i], label);
            views.setTextViewText(DAY_TEMP_IDS[i], temp);
            views.setImageViewResource(DAY_ICON_IDS[i], icon);
        }
    }

    private static String formatTemperature(String rawTemp) {
        if (TextUtils.isEmpty(rawTemp)) {
            return "—";
        }
        String trimmed = rawTemp.trim();
        if (trimmed.endsWith("°")) {
            return trimmed;
        }
        return trimmed + "°";
    }

    @DrawableRes
    private static int resolveIcon(int conditionId) {
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
}