package com.augmentis.ayp.findlocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.SharedPreferencesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class FindLocationActivityFragment extends Fragment {

    private static final String TAG = "FindLocationF";
    private static final int REQUEST_PERM_LOCATION_ACCESS_LOC = 888;
    private Location mLocation;

    public FindLocationActivityFragment() {
    }

    private TextView mLongtitudeText;
    private TextView mLatitudeText;
    private boolean mHasFinePermission;
    private GoogleApiClient mGoogleApiClient;
    private boolean mGoogleApiConnected;
    private boolean mUsingFuse;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiConnected = false;

        mUsingFuse = LocationPref.getSharedPref(getActivity(),
                LocationPref.PREF_USE_FUSE);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiConnected = false;
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        mGoogleApiConnected = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        mUsingFuse = LocationPref.getSharedPref(getActivity(), LocationPref.PREF_USE_FUSE);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_find_location, container, false);

        mLongtitudeText = (TextView) v.findViewById(R.id.longitude_text);
        mLatitudeText = (TextView) v.findViewById(R.id.latitude_text);

        updateLocation();

        return v;
    }

    private void updateLocation() {
        if(hasPermission()) {
            requestLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if(requestCode == REQUEST_PERM_LOCATION_ACCESS_LOC) {

            if(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "Fine-Location-Permission accepted");

                // update when done
                updateLocation();
            }
        }
    }

    private boolean hasPermission() {
        int permissionFineStatus =
                ContextCompat.checkSelfPermission(
                        getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION);

        mHasFinePermission = permissionFineStatus == PackageManager.PERMISSION_GRANTED;
        List<String> requestPermissions = new ArrayList<>();
        if(!mHasFinePermission) {
            requestPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if(requestPermissions.size() > 0) {
            requestPermissions(
                    requestPermissions.toArray(new String[] {}),
                    REQUEST_PERM_LOCATION_ACCESS_LOC);
        }

        Log.d(TAG, "hasFinePermission: " + mHasFinePermission);
        return mHasFinePermission;
    }

    @SuppressWarnings("ALL")
    protected void requestLocation() {
        Log.d(TAG, "REQUEST Location");

        LocationManager locationManager = (LocationManager)
                getActivity().getSystemService(Context.LOCATION_SERVICE);

        locationManager.removeUpdates(mLocationListener);

        Log.d(TAG, "Google API Check = " + mGoogleApiClient + ", Fuse check = " + mUsingFuse);
        if(mGoogleApiConnected && mUsingFuse) {
            Log.d(TAG, "From FuseLocationAPI");

            LocationRequest request = LocationRequest.create();
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            request.setNumUpdates(1000);
            request.setInterval(1000);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    request, new com.google.android.gms.location.LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            Log.d(TAG, "Got location from Fuse");
                            setLocationOutput(location);
                        }
                    });

        } else {
            Log.d(TAG, "From LocationManager");
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = locationManager.getBestProvider(criteria, true);
            Log.d(TAG, "LocationManager Using " + provider);
            locationManager.requestLocationUpdates(provider, 1000, 0.0f, mLocationListener);
        }
    }

    GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, "Google API connected");
            mGoogleApiConnected = true;
            updateLocation();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "Google API connection suspended");
            mGoogleApiConnected = false;
        }
    };

    private void setLocationOutput(Location location) {
        mLongtitudeText.setText(String.valueOf(location.getLongitude()));
        mLatitudeText.setText(String.valueOf(location.getLatitude()));
    }

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            Log.d(TAG, "Got location from Location Manager");
            setLocationOutput(location);
        }

        @Override
        public void onStatusChanged(String provideName, int status, Bundle bundle) {
            String locationStatus = "";

            switch (status) {
                case LocationProvider.AVAILABLE :
                    locationStatus = "available";
                    break;

                case LocationProvider.OUT_OF_SERVICE:
                    locationStatus = "out_of_service";
                    break;

                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    locationStatus = "temporarily_unavailable";
                    break;
            }
            Log.d(TAG, "Provider: " + provideName + " : status changed -> " + locationStatus);
        }

        @Override
        public void onProviderEnabled(String provideName) {
            Log.d(TAG, "Provider: " + provideName + " has been enabled");
        }

        @Override
        public void onProviderDisabled(String provideName) {
            Log.d(TAG, "Provider: " + provideName + " has been disabled");
        }
    };

}
