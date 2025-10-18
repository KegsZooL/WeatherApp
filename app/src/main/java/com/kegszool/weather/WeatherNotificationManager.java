package com.kegszool.weather;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

final class WeatherNotificationManager {

    private static final String CHANNEL_ID = "weather_status_channel";
    private static final int NOTIFICATION_ID = 1002;

    private WeatherNotificationManager() {
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    static void showWeatherNotification(Context context, Weather.WeatherData data) {
        if (context == null || data == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        createChannelIfNeeded(appContext, notificationManager);

        PendingIntent contentIntent = buildContentIntent(appContext);
        String title = resolveTitle(appContext, data.getLocation());
        String content = buildContentText(data.getTemperature(), data.getDescription());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(resolveSmallIcon())
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        Bitmap largeIcon = loadLargeIcon(appContext, data.getConditionId());
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    static void cancel(Context context) {
        if (context == null) {
            return;
        }
        NotificationManagerCompat.from(context.getApplicationContext()).cancel(NOTIFICATION_ID);
    }

    private static void createChannelIfNeeded(Context context, NotificationManagerCompat notificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel existing = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.weather_notification_channel_name),
                android.app.NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(context.getString(R.string.weather_notification_channel_description));
        channel.enableVibration(false);
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    private static PendingIntent buildContentIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(intent);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        return stackBuilder.getPendingIntent(0, flags);
    }

    private static String resolveTitle(Context context, @Nullable String location) {
        if (!TextUtils.isEmpty(location)) {
            return location;
        }
        return context.getString(R.string.app_name);
    }

    private static String buildContentText(@Nullable String temperatureRaw, @Nullable String descriptionRaw) {
        String temperature = "";
        if (!TextUtils.isEmpty(temperatureRaw)) {
            temperature = temperatureRaw.trim();
            if (!temperature.endsWith("°C")) {
                if (temperature.endsWith("°")) {
                    temperature = temperature + "C";
                } else {
                    temperature = temperature + "°C";
                }
            }
        }

        String description = TextUtils.isEmpty(descriptionRaw) ? "" : descriptionRaw.trim();

        if (!temperature.isEmpty() && !description.isEmpty()) {
            return temperature + " • " + description;
        } else if (!temperature.isEmpty()) {
            return temperature;
        } else if (!description.isEmpty()) {
            return description;
        }
        return "";
    }

    private static Bitmap loadLargeIcon(Context context, int conditionId) {
        @DrawableRes int iconRes = resolveConditionIcon(conditionId);
        if (iconRes == 0) {
            return null;
        }
        return BitmapFactory.decodeResource(context.getResources(), iconRes);
    }

    @DrawableRes
    private static int resolveSmallIcon() {
        return R.drawable.ic_baseline_location_on_24;
    }

    @DrawableRes
    private static int resolveConditionIcon(int conditionId) {
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
        return 0;
    }
}
