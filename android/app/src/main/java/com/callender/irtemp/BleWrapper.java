package com.callender.irtemp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BleWrapper {

    private static final String TAG = "IRTEMP";

    /* Defines (in milliseconds) how often RSSI should be updated */
    private static final int RSSI_UPDATE_TIME_INTERVAL = 1500; // 1.5 seconds

    /* Callback object through which we are returning results to the caller */
    private BleWrapperUiCallbacks mUiCallback = null;

    /* Define NULL object for UI callbacks */
    private static final BleWrapperUiCallbacks NULL_CALLBACK = new BleWrapperUiCallbacks.Null(); 

    /* Creates BleWrapper object, set its parent activity and callback object */
    public BleWrapper(Activity parent, BleWrapperUiCallbacks callback) {
        this.mParent = parent;
        mUiCallback = callback;
        if (mUiCallback == null) mUiCallback = NULL_CALLBACK;
    }

    public BluetoothManager           getManager() { return mBluetoothManager; }
    public BluetoothAdapter           getAdapter() { return mBluetoothAdapter; }
    public BluetoothDevice            getDevice()  { return mBluetoothDevice; }
    public BluetoothGatt              getGatt()    { return mBluetoothGatt; }
    public BluetoothGattService       getCachedService() { return mBluetoothSelectedService; }
    public List<BluetoothGattService> getCachedServices() { return mBluetoothGattServices; }
    public boolean                    isConnected() { return mConnected; }

    /* Check if this host supports BT and BLE */
    public boolean checkBleHardwareAvailable() {

        // First check general Bluetooth Hardware:
        // get BluetoothManager...
        final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) return false;

        // .. and then get adapter from manager
        final BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) return false;

        // and then check if BT LE is also available
        return mParent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /* Before continuing, check if BT is turned ON and enabled. 
     * Call this from onResume to ensure BT is ON when the application is in the foreground.
     */
    public boolean isBtEnabled() {
        final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) return false;

        final BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) return false;

        return adapter.isEnabled();
    }

    /* Start scanning for BT LE devices in the vicinity */
    public void startScanning() {
        mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
    }

    /* Stops current scanning */
    public void stopScanning() {
        mBluetoothAdapter.stopLeScan(mDeviceFoundCallback); 
    }

    /* Initialize BLE and get BT Manager & Adapter */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        if (mBluetoothAdapter == null) mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    /* Connect to the device with specified address */
    public boolean connect(final String deviceAddress) {
        if (mBluetoothAdapter == null || deviceAddress == null) return false;
        mDeviceAddress = deviceAddress;

        // Check if we need to connect from scratch or just reconnect to previous device
        if (mBluetoothGatt != null && mBluetoothGatt.getDevice().getAddress().equals(deviceAddress)) {
            // just reconnect
            return mBluetoothGatt.connect();
        }
        else {
            // Connect from scratch.
            // Get BluetoothDevice object for specified address.
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
            if (mBluetoothDevice == null) {
                // we got wrong address - that device is not available!
                return false;
            }
            // connect with remote device
            mBluetoothGatt = mBluetoothDevice.connectGatt(mParent, false, mBleCallback);
        }
        return true;
    }

    /* disconnect the device. It is still possible to reconnect to it later with this Gatt client */
    public void disconnect() {
        if (mBluetoothGatt != null) mBluetoothGatt.disconnect();
        mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
    }

    /* close GATT client completely */
    public void close() {
        if (mBluetoothGatt != null) mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /* request new RSSi value for the connection*/
    public void readPeriodicalyRssiValue(final boolean repeat) {
        mTimerEnabled = repeat;
        // check if we should stop checking RSSI value
        if ((mConnected == false) || (mBluetoothGatt == null) || (mTimerEnabled == false)) {
            mTimerEnabled = false;
            return;
        }

        mTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGatt == null   ||
                   mBluetoothAdapter == null ||
                   mConnected == false)
                {
                    mTimerEnabled = false;
                    return;
                }

                // request RSSI value
                mBluetoothGatt.readRemoteRssi();
                // add call it once more in the future
                readPeriodicalyRssiValue(mTimerEnabled);
            }
        }, RSSI_UPDATE_TIME_INTERVAL);
    }

    /* starts monitoring RSSI value */
    public void startMonitoringRssiValue() {
        readPeriodicalyRssiValue(true);
    }
    
    /* stops monitoring of RSSI value */
    public void stopMonitoringRssiValue() {
        readPeriodicalyRssiValue(false);
    }
    
    /* request to discover all services available on the remote devices
     * results are delivered through callback object 
     */
    public void startServicesDiscovery() {
        if (mBluetoothGatt != null) 
            mBluetoothGatt.discoverServices();
    }

    /* gets services and calls UI callback to handle them
     * before calling getServices() make sure service discovery is finished! 
     */
    public void getSupportedServices() {
        if (mBluetoothGattServices != null && mBluetoothGattServices.size() > 0) mBluetoothGattServices.clear();
        // keep reference to all services in local array:
        if (mBluetoothGatt != null) {
            mBluetoothGattServices = mBluetoothGatt.getServices();  
        }
        
        mUiCallback.uiAvailableServices(mBluetoothGatt, mBluetoothDevice, mBluetoothGattServices);
    }

    /* request to fetch newest value stored on the remote device for particular characteristic */
    public void requestCharacteristicValue(BluetoothGattCharacteristic charactistic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        mBluetoothGatt.readCharacteristic(charactistic);
        // new value available will be notified in Callback Object
    }

    public static short byteArrayToShort(byte[] byteArray)
    {
        return (short) ((byteArray[0] & 0xFF) |
                       ((byteArray[1] & 0xFF) << 8)) ;
    }

    /* get characteristic's value (and parse it for some types of characteristics) 
     * before calling this you should always update the value by calling requestCharacteristicValue() 
     */
    @SuppressLint("SimpleDateFormat")
    public void getCharacteristicValue(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || characteristic == null) return;

        byte[] rawValue = characteristic.getValue();
        String strValue = null;
        int intValue = 0;

        short[] dataAsShortArray = null;

        Log.d(TAG, "getCharacteristicValue:");

        // Let's read and do real parsing of some characteristic
        // to get meaningful value from it.
        UUID uuid = characteristic.getUuid();

        if (rawValue.length <= 0) 
            return;

        if (uuid.equals(BleDefinedUUIDs.Characteristic.TEMPERATURE_CHAR)) {

            Log.d(TAG, "received TEMPERATURE data");

            //short value = byteArrayToShort(rawValue);

            dataAsShortArray = new short[rawValue.length / 2];

            for (int i=0; i<dataAsShortArray.length; i++) {
                ByteBuffer bb = ByteBuffer.allocate(2);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(rawValue[(2*i + 0)]);
                bb.put(rawValue[(2*i + 1)]);
                dataAsShortArray[i] = bb.getShort(0);
            }

            final StringBuilder stringBuilder = new StringBuilder(dataAsShortArray.length);

            stringBuilder.append(String.format("Temperature: "));

            for (int shortChar : dataAsShortArray) {
                stringBuilder.append(String.format("%2dC ", (shortChar & 0xFFFF)));
            }

            strValue = stringBuilder.toString();
        }
        else {
            // not known type of characteristic, so we need to handle this in "general" way.
            //
            if (rawValue.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(rawValue.length);
                for (byte byteChar : rawValue) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }
                strValue = stringBuilder.toString();
            }
        }
        Log.d(TAG, strValue);

        String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS  ").format(new Date());
        
        mUiCallback.uiNewValueForCharacteristic(mBluetoothGatt,
                                                mBluetoothDevice,
                                                mBluetoothSelectedService,
                                                characteristic,
                                                strValue,
                                                dataAsShortArray,
                                                timestamp);
    }


    /* set new value for particular characteristic */
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic characteristic, final byte[] dataToWrite) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || characteristic == null) {
            Log.e(TAG, "writeDataToCharacteristic: bad parms");
            return;
        }

        // first set it locally....
        if (characteristic.setValue(dataToWrite) == false) {
            Log.e(TAG, "writeDataToCharacteristic: setValue failed");
        }

        // ... and then "commit" changes to the peripheral
        if (mBluetoothGatt.writeCharacteristic(characteristic) == false) {
            Log.e(TAG, "writeDataToCharacteristic: writeCharacteristic failed");
        }
    }

    /* enables/disables notification for characteristic */
    public void setNotificationForCharacteristic(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        boolean success = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (!success) {
            Log.e(TAG, "Setting proper notification status for characteristic failed!");
        }

        // This is also sometimes required to enable notifications/indications
        //
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleDefinedUUIDs.Descriptor.CHAR_CLIENT_CONFIG);
        if (descriptor != null) {
            byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                                   BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /* defines callback for scanning results */
    private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            mUiCallback.uiDeviceFound(device, rssi, scanRecord);
        }
    };

    /* Callbacks called for any action on particular Ble Device */
    private final BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                mUiCallback.uiDeviceConnected(mBluetoothGatt, mBluetoothDevice);

                // now we can start talking with the device, e.g.
                mBluetoothGatt.readRemoteRssi();
                // response will be delivered to callback object!

                // in our case we would also like automatically to call for services discovery
                startServicesDiscovery();

                // and we also want to get RSSI value to be updated periodically
//              startMonitoringRssiValue();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // now, when services discovery is finished, we can call getServices() for Gatt
                getSupportedServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            // we got response regarding our request to fetch characteristic value

            if (status == BluetoothGatt.GATT_SUCCESS) {
                getCharacteristicValue(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic)
        {
            Log.d(TAG, "onCharacteristicChanged");

            // characteristic's value was updated due to enabled notification, lets get this value
            // the value itself will be reported to the UI inside getCharacteristicValue
            getCharacteristicValue(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status)
        {
            mUiCallback.uiCharacteristicWriteStatus(gatt, characteristic, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                // we got new value of RSSI of the connection, pass it to the UI
                 mUiCallback.uiNewRssiAvailable(mBluetoothGatt, mBluetoothDevice, rssi);
            }
        };
    };

    private Activity mParent = null;
    private boolean mConnected = false;
    private String mDeviceAddress = "";

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice  mBluetoothDevice = null;
    private BluetoothGatt    mBluetoothGatt = null;
    private BluetoothGattService mBluetoothSelectedService = null;
    private List<BluetoothGattService> mBluetoothGattServices = null;

    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
}
