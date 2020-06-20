package com.samblancat.slowlog;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MyReceiver extends BroadcastReceiver {
    public static LocationManager locationManager;
    public static MyLocationListener listener;
    public static double mylat=0, mylng=0, myAlt=0;
    public int MINUTIME = 20;
    public int HR_DEB = 7;
    public int HR_FIN = 20;
    SharedPreferences sharedPref;
    public Context Ctxt;

    @Override
    public void onReceive(Context context, Intent intent) {
        int minut = Calendar.getInstance().get(Calendar.MINUTE);
        int heure = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        Ctxt = context;

        //Test si délai période passée
        if ( ((minut % MINUTIME)==0)&&(heure>HR_DEB)&&(heure<HR_FIN) ) {
            //Sauve la Position et nb point
            sharedPref = Ctxt.getSharedPreferences("POSPREFS", Ctxt.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            int x = sharedPref.getInt("nb", 0);
            editor.putInt("nb", ++x);
            editor.apply();

            //Lance le GPS !!!
            mylng=0;
            mylat=0;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
                listener = new MyLocationListener();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
                Log.d("rec", "gps Start");
            }
        } else {
            //Si pas TIMEOUT -> test si éteint quand même GPS
            //Stoppe le GPS
            try { locationManager.removeUpdates(listener); }
            catch (Exception e) { }
            listener = null;
            Log.d("rec", "gps stopped / no REC");
        }
    }

    public class MyLocationListener implements LocationListener {
        public void onLocationChanged(final Location loc) {
            Log.d("rec", "gps Receive");
            //essaie de capter Position
            mylat = loc.getLatitude();
            mylng = loc.getLongitude();
            myAlt = loc.getAltitude();

            //Sauve la Position et nb point
            sharedPref = Ctxt.getSharedPreferences("POSPREFS", Ctxt.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putFloat("llat", (float) mylat);
            editor.putFloat("llng", (float) mylng);
            //Sauve nb de Positions OK !
            int x = sharedPref.getInt("nbok", 0);
            editor.putInt("nbok", ++x);
            editor.apply();

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd/HH:mm:ss");
            String formatdate = df.format(c.getTime());
            //prend mylat/lng qui sont filtrés du 0.0
            appendGPX(mylat, mylng, myAlt, formatdate);

            //Stoppe le GPS
            locationManager.removeUpdates(listener);
            listener = null;
            Log.d("rec", "gps stopped / ok");
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
        public void onProviderDisabled(String provider) {

        }
        public void onProviderEnabled(String provider) {

        }
    };


    public void appendGPX(Double la, Double lo, Double ele, String nom){
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(Ctxt, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }
        String path = path0 + "/slowlog.gpx";

        File gpx = new File(path);
        String path2 = path0 + "/gpslocator.tmp";
        File gpx2 = new File(path2);

        FileWriter fw = null;
        try {
            fw = new FileWriter(gpx2.getAbsoluteFile(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fw);
        try {
            BufferedReader br = new BufferedReader(new FileReader(gpx));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length()>8) {
                    if (line.substring(0, 8).equals("</trkseg")) {
                        //Insertion du wpt a la fin
                        bw.write("<trkpt lat=\""+la.toString()+"\" lon=\""+lo.toString()+"\">\r\n");
                        bw.write("<ele>"+ele.toString()+"</ele>\r\n");
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        String formatdate = df.format(c.getTime());
                        df = new SimpleDateFormat("HH:mm:ss");
                        formatdate = formatdate+"T"+df.format(c.getTime());
                        bw.write("<time>"+formatdate+"Z</time>\r\n");
                        bw.write("<name>"+nom+"</name>\r\n");
                        bw.write("</trkpt>\r\n");
                    }
                }
                bw.write(line+"\r\n");
            }
            br.close();
            bw.close();
            if (fw != null) fw.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }
        //efface le source
        boolean b =gpx.delete();
        //renomme le tmp en Gps
        b = gpx2.renameTo(gpx);
        if (gpx2.exists()) b=gpx2.delete();
    }

}