package com.example.leesama.stormy.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.leesama.stormy.R;
import com.example.leesama.stormy.weather.Current;
import com.example.leesama.stormy.weather.Day;
import com.example.leesama.stormy.weather.Forecast;
import com.example.leesama.stormy.weather.Hour;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends ActionBarActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private Forecast mForecast;

    @InjectView(R.id.timeTextView) TextView mTimeLabel;
    @InjectView(R.id.temperatureTextView) TextView mTemperatureLabel;
    @InjectView(R.id.humidityValueTextView) TextView mHumidityValue;
    @InjectView(R.id.precipValueTextView) TextView mPrecipValue;
    @InjectView(R.id.summaryTextView) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.locationTextView) TextView mLocationView;
    @InjectView(R.id.refreshIcon) ImageView mRefresh;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mProgressBar.setVisibility(View.INVISIBLE);

        final double latitude = 13.0827;
        final double longitude = 80.2707;

        mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude, longitude);
            }
        });

        getForecast(latitude, longitude);
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = "55a24f17152dce330fef2667d75d0d06";

        String forecastURL = "https://api.forecast.io/forecast/"+apiKey+"/"+latitude+","+longitude;

        if(isNetworkAvailable()) {

            toggleRefresh();

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder().url(forecastURL).build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleRefresh();
                            }
                        });
                        if (response.isSuccessful()) {
                            mForecast = parseForecastDetails(response.body().string());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                    mProgressBar.setVisibility(View.INVISIBLE);
                                    mRefresh.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        else
                            alertUserError();
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught :", e);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            Toast.makeText(this, "Network Unavailable!!", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility() != View.VISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefresh.setVisibility(View.INVISIBLE);
        }
        else
        {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        Current mCurrent = mForecast.getCurrent();
        mTemperatureLabel.setText(mCurrent.getTemperature()+"");
        mPrecipValue.setText(mCurrent.getPrecipChance()+"%");
        mSummaryLabel.setText(mCurrent.getSummary());
        mTimeLabel.setText(mCurrent.getFormattedTime());
        mHumidityValue.setText(mCurrent.getHumidity()+"");
        mIconImageView.setImageDrawable(getResources().getDrawable(mCurrent.getIconId()));
        mLocationView.setText(mCurrent.getTimeZone());
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException {
        Forecast forecast = new Forecast();

        forecast.setCurrent(getCurrentWeather(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));
        return forecast;
    }

    private Day[] getDailyForecast(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);

        JSONArray data = forecast.getJSONObject("daily").getJSONArray("data");

        Day[] days = new Day[data.length()];

        for(int i=0; i< data.length();i++) {
            Day day = new Day();
            JSONObject jHour = data.getJSONObject(i);
            day.setSummary(jHour.getString("summary"));
            day.setIcon(jHour.getString("icon"));
            day.setTemperature(jHour.getDouble("temperatureMax"));
            day.setTime(jHour.getLong("time"));
            day.setTimeZone(forecast.getString("timezone"));

            days[i] = day;
        }

        return days;
    }

    private Hour[] getHourlyForecast(String jsonData) throws JSONException {

        JSONObject forecast = new JSONObject(jsonData);

        JSONArray data = forecast.getJSONObject("hourly").getJSONArray("data");

        Hour[] hours = new Hour[data.length()];

        for(int i=0; i< data.length();i++) {
            Hour hour = new Hour();
            JSONObject jHour = data.getJSONObject(i);
            hour.setSummary(jHour.getString("summary"));
            hour.setIcon(jHour.getString("icon"));
            hour.setTemperature(jHour.getDouble("temperature"));
            hour.setTime(jHour.getLong("time"));
            hour.setTimeZone(forecast.getString("timezone"));

            hours[i] = hour;
        }

        return hours;
    }

    private Current getCurrentWeather(String data) throws JSONException {
        JSONObject forecast = new JSONObject(data);

        JSONObject currently = forecast.getJSONObject("currently");

        Current current = new Current();
        current.setHumidity(currently.getDouble("humidity"));
        current.setTime(currently.getLong("time"));
        current.setPrecipChance(currently.getDouble("precipProbability"));
        current.setIcon(currently.getString("icon"));
        current.setSummary(currently.getString("summary"));
        current.setTemperature(currently.getDouble("temperature"));
        current.setTimeZone(forecast.getString("timezone"));
        return current;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        if(networkInfo != null && networkInfo.isConnected()) {
         return true;
        }
        return false;
    }

    private void alertUserError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(),"error_dialog");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.dailyButton)
    public void startDailyActivity(View view) {
        startActivity(new Intent(this,DailyForecastActivity.class));
    }
}
