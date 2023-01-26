package com.example.droidascontroller

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log
import java.io.*
import java.net.Socket

class droidascontroller {

    var socket : Socket? = null;
    lateinit var dinstream : DataInputStream
    lateinit var doutstream : DataOutputStream
    lateinit var usbdevice : UsbDevice
    lateinit var interface0 : UsbInterface
    lateinit var endpointIN :UsbEndpoint
    lateinit var bytes: ByteArray;
    constructor(){

    }
    fun connectServer(address : String) {
        Log.d("APP","check 3 : Inside init Server")
        try {
            socket = Socket(address,12345)
            Log.d("SOCKET","socket connected")
        }
        catch (e: Exception)
        {
            Log.d("SOCKET",e.message.toString());
        }
        try {
            val binstream : BufferedInputStream = BufferedInputStream(socket!!.getInputStream());
            val boutstream : BufferedOutputStream = BufferedOutputStream(socket!!.getOutputStream());
            dinstream = DataInputStream(binstream)
            doutstream = DataOutputStream(boutstream)
        }
        catch (e: Exception)
        {
            Log.d("SOCKET",e.message.toString());
        }

    }
    fun work(){
        Log.d("APP","check 4 : started working")
        interface0 = usbdevice.getInterface(0)
        endpointIN = interface0.getEndpoint(0)
        bytes = ByteArray(endpointIN.maxPacketSize)
        manager.openDevice(usbdevice).apply {
            if(!claimInterface(interface0,true))
            {
                Log.e("USB","USB Interface Claim Failed")
            }
            if(this.controlTransfer(UsbConstants.USB_DIR_OUT,9,1,0,null,0,0)<0)
            {
                Log.e("USB","(controltranser) Configuration not set")
            }
            try {
                while (!isDiscon)
                {
                    bulkTransfer(endpointIN,bytes,bytes.size,0);
                    doutstream.write(bytes,2,12)
                    doutstream.flush()
                }
            }
            catch (e : java.lang.Exception)
            {
                Log.d("APP","Exception + ${e.message}")
                socket = null
            }
            Log.d("APP","check 4 : stopped working")
            // cleanup
            socket!!.close();socket = null
        }
    }
    fun handleintent(usbdevice: UsbDevice)
    {
        this.usbdevice = usbdevice
        work()
    }
}