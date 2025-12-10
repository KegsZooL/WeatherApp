package com.kegszool.weather;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public record ForecastViewHolder(
    View root,
    TextView dayLabel,
    ImageView iconView,
    TextView temperatureView
) {

    public ForecastViewHolder(
            View root,
            TextView dayLabel,
            ImageView iconView,
            TextView temperatureView
    ) {
        this.root = root != null ? root : dayLabel;
        this.dayLabel = dayLabel;
        this.iconView = iconView;
        this.temperatureView = temperatureView;
    }
}
