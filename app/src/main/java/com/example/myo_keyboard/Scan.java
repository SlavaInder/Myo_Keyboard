package com.example.myo_keyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class Scan extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {

/* ==================================== Local Variables ==================================== */

    // inter process communication
    static final String DEVICE_KEY = "Device Key";

    // constants (time is given in ms)
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;

    // advertising UUID of MYO
    private static final UUID MYO_CONTROL_UUID = UUID.fromString("d5060001-a904-deb9-4748-2c7f4a124842");
    private static final UUID[] MYO_CONTROL_UUID_ARRAY = {MYO_CONTROL_UUID};


    // BLE objects
    private MyoBandDevice mMyoBandDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBLEDevices = new ArrayList<>();
    private ArrayList<String> mBLEDeviceAddresses= new ArrayList<>();

    // Handler to search for Myo
    private Handler TimeOutHandler;

    // UI elements
    private ImageButton SearchButton;
    private ArrayAdapter<String> BLEDevicesArrayAdapter;



/* ======================================= Lifecycle ======================================= */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // init handlers
        TimeOutHandler = new Handler();

        // Initialize array adapter for paired devices
        BLEDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // init Ui elements
        SearchButton = (ImageButton) findViewById(R.id.Search);
        ListView BLEDevicesListView = (ListView) findViewById(R.id.BLEDevices);
        BLEDevicesListView.setAdapter(BLEDevicesArrayAdapter);
        BLEDevicesListView.setOnItemClickListener(deviceClickedHandler);

        // Initializes Bluetooth adapter.
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // init global MyoHandler class instance
        mMyoBandDevice = com.example.myo_keyboard.MyoBandDevice.getInstance();
    }

/* ========================================== BLE ========================================== */

    // this method is called automatically !after! device is successfully found
    // this device is passed to func alongside advertising package (in ScanRecord variable)
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // if new device is found
        if (!(mBLEDeviceAddresses.contains(device.getAddress()))) {

            // Device Log
            String msg = "name=" + device.getName() + ", bondStatus="
                    + device.getBondState() + ", address="
                    + device.getAddress() + ", type" + device.getType();
            Log.i("ScanActivity", msg);

            // append arrays
            mBLEDevices.add(device);
            mBLEDeviceAddresses.add(device.getAddress());

            // add new entry of the list
            BLEDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        }
    }

/* ======================================== Buttons ======================================== */

    // activate BLE scan
    public void onClickSearch(View view) {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // clear arrays
        mBLEDevices.clear();
        mBLEDeviceAddresses.clear();

        // show message
        Toast.makeText(getApplicationContext(), "Scanning BLE devices...", Toast.LENGTH_SHORT).show();

        // Get the BLE device
        mBluetoothAdapter.startLeScan(MYO_CONTROL_UUID_ARRAY, this);

        // set up an additional thread to activate when some time passes
        // this thread will shut down LeScan and allow onClickMyo to proceed
        TimeOutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Stop scanning
                mBluetoothAdapter.stopLeScan(Scan.this);

                // report timeout message
                Toast.makeText(getApplicationContext(), "Scan timeout", Toast.LENGTH_SHORT).show();

                // set this button to enabled
                SearchButton.setEnabled(true);
            }
        }, SCAN_PERIOD);
    }


    // Handler for list elements
    private AdapterView.OnItemClickListener deviceClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            // Init connection
            mMyoBandDevice.setBtDevice(mBLEDevices.get(position), getApplicationContext());
            finish();
        }
    };

}
