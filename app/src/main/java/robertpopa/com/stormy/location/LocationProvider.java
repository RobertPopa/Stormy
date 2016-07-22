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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import robertpopa.com.stormy.ui.MainActivity;

/**
 * Created by RobertP on 19/07/16.
 */
public class LocationProvider implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public abstract interface LocationCallback{
        public void handleNewLocation(Location location, String address);
    }

    private static final String TAG = LocationProvider.class.getSimpleName();
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final int REQUEST_CHECK_SETTINGS = 0x1;

    private Context mContext;
    private LocationCallback mCallback;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private Location mLocation;

    public LocationProvider(Context context, LocationCallback callback) {
        mContext = context;
        mCallback = callback;

        // Build new Google API client
        buildGoogleApiClient(context);

        // Create new location request
        createLocationRequest();

        // Build new location settings request
        buildLocationSettingsRequest();
    }

    private void buildGoogleApiClient(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
    }

    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);
    }

    private void buildLocationSettingsRequest() {
        mLocationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.i(TAG, "Location: " + mLocation);
        if (  mLocation == null ){
            checkLocationSettings();
        } else {
            startIntentService(mLocation);
        }
    }

    private void checkLocationSettings() {
        Log.i(TAG, "Checking location settings.");
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult((MainActivity)mCallback, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                                "not created.");
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
        connect();
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

    public void startLocationUpdate() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
}
