package com.github.kevbarrios.zbtprinter;

import java.io.IOException;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.zebra.sdk.comm.*;
import com.zebra.sdk.printer.*;

public class ZebraBluetoothPrinter extends CordovaPlugin {

    private static final String LOG_TAG = "ZebraBluetoothPrinter";
    private static final int REQUEST_ENABLE_BT = 1;

    String [] permissions = { Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION };

    public ZebraBluetoothPrinter() {
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("print")) {
            try {
                String mac = args.getString(0);
                String msg = args.getString(1);
                sendData(callbackContext, mac, msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        if (action.equals("find")) {
            try {
                findPrinter(callbackContext);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean hasPermisssion() {
        for(String p : permissions){
            if(!PermissionHelper.hasPermission(this, p)){
                return false;
            }
        }
        return true;
    }

    public void findPrinter(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                callbackContext.error("Device doesn't support Bluetooth");
            } else {
                try {
                    boolean btPermission = hasPermisssion();
                    if (!btPermission) {
                        JSONArray printers = listDevices(adapter);
                        if (printers != null) {
                            callbackContext.success(printers);
                        } else {
                            callbackContext.error("Not found devices");
                        }
                    } else {
                        callbackContext.error("Permission denied");
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    Log.e(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private JSONArray listDevices(BluetoothAdapter adapter) {
        JSONArray printers = new JSONArray();
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        try {
            if (devices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : devices) {
                    String deviceName  = device.getName();
                    String deviceHardwareAddress  = device.getAddress(); // MAC address

                    JSONObject p = new JSONObject();
                    p.put("name", deviceName);
                    p.put("address", deviceHardwareAddress);
                    printers.put(p);
                }
            }
        }catch (Exception e) {
            System.err.println(e.getMessage());
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
        return printers;
    }
    /*
     * This will send data to be printed by the bluetooth printer
     */
    void sendData(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(mac);
                    // Verify the printer is ready to print
                    if (isPrinterReady(thePrinterConn)) {
                        // Open the connection - physical connection is established here.
                        thePrinterConn.open();
                        // Send the data to printer as a byte array.
                        thePrinterConn.write(msg.getBytes());
                        // Make sure the data got to the printer before closing the connection
                        Thread.sleep(500);
                        // Close the insecure connection to release resources.
                        thePrinterConn.close();
                        callbackContext.success("Done");
                    } else {
                        callbackContext.error("Printer is not ready");
                    }
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }

    private Boolean isPrinterReady(Connection connection) throws ConnectionException, ZebraPrinterLanguageUnknownException {
        Boolean isOK = false;
        try {
            connection.open();
            // getInstance(connection) default lenguage or getInstance(PrinterLanguage,connection) set a lenguage
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(PrinterLanguage.ZPL, connection);
            // Creates a ZebraPrinter object to use Zebra specific functionality like getCurrentStatus()
            PrinterStatus printerStatus = printer.getCurrentStatus();
            if (printerStatus.isReadyToPrint) {
                isOK = true;
            } else if (printerStatus.isPaused) {
                throw new ConnectionException("Cannot print because the printer is paused");
            } else if (printerStatus.isHeadOpen) {
                throw new ConnectionException("Cannot print because the printer media door is open");
            } else if (printerStatus.isPaperOut) {
                throw new ConnectionException("Cannot print because the paper is out");
            } else {
                throw new ConnectionException("Cannot print");
            }
            return isOK;
        } catch (ConnectionException e) {
            throw new ConnectionException("Cannot connected: " + e.getMessage());
        }
    }
}