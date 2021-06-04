package com.kbarrios.btprinter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;

import com.zebra.sdk.comm.*;
import com.zebra.sdk.printer.*;

public class BluetoothPrinter extends CordovaPlugin {

    private static final String LOG_TAG = "BluetoothPrinter";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 0;

    private static String[] binaryArray = { "0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000",
            "1001", "1010", "1011", "1100", "1101", "1110", "1111" };
    private static String hexStr = "0123456789ABCDEF";

    public BluetoothPrinter() {
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("find")) {
            try {
                findPrinter(callbackContext);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        if (action.equals("printZPL")) {
            try {
                String mac = args.getString(0);
                String msg = args.getString(1);
                sendZPL(callbackContext, mac, msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        if (action.equals("printText")) {
            try {
                String mac = args.getString(0);
                String msg = args.getString(1);
                sendText(callbackContext, mac, msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        if (action.equals("printImage")) {
            try {
                String mac = args.getString(0);
                String msg = args.getString(1);
                sendBase64(callbackContext, mac, msg);
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
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                callbackContext.error("Device doesn't support Bluetooth");
            } else {
                try {
                    JSONArray printers = null;
                    // If Bluetooth is not on, request that it be enabled.
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        this.cordova.getActivity().startActivityForResult(enableIntent,REQUEST_ENABLE_BT);;
                        callbackContext.success(new JSONArray());
                    } else {
                        printers = listDevices(bluetoothAdapter);
                        if (printers != null) {
                            callbackContext.success(printers);
                        } else {
                            callbackContext.error("Not found devices");
                        }
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
     * This will send ZPL to be printed by the bluetooth printer
     */
    void sendZPL(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
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
                        Thread.sleep(1000);
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
    /*
     * This will send text to be printed by the bluetooth printer
     */
    void sendText(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    // If Bluetooth is not on, request that it be enabled.
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        cordova.getActivity().startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                        callbackContext.error("Bluetooth off");
                    } else {
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);
                        bluetoothAdapter.cancelDiscovery();
                        OutputStream mmOutputStream;
                        if (device != null) {
                            BluetoothSocket mSocket;
                            try {
                                mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                                // Open the connection - physical connection is established here.
                                mSocket.connect();
                                mmOutputStream = mSocket.getOutputStream();
                                // Send the data to printer.
                                mmOutputStream.write(msg.getBytes());
                                // Make sure the data got to the printer before closing the connection
                                Thread.sleep(1000);
                                mmOutputStream.flush();
                                mmOutputStream.close();
                                mSocket.close();
                                callbackContext.success("Was successfully printed");
                            } catch (IOException e) {
                                callbackContext.error("Socket error: " + e.getMessage());
                            }
                        } else {
                            callbackContext.error("Could not connect to " + mac);
                        }
                    }
                } catch(Exception e){
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }
    /*
     * This will send Base64 to be printed by the bluetooth printer
     */
    void sendBase64(final CallbackContext callbackContext, final String mac, final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    // If Bluetooth is not on, request that it be enabled.
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        cordova.getActivity().startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                        callbackContext.error("Bluetooth off");
                    } else {
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);
                        bluetoothAdapter.cancelDiscovery();
                        OutputStream mmOutputStream;
                        if (device != null) {
                            BluetoothSocket mSocket;
                            try {
                                mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                                // Open the connection - physical connection is established here.
                                mSocket.connect();
                                mmOutputStream = mSocket.getOutputStream();
                                // Base64 convert
                                byte[] decodedString = printBase64(msg);
                                Log.d("WebView", decodedString.toString());
                                Log.d("msg", msg);
                                // Send the data to printer.
                                mmOutputStream.write(decodedString);
                                // Make sure the data got to the printer before closing the connection
                                Thread.sleep(1000);
                                mmOutputStream.flush();
                                mmOutputStream.close();
                                mSocket.close();
                                callbackContext.success("Was successfully printed");
                            } catch (IOException e) {
                                callbackContext.error("Socket error: " + e.getMessage());
                            } catch (Exception e) {
                                callbackContext.error("Error: " + e.getMessage());
                            }
                        } else {
                            callbackContext.error("Could not connect to " + mac);
                        }
                    }
                } catch (Exception e) {
                    // Handle communications error here.
                    callbackContext.error(e.getMessage());
                }
            }
        }).start();
    }

    private byte[] printBase64(String msg) throws Exception {
        try {
            String base64Image = msg.split(",")[1];
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);

            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Bitmap bitmap = decodedByte;
            int mWidth = bitmap.getWidth();
            int mHeight = bitmap.getHeight();

            bitmap = resizeImage(bitmap, mWidth, mHeight);

            byte[] bt = decodeBitmapBase64(bitmap);

            return bt;

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
            throw new Exception("Bitmap error: " + e.getMessage());
        }
    }

    private static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();

        if (width > w) {
            float scaleWidth = ((float) w) / width;
            float scaleHeight = ((float) h) / height + 24;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleWidth);
            Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                    height, matrix, true);
            return resizedBitmap;
        } else {
            Bitmap resizedBitmap = Bitmap.createBitmap(w, height + 24, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(resizedBitmap);
            Paint paint = new Paint();
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, (w - width) / 2, 0, paint);
            return resizedBitmap;
        }
    }

