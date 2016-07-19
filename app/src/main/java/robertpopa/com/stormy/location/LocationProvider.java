package robertpopa.com.stormy.location;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by RobertP on 19/07/16.
 */
public class LocationProvider implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    public abstract interface LocationCallback{
        public void handleNewLocation(Location location, String address);
    }

    private static final String TAG = LocationProvider.class.getSimpleName();
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private Context mContext;
    private LocationCallback mCallback;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLocation;

    public LocationProvider(Context context, LocationCallback callback) {
        mContext = context;
        mCallback = callback;

        // Create new Google API client
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        // Create new location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (  location == null ){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            mLocation = location;
            startIntentService(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if ( connectionResult.hasResolution() && mContext instanceof Activity ){
            Activity activity = (Activity) mContext;
            try {
                connectionResult.startResolutionForResult(activity, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    public void connect(){
        mGoogleApiClient.connect();
    }

    public void disconnect(){
        if ( mGoogleApiClient.isConnected() ) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    public boolean hasLocation(){
        return mLocation == null ? false : true;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
        startIntentService(mLocation);
    }

    protected void startIntentService(Location location) {
        Intent intent = new Intent(mContext, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, new AddressResultReceiver(new Handler()));
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        mContext.startService(intent);
    }

    private class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            String address = resultData.getString(Constants.RESULT_DATA_KEY);

            // Send back the response
            mCallback.handleNewLocation(mLocation, address);
        }
    }

}
