package com.keepcloseapp.keepclose;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import com.keepcloseapp.keepclose.kcMember;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.*;

import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class Main extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    LocationManager locationManager;
    Location currentLocation = null;
    LocationListener locationListener;
    CognitoCachingCredentialsProvider credentialsProvider;
    AmazonDynamoDBClient ddbClient;
    DynamoDBMapper dataMapper;
    String userName;
    kcMember user;
    // Define a listener that responds to location updates



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO: Get rid of strict mode stuff because networking should be fixed
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        //TODO: Setup username/password recognition and get rid of hard coded username
        userName = "pbubnar";
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    private void setUpMap() {

        // Acquire a reference to the system Location Manager
        startLocationMapping();
        initializeAmazonComponents();

        LatLng startLL = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        CameraUpdate startLocation = CameraUpdateFactory.newLatLng(startLL);
        updateDBLocation(startLL);
        mMap.moveCamera(startLocation);


    }

    public void startLocationMapping()
    {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener(){
                public void onLocationChanged(Location location) {
                    Log.w("LOCATION_CHANGE", "LOCATION WAS JUST UPDATED");
                    // Called when a new location is found by the network location provider
                    makeUseOfNewLocation(location);
                    currentLocation = location;
                }
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            if(checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION")==PackageManager.PERMISSION_GRANTED)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            else
            {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Permission Needed!");
                alert.setMessage("Keep Close needs your location to show your friends where you are!");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                        System.exit(0);
                    }
                });
            }
        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


    }

    public void makeUseOfNewLocation(Location currentLoc)
    {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    public void initializeAmazonComponents()
    {
        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:65a4dd82-1a07-4d7a-8abc-ba08e7b1b1eb", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        dataMapper = new DynamoDBMapper(ddbClient);

        //TODO:Move network operations to seperate thread
        user = dataMapper.load(kcMember.class,userName);

    }

    public void updateDBLocation(LatLng currentLoc)
    {
        Log.w("UPDATE_DB","Pushing user LatLng to DB for update");
        user.setLat((String.valueOf(currentLoc.latitude)));
        user.setLng((String.valueOf(currentLoc.longitude)));
        dataMapper.save(user);
    }


}
