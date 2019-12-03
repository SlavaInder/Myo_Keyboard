package com.example.myo_keyboard;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class MyoBandDevice extends BluetoothGattCallback {

/* ==================================== Singleton init ==================================== */

    private static final MyoBandDevice ourInstance = new MyoBandDevice();
    public static MyoBandDevice getInstance() {
        return ourInstance;
    }
    private MyoBandDevice() { }


/* ==================================== Local Variables ==================================== */

    // State variable
    public boolean isConnected =false;

    // output handler
    IReportEmg mEmgReceiver = null;

    // Bluetooth variables
    public BluetoothDevice mBluetoothDevice;
    public BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCommandCharacteristic;

    // handlers for proper descriptor write
    private boolean isProcessingQueue = false;
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();

    // Service ID
    private static final UUID MYO_CONTROL_UUID = UUID.fromString("d5060001-a904-deb9-4748-2c7f4a124842");
    private static final UUID MYO_EMG_DATA_UUID = UUID.fromString("d5060005-a904-deb9-4748-2c7f4a124842");

    // Characteristics ID
    private static final UUID MYO_INFO_UUID = UUID.fromString("d5060101-a904-deb9-4748-2c7f4a124842");
    private static final UUID FIRMWARE_UUID = UUID.fromString("d5060201-a904-deb9-4748-2c7f4a124842");
    private static final UUID COMMAND_UUID = UUID.fromString("d5060401-a904-deb9-4748-2c7f4a124842");
    private static final UUID EMG_0_UUID = UUID.fromString("d5060105-a904-deb9-4748-2c7f4a124842");
    private static final UUID EMG_1_UUID = UUID.fromString("d5060205-a904-deb9-4748-2c7f4a124842");
    private static final UUID EMG_2_UUID = UUID.fromString("d5060305-a904-deb9-4748-2c7f4a124842");
    private static final UUID EMG_3_UUID = UUID.fromString("d5060405-a904-deb9-4748-2c7f4a124842");
    private static final UUID[] EMG_UUIDS = {EMG_0_UUID, EMG_1_UUID, EMG_2_UUID, EMG_3_UUID};

    // common for all characteristics among all devices descriptor to enable notifications
    private static final UUID CLIENT_CHARACTERISTIC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic[] mEmgCharacteristics = new BluetoothGattCharacteristic[4];


/* ================================== Interface functions ================================== */

    public void setBtDevice(BluetoothDevice BtDevice, Context context) {
        // set internal variable to connected
        this.isConnected = true;

        // set BLE device
        this.mBluetoothDevice = BtDevice;

        // connect gatt
        mBluetoothGatt = mBluetoothDevice.connectGatt(context, false, this);
    }


    public void setReceiver(IReportEmg receiver) {
        this.mEmgReceiver = receiver;
    }


    public void vibrate() {
        // send collecting command
        setMyoControlCommand(MyoCommandList.sendVibration2());
    }


    public void startCollectEmgData() {
        // send collecting command
        setMyoControlCommand(MyoCommandList.sendEmgOnly());
    }

    public void stopCollectEmgData() {
        // send stop command
        setMyoControlCommand(MyoCommandList.sendUnsetData());
    }


    /* ================================== Bluetooth functions ================================== */

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        // GATT Connected
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // Searching GATT Services
            gatt.discoverServices();
        }
        // GATT Disconnected
        else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // Close GATT services
            stopCallback();
        }
    }


    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if (status != BluetoothGatt.GATT_SUCCESS)
            return;

        // find EMG gatt service
        BluetoothGattService emgBtService = gatt.getService(MYO_EMG_DATA_UUID);

        //for (int i = 0; i < EMG_UUIDS.length; i++) {
        for (int i = 3; i >= 0; i--) {
            UUID emgUUID = EMG_UUIDS[i];

            Log.i("BLE", "Try to get EMG service " + i);


            // getting CommandCharacteristic
            mEmgCharacteristics[i] = emgBtService.getCharacteristic(emgUUID);
            if (mEmgCharacteristics[i] == null) {
                //callback_msg = "Not Found EMG-Data Characteristic";
                Log.i("BLE", "Cannot get characteristics");
            } else {

                // enable notifications
                boolean reg = gatt.setCharacteristicNotification(mEmgCharacteristics[i], true);
                if (!reg) {
                    Log.e("BLE", "Data notification OFF");
                } else {
                    Log.i("BLE", "Data notification ON");
                    // write descriptor supporting enabling notifications
                    BluetoothGattDescriptor descriptor = mEmgCharacteristics[i].getDescriptor(CLIENT_CHARACTERISTIC_UUID);
                    if (descriptor == null) {
                        Log.e("BLE", "Cannot get descriptor");
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        writeGattDescriptor(descriptor);

                        Log.i("BLE", "Set descriptor: " + descriptor.toString());
                        Log.i("BLE", "Acquired characteristic: " + Integer.toString(mEmgCharacteristics[i].getInstanceId()));
                    }
                }
            }
        }

        // find control gatt service
        BluetoothGattService controlBtService = gatt.getService(MYO_CONTROL_UUID);
        // Get the MyoInfoCharacteristic
        BluetoothGattCharacteristic characteristic = controlBtService.getCharacteristic(MYO_INFO_UUID);
        readGattCharacteristics(characteristic);
        // Get CommandCharacteristic
        mCommandCharacteristic = controlBtService.getCharacteristic(COMMAND_UUID);

    }

    private long lastSendNeverSleepTimeMs = System.currentTimeMillis();
    private final static long NEVER_SLEEP_SEND_TIME = 10000;  // Milli Second
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        long systemTimeMs = System.currentTimeMillis();
        byte[] emgData = characteristic.getValue();
        int[][] emgChannels = new int[2][8];
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 8; j++)
                emgChannels[i][j] = emgData[i * 8 + j];



        for (int i = 0; i < 2; i++) {
            StringBuilder sbForFile = new StringBuilder();
            sbForFile.append(systemTimeMs);
            for (int j = 0; j < 8; j++)
                sbForFile.append(',').append(emgChannels[i][j]);
            sbForFile.append('\n');
            String strForFile = "ID: " + Integer.toString(characteristic.getInstanceId()) + ", TS: " + sbForFile.toString();

            Log.i("mFileWriter", strForFile);

        }

        if (!(mEmgReceiver == null)) {
            mEmgReceiver.OnReportEmg(emgChannels);
        }

        if (systemTimeMs > lastSendNeverSleepTimeMs + NEVER_SLEEP_SEND_TIME) {
            // set Myo [Never Sleep Mode]
            setMyoControlCommand(MyoCommandList.sendUnSleep());
            lastSendNeverSleepTimeMs = systemTimeMs;

        }
    }


    private void writeGattDescriptor(BluetoothGattDescriptor d) {
        //put the descriptor into the write queue
        descriptorWriteQueue.add(d);
        runNextBtRequest();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        // pop the item that we just finishing writing
        descriptorWriteQueue.remove();
        isProcessingQueue = false;

        // if there is more to write, do it!
        runNextBtRequest();
    }


    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        characteristicReadQueue.remove();

        if (status != BluetoothGatt.GATT_SUCCESS) {
            UUID characteristicUUID = characteristic.getUuid();

            if (characteristic.getUuid().equals(MYO_INFO_UUID)) {
                // and what?
            }
        }

        isProcessingQueue = false;
        runNextBtRequest();
    }


    private void readGattCharacteristics(BluetoothGattCharacteristic c) {
        characteristicReadQueue.add(c);
        runNextBtRequest();
    }

    private void runNextBtRequest() {
        if (isProcessingQueue)
            return;

        isProcessingQueue = true;

        if (descriptorWriteQueue.size() > 0)
            mBluetoothGatt.writeDescriptor(descriptorWriteQueue.element());
        else if (characteristicReadQueue.size() > 0)
            mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
        else
            isProcessingQueue = false;
    }

    public boolean setMyoControlCommand(byte[] command) {
        if (mCommandCharacteristic == null)
            return false;

        mCommandCharacteristic.setValue(command);
        if (mCommandCharacteristic.getProperties() != BluetoothGattCharacteristic.PROPERTY_WRITE)
            return false;

        return mBluetoothGatt.writeCharacteristic(mCommandCharacteristic);
    }


    public void stopCallback() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

}
