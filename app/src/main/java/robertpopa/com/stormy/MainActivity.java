package robertpopa.com.stormy;

import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.MessageFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import robertpopa.com.stormy.forecast.Constants;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ALERT_DIALOG_TAG = "alert_dialog";
    private CurrentWeather mCurrentWeather;

    @BindView(R.id.timeValue) TextView mTimeValue;
    @BindView(R.id.temperatureValue) TextView mTemperatureValue;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;

    @OnClick(R.id.refreshImageView) void refresh(){
        loadWeather();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mProgressBar.setVisibility(View.INVISIBLE);

        // Load the weather
        loadWeather();

    }

    private void loadWeather() {
        String forecastURL = Constants.BASE_URL + "/" + Constants.API_KEY + "/" + Constants.LATITUDE + "," + Constants.LONGITUDE;

        if ( isNetworkAvailable() ) {

            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastURL)
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
                            mCurrentWeather = getCurrentDetails(jsonData);
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
        mTemperatureValue.setText(String.valueOf(mCurrentWeather.getTemperature()));
        mTimeValue.setText(MessageFormat.format("At {0} it will be", mCurrentWeather.getFormattedTime()));
        mHumidityValue.setText(String.valueOf(mCurrentWeather.getHumidity()));
        mPrecipValue.setText(String.valueOf(mCurrentWeather.getPrecipChance() + "%"));
        mSummaryLabel.setText(mCurrentWeather.getSummary());

        Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject currently = forecast.getJSONObject("currently");
        CurrentWeather result = new CurrentWeather();
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

    public boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
        if ( activeNetworkInfo != null && activeNetworkInfo.isConnected() ) {
            return true;
        } else {
            return false;
        }
    }
}
