package com.callender.irtemp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public interface BleWrapperUiCallbacks {

    public void uiDeviceFound(final BluetoothDevice device, int rssi, byte[] record);

    public void uiDeviceConnected(final BluetoothGatt gatt,
                                  final BluetoothDevice device);

    public void uiDeviceDisconnected(final BluetoothGatt gatt,
                                     final BluetoothDevice device);

    public void uiAvailableServices(final BluetoothGatt gatt,
                                    final BluetoothDevice device,
                                    final List<BluetoothGattService> services);

    public void uiNewValueForCharacteristic(final BluetoothGatt gatt,
                                            final BluetoothDevice device,
                                            final BluetoothGattService service,
                                            final BluetoothGattCharacteristic ch,
                                            final String strValue,
                                            final short[] data,
                                            final String timestamp);

    public void uiCharacteristicWriteStatus(final BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic ch,
                                            final int status);

    public void uiNewRssiAvailable(final BluetoothGatt gatt,
                                   final BluetoothDevice device,
                                   final int rssi);

    /* define Null Adapter class for that interface */
    public static class Null implements BleWrapperUiCallbacks {
        @Override
        public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {}
        @Override
        public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {}
        @Override
        public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device,
                                        List<BluetoothGattService> services) {}
        @Override
        public void uiNewValueForCharacteristic(BluetoothGatt gatt,
                                                BluetoothDevice device,
                                                BluetoothGattService service,
                                                BluetoothGattCharacteristic ch,
                                                String strValue,
                                                short[] data,
                                                String timestamp) {}
        @Override
        public void uiCharacteristicWriteStatus(final BluetoothGatt gatt,
                                                final BluetoothGattCharacteristic ch,
                                                final int status) {}
        @Override
        public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, int rssi) {}


        @Override
        public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {}
    }
}
