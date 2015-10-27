package com.keepcloselibs;
import com.keepcloseapp.*;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.android.gms.maps.model.LatLng;
import com.keepcloseapp.keepclose.kcMember;

public class locationManagement {
    private LocationManager locationManager;
    private LocationListener locationListener;
    private kcMember user;
    private DynamoDBMapper dataMapper;
    private String userName;
    private Context mContext;

    public void createLocationServices(Context context) {
        mContext = context;


        userName = "pbubnar";
        initializeAmazonDataMapper(context);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Log.w("LOCATION_CHANGE", "LOCATION WAS JUST UPDATED");
                // Called when a new location is found by the network location provider
                makeUseOfNewLocation(location);

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
    }


    public void turnOnGPS(Context context)
    {
        final Context myContext = context;
        if (context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        else {
            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle("Permission Needed!");
            alert.setMessage("Keep Close needs your location to show your friends where you are!");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    ((Activity)myContext).finish();
                    System.exit(0);
                }
            });
        }
    }

    public void turnOffGPS(Context context)
    {
        if (context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED)
        locationManager.removeUpdates(locationListener);
    }

    public Location getLastKnownLoc(Context context)
    {
        Location lkLoc = null;

        if (context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED)
             lkLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lkLoc == null)
                    lkLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        return lkLoc;

    }








    private void makeUseOfNewLocation(Location currentLoc)
    {
        LatLng currentLatLng = new LatLng(currentLoc.getLatitude(),currentLoc.getLongitude());
        updateUserLoc(currentLatLng);
        turnOffGPS(mContext);

    }

    private void updateUserLoc(LatLng currentLoc)
    {

        Log.w("UPDATE_DB","Pushing user LatLng to DB for update");
        user.setLat((String.valueOf(currentLoc.latitude)));
        user.setLng((String.valueOf(currentLoc.longitude)));
        new locationUpdateDB().execute(user);
    }

    private void initializeAmazonDataMapper(Context context)
    {
        CognitoCachingCredentialsProvider credentialsProvider;
        AmazonDynamoDBClient ddbClient;

        credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                "us-east-1:65a4dd82-1a07-4d7a-8abc-ba08e7b1b1eb", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        dataMapper = new DynamoDBMapper(ddbClient);



    }


    private class locationUpdateDB extends AsyncTask<kcMember,Void,Void> {
        @Override
        protected Void doInBackground(kcMember...kcMembers) {

            kcMember asyncUser = dataMapper.load(kcMember.class,userName);
            asyncUser.setLat(kcMembers[0].getLat());
            asyncUser.setLng(kcMembers[0].getLng());
            dataMapper.save(asyncUser);
            return null;
        }
    }

}
