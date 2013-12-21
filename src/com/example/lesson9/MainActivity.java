package com.example.lesson9;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;


public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    final static String ACTION_response = "com.example.lesson9.intentServiceLoaderWeather";
    final static int TOWN_LOAD = 0;
    final static int WEATHER_COND = 1;
    final static int ICON_LOAD = 2;

    MyBroadcastReceiver myBroadcastReceiver;
    ArrayList<Location> locations = new ArrayList<Location>();
    ArrayList<WeatherCond> weathers = new ArrayList<WeatherCond>();
    ArrayList<Bitmap> weatherIcons = new ArrayList<Bitmap>();
    boolean isTownListChanged = true;
    int curTown = -1;
    boolean firstShow = true;

    // views
    TextView townName;
    TextView countryName;
    TextView tempNow;
    TextView pressureNow;
    ImageView weatherIconView;

    TextView[] tempTexts;
    ImageView[] weatherIconViews;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // town getting
        //   loading example:
        //   Intent updateServiceIntent = new Intent(MainActivity.this, TownLoader.class);
        //   startService(updateServiceIntent.putExtra("url", createTownRequest("Peterburg", 3)));

        Location loc = new Location();
        loc.param[Location.TOWN] = "Peterburg";
        loc.param[Location.COUNTRY] = "Russia";
        loc.param[Location.LATITUDE] = "59.894";
        loc.param[Location.LONGITUDE] = "30.264";
        locations.add(loc.makeCopy());
        loc.param[Location.TOWN] = "Petrozavodsk";
        loc.param[Location.COUNTRY] = "Russia";
        loc.param[Location.LATITUDE] = "61.817";
        loc.param[Location.LONGITUDE] = "34.333";
        locations.add(loc.makeCopy());
        loc.param[Location.TOWN] = "London";
        loc.param[Location.COUNTRY] = "United Kingdom";
        loc.param[Location.LATITUDE] = "51.517";
        loc.param[Location.LONGITUDE] = "-0.106";
        locations.add(loc.makeCopy());

        updateWeather(0);


        // views
        townName = (TextView) findViewById(R.id.townNameText);
        countryName = (TextView) findViewById(R.id.countryNameText);
        tempNow = (TextView) findViewById(R.id.tempNowText);
        pressureNow = (TextView) findViewById(R.id.pressureNowText);
        weatherIconView = (ImageView) findViewById(R.id.weatherIconView);

        tempTexts = new TextView[]{
                (TextView) findViewById(R.id.temp1Text),
                (TextView) findViewById(R.id.temp2Text),
                (TextView) findViewById(R.id.temp3Text)
        };
        weatherIconViews = new ImageView[]{
                (ImageView) findViewById(R.id.icon1View),
                (ImageView) findViewById(R.id.icon2View),
                (ImageView) findViewById(R.id.icon3View)
        };

        // service response catching
        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(ACTION_response);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(myBroadcastReceiver, intentFilter);

    }

    String createTownRequest(String town, int resultNum){
        try{
            return "http://api.worldweatheronline.com/free/v1/search.ashx?q=" + URLEncoder.encode(town, "UTF-8") + "&format=xml&timezone=no&popular=no&num_of_results=" + resultNum + "&key=xphb83vgt4yvbznxu34fxhyu";
        } catch (UnsupportedEncodingException ex){
            Log.e("requestBuilding", "UnsupportedEncodingException");
            return "";
        }
    }
    String createWeatherRequest(Location town, int dayNum){
            return "http://api.worldweatheronline.com/free/v1/weather.ashx?q=" + town.param[Location.LATITUDE] + "%2C" + town.param[Location.LONGITUDE] + "&format=xml&num_of_days=" + dayNum + "&key=xphb83vgt4yvbznxu34fxhyu";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // un-register BroadcastReceiver
        unregisterReceiver(myBroadcastReceiver);
    }


    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(TownLoader.LOADING_ERROR, false)){
                Log.w("MainActivity", "Service hasn't downloaded data");
                return;
            }

            int type = intent.getExtras().getInt("type");
            if (type == TOWN_LOAD){
                for (int i = 0; ; i++){
                    String[] a = intent.getStringArrayExtra("data" + i);
                    if (a == null) break;
                    Location l = new Location();
                    l.param = a;
                    locations.add(l);
                    isTownListChanged = true;
                    //!!
                    if (firstShow){
                        updateWeather(0);
                        firstShow = false;
                    }
                    break;
                }
            } else if (type == WEATHER_COND){
                weathers.clear();
                for (int i = 0; ; i++){
                    String[] a = intent.getStringArrayExtra("data" + i);
                    boolean t = intent.getExtras().getBoolean("dataType" + i);
                    if (a == null) break;
                    WeatherCond w = new WeatherCond(t);
                    w.param = a;
                    weathers.add(w);
                    weatherIcons.add(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
                    Intent iconServiceIntent = new Intent(MainActivity.this, IconLoader.class);
                    startService(iconServiceIntent.putExtra("url", w.param[w.ICON_URL]).putExtra("number", i));

                }
                showWeather();
            } else if (type == ICON_LOAD){
                byte[] data = intent.getByteArrayExtra("data");
                int number = intent.getExtras().getInt("number");
                weatherIcons.set(number, BitmapFactory.decodeByteArray(data, 0, data.length));
                updateIcon(number);
            }
            else {
                Log.e("MainActivity", "Unrigistered broadcast");
            }

        }
    }


    void updateWeather(int town){
        if (town == curTown) return;
        if (town != -1){
            curTown = town;
        }
        Intent updateServiceIntent = new Intent(MainActivity.this, WeatherUpdater.class);
        startService(updateServiceIntent.putExtra("url", createWeatherRequest(locations.get(curTown), 3)));
   }

    void forceUpdateWeather(int town){
        if (town != -1){
            curTown = town;
        }
        Intent updateServiceIntent = new Intent(MainActivity.this, WeatherUpdater.class);
        startService(updateServiceIntent.putExtra("url", createWeatherRequest(locations.get(curTown), 3)));
    }

    void updateIcon(int day){
        Bitmap pic = weatherIcons.get(day);
        int size;
        ImageView curIcon;
        if (day == 0){
            curIcon = weatherIconView;
            size = Math.max(curIcon.getWidth(), curIcon.getHeight());
        } else {
            curIcon = weatherIconViews[day - 1];
            size = curIcon.getHeight();
        }

        curIcon.setImageBitmap(Bitmap.createScaledBitmap(pic, size, size, false));

    }

    void showWeather(){
        townName.setText(locations.get(curTown).param[Location.TOWN]);
        countryName.setText(locations.get(curTown).param[Location.COUNTRY]);
        tempNow.setText(weathers.get(0).param[WeatherCond.TEMP_NOW] + "°C");
        pressureNow.setText(Math.round(Integer.parseInt(weathers.get(0).param[WeatherCond.PRESURE_NOW]) * 0.72) + " mm Hg");

        for (int i = 0; i < weatherIconViews.length; i++){
            tempTexts[i].setText(weathers.get(i + 1).param[WeatherCond.TEMP_MIN] + "°C" + "   -   " + weathers.get(i + 1).param[WeatherCond.TEMP_MAX] + "°C");
        }

    }


    // menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        menu.add(Menu.NONE, 100, 1, "-- Add new --");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() > 200){
            updateWeather(item.getItemId() - 201);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isTownListChanged){
            for (int i = menu.size() - 1; i > 0; i--){
                menu.removeItem(i);
            }
            for (int i = 0; i < locations.size(); i++){
                menu.add(Menu.NONE, 200 + menu.size(), Menu.NONE, locations.get(i).param[Location.TOWN]);
            }
            isTownListChanged = false;
        }
        return super.onPrepareOptionsMenu(menu);
    }

}
