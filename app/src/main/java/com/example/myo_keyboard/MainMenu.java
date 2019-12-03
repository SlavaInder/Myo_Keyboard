package com.example.myo_keyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainMenu extends AppCompatActivity {

/* ==================================== Local Variables ==================================== */

    // Bluetooth variable
    private MyoBandDevice mMyoBandDevice;

    // UI elements
    private Button ScanButton;
    private Button VibrateButton;
    private Button MouseButton;
    private Button KeyboardButton;
    private Button SaveButton;

    private TextView DeviceNameTextView;
    private TextView DeviceAddressTextView;


/* ======================================= Lifecycle ======================================= */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init UI interface
        ScanButton = (Button) findViewById(R.id.Scan);
        VibrateButton =(Button) findViewById(R.id.Vibrate);
        MouseButton = (Button) findViewById(R.id.Mouse);
        KeyboardButton = (Button) findViewById(R.id.Keyboard);
        SaveButton = (Button) findViewById(R.id.Save);

        DeviceNameTextView = (TextView) findViewById(R.id.DeviceName);
        DeviceAddressTextView = (TextView) findViewById(R.id.DeviceAddress);

        // init global MyoHandler class instance
        mMyoBandDevice = com.example.myo_keyboard.MyoBandDevice.getInstance();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (mMyoBandDevice.isConnected == false) {

            // show that no device exists
            DeviceNameTextView.setText("None");
            DeviceAddressTextView.setText("None");


            // set all buttons disabled
            VibrateButton.setEnabled(false);
            MouseButton.setEnabled(false);
            KeyboardButton.setEnabled(false);
            SaveButton.setEnabled(false);
        }
        else {

            // show device properties
            DeviceNameTextView.setText(mMyoBandDevice.mBluetoothDevice.getName());
            DeviceAddressTextView.setText(mMyoBandDevice.mBluetoothDevice.getAddress());

            // enable buttons
            VibrateButton.setEnabled(true);
            MouseButton.setEnabled(true);
            KeyboardButton.setEnabled(true);
            SaveButton.setEnabled(true);
        }
    }


/* ======================================== Buttons ======================================== */

    // init an activity scanning bluetooth; returns device object
    public void onClickScan (View view) {
        Intent pickDevice = new Intent(this, Scan.class);
        startActivity(pickDevice);
    }

    public void onClickVibrate (View view) {
        mMyoBandDevice.vibrate();
    }

    public void OnClickKeys (View view) {
        Intent launchKeyboard = new Intent(this, Keyboard.class);
        startActivity(launchKeyboard);
    }
}
