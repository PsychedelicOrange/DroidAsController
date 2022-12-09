package com.example.droidascontroller

import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConfiguration
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.PreferenceFragmentCompat
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

private lateinit var manager : UsbManager
private val usbReceiver = object : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
            if(UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action){
                Log.d("APP","device connected.");
                }
            if(UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action){
                Log.d("APP","device disconnected.");
            }
        }
    }
fun ByteArray.toHexString(separator: CharSequence = " ",  prefix: CharSequence = "[",  postfix: CharSequence = "]") =
    this.joinToString(separator, prefix, postfix) {
        String.format("0x%02X", it)
    }
@RequiresApi(Build.VERSION_CODES.O)
fun handleintent(intent: Intent)
{
    Log.d("General","Inside handleIntent()")
    var usbdevice :UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
    if(usbdevice != null)
    {
        Log.d("APP","Device Connected: ${usbdevice.deviceName}")
        var interface0 = usbdevice.getInterface(0)
        var endpointIN = interface0.getEndpoint(0)
        var config = usbdevice.getConfiguration(0)
        Log.d("USB",endpointIN.toString())

        Thread(Runnable {
            lateinit var serverSocket : ServerSocket;
            lateinit var socket : Socket;
            lateinit var dinstream : DataInputStream
            lateinit var doutstream : DataOutputStream
            var bytes: ByteArray = ByteArray(endpointIN.maxPacketSize)
            var bytebuff: ByteBuffer =ByteBuffer.allocate(endpointIN.maxPacketSize)
                try {
                    serverSocket = ServerSocket(12345)
                    Log.d("SOCKET","socket created")
                }
                catch (e: IOException)
                {
                    Log.d("SOCKET",e.message.toString());
                }
                try {
                    socket = serverSocket.accept();
                    Log.d("SOCKET",socket.toString());
                }
                catch (e: IOException)
                {
                    Log.d("SOCKET",e.message.toString());
                }
                try {
                    val binstream : BufferedInputStream = BufferedInputStream(socket.getInputStream());
                    val boutstream : BufferedOutputStream = BufferedOutputStream(socket.getOutputStream());
                    dinstream = DataInputStream(binstream)
                    doutstream = DataOutputStream(boutstream)
                }
                catch (e: IOException)
                {
                    Log.d("SOCKET",e.message.toString());
                }
                manager.openDevice(usbdevice).apply {
                if(!claimInterface(interface0,true))
                {
                    Log.e("USB","USB Interface Claim Failed")
                }
                if(this.controlTransfer(UsbConstants.USB_DIR_OUT,9,1,0,null,0,0)<0)
                {
                    Log.e("USB","(controltranser) Configuration not set")
                }
                while (true)
                {
                    bulkTransfer(endpointIN,bytes,bytes.size,0);
                    doutstream.write(bytes,2,12)
                    doutstream.flush()
                    var out :String =""
                    for(i in bytes.indices)
                    {
                        out+=bytes[i]
                    }
                    Log.d("DATA",out)
                }
            }

        }).start();
    }
    else
    {
        Log.d("USB","no device connected")
    }
}
class SettingsActivity : AppCompatActivity() {

    @OptIn(ExperimentalStdlibApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment())
                    .commit()
        }

        // app started
        Log.d("General","Hello");

        // register controller_device_attached callback for @xml/device_filter
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        val filter2 = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver,filter2)
        registerReceiver(usbReceiver, filter)
        Log.d("General","FilterRegistered");

        // declare manager and device
        manager = getSystemService(Context.USB_SERVICE) as UsbManager

        handleintent(intent);
        Log.d("General","Intent Handled.");
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}