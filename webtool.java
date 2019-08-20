package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import static android.os.Environment.DIRECTORY_DOWNLOADS;



public class webtool extends AsyncTask<String , Integer , Integer> {

    private static final String PATH_FOLDER = "/OrangeRX";
    //TextView StatustextView = ;

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
//        mInfoTextView.setText("Этаж: " + values[0]);
//        StatustextView.setText(" ");
    }
    @Override
    protected Integer doInBackground(String... params) {
        int result = 0;
        int count;
        String[] fname;
        try {
//            Log.e("HTTPS", " START ");
            URL url = new URL("https://rw9uao.github.io/firmware/files.xml");
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setHostnameVerifier(hostnameVerifier);
            urlConnection.connect();

            publishProgress(0);

            if (urlConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                InputStream input = new BufferedInputStream(url.openStream());
                //Log.e("XML", " START ");
                //Log.e("XML", "["+params[0]+"]");
                //if(params[0].equals("R617X")){
                //    Log.e("XML","HID okay");
                //}
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new InputStreamReader(input));
                    while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                        if (parser.getEventType() == XmlPullParser.START_TAG
                                && parser.getName().equals("file")) {
                            //Log.d("XML",parser.getAttributeValue(0) + " "
                            //        + parser.getAttributeValue(1) + " " );
                            fname = parser.getAttributeValue(0).split("_");
                            //Log.d("XML","HID: " + fname[0] + " SID: " + fname[1] );
                            if( fname[0].equals( params[0] ) ){
                                Log.e("XML","HID found");
                            }
                        }
                        parser.next();
                    }
                }catch (Throwable t) {
                    Log.e("XML","Ошибка при загрузке XML-документа: " + t.toString() );
                }
/*
                    String strInfo = "";
                    File[] externalFilesDirs = getExternalFilesDirs(DIRECTORY_DOWNLOADS);
                    strInfo += "\ngetExternalFilesDirs(null):\n";
                    for(File f : externalFilesDirs){
                        strInfo += f.getAbsolutePath() + "\n";
                    }
                    Log.e("HTTPS", strInfo);*/

                String fullPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getAbsolutePath() + PATH_FOLDER;
//                    File internalDcimDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
                File internalDcimDir = new File(fullPath);
                Log.e("myLogs", fullPath);
                if(! internalDcimDir.exists() ){
                    internalDcimDir.mkdirs();
                }
                OutputStream output = new FileOutputStream(internalDcimDir + "/index.txt");

                byte data[] = new byte[1024];

                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                //Log.e("myLogs", "Файл записан");
                urlConnection.disconnect();
            }else{
                int responseCode = urlConnection.getResponseCode();
                Log.e("myLogs", " HTTP response: " + responseCode);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
            //Log.e("verify", hostname);
            return hv.verify(hostname, session);
        }
    };
}