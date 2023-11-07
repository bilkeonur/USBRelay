package com.ob.usbrelay;

import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Bundle;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Button;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    UsbDevice[] UsbRelayModules = null;

    String ACTION_USB_PERMISSION = "com.ob.usbrelay.USB_PERMISSION";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnRelayPermission = findViewById(R.id.btnRelayPermission);
        Button btnRelay1On = findViewById(R.id.btnRelay1On);
        Button btnRelay1Off = findViewById(R.id.btnRelay1Off);
        Button btnRelay2On = findViewById(R.id.btnRelay2On);
        Button btnRelay2Off = findViewById(R.id.btnRelay2Off);

        btnRelayPermission.setOnClickListener(view -> {
            setUSBRelayPermission();
        });

        btnRelay1On.setOnClickListener(view -> {
            turnOnOffRelay(UsbRelayModules[0], 1, true);
        });

        btnRelay1Off.setOnClickListener(view -> {
            turnOnOffRelay(UsbRelayModules[0], 1, false);
        });

        btnRelay2On.setOnClickListener(view -> {
            turnOnOffRelay(UsbRelayModules[0], 2, true);
        });

        btnRelay2Off.setOnClickListener(view -> {
            turnOnOffRelay(UsbRelayModules[0], 2, false);
        });

        refreshUsbPorts();
    }

    private void refreshUsbPorts()
    {
        if (UsbRelayModules != null)
        {
            UsbRelayModules = null;
        }

        try
        {
            UsbRelayModules = getUsbRelaysArray();

            if ((UsbRelayModules != null))
            {
                List<String> relaysList = new ArrayList<>();

                for (UsbDevice usbHidDev:UsbRelayModules)
                {
                    relaysList.add(String.valueOf(usbHidDev.getDeviceId()));
                }
            }
        }
        catch (Exception e)
        {
            e.getStackTrace();
        }
    }

    private void turnOnOffRelay(UsbDevice device, int relayNum, boolean onOff)
    {
        try
        {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            UsbDeviceConnection connection = manager.openDevice(device);
            if (connection == null || !connection.claimInterface(device.getInterface(0), true))
            {
                Log.d("ERROR","No connection to this device!");
                return;
            }

            byte[] data = new byte[8];
            Arrays.fill(data, (byte) 0);
            data[0] = onOff ? (byte) (255) : (byte) (253);
            data[1] = (byte) relayNum;
            sendCommand(data, 0, data.length, 20, connection, device.getInterface(0).getEndpoint(0));
        }
        catch (Exception e)
        {
            Log.d("ERROR",e.getMessage());
        }
    }

    public void sendCommand(byte[] data, int offset, int size, int timeout, UsbDeviceConnection connection, UsbEndpoint endPoint)
    {
        if (offset != 0)
        {
            data = Arrays.copyOfRange(data, offset, size);
        }

        if (endPoint == null)
        {
            Log.d("ERROR","Command Not Executed!");
        }
        else
        {
            connection.controlTransfer(0x21, 0x09, 0x0300, 0x00, data, size, timeout);
        }
    }

    private UsbDevice[] getUsbRelaysArray()
    {
        List<UsbDevice> relays = null;
        UsbManager manager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);

        if (manager == null)
        {
            return null;
        }

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext())
        {
            UsbDevice device = deviceIterator.next();
            Log.d("DEVICE","****************************");
            Log.d("DEVICE",device.getDeviceName());
            Log.d("DEVICE",String.valueOf(device.getDeviceId()));
            Log.d("DEVICE",String.valueOf(device.getVendorId()));
            Log.d("DEVICE","****************************");

            if (relays == null)
            {
                relays = new ArrayList<>();
            }

            if (device.getVendorId() == 0x16C0 && device.getProductId() == 0x05DF)
            {
                relays.add(device);
            }
        }

        if (relays == null || relays.size() == 0)
        {
            return null;
        }
        else
        {
            return relays.toArray(new UsbDevice[relays.size()]);
        }
    }

    private BroadcastReceiver usbRelayReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(ACTION_USB_PERMISSION))
            {
                synchronized (this)
                {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        if (device == null)
                        {
                            Toast.makeText(context, "Check USB Permissions", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    };

    public void setUSBRelayPermission()
    {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
        );

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbRelayReceiver, filter);
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext())
        {
            UsbDevice device = deviceIterator.next();

            if (device.getVendorId() == 5824)
            {
                manager.requestPermission(device, permissionIntent);
            }
        }
    }
}