package com.keepcloseapp.keepclose;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;

import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.location.Location;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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
    Boolean FABexists = false;
    Runnable drawRunnable;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        userName = preferences.getString("Username", null);
        editor = preferences.edit();


        setContentView(R.layout.activity_main);
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        friendMarker = new ArrayList<>();
        setUpMapIfNeeded();
        startDraw();


    }





    @Override
    protected void onResume(){
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

        new redrawUser().execute();







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
        ImageView iconSettings = new ImageView(this);

        if(user.getGPS().equalsIgnoreCase("ON"))
            iconGPS.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_location_on_white_24dp));
        if(user.getGPS().equalsIgnoreCase("OFF"))
            iconGPS.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_location_off_white_24dp));
        iconFriends.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_group_add_white_24dp));
        iconSettings.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_exit_to_app_white_24dp));
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
                        .setContentView(iconSettings)
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
                    new updateDB().execute(user);
                    iconGPS.setImageResource(R.drawable.ic_location_off_white_24dp);
                    Toast.makeText(mContext, "GPS turned off", Toast.LENGTH_SHORT).show();
                }
                else if(user.getGPS().equalsIgnoreCase("OFF")) {

                    locationServices.turnOnGPS(mContext);

                    user.setGPS("ON");
                    new updateDB().execute(user);
                    startAlarm();
                    iconGPS.setImageResource(R.drawable.ic_location_on_white_24dp);
                    Toast.makeText(mContext, "GPS turned on", Toast.LENGTH_SHORT).show();
                }
            }
        });
        iconFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder groupManager = new AlertDialog.Builder(mContext);
                groupManager.setTitle("Groups");
                groupManager.setMessage("Would you like to create or join a group?");
                groupManager.setNegativeButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface v, int i) {
                        new createGroup().execute(user);
                    }
                });
                groupManager.setPositiveButton("Join", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface v, int i) {
                        AlertDialog.Builder joinGroup = new AlertDialog.Builder(mContext);
                        final EditText input = new EditText(mContext);
                        input.setHint("Username to join");
                        joinGroup.setTitle("Join");
                        joinGroup.setMessage("Please enter the username of the creator of the group!");
                        joinGroup.setView(input);
                        joinGroup.setPositiveButton("Join!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface v, int i) {
                                new joinGroup().execute(input.getText().toString());
                            }
                        });
                        joinGroup.create();
                    }
                });
                groupManager.create();
//                Toast.makeText(mContext, "Group Popup", Toast.LENGTH_SHORT).show();
            }
        });
        iconSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Logging Out", Toast.LENGTH_SHORT).show();
                editor.clear();
                editor.commit();
                Intent intent = new Intent(mContext, Login.class);
                startActivity(intent);
                finish();
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
                redrawEverything();
            }
        };

        drawRunnable.run();
    }

    public void redrawEverything()
    {
        new redrawUser().execute();
        new redrawMarkers().execute();
        handler.postDelayed(drawRunnable, (pingSetting / 2)-200);
    }



    private class updateDB extends AsyncTask<kcMember,Void,Void> {
        @Override

        protected Void doInBackground(kcMember...kcMembers) {


            dataMapper.save(kcMembers[0]);


            return null;

        }
    }

    private class redrawMarkers extends AsyncTask<Void,Void,List<kcMember>> {
        @Override

        protected List<kcMember> doInBackground(Void...voids) {


            DynamoDBScanExpression asyncScanExpression = new DynamoDBScanExpression();
            asyncScanExpression.addFilterCondition("Group",
                    new Condition()
                            .withComparisonOperator(ComparisonOperator.EQ)
                            .withAttributeValueList(new AttributeValue().withS(user.getGroup())));
            return dataMapper.scan(kcMember.class, asyncScanExpression);





        }
        @Override
        protected void onPostExecute(List<kcMember> groupList) {

            friendMarker.clear();
            for (kcMember friend : groupList) {
                if(friend.getUserID().equalsIgnoreCase(user.getUserID())){}
                else{
                    MarkerOptions fMarker = new MarkerOptions()
                            .position(new LatLng(Double.valueOf(friend.getLat()), Double.valueOf(friend.getLng())))
                            .title(friend.getUserID())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    flMarker = mMap.addMarker(fMarker);
                    friendMarker.add(flMarker);
                }


            }



        }
    }

    private class redrawUser extends AsyncTask<Void,Void,kcMember> {
                @Override

                protected kcMember doInBackground(Void...voids) {


                    return dataMapper.load(kcMember.class, userName);


                }
                @Override
                protected void onPostExecute(kcMember asyncUser) {

                    user = asyncUser;

                    if(user == null)
                    {
                        editor.clear();
                        editor.commit();
                        Intent intent = new Intent(mContext, Login.class);
                        startActivity(intent);
                        finish();
                    }

                    LatLng currentLatLng = new LatLng(Double.valueOf(user.getLat()), Double.valueOf(user.getLng()));
                    if(userMarker != null)
                        userMarker.setPosition(currentLatLng);
                    else
                        initializeMarker(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
            if(!FABexists) {
                createFAB();
                if(user.getGPS().equalsIgnoreCase("ON"))
                    startAlarm();
                FABexists = true;
            }


        }
    }


    private class createGroup extends AsyncTask<kcMember,Void,Void> {
        @Override

        protected Void doInBackground(kcMember...kcMembers) {

            kcMembers[0].setGroup(kcMembers[0].getUserID());
            dataMapper.save(kcMembers[0]);


            return null;

        }
    }
    private class joinGroup extends AsyncTask<String,Void, Boolean> {
        @Override

        protected Boolean doInBackground(String...strings) {

            kcMember asyncuser = dataMapper.load(kcMember.class, userName);
            asyncuser.setGroup(strings[0]);
            dataMapper.save(asyncuser);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success)
            Toast.makeText(mContext,"Group joined successfully!",Toast.LENGTH_SHORT);
        }

    }

    private class leaveGroup extends AsyncTask<kcMember,Void,Void> {
        @Override

        protected Void doInBackground(kcMember...kcMembers) {

            kcMembers[0].setGroup("Null");
            dataMapper.save(kcMembers[0]);


            return null;

        }
    }




}



