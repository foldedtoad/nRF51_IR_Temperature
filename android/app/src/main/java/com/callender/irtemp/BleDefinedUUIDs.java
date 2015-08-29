package com.callender.irtemp;

import java.util.UUID;

public class BleDefinedUUIDs {

    public static class Service {
        final static public UUID IRTEMP_SERVICE           = UUID.fromString("0000fad0-0eea-45d1-a44a-bb3ce36fced6");
        final static public UUID BATTERY_SERVICE          = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    };

    public static class Characteristic {
        final static public UUID TEMPERATURE_CHAR         = UUID.fromString("0000fad1-0eea-45d1-a44a-bb3ce36fced6");
        final static public UUID INTERVAL_CHAR            = UUID.fromString("0000fad2-0eea-45d1-a44a-bb3ce36fced6");
    }

    public static class Descriptor {
        final static public UUID CHAR_USER_DESC           = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
        final static public UUID CHAR_CLIENT_CONFIG       = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        final static public UUID CHAR_PRESENT_FORMAT      = UUID.fromString("00002904-0000-1000-8000-00805f9b34fb");
    }

}
