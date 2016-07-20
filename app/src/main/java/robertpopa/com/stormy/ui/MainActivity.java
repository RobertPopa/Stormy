package robertpopa.com.stormy.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import robertpopa.com.stormy.R;
import robertpopa.com.stormy.location.LocationProvider;
import robertpopa.com.stormy.utils.StormyUtils;
import robertpopa.com.stormy.weather.Constants;
import robertpopa.com.stormy.weather.Current;
import robertpopa.com.stormy.weather.Day;
import robertpopa.com.stormy.weather.Forecast;
import robertpopa.com.stormy.weather.Hour;

public class MainActivity extends AppCompatActivity implements LocationProvider.LocationCallback{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ALERT_DIALOG_TAG = "alert_dialog";
    public static final String DAILY_FORECAST_TAG = "DAILY_FORECAST_TAG";
    public static final String HOURLY_FORECAST_TAG = "HOURLY_FORECAST_TAG";
    public static final String LOCATION_TAG = "LOCATION_TAG";

    private Forecast mForecast;
    private LocationProvider mLocationProvider;
    private double mCurrentLatitude;
    private double mCurrentLongitude;
    private String mCurrentAddress;
    private String mCurrentUnitSystem;
    private boolean mIsNetworkAvailable;

    @BindView(R.id.timeValue) TextView mTimeValue;
    @BindView(R.id.temperatureValue) TextView mTemperatureValue;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;
    @BindView(R.id.locationLabel) TextView mLocationLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        // Hide progress bar initially
        mProgressBar.setVisibility(View.INVISIBLE);

        // Set default unit system
        mCurrentUnitSystem = "si";

        // Get the current location
        mLocationProvider = new LocationProvider(this, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( !mLocationProvider.hasLocation() ) {
            mLocationProvider.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationProvider.disconnect();
    }

    private void loadWeather() {

        mIsNetworkAvailable = new StormyUtils(this).checkNetwork();

        if ( mIsNetworkAvailable ) {

            String language = StormyUtils.getUserLanguage();

            // Example URL including query filters
            // https://api.forecast.io/forecast/a6a52c11070a4f4cca26881e67cd684c/40.3657,-3.4824?units=si&lang=es
            StringBuilder forecastURL = new StringBuilder()
                        .append(Constants.BASE_URL)
                        .append("/")
                        .append(Constants.API_KEY)
                        .append("/")
                        .append(mCurrentLatitude)
                        .append(",")
                        .append(mCurrentLongitude)
                        .append("?lang=" + language)
                        .append("&units=" + mCurrentUnitSystem);

            Log.i(TAG, "URL: " + forecastURL.toString());

            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastURL.toString())
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            toggleRefresh();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    String jsonData = response.body().string();
                    Log.v(TAG, jsonData);

                    if (response.isSuccessful()) {
                        try {
                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException caught: ", e);
                        }
                    } else {
                        alertUserAboutError();
                    }
                }
            });

        } else {
            AlertDialogFragment dialog = new AlertDialogFragment();
            dialog.setMessage("Network is unavailable.");
            dialog.show(getFragmentManager(), ALERT_DIALOG_TAG);
        }

    }

    private void toggleRefresh() {
        if ( mProgressBar.getVisibility() == View.INVISIBLE ) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        mTemperatureValue.setText(String.valueOf(mForecast.getCurrent().getTemperature()) + "°");
        mTimeValue.setText(mForecast.getCurrent().getFormattedTime());
        mHumidityValue.setText(String.valueOf(mForecast.getCurrent().getHumidity()));
        mPrecipValue.setText(String.valueOf(mForecast.getCurrent().getPrecipChance() + "%"));
        mSummaryLabel.setText(mForecast.getCurrent().getSummary());
        mLocationLabel.setText(mCurrentAddress);

        Drawable drawable = getResources().getDrawable(mForecast.getCurrent().getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException {
        Forecast forecast = new Forecast();

        forecast.setCurrent(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));

        return forecast;
    }

    private Day[] getDailyForecast(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");

        Day[] days = new Day[data.length()];

        for ( int i=0; i < data.length(); i++ ) {
            JSONObject jsonHour = data.getJSONObject(i);
            Day day = new Day();

            day.setSummary(jsonHour.getString("summary"));
            day.setTemperatureMax(jsonHour.getDouble("temperatureMax"));
            day.setIcon(jsonHour.getString("icon"));
            day.setTime(jsonHour.getLong("time"));
            day.setTimeZone(timezone);

            days[i] = day;
        }

        return days;
    }

    private Hour[] getHourlyForecast(String jsonData) throws JSONException  {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");

        Hour[] hours = new Hour[data.length()];

        for ( int i=0; i < data.length(); i++ ) {
            JSONObject jsonHour = data.getJSONObject(i);
            Hour hour = new Hour();

            hour.setSummary(jsonHour.getString("summary"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTime(jsonHour.getLong("time"));
            hour.setTimeZone(timezone);

            hours[i] = hour;
        }

        return hours;
    }

    private Current getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject currently = forecast.getJSONObject("currently");
        Current result = new Current();
        result.setHumidity(currently.getDouble("humidity"));
        result.setIcon(currently.getString("icon"));
        result.setPrecipChance(currently.getDouble("precipProbability"));
        result.setSummary(currently.getString("summary"));
        result.setTemperature(currently.getDouble("temperature"));
        result.setTime(currently.getLong("time"));

        result.setTimeZone(timezone);

        Log.d(TAG, result.getFormattedTime());

        return result;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), ALERT_DIALOG_TAG);
    }

    @OnClick(R.id.refreshImageView) void refresh(){
        loadWeather();
    }

    @OnClick(R.id.dailyButton)
    public void startDailyActivity(View view){
        if ( mForecast != null ) {
            Intent intent = new Intent(this, DailyForecastActivity.class);
            intent.putExtra(LOCATION_TAG, mCurrentAddress);
            intent.putExtra(DAILY_FORECAST_TAG, mForecast.getDailyForecast());
            startActivity(intent);
        }
    }

    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view){
        if ( mForecast != null ) {
            Intent intent = new Intent(this, HourlyForecastActivity.class);
            intent.putExtra(HOURLY_FORECAST_TAG, mForecast.getHourlyForecast());
            startActivity(intent);
        }
    }

    @Override
    public void handleNewLocation(Location location, String address) {
        Log.i(TAG, "New location: " + location);
        mCurrentLatitude = location.getLatitude();
        mCurrentLongitude = location.getLongitude();
        mCurrentAddress = address;

        // Load the weather
        loadWeather();
    }
}
