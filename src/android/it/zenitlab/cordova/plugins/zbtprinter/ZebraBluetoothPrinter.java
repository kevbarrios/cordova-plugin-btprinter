package com.github.michael79bxl.zbtprinter;

import java.io.IOException;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.zebra.android.discovery.*;
import com.zebra.sdk.comm.*;
import com.zebra.sdk.printer.*;

public class ZebraBluetoothPrinter extends CordovaPlugin {

    private static final String LOG_TAG = "ZebraBluetoothPrinter";
    //String mac = "AC:3F:A4:1D:7A:5C";

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
    
    public void findPrinter(final CallbackContext callbackContext) {
      cordova.getThreadPool().execute(() -> {
        JSONArray printers = NonZebraDiscovery();
        if (printers != null) {
          callbackContext.success(printers);
        } else {
          callbackContext.error("Discovery Failed");
        }
      });   
    }

    private JSONArray NonZebraDiscovery() {
      JSONArray printers = new JSONArray();
      try {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = adapter.getBondedDevices();

        for (BluetoothDevice device : devices) {
          String name = device.getName();
          String mac = device.getAddress();

          JSONObject p = new JSONObject();
          p.put("name", name);
          p.put("address", mac);
          printers.put(p);
        }
      } catch (Exception e) {
        System.err.println(e.getMessage());
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
      connection.open();
      // Creates a ZebraPrinter object to use Zebra specific functionality like getCurrentStatus()
      ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
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
    }
}

