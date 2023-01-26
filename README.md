# DroidAsController
![logo](https://raw.githubusercontent.com/PsychedelicOrange/DroidAsController/v2.0.0/Windows_Swing_application/DroidAsControllerServer/src/DroidAsControllerServer/icon_pc.png)
Turns Wired Gamepad into Wireless using Android.
Have to plug your gaming pc to the tv, want to play in the couch, but don't have a wireless controller ?
![infographic](https://raw.githubusercontent.com/PsychedelicOrange/DroidAsController/v2.0.0/Windows_Swing_application/DroidAsControllerServer/src/DroidAsControllerServer/main.jpg)
## How it works
* Android acts as a usb host for the controller, gets data from controller and sends it to pc wirelesslly over tcp channel
* Pc gets the data and feeds it to vJoy,a virtual controller software.
* x360ce makes any device act as a xbox 360 controller, we can map our buttons and axis correctly using x360ce , Alternatively mapping can be done in steam big picture mode.

## Use 
* [Download](https://github.com/PsychedelicOrange/DroidAsController/releases)
* Install App on android device. 
* Download and install [java](https://www.oracle.com/in/java/technologies/downloads/#jdk19-windows), [vJoy](https://github.com/shauleiz/vJoy), and optionally [x360ce](https://github.com/x360ce/x360ce).
* Connect the phone and pc to same wifi.
* Notedown IP of the pc and enter in the app.
* Connect your controller to the phone and give permission to the app. 

## Project IN PROGRESS, but working
* Feel free to report bugs and leave suggestions in Issues tab
* Refer to Resources.

## NOTES
* If you want the app to open [automatically](https://developer.android.com/guide/topics/connectivity/usb/host#using-intents) after your controller is connected, note down the product and vendor id , add it to xml/device_filter.xml
## Credits
Thanks to https://github.com/MohamedMassoud/Android-Gamepad-To-PC
& https://github.com/rlj1202/JvJoyInterface
Reverse engineered this one cause it was'nt open source and did'nt work with many buttons on my controller.
## Resources
### ANDROID
* https://developer.android.com/guide/topics/connectivity/usb/host
* https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input
### USB
* https://www.beyondlogic.org/usbnutshell/usb4.shtml
* https://www.partsnotincluded.com/understanding-the-xbox-360-wired-controllers-usb-data/
* https://www.keil.com/pack/doc/mw/USB/html/_u_s_b__configuration__descriptor.html
### MISCELLANIOUS
* https://developer.android.com/studio/command-line/adb

## Further work
* Add vibration / rumble support
* Integrate vJoy configuration in GUI
* Add Simple Mapping Feature
* Put app on playstore
* Support for multiple controllers ✅
