package com.samblancat.slowlog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    Context mContext;
    public static String gpxini = "slowlog.gpx";
    SharedPreferences sharedPref;
    public static int running=0, count=0, countok=0;
    PendingIntent pendingIntent=null;
    public static String path, path0;
    public static File gpx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mContext = this;
        Log.d("log", "Create ");

        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        //Retrouve last Position courante destination en cours ?
        running = sharedPref.getInt("run", 0);
        count = sharedPref.getInt("nb", 0);
        countok = sharedPref.getInt("nbok", 0);

        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }
        path = path0 +"/" + gpxini;
        gpx = new File(path);
        long ll = gpx.length();
        if ( ll <1 ) {
            Log.d("GPX :", "Creer file : " + path);
            Toast.makeText(mContext, "Empty file !", Toast.LENGTH_LONG).show();
            CreerGpx(path);
            count = 0;
            countok = 0;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("nb", count);
            editor.putInt("nbok", countok);
            editor.apply();
        }

        //Lance le Receiver d'Alarm manager (toutes les 30 secs)
        Intent intent = new Intent(mContext, MyReceiver.class);
        intent.putExtra("key", "Alert");
        pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar cal = Calendar.getInstance();
        long triggerInMillis = cal.getTimeInMillis()+30*1000;

        ImageButton img = findViewById(R.id.runbutt);
        if (running > 0) {
            img.setBackgroundResource(R.drawable.bouton_no);
            Animation animblink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
            img.startAnimation(animblink);
            //Lance Alarm / timer
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,  triggerInMillis, 30000, pendingIntent);
        } else {
            img.setBackgroundResource(R.drawable.bouton_ok);
            img.clearAnimation();
        }

        // Click Long pour Save New Waypoint ?
        img.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
            Toast.makeText(mContext, "Reset compteur !", Toast.LENGTH_LONG).show();
            sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
            //Remet a zero !
            count = 0;
            countok = 0;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("nb", count);
            editor.putInt("nbok", countok);
            editor.apply();
            TextView tx= findViewById(R.id.counttxt);
            tx.setText(countok+"/"+count +" points");
            //Efface fichier !
            if (gpx.exists()) gpx.delete();
            //Recrée à Zéro !
            CreerGpx(path);
            return false;
            }
        });

        TextView tx= findViewById(R.id.counttxt);
        tx.setText(countok+"/"+count +" points");

        MediaScannerConnection.scanFile(mContext, new String[]{gpx.getAbsolutePath()}, null, null);
    }


    @Override
    public void onResume() {
        //Auto-generated method stub
        super.onResume();
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        //Retrouve last Position courante destination en cours ?
        count = sharedPref.getInt("nb", 0);
        countok= sharedPref.getInt("nbok", 0);
        TextView tx= findViewById(R.id.counttxt);
        tx.setText(countok+"/"+count +" points");
        MediaScannerConnection.scanFile(mContext, new String[]{gpx.getAbsolutePath()}, null, null);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d("main", "onPause");
        //ASSURE FIN DU PROCESS !!!
     //   if (running<1) android.os.Process.killProcess(android.os.Process.myPid());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //ASSURE FIN DU PROCESS !!!
        if (running<1) android.os.Process.killProcess(android.os.Process.myPid());
    }


    //Run / Stopp le logging
    public void runcmd(View view) {
        ImageButton img = findViewById(R.id.runbutt);
        if (running == 0) {
            img.setBackgroundResource(R.drawable.bouton_no);
            running = 1;
            Animation animblink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
            img.startAnimation(animblink);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Calendar cal = Calendar.getInstance();
            long triggerInMillis = cal.getTimeInMillis()+30*1000;
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,  triggerInMillis, 30000, pendingIntent);
            Toast.makeText(mContext, "Recording !", Toast.LENGTH_LONG).show();
        } else {
            img.setBackgroundResource(R.drawable.bouton_ok);
            running = 0;
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            img.clearAnimation();
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("run", running);
        editor.apply();
    }



    public void CreerGpx(String path) {
        File gpx = new File(path);
        try {
            gpx.createNewFile();
            Log.d("TAG :", "create file ok");
            // true = append file
            FileWriter fw = new FileWriter(gpx.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n");
            bw.write("<gpx xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/11.xsd\"\r\n");
            bw.write("xmlns=\"http://www.topografix.com/GPX/1/1\"\r\n");
            bw.write("xmlns:ns3=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"\r\n");
            bw.write("xmlns:ns2=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"\r\n");
            bw.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
            bw.write("xmlns:ns1=\"http://www.cluetrust.com/XML/GPXDATA/1/0\"\r\n");
            bw.write("creator=\"Huami Amazfit Sports Watch\" version=\"1.1\">\r\n");
            bw.write("<metadata>\r\n");
            bw.write("<name>Amazfit GPS Locator</name>\r\n");
            bw.write("<author>\r\n");
            bw.write("<name>Samblancat G</name>\r\n");
            bw.write("</author><time>2020-06-12T10:52:19Z</time>\r\n");
            bw.write("</metadata>\r\n");
            bw.write("<trk>\r\n");
            bw.write("<name>Slow GPS Log</name>\r\n");
            bw.write("<trkseg>\r\n");

            bw.write("</trkseg>\r\n");
            bw.write("</trk>\r\n");
            bw.write("</gpx>\r\n");

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Sync la Media-connection pour visu sur Windows usb
        try {
            MediaScannerConnection.scanFile(mContext, new String[]{gpx.getAbsolutePath()}, null, null);
        } catch (Exception e) {
            Log.d("creer", "Mediascanner-bug");
        }
    }

}