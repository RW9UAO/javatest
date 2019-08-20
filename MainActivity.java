package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.R.layout.simple_list_item_1;
import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.util.Log.d;
import static android.util.Log.e;
import static java.lang.String.format;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_BT_ENABLE = 1;
    private static final ParcelUuid UID_SERVICE =
            //ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb");
            ParcelUuid.fromString("00002900-0000-1000-8000-00805f9b34fb");

    //private BluetoothAdapter bluetoothAdapter;

    private Widget w;
    private Logic l;
    private Intent intent;

    private boolean ifound = false;
    String MACaddress;
//    Handler h;

//    @SuppressLint("HandlerLeak")
    private final Handler h = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 11:
                    l.ble.scan_stop();
                    Bundle b = new Bundle();
                    b.putString("MAC", MACaddress);
                    intent.putExtras(b);
                    startActivity(intent);
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intent = new Intent(this, ConfigureReceiver.class);

        w = new Widget(
                ((Button) findViewById(R.id.clear)),
                ((TextView) findViewById(R.id.status)),
                ((ListView) findViewById(R.id.devices))
        );

        /*
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, PERMISSION_BT_ENABLE);
        }
*/

        l = new Logic();

        requestPermissions(new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_COARSE_LOCATION);

        w.switchBle.setOnClickListener(view -> {
            l.view.clearDevicesList();
        });

        l.ble.scan();

        //myThread.start();
    }
/*    Thread myThread = new Thread( // создаём новый поток
            new Runnable() { // описываем объект Runnable в конструкторе
                public void run() {
                    while ( ! ifound ) {
                        try{
                            Thread.sleep(10);
                        }
                        catch(InterruptedException ex){
                            Thread.currentThread().interrupt();
                        }
                    }
                    l.ble.scan_stop();
                    if ( ifound && intent != null) {
                        //startActivity(intent); <- work, bt not correct
                        runOnUiThread(() -> startActivity(intent));
                    }
                }
            }
    );*/
//-----------------------------------------------------------
    private class Widget {
        public final Button switchBle;
        public final TextView status;
        public final ListView devices;

        public Widget(Button switchBle, TextView status, ListView devices) {
            this.switchBle = switchBle;
            this.status = status;
            this.devices = devices;
        }
    }

    private class Logic {
        public final LogicBle ble = new LogicBle();
        public final LogicView view = new LogicView();
    }

    private class LogicBle {
        private final BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        private final BluetoothAdapter adapter = Objects.requireNonNull(manager).getAdapter();
        private final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

        private HashMap<String, ScanResult> devicesMap = new HashMap<>();

        List<ScanFilter> filters = new ArrayList<>();

        private ScanCallback mLeScanCallback = new ScanCallback(){
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ifound = l.view.addDeviceToList(result);
                if(ifound){
                    h.sendEmptyMessage(11);
                }
                d("SCAN", result.toString());
            }
            @Override
            public void onBatchScanResults(final List<ScanResult> results) {
                Log.e("SCAN", "batch");
                for(int i = 0; i < results.size(); i++) {
                    ScanResult result = results.get(i);
                    ifound = l.view.addDeviceToList(result);
                    d("SCAN", result.toString());
                }
            }
        };
        public void scan_stop(){
            if(scanner != null){
                scanner.stopScan(mLeScanCallback);
            }
        };
        public void scan() {
            if (adapter == null || !adapter.isEnabled()) {
                Log.d("SCAN", "RQST");
                //Intent enableBtIntent = new Intent(ACTION_REQUEST_ENABLE);
                //startActivityForResult(enableBtIntent, PERMISSION_BT_ENABLE);
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
            if(scanner != null){
                // full scan
                //scanner.startScan(mLeScanCallback);
                //scanner.startScan(new ScanCallback() {

                // scan with UUID filter
//                ScanFilter beaconFilter = new ScanFilter.Builder()
//                        .setServiceUuid(UID_SERVICE).build();
//                filters.add(beaconFilter);
                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                scanner.startScan(null, settings, mLeScanCallback);
//                scanner.startScan(filters, settings, new ScanCallback() {

                    /*@Override
                    public void onScanFailed(int errorCode) {
                        e("Scanner", "Scan Fail");
                    }*/
/*
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        ifound = l.view.addDeviceToList(result);
                        d("SCAN", result.toString());
                    }
                });*/
                //if(ifound){
                //    scanner.stopScan(leCallback()); //.stopScan(new ScanCallback(){});
                //}
            }/*else{
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Please enable BT", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }*/
        }
    }

    private class LogicView {
        private static final String
                TEMPLATE_DEVICE_ITEM_LIST =
                "[%d] %s\n%s",
                NA = "n/a";

        private ArrayAdapter<String> devicesAdapter = new ArrayAdapter<>(
                getApplicationContext(), simple_list_item_1);

        private HashMap<String, ScanResult> devicesMap = new HashMap<>();

        private String nameIsBlankThenNoneAvailable(String str) {
            return str != null && !str.isEmpty() ? str : LogicView.NA;
        }

        public LogicView() {
            w.devices.setAdapter(devicesAdapter);
/*
            w.devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
                                        long id) {
                    Toast.makeText(getApplicationContext(), ((TextView) itemClicked).getText(),
                            Toast.LENGTH_SHORT).show();
                }
            });*/
        }

        public void clearDevicesList() {
            devicesMap.clear();
            devicesAdapter.clear();
            devicesAdapter.notifyDataSetChanged();
        }

        @SuppressLint("DefaultLocale")
        public boolean addDeviceToList(ScanResult scanResult) {
            boolean value = false;
            byte[] rawData;
            String s;
            if (devicesMap.keySet().stream().noneMatch(it -> it.equals(scanResult.getDevice().getAddress()))) {
                int rssi = scanResult.getRssi();
                String name = scanResult.getDevice().getName();
                String address = scanResult.getDevice().getAddress();
                //ParcelUuid[] data =  scanResult.getDevice().getUuids();

                devicesMap.put(address, scanResult);
                devicesAdapter.add(format(TEMPLATE_DEVICE_ITEM_LIST,
                        rssi, nameIsBlankThenNoneAvailable(name), address));
                devicesAdapter.notifyDataSetChanged();

                //String UUIDx = UUID.nameUUIDFromBytes(scanResult.getScanRecord().getBytes()).toString();
                //String UUIDx = scanResult.getScanRecord().getServiceData().toString();
                //Log.e("UUID", " as String ->>" + UUIDx);

                rawData = scanResult.getScanRecord().getServiceData(UID_SERVICE);
                if (rawData != null) {
                    s = new String(rawData);
                    Log.e("UUID", s);
                    if(s.startsWith("BEACON") || s.startsWith("IDLE") ){
                        Log.e("UUID", "beacon found");
                        //startActivity(new Intent(this, ConfigureReceiver.class));
                        //Intent intent = new Intent(this, ConfigureReceiver.class);
                        //l.ble.scanner.stopScan(new ScanCallback(){});
                        //startActivity(intent);
                        value = true;
                        MACaddress = scanResult.getDevice().getAddress();
                    }else{
                        w.status.setText("not BEACON");
                    }
                }
                //for (int startByte = 0; startByte < rawData.length; startByte++) {
                    //if (rawData[startByte+0] == 0xaa && rawData[startByte+1] == 0xfe &&
                    //        rawData[startByte+2] == 0x00) {
                    //    Log.e("UUID","FOUND!");
                    //}
                //}
            }
            return value;
        }
    }


}
