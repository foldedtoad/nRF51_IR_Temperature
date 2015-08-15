# nRF51_IR_Temperature
Use the nRF51 (PCA10028) with TI TMP006 Infrared Temperature sensor via Arduino protoboard shield.

Here is a photo of the [hardware](https://github.com/foldedtoad/nRF51_IR_Temperature/blob/master/docs/PCA10028_TMP006.jpg) which show the Arduino R3 protoboard shield fitted onto the Nordic PCA10028 board.  
The white protoboard area has the actual TMP006 Infrared Temperature sensor.  
The TMP006 module was purchased from Adafruit as was the Arduino protoboard shield.  

**NOTE**
This project can be built on Linux or OSX, but it has not yet been updated to support Windows.

##Project Assumptions  

* Nordic's SDK 8.x
* Nordics SoftDevice 8  (S110)
* ARM LaunchPad [toolchain](https://launchpad.net/gcc-arm-embedded)
* Python 2.7 with pip installed (see firmware *builder* readme for details)

Optional, but highly recommended  
* Android 4.3 system (or later) with Bluetooth LE support.
* Nordic's Master Control Panel app installed on above Android system.

##Project Features  

* Remotely measure the temperature of object and relays readings to central device. 
* Unified firmware image - single consolidated flash image which has softdevice + app + bootloader.
* OTA-DFU - Over-The-Air, Device Firmware Update: single-bank version
* Software-triggered Bootloader entry - Use Nordic's Master Control Panel to update application firmware. 
