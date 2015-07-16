# nRF51_IR_Temperature
Use the nRF51 (PCA10028) with TI TMP006 Infrared Temperature sensor via Arduino protoboard shield.

Here is a photo of the [hardware](https://github.com/foldedtoad/nRF51_IR_Temperature/blob/master/docs/PCA10028_TMP006.jpg) which show the Arduino R3 protoboard shield fitted onto the Nordic PCA10028 board.  
The white protoboard area has the actual TMP006 Infrared Temperature sensor.  
The TMP006 module was purchased from Adafruit as was the Arduino protoboard shield.  

The firmware assumes that Nordic's SDK 8.x and SoftDevice 8 are available on the build system.

This project support top-level features  

* Unified firmware image - single consolidated flash image which has softdevice + app + bootloader.
* OTA-DFU - Overt-The-Air, Device Firmware Update: single-bank version
* Software-triggered Bootloader entry - Use Nordic's Master Control Panel to update application firmware. 
