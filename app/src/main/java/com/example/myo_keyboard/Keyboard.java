package com.example.myo_keyboard;

import org.tensorflow.lite.Interpreter;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Keyboard extends AppCompatActivity implements IReportEmg {

/* ==================================== Local Variables ==================================== */

    // constants
    final private int MEAN_LEN = 60;
    final private long  TIME_CONST = 1000;
    final private String symbols = "_fdsa";

    // tflite interpreter
    Interpreter tflite;

    // classification result
    int[] predictions_order = new int[MEAN_LEN];
    int[] prediction_values = new int[5];
    int decision = 0;

    // results from the last time
    long prev_decision_time = 0;

    // sliding window
    int[][] mwindow = new int[8][80];
    // Input feature vector shape is [56]
    float[] inputval = new float[56];
    // Output class vector shape is [1][5]
    float[][] outputval = new float[1][5];

    // Ui elements
    private EditText ClassNumberView;
    private MultiAutoCompleteTextView TextEntryView;

    // BLE object
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
        TextEntryView = (MultiAutoCompleteTextView) findViewById(R.id.TextEntry);

        // init Ui handler
        uiHandler = new UiHandler();

        // init global MyoHandler class instance
        mMyoBandDevice = com.example.myo_keyboard.MyoBandDevice.getInstance();

        // init tflite interpreter
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    public void onClickClear (View view) {
        TextEntryView.setText("");
    }

/* =================================== Tensorflow helper =================================== */

    public int doInference(){
        // Run inference passing input shape and getting output shape
        tflite.run(inputval, outputval);

        // get the maximal value
        float locmax = outputval[0][0];
        int locmaxind = 0;
        for (int i=1; i<5;i++) {
            if (locmax < outputval[0][i]) {
                locmax = outputval[0][i];
                locmaxind = i;
            }
        }

        return locmaxind;
    }


    // Memory-map the model file in assets
    private MappedByteBuffer loadModelFile() throws IOException {
        // Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("asdf2.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


/* ====================================== EMG Handler ====================================== */


    @Override
    public void OnReportEmg(int[][] channels) {

        // put your code here
        for (int i = 0; i < 8; i++) {
            // shift array on 1 segment
            for (int j = 0; j < 78; j++) {
                mwindow[i][j] = mwindow[i][j+2];
            }
            // add new elements from bluetooth
            mwindow[i][78] = channels[0][i];
            mwindow[i][79] = channels[1][i];
        }

        // get features from current window
        inputval = FeatureProcessor.get_features(mwindow);

        // pop up the earliest prediction
        prediction_values[predictions_order[0]] = prediction_values[predictions_order[0]] - 1;

        // shift order of predictions
        for (int i=0; i<MEAN_LEN - 1; i++) {
            predictions_order[i] = predictions_order[i+1];
        }

        // get new prediction
        predictions_order[MEAN_LEN-1] = doInference();
        prediction_values[predictions_order[MEAN_LEN-1]] = prediction_values[predictions_order[MEAN_LEN-1]] + 1;

        // make a decision
        decision = 0;
        for (int i=1; i<5; i++) {
            if ((i == 1) && (prediction_values[i] > MEAN_LEN *5/8)) {
                decision = i;
            }
            if ((i == 2) && (prediction_values[i] > MEAN_LEN *4/8)) {
                decision = i;
            }
            if ((i > 2) && (prediction_values[i] > MEAN_LEN * 3/4)) {
                decision = i;
            }
        }

        long current_time = System.currentTimeMillis();

        // update ui if necessary
        if ((decision > 0) && (current_time - prev_decision_time > TIME_CONST)) {
            prev_decision_time = current_time;
            uiHandler.sendEmptyMessage(decision);
        } else {
            uiHandler.sendEmptyMessage(0);
        }
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
            ClassNumberView.setText(Integer.toString(decision));

            if (msg.what > 0) {
                String current_text = TextEntryView.getText().toString();
                int current_selector = TextEntryView.getSelectionEnd();
                TextEntryView.setText(current_text.substring(0, current_selector) +  symbols.substring(msg.what, msg.what + 1) + current_text.substring(current_selector, current_text.length()));
                TextEntryView.setSelection(current_selector + 1);
            }
        }
    }

}
