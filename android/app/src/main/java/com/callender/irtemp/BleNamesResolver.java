package com.callender.irtemp;

import java.util.HashMap;

public class BleNamesResolver {
    private static HashMap<String, String> mServices = new HashMap<String, String>();
    private static HashMap<String, String> mCharacteristics = new HashMap<String, String>();

    static public String resolveServiceName(final String uuid)
    {
        String result = mServices.get(uuid);
        if (result == null) result = "Unknown Service";
        return result;
    }

    static public String resolveCharacteristicName(final String uuid)
    {
        String result = mCharacteristics.get(uuid);
        if (result == null)
            result = "Unknown Characteristic";
        return result;
    }

    static public String resolveUuid(final String uuid) {
        String result = mServices.get(uuid);
        if (result != null) 
            return "Service: " + result;

        result = mCharacteristics.get(uuid);
        if (result != null) 
            return "Characteristic: " + result;

        result = "Unknown UUID";
        return result;
    }

    static public boolean isService(final String uuid) {
        return mServices.containsKey(uuid);
    }

    static public boolean isCharacteristic(final String uuid) {
        return mCharacteristics.containsKey(uuid);
    }
    
    static {
        mServices.put("0000fad0-0eea-45d1-a44a-bb3ce36fced6", "IRTempService");
        mServices.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access");
        mServices.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute");
        mServices.put("0000180f-0000-1000-8000-00805f9b34fb", "BatteryService");

        mCharacteristics.put("0000fad1-0eea-45d1-a44a-bb3ce36fced6", "Temperature Characteristic");
        mCharacteristics.put("0000fad2-0eea-45d1-a44a-bb3ce36fced6", "Interval Characteristic");

        mCharacteristics.put("00002a04-0000-1000-8000-00805f9b34fb", "Peripheral Preferred Connection Parameters");
        mCharacteristics.put("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
        mCharacteristics.put("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");
        mCharacteristics.put("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level");
    }
}
