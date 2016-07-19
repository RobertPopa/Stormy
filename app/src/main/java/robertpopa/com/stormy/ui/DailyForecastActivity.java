package robertpopa.com.stormy.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import robertpopa.com.stormy.R;
import robertpopa.com.stormy.adapter.DayAdapter;
import robertpopa.com.stormy.weather.Day;

public class DailyForecastActivity extends ListActivity {
    private Day[] mDays;

    @BindView(R.id.locationLabel)
    TextView mLocationLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        Parcelable[] parcelables = intent.getParcelableArrayExtra(MainActivity.DAILY_FORECAST_TAG);
        mDays = Arrays.copyOf(parcelables, parcelables.length, Day[].class);

        DayAdapter adapter = new DayAdapter(this, mDays);
        setListAdapter(adapter);

        String location = intent.getStringExtra(MainActivity.LOCATION_TAG);
        mLocationLabel.setText(location);
    }

}
