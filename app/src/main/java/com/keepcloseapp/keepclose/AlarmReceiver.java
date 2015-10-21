package com.keepcloseapp.keepclose;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.keepcloselibs.locationManagement;


public class AlarmReceiver extends BroadcastReceiver {



    @Override
    public void onReceive(Context context, Intent intent) {

        locationManagement locationServices = new locationManagement();
        locationServices.createLocationServices(context);



        locationServices.turnOnGPS(context);


        Log.w("ALARM", "Triggered Loc");
        Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
    }




}
