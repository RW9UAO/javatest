package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRadioButton;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;
import static android.util.Log.d;


public class ConfigureReceiver extends AppCompatActivity {

    //RadioButton rb;
    BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    AdvertiseSettings settings;
    AdvertiseData data;
    ParcelUuid pUuid;
    BluetoothManager manager;
    BluetoothAdapter adapter;
    BluetoothLeScanner scanner;
    List<ScanFilter> filters;
    Spinner uartSpinner;
    Spinner autobindSpinner;

    private static final ParcelUuid UID_SERVICE =
            ParcelUuid.fromString("00002900-0000-1000-8000-00805f9b34fb");

    String payloadSTART = "START     ";
    String payloadACK   = "ACK       ";
    String payloadIDLE  = "IDLE      ";
    String payloadGETC  = "GET_C     ";
    String payloadGETH  = "GET_H     ";
    String payloadGETS  = "GET_S     ";
    String payloadBEACON="BEACON";

    TextView StatustextView;
    TextView TextViewMACv;
    TextView TextViewHIDv;
    TextView TextViewSIDv;
    Button Buttonreread;

    String MACaddress;
    String receiver_config;
//    Handler CRh;

    final int STATE_POWER_ON    = 0;
    final int STATE_IDLE        = 1;
    final int STATE_SEND_IDLE   = 2;
    final int STATE_SEND_START  = 3;
    final int STATE_SEND_ACK    = 4;
    final int STATE_SEND_GETH    = 5;
    final int STATE_SEND_GETS    = 6;
    final int STATE_SEND_GETC    = 7;
    boolean have_config = false;
    boolean have_HID = false;
    boolean have_SID = false;

    public int StateMachine = STATE_POWER_ON;

    @SuppressLint("HandlerLeak")
    private final Handler h = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case STATE_POWER_ON:
                    break;
                case STATE_IDLE:
                    //StatustextView.setText("state IDLE");
                    if(have_HID == false){
                        advSetPayload(payloadGETH);
                        StateMachine = STATE_SEND_GETH;
                    }else
                    if(have_SID == false){
                        advSetPayload(payloadGETS);
                        StateMachine = STATE_SEND_GETS;
                    }else
                    if(have_config == false){
                        advSetPayload(payloadGETC);
                        StateMachine = STATE_SEND_GETC;
                    }else{
                        //advSetPayload(payloadIDLE);
                    }
                    break;
                case STATE_SEND_START:
                    advSetPayload(payloadSTART);
                    break;
                case STATE_SEND_IDLE:
                    advSetPayload(payloadIDLE);
                    break;
                case STATE_SEND_ACK:
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_receiver);
        StatustextView = findViewById(R.id.StatusTextView);
        TextViewMACv = findViewById(R.id.TextViewMACv);
        TextViewHIDv = findViewById(R.id.TextViewHIDv);
        TextViewSIDv = findViewById(R.id.TextViewSIDv);
        Buttonreread = findViewById(R.id.reread);


        makespinners();

        Bundle b = getIntent().getExtras();
        if( b != null){
            MACaddress = b.getString("MAC");
            TextViewMACv.setText(MACaddress);
        }

        //rb.setHighlightColor(GREEN);
        //rb.setTextColor(GREEN);
        //rb.setChecked(true);
        //rb.setChecked(false);
        //rb.setEnabled(true);

        Buttonreread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                have_config = false;
                advSetPayload(payloadGETC);
                StateMachine = STATE_SEND_GETC;
            }
        });
        uartSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View itemSelected,
                                       int selectedItemPosition, long id) {
                //Log.v("item", (String) parent.getItemAtPosition(selectedItemPosition));
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        autobindSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View itemSelected,
                                       int selectedItemPosition, long id) {
                //Log.v("item", (String) parent.getItemAtPosition(selectedItemPosition));
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        have_config = false;
        have_HID = false;
        have_SID = false;

        scan();

        if(StateMachine == 0){
            StateMachine = STATE_SEND_START;
            advStart(payloadSTART);
            //StatustextView.setText(payloadSTART);
        }
    }

    public void makespinners(){
        uartSpinner = (Spinner) findViewById(R.id.uart_spinner);
        autobindSpinner = (Spinner) findViewById(R.id.autobind_spinner);

        // Create an ArrayAdapter using the string array and a default spinner
        ArrayAdapter<?> uartAdapter = ArrayAdapter
                .createFromResource(this, R.array.uart_types_array,
                        android.R.layout.simple_spinner_item);
        ArrayAdapter<?> autobindAdapter = ArrayAdapter
                .createFromResource(this, R.array.autobind_types_array,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        uartAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autobindAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        uartSpinner.setAdapter(uartAdapter);
        autobindSpinner.setAdapter(autobindAdapter);

        uartSpinner.setEnabled(false);
        autobindSpinner.setEnabled(false);

        //uartSpinner.setSelection(2);    // set position
    }
    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.e( "BLE", "Advertising START" );
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
            StatustextView.setText("error: " + errorCode);
            super.onStartFailure(errorCode);
        }
    };

    public void advSetPayload(String payload){
        advertiser.stopAdvertising(advertisingCallback);
        data = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .addServiceUuid( pUuid )
                .addServiceData( pUuid, payload.getBytes( Charset.forName( "UTF-8" ) ) )
                .build();
        advertiser.startAdvertising( settings, data, advertisingCallback );
        StatustextView.setText(payload);
    }

    public void advStart(String payload){
        settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .setConnectable( false )
//                .setTimeout(1000)   // in ms
                .build();

        pUuid = new ParcelUuid( UUID.fromString( getString( R.string.ble_uuid ) ) );

        data = new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .addServiceUuid( pUuid )
                .addServiceData( pUuid, payload.getBytes( Charset.forName( "UTF-8" ) ) )
                .build();

        advertiser.startAdvertising( settings, data, advertisingCallback );
        StatustextView.setText(payload);
    };
