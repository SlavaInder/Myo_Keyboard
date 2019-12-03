package com.example.myo_keyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

public class Keyboard extends AppCompatActivity implements IReportEmg {

/* ==================================== Local Variables ==================================== */

    int temp;

    private EditText ClassNumberView;

    private MyoBandDevice mMyoBandDevice;

    // Handler to display messages
    private UiHandler uiHandler;


/* ======================================= Lifecycle ======================================= */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard);

        // init Ui
        ClassNumberView = (EditText) findViewById(R.id.ClassNumber);

        // init Ui handler
        uiHandler = new UiHandler();

        // init global MyoHandler class instance
        mMyoBandDevice = com.example.myo_keyboard.MyoBandDevice.getInstance();
    }


    @Override
    protected void onResume() {
        super.onResume();

        // set streaming to tis window
        mMyoBandDevice.setReceiver(this);

        // when entering this window, start collecting data
        mMyoBandDevice.startCollectEmgData();
    }


    @Override
    protected void onPause() {
        super.onPause();

        // when moving back from the window, stop sending data to save energy
        mMyoBandDevice.stopCollectEmgData();
    }

/* ======================================== Buttons ======================================== */


/* ====================================== EMG Handler ====================================== */


    @Override
    public void OnReportEmg(int[][] channels) {
        temp = channels[0][0];

        // update ui
        uiHandler.sendEmptyMessage(0);
    }


/* ====================================== Ui  Handler ====================================== */

    private class UiHandler extends Handler {
        private boolean isUpdating = false;
        private void startUpdate() {
            isUpdating = true;
        }
        private void stopUpdate() {
            isUpdating = false;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ClassNumberView.setText(Integer.toString(temp));

        }
    }

}
