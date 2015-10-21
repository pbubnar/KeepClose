package com.keepcloseapp.keepclose;




import android.util.Log;
import java.util.TimerTask;


public class DrawTimerTask extends TimerTask {




    @Override
    public void run() {

        Main.redrawMarkers();


        Log.w("ALARM", "Triggered Draw ");

    }



}