//    public void advStop(){
//        advertiser.stopAdvertising(advertisingCallback);
//    };


//    private HashMap<String, ScanResult> devicesMap = new HashMap<>();

    private ScanCallback mLeScanCallback = new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            parceBLE(result);
            //d("SCAN", result.toString());
        }
        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            Log.e("SCAN", "batch");
            for(int i = 0; i < results.size(); i++) {
                ScanResult result = results.get(i);
                //ifound = l.view.addDeviceToList(result);
                d("SCAN", result.toString());
            }
        }
    };
    public void scan() {
        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = Objects.requireNonNull(manager).getAdapter();
        scanner = adapter.getBluetoothLeScanner();
        filters = new ArrayList<>();

        // full scan
//        scanner.startScan(mLeScanCallback);

        // scan with UUID filter
//        ScanFilter beaconFilter = new ScanFilter.Builder()
//                .setServiceUuid(UID_SERVICE).build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
/*        ScanFilter beaconFilter = new ScanFilter.Builder()
                .setDeviceAddress(macAddress)
                .build();

        beaconFilter = ScanFilter.Builder()
                .setServiceUuid(BluetoothBlind.Service.ParcelUUID)
                .build();*/

        ScanFilter.Builder beaconFilter = new ScanFilter.Builder();
        //beaconFilter.setServiceUuid(UID_SERVICE);
        beaconFilter.setDeviceAddress(MACaddress);

        filters.add(beaconFilter.build());
        scanner.startScan(filters, settings, mLeScanCallback);
//        scanner.startScan(null, settings, mLeScanCallback);

    }
    public void parceBLE(ScanResult scanResult) {
        byte[] rawData;
        String s;
        rawData = scanResult.getScanRecord().getServiceData(UID_SERVICE);
        if (rawData != null) {
            //rb.setHighlightColor(GREEN);
            //rb.setTextColor(GREEN);
            //rb.setChecked(true);
            //rb.invalidate();
            s = new String(rawData);
            //Log.e("UUID", s);
            if (s.startsWith("BEACON")) {
                if(StateMachine == 0 || StateMachine == 1){
                    StateMachine = STATE_SEND_START;
                    h.sendEmptyMessage(STATE_SEND_START);
                }
            }
            if (s.startsWith("IDLE")) {
                if(StateMachine != 1 ) {    // != STATE_IDLE
                    StateMachine = STATE_IDLE;
                    h.sendEmptyMessage(STATE_SEND_IDLE);
                }
                if(StateMachine == 1){
                    h.sendEmptyMessage(STATE_IDLE);
                }
            }
            if(StateMachine == 5){//STATE_SEND_GETH
                if(rawData[0] == (byte)'H'){
                    String hid = s.substring(1);
                    // remove spaces
                    hid = hid.replaceAll("\\s", "");
                    TextViewHIDv.setText(hid);
                    have_HID = true;
                    StateMachine = STATE_IDLE;
                    h.sendEmptyMessage(STATE_IDLE);
                    // look for update
                    new webtool().execute(hid);
                }
            }
            if(StateMachine == 6){//STATE_SEND_GETS
                if(rawData[0] == (byte)'S'){
                    TextViewSIDv.setText(s.substring(1));
                    have_SID = true;
                    StateMachine = STATE_IDLE;
                    h.sendEmptyMessage(STATE_IDLE);
                }
            }
            if(StateMachine == 7){//STATE_SEND_GETC
                if(rawData[0] == (byte)'C'){
                    //receiver_config = s;
                    have_config = true;
                    StateMachine = STATE_IDLE;
                    h.sendEmptyMessage(STATE_SEND_IDLE);
                    ParceReceiverConfig(rawData);
                }
            }
        }/*else{
            rb.setTextColor(GRAY);
            //rb.setChecked(false);
            rb.invalidate();
        }*/
    }
    public void ParceReceiverConfig(byte[] rawData){
        int t;

        t = rawData[1] - 0x30;
        if(t < 5){
            uartSpinner.setEnabled(true);
            uartSpinner.setSelection(t);
        }

        t = rawData[2] - 0x30;
        if(t < 5){
            autobindSpinner.setEnabled(true);
            autobindSpinner.setSelection(t);
        }
    }
}
