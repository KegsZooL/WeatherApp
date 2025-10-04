package com.kegszool.weather;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Weather extends AsyncTask<String, Void, String> {

    String result; String error;
    @Override
    protected String doInBackground(String... urls) {
        result = "";
        URL link;
        HttpURLConnection myConnection;
        try{
            link = new URL(urls[0]);
            myConnection = (HttpURLConnection)link.openConnection();
            InputStream in = myConnection.getInputStream();
            InputStreamReader myStreamReader = new InputStreamReader(in);
            int data = myStreamReader.read();
            while (data != -1){
                char current = (char)data;
                result += current;
                data = myStreamReader.read();
            }
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            error = "City Not Found!";
            Log.d("IOError: ", error);
            MainActivity.error = error;
        }
        return null;

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        try{
            JSONObject myObject = new JSONObject(result);
            Log.d("myObject", myObject.toString());

            JSONObject city = new JSONObject(myObject.getString("city"));
            String placeName = city.getString("name");
            Log.d("placeName", placeName);
            String country = city.getString("country");
            Log.d("country", country);
            String location = placeName+", "+country;

            JSONArray list = myObject.getJSONArray("list");
            Log.d("list", list.toString());

            JSONObject list0 = list.getJSONObject(0);
            Log.d("list0", list0.toString());

            JSONArray weather = list0.getJSONArray("weather");
            Log.d("weather", weather.toString());

            String desc = weather.getJSONObject(0).getString("description");
            desc = desc.substring(0, 1).toUpperCase() + desc.substring(1);
            Log.d("desc", desc);


            JSONObject main = list0.getJSONObject("main");
            Log.d("main", main.toString());

            double mainT = main.getDouble("temp");
            Log.d("mainTemp", Double.toString(mainT));
            int temp = (int) Math.round(mainT);
            String mainTemp = String.valueOf(temp) ;

            String humidity = main.getString("humidity") + " %";
            Log.d("humidity", humidity);

            String pressure = main.getString("pressure") + " hPa";
            Log.d("pressure", pressure);

            JSONObject wind = list0.getJSONObject("wind");
            double windTemp = wind.getDouble("speed");
            windTemp = windTemp * 3.6;
            final DecimalFormat df = new DecimalFormat("0.0");
            String windSpeed = df.format(windTemp) + " km/h";
            Log.d("windSpeed", windSpeed);

            int visible = list0.getInt("visibility");
            int tempVisible = visible/1000;
            String visibilityT = String.valueOf(tempVisible);
            String visibility = visibilityT + " km";
            Log.d("visibility", visibility);

            List<String> day = new ArrayList<>();
            List<String> dayIcon = new ArrayList<>();
            List<String> dayTemp = new ArrayList<>();

            String[] currDateCheck = list0.getString("dt_txt").split(" ");
            String currDate = currDateCheck[0];
            Log.d("currDate", currDate);

            int counter = 0;
            int index = 0;
            int min = Integer.MAX_VALUE;

            for (int i = 0; i < 40; i++){
                JSONObject listTemp = list.getJSONObject(i);
                String tempDate = listTemp.getString("dt_txt");
                String[] tempTime = tempDate.split(" ");
                String timeCheck = tempTime[1];

                String currTempDate = tempTime[0];

                JSONObject mainLoop = listTemp.getJSONObject("main");
                String timeLoop = listTemp.getString("dt_txt");

                String[] timeSplit = timeLoop.split(" ");
                String loopDate = timeSplit[0];
                String[] loopDateList = loopDate.split("-");
                String dayLoop = loopDateList[2];

                String timeTemp = timeSplit[1];
                String[] hourList = timeTemp.split(":");
                String hour = hourList[0];
                Log.d("Hour: ", hour);

                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date();
                String currDateTemp = formatter.format(date);
                String[] currTimeSplit = currDateTemp.split(" ");
                String currDate1 = currTimeSplit[0];
                String[] currDateList = currDate1.split("-");
                String dayNow = currDateList[2];

                String currTime = currTimeSplit[1];
                String[] currHourList = currTime.split(":");
                String currHour = currHourList[0];
                Log.d("currHour: ", currHour);

                int hourInt = Integer.parseInt(hour);
                int currHourInt = Integer.parseInt(currHour);
                int dayNowInt = Integer.parseInt(dayNow);
                int dayLoopInt = Integer.parseInt(dayLoop);

                // Taking consideration of JSON time = 00:00:00
                if (hourInt == 0 && currHourInt != 0){
                    hourInt = 24;
                }

                int diff = currHourInt - hourInt;
                Log.d("Diff: ", String.valueOf(diff));


                if (diff >= 0 && diff < min && dayNowInt == dayLoopInt){
                    min = diff;
                    index = i;
                    Log.d("Index Date1: ", dayLoop);

                }

                if (currTempDate.compareTo(currDate) > 0 && timeCheck.compareTo("12:00:00") == 0 && counter < 4){
                    currDate = currTempDate;
                    counter++;
                    Log.d("Dates", tempDate);
                    Log.d("Counter", String.valueOf(counter));


                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd");
                    @SuppressLint("SimpleDateFormat") Format f = new SimpleDateFormat("EEEE");
                    Date dateLoop = simpleFormat.parse(tempTime[0]);
                    String dayFoo = f.format(dateLoop);
                    Log.d("Day: ", dayFoo);
                    day.add(dayFoo);


                    double mainTempMin = mainLoop.getDouble("temp_min");
                    Log.d("mainTempMin", Double.toString(mainTempMin));
                    int tempCc = (int) Math.floor(mainTempMin);
                    double mainTempMax = mainLoop.getDouble("temp_max");
                    Log.d("mainTempMax", Double.toString(mainTempMax));
                    int tempCc2 = (int) Math.ceil(mainTempMax);
                    String mainTempCc = String.valueOf(tempCc);
                    String mainTempCc2 = String.valueOf(tempCc2);
                    String mainTempStr = mainTempCc2 + "° | " + mainTempCc + "°";
                    dayTemp.add(mainTempStr);

                    JSONArray weatherLoop = listTemp.getJSONArray("weather");
                    String iconLoop = weatherLoop.getJSONObject(0).getString("icon");
                    Log.d("icon", iconLoop);
                    dayIcon.add(iconLoop);
                }
            }
            Log.d("Day1: ", day.get(0));
            Log.d("Temp1: ", dayTemp.get(0));
            Log.d("Icon1: ", dayIcon.get(0));

            Log.d("Index: ", String.valueOf(index));

            int fiveHourIndex = index + 5;
            List<String> hours = new ArrayList<>();
            List<String> hoursIcon = new ArrayList<>();
            List<String> hoursTemp = new ArrayList<>();

            for (int i = index; i < fiveHourIndex; i++){
                JSONObject listTemp = list.getJSONObject(i);
                JSONObject mainLoop = listTemp.getJSONObject("main");


                String timeLoop = listTemp.getString("dt_txt");
                Log.d("time", timeLoop);
                String[] loopSplit = timeLoop.split(" ");
                String[] timeStr = loopSplit[1].split(":");
                Log.d("s1", timeStr[0]);
                int hourStr = Integer.parseInt(timeStr[0]);
                String hour;

                if (hourStr > 12){
                    int tempLoop = hourStr - 12;
                    String hourT = String.valueOf(tempLoop);
                    hour = hourT + " PM";

                }
                else if (hourStr == 12){
                    String hourT = String.valueOf(hourStr);
                    hour = hourT + " PM";
                }
                else if (hourStr == 0){
                    String hourT = String.valueOf(12);
                    hour = hourT + " AM";
                }
                else{
                    String hourT = String.valueOf(hourStr);
                    hour = hourT + " AM";
                }
                hours.add(hour);

                double hourTempD = mainLoop.getDouble("temp");
                Log.d("mainTemp", Double.toString(hourTempD));
                int tempInt = (int) Math.round(hourTempD);
                String tempIntT = String.valueOf(tempInt);
                String hourTemp = tempIntT + "°";
                hoursTemp.add(hourTemp);

                JSONArray weatherLoop = listTemp.getJSONArray("weather");
                String hourIcon = weatherLoop.getJSONObject(0).getString("icon");
                Log.d("icon", hourIcon);
                hoursIcon.add(hourIcon);
            }
            MainActivity.location.setText(location);
            MainActivity.mainTemp.setText(mainTemp);
            MainActivity.description.setText(desc);
            MainActivity.humidity.setText(humidity);
            MainActivity.pressure.setText(pressure);
            MainActivity.visibility.setText(visibility);
            MainActivity.windSpeed.setText(windSpeed);
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
            error = "City Not Found!";
            Log.d("JSONError: ", error);
        }
    }
}