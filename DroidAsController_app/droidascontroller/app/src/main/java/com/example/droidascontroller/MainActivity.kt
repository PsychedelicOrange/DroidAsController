package com.example.droidascontroller

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.*
import java.util.*

//settings
var useAttach = true

//usb variables
private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
lateinit var permissionIntent : PendingIntent;
lateinit var manager : UsbManager
private val usbReceiver = object : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_USB_PERMISSION == intent.action) {
            synchronized(this) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.apply {
                        Log.d("APP","Permission has been granted")
                        tview.text = "Device Connected!"
                        isDiscon = false;
                        if(inputIp.text.isNotEmpty())
                        {
                            Thread{
                                dac.connectServer(inputIp.text.toString())
                                dac.handleintent(device);
                            }.start()
                        }
                        else {
                            Log.d("APP","No IP provided")
                        }
                    }
                } else {
                    Log.d("APP", "permission denied for device $device")
                    tview.text = "Permission denied for device ${device!!.deviceName} Please restart app to try again!"
                }
            }
        }
        if(UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action){
            isDiscon = false;
            tview.text = "Device Connected!"
            Log.d("APP","device connected.");
            var usbdevice :UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            Log.d("APP",usbdevice.toString())

            Thread{
                if(dac.socket == null) {
                    dac.connectServer(inputIp.text.toString())
                }
                if (usbdevice != null) {
                    dac.handleintent(usbdevice)
                }
            }.start()

        }
        if(UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action){
            isDiscon = true;
            Log.d("APP","device disconnected.");
            tview.text = "Device Disconnected, Connect your Controller !"
            var deviceList = manager!!.deviceList;
            if(!useAttach)
                waitForControllerConnect()
        }
    }
}

// to do : i do not know how to work around this yet
lateinit var tview: TextView;
lateinit var inputIp : EditText;

//meta
var isDiscon = true; // closes socket on controller disconnect
var dac : droidascontroller = droidascontroller()

class MainActivity : AppCompatActivity() {
    lateinit var sharedPref : SharedPreferences;

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("APP","onCreate() ran again ! ")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize USB manager
        manager = getSystemService(Context.USB_SERVICE) as UsbManager;

        // Initialize Shared Preferences (Stores IP in cache)
        sharedPref = this.getSharedPreferences("com.example.droidascontroller.cachedIP",Context.MODE_PRIVATE)

        // Initialize UI
        inputIp = findViewById(R.id.ipinput);
        val address = sharedPref.getString("com.example.droidascontroller.cachedIP", "")
        Log.d("APP","$address");
        inputIp.setText(address)
        tview = findViewById(R.id.textView)
        tview.text = "Connect your Controller !"

        // Register intents for Attach (OPTIONAL) and Detach
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        if(useAttach)
            registerReceiver(usbReceiver, filter)
        val filter2 = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver,filter2)

        // Register intents for explicit permission
        permissionIntent =
            PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), FLAG_MUTABLE)
        val filter3 = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver,filter3)

        waitForControllerConnect()

    }

    fun onClickSave(view : View)// save IP to cache
    {
        with (sharedPref.edit()) {
            putString("com.example.droidascontroller.cachedIP",inputIp.text.toString())
            apply()
        }
        with (sharedPref) {
            getString("com.example.droidascontroller.cachedIP","not working")?.let {
                Log.d("APP",
                    "SAVED :$it"
                )
            }
        }
    }

}
fun waitForControllerConnect()// waits for controller to be connected and requests permission
{
    var deviceList = manager!!.deviceList;
    var waitConnect = Thread{
        while(deviceList.isEmpty())
        {
            deviceList = manager!!.deviceList;
            Thread.sleep(1000);
        }
        var device = deviceList.getValue(deviceList.keys.first());
        Log.d("APP","Found connected : $device")
        manager.requestPermission(device, permissionIntent);
    };
    waitConnect.start()
}