    public static byte[] decodeBitmapBase64(Bitmap bmp) {
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        List<String> list = new ArrayList<String>(); // binaryString list
        StringBuffer sb;
        int bitLen = bmpWidth / 8;
        int zeroCount = bmpWidth % 8;
        String zeroStr = "";
        if (zeroCount > 0) {
            bitLen = bmpWidth / 8 + 1;
            for (int i = 0; i < (8 - zeroCount); i++) {
                zeroStr = zeroStr + "0";
            }
        }

        for (int i = 0; i < bmpHeight; i++) {
            sb = new StringBuffer();
            for (int j = 0; j < bmpWidth; j++) {
                int color = bmp.getPixel(j, i);

                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
                // if color close to white，bit='0', else bit='1'
                if (r > 160 && g > 160 && b > 160) {
                    sb.append("0");
                } else {
                    sb.append("1");
                }
            }
            if (zeroCount > 0) {
                sb.append(zeroStr);
            }
            list.add(sb.toString());
        }

        List<String> bmpHexList = binaryListToHexStringList(list);
        String commandHexString = "1D763000";

        // construct xL and xH
        // there are 8 pixels per byte. In case of modulo: add 1 to compensate.
        bmpWidth = bmpWidth % 8 == 0 ? bmpWidth / 8 : (bmpWidth / 8 + 1);
        int xL = bmpWidth % 256;
        int xH = (bmpWidth - xL) / 256;

        String xLHex = Integer.toHexString(xL);
        String xHHex = Integer.toHexString(xH);
        if (xLHex.length() == 1) {
            xLHex = "0" + xLHex;
        }
        if (xHHex.length() == 1) {
            xHHex = "0" + xHHex;
        }
        String widthHexString = xLHex + xHHex;

        // construct yL and yH
        int yL = bmpHeight % 256;
        int yH = (bmpHeight - yL) / 256;

        String yLHex = Integer.toHexString(yL);
        String yHHex = Integer.toHexString(yH);
        if (yLHex.length() == 1) {
            yLHex = "0" + yLHex;
        }
        if (yHHex.length() == 1) {
            yHHex = "0" + yHHex;
        }
        String heightHexString = yLHex + yHHex;

        List<String> commandList = new ArrayList<String>();
        commandList.add(commandHexString + widthHexString + heightHexString);
        commandList.addAll(bmpHexList);

        return hexList2Byte(commandList);
    }

    public static List<String> binaryListToHexStringList(List<String> list) {
        List<String> hexList = new ArrayList<String>();
        for (String binaryStr : list) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < binaryStr.length(); i += 8) {
                String str = binaryStr.substring(i, i + 8);

                String hexString = myBinaryStrToHexString(str);
                sb.append(hexString);
            }
            hexList.add(sb.toString());
        }
        return hexList;

    }

    public static String myBinaryStrToHexString(String binaryStr) {
        String hex = "";
        String f4 = binaryStr.substring(0, 4);
        String b4 = binaryStr.substring(4, 8);
        for (int i = 0; i < binaryArray.length; i++) {
            if (f4.equals(binaryArray[i])) {
                hex += hexStr.substring(i, i + 1);
            }
        }
        for (int i = 0; i < binaryArray.length; i++) {
            if (b4.equals(binaryArray[i])) {
                hex += hexStr.substring(i, i + 1);
            }
        }

        return hex;
    }

    public static byte[] hexList2Byte(List<String> list) {
        List<byte[]> commandList = new ArrayList<byte[]>();

        for (String hexStr : list) {
            commandList.add(hexStringToBytes(hexStr));
        }
        byte[] bytes = sysCopy(commandList);
        return bytes;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static byte[] sysCopy(List<byte[]> srcArrays) {
        int len = 0;
        for (byte[] srcArray : srcArrays) {
            len += srcArray.length;
        }
        byte[] destArray = new byte[len];
        int destLen = 0;
        for (byte[] srcArray : srcArrays) {
            System.arraycopy(srcArray, 0, destArray, destLen, srcArray.length);
            destLen += srcArray.length;
        }
        return destArray;
    }
}