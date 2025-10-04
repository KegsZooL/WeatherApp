package com.kegszool.weather;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public GpsTracker gpsTracker;
    static double latitude, longitude, prevLat, prevLon;
    @SuppressLint("StaticFieldLeak")
    static TextView location, description, humidity, pressure, mainTemp,windSpeed, visibility;
    @SuppressLint("StaticFieldLeak")
    static EditText search;

    static String cityName = "Chelybinsk";
    static String prev = "";
    static String key = "488f4111e6b7924073ff22cd896b2e2a";
    static String error = "";

    @Override
    protected void onResume() {
        super.onResume();
        if (GpsTracker.isFromSetting){
            finish();
            startActivity(getIntent());
            getLocation(latitude, longitude);
            GpsTracker.isFromSetting=false;
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (GpsTracker.isFromSetting){
            finish();
            startActivity(getIntent());
            getLocation(latitude,longitude);
            GpsTracker.isFromSetting=false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        search = findViewById(R.id.search);
        search.clearFocus();
        location = findViewById(R.id.city);
        description = findViewById(R.id.description);
        mainTemp = findViewById(R.id.tempMain);
        humidity = findViewById(R.id.humidity);
        pressure = findViewById(R.id.pressure);
        windSpeed = findViewById(R.id.windSpeed);
        visibility = findViewById(R.id.visibility);

        search.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE){
                cityName = search.getText().toString();
                if(cityName.isEmpty() || cityName.equals(" ")){
                    Toast.makeText(getApplicationContext(), "Please enter a city!", Toast.LENGTH_SHORT).show();
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(15);
                    getWeather(prev, key);
                }
                getWeather(cityName,key);
            }
            return false;
        });
        prev = cityName;
        getLocation(latitude,longitude);
    }

    private void getWeather(String cityName, String key) {
        Log.d("CityName: ", cityName);
        Weather getData = new Weather();
        getData.execute("https://api.openweathermap.org/data/2.5/forecast?q="+cityName+"&appid="+key+"&units=metric");
    }

    public void getLocation(Double lat, Double lon){
        gpsTracker = new GpsTracker(MainActivity.this);
        if(gpsTracker.canGetLocation()){
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            lat = latitude;
            lon = longitude;
            if (lat == 0.0 && lon == 0.0){
                startActivity(getIntent());
                lat = gpsTracker.latitude;
                lon = gpsTracker.longitude;
            }
            Log.d("Lat: ", lat.toString());
            Weather getData = new Weather();
            getData.execute("https://api.openweathermap.org/data/2.5/forecast?lat="+lat+"&lon="+lon+"&appid="+key+"&units=metric");
            prevLat = lat;
            prevLon = lon;
        }else{
            gpsTracker.showSettingsAlert();
        }
    }
}