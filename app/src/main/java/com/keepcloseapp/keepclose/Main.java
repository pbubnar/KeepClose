package com.keepcloseapp.keepclose;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.MainThread;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.location.Location;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.Timer;



import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import com.google.android.gms.maps.model.Marker;
import com.oguzdev.circularfloatingactionmenu.library.*;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.*;

import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import com.keepcloselibs.*;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;


public class Main extends FragmentActivity {

    private static GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private PendingIntent pendingIntent;
    locationManagement locationServices;
    Location currentLocation = null;
    CognitoCachingCredentialsProvider credentialsProvider;
    AmazonDynamoDBClient ddbClient;
    static DynamoDBMapper dataMapper;
    static String userName;
    static kcMember user;
    Marker userMarker;
    Context mContext = this;
    int pingSetting =30000;
    List<Marker> friendMarker;
    Marker flMarker;
    Handler handler;
    Runnable drawRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO: Get rid of strict mode stuff because networking should be fixed
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        //TODO: Setup username/password recognition and get rid of hard coded username
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        friendMarker = new ArrayList<>();
        userName = "pbubnar";
        setUpMapIfNeeded();
        //TODO: Setup If needed version of FAB
        if(user.getGPS().equalsIgnoreCase("ON"))
            startAlarm();
        createFAB();
        startDraw();

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
        initializeAmazonComponents();
        startLocationMapping();

        LatLng startLL = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        CameraUpdate startLocation = CameraUpdateFactory.newLatLng(startLL);
        mMap.moveCamera(startLocation);
        mMap.getUiSettings().setMapToolbarEnabled(false);


    }

    public void startLocationMapping()
    {
        locationServices = new locationManagement();
        locationServices.createLocationServices(this);




        currentLocation = locationServices.getLastKnownLoc(this);
        if(currentLocation == null) {
            AlertDialog.Builder noLocationAlert = new AlertDialog.Builder(this);
            noLocationAlert.setTitle("Location Not Found");
            noLocationAlert.setMessage("Keep Close cannot determine your location, so it has to close.");
            noLocationAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                    System.exit(0);
                }
            });


        }

        initializeMarker(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));




    }

    public void redrawMarkers()
    {
        user = dataMapper.load(kcMember.class, userName);
        LatLng currentLatLng = new LatLng(Double.valueOf(user.getLat()), Double.valueOf(user.getLng()));
        userMarker.setPosition(currentLatLng);


        //Draw all users in same group
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.addFilterCondition("Group",
                new Condition()
                        .withComparisonOperator(ComparisonOperator.EQ)
                        .withAttributeValueList(new AttributeValue().withS(user.getGroup())));

        List<kcMember> scanResult = dataMapper.scan(kcMember.class, scanExpression);
        friendMarker.clear();
        for (kcMember friend : scanResult) {
            if(friend.getUserID().equalsIgnoreCase(user.getUserID())){}
            else{
                MarkerOptions fMarker = new MarkerOptions()
                        .position(new LatLng(Double.valueOf(friend.getLat()), Double.valueOf(friend.getLng())))
                        .title(friend.getUserID())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                flMarker = mMap.addMarker(fMarker);
                friendMarker.add(flMarker);
            }


        }


        handler.postDelayed(drawRunnable, pingSetting/2);

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



    public void  initializeMarker(LatLng currentLoc) {
        MarkerOptions a = new MarkerOptions()
                .position(currentLoc)
                .title(user.getUserID())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        userMarker = mMap.addMarker(a);
    }

    public void createFAB()
    {

        //Build FAB
        final ImageView menuFABIcon = new ImageView(this);
        // Set image of lower right corner image
        menuFABIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_more_vert_white_24dp));
        final FloatingActionButton rightLowerButton = new FloatingActionButton.Builder(this)
                .setContentView(menuFABIcon)
                .setBackgroundDrawable(R.drawable.button_action_red_touch)
                .build();



        //Build Sub buttons
        SubActionButton.Builder rLSubBuilder = new SubActionButton.Builder(this);
        final ImageView iconGPS = new ImageView(this);
        ImageView iconFriends = new ImageView(this);
        ImageView iconLogout = new ImageView(this);

        if(user.getGPS().equalsIgnoreCase("ON"))
            iconGPS.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_location_on_white_24dp));
        if(user.getGPS().equalsIgnoreCase("OFF"))
            iconGPS.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_location_off_white_24dp));
        iconFriends.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_group_add_white_24dp));
        iconLogout.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_exit_to_app_white_24dp));
        //attach sub buttons to FAB

        final FloatingActionMenu rightLowerMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(rLSubBuilder
                        .setContentView(iconGPS)
                        .setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.button_action_blue_touch))
                        .setLayoutParams(new FloatingActionButton.LayoutParams(125,125))
                        .build())
                .addSubActionView(rLSubBuilder
                        .setContentView(iconFriends)
                        .build())
                .addSubActionView(rLSubBuilder
                        .setContentView(iconLogout)
                        .build())
            .attachTo(rightLowerButton)
                .build();

        //On click listeners for sub buttons for actions
        iconGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if(user.getGPS().equalsIgnoreCase("ON")){
                    locationServices.turnOffGPS(mContext);
                    user.setGPS("OFF");
                    cancelAlarm();
                    dataMapper.save(user);
                    iconGPS.setImageResource(R.drawable.ic_location_off_white_24dp);
                    Toast.makeText(mContext, "GPS turned off", Toast.LENGTH_SHORT).show();
                }
                else if(user.getGPS().equalsIgnoreCase("OFF")) {
                    if(checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION")==PackageManager.PERMISSION_GRANTED)
                        locationServices.turnOnGPS(mContext);
                    else
                    {
                        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                        alert.setTitle("Permission Needed!");
                        alert.setMessage("Keep Close needs your location to show your friends where you are!");
                        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();
                                System.exit(0);
                            }
                        });
                    }
                    user.setGPS("ON");
                    dataMapper.save(user);
                    startAlarm();
                    iconGPS.setImageResource(R.drawable.ic_location_on_white_24dp);
                    Toast.makeText(mContext, "GPS turned on", Toast.LENGTH_SHORT).show();
                }
            }
        });
        iconFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Group Works", Toast.LENGTH_SHORT).show();
            }
        });
        iconLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Logout Works", Toast.LENGTH_SHORT).show();
            }
        });




    }



    public void startAlarm() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = pingSetting;

        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
        Log.w("ALARM","Started");
    }

    public void cancelAlarm() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
    }

    public void startDraw()
    {
        Log.w("HANDLER","Handler Created");
        handler = new Handler();
        drawRunnable = new Runnable(){
            public void run() {
                Log.w("HANDLER","Redraw executed");
                redrawMarkers();
            }
        };

        drawRunnable.run();
    }

}


