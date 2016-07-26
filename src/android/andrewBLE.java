
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class andrewBLE extends CordovaPlugin implements BluetoothAdapter.LeScanCallback{
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothGatt mBluetoothGatt;

  private long scanPeriod;
  private int signalStrength;
  private String mBluetoothDeviceAddress;
  private boolean mScanning = false;

  private Handler mHandler;
  private CallbackContext
          connectCallback,
          onButtonPressedCallback,
          onConnectionStateChangeCallback;

  private static final int REQUEST_ENABLE_BLUETOOTH = 1;
  private String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
  private static final String TAG = "andrewBLE";

  public andrewBLE(){
      mHandler = new Handler();

      //My Default values
      this.scanPeriod     = 5000;
      this.signalStrength = -75;

      final BluetoothManager bluetoothManager =
              (BluetoothManager) cordova.getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
      mBluetoothAdapter = bluetoothManager.getAdapter();
  }
  
  
  /*public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }*/


  @Override
  public void onDestroy() {
      super.onDestroy();
  }

  @Override
  public boolean execute(final String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if(action.equals("startScan")){
      connectCallback = callbackContext;
      if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        cordova.startActivityForResult(this, enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
      }
      else {
        //Escanea y conecta
        scanLeDevice(true);
      }
    }
    else if(action.equals("stopScan")){
      scanLeDevice(false);
      callbackContext.success();
    }
    else if(action.equals("disconnect")){
      if (mBluetoothAdapter == null || mBluetoothGatt == null) {
        Log.w(TAG, "BluetoothAdapter not initialized");
        callbackContext.error("BluetoothAdapter not initialized.");
      }
      else{
        Log.i(TAG, "Disconecting BTLE");
        mBluetoothGatt.disconnect();

        Log.i(TAG, "Closing BTLE");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        
        callbackContext.success();
      }
    }else if (action.equals("onButtonPressed")) {
      onButtonPressedCallback = callbackContext;
      PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
      result.setKeepCallback(true);
      callbackContext.sendPluginResult(result);
    }
    else if(action.equals("onConnectionStateChange")){
      onConnectionStateChangeCallback = callbackContext;
      PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
      result.setKeepCallback(true);
      callbackContext.sendPluginResult(result);
    }
    else{
      Log.e(TAG, "Operacion no soportada: "+action);
      return false;
    }


    return true;

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Check which request we're responding to
    if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
      // Make sure the request was successful
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "User enabled Bluetooth");
        //Escanea y conecta
        scanLeDevice(true);
      }
      else{
        Log.d(TAG, "User did *NOT* enable Bluetooth");
        if (connectCallback != null) {
          connectCallback.error("Por favor encienda el Bluetooth");
        }
      }
      connectCallback = null;
    }
  }
  
  private void scanLeDevice(final boolean enable) {
      if (enable && !mScanning) {
          Log.d(TAG, "Escaneando...");

          // Stops scanning after a pre-defined scan period.
          mHandler.postDelayed(new Runnable() {
              @Override
              public void run() {
                  Log.w(TAG,"Tiempo de Escaneo agotado");

                  mScanning = false;
                  mBluetoothAdapter.stopLeScan(this);
              }
          }, scanPeriod);

          mScanning = true;
          mBluetoothAdapter.startLeScan(this);
          //mBluetoothAdapter.startLeScan(uuids, this);
      }
      else {
          mScanning = false;
          mBluetoothAdapter.stopLeScan(this);
      }
  }

  // Device scan callback.
  //private BluetoothAdapter.LeScanCallback
    //      this = new BluetoothAdapter.LeScanCallback() {
      @Override
      public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
          final int new_rssi = rssi;
          if (rssi >= signalStrength) {
              Log.i(TAG, "New BLUETOOTH: "+device.getName()+"; RSSI: "+rssi);
              //if(device.getName() != null && device.getName().replace(" ","").equals("MLE-15")){
              //if(device.getAddress().equals("FF:FF:B0:00:F7:E1")){ //Negro
              if(device.getAddress().equals("FF:FF:F0:00:CF:0E")){   //Blanco
                  Log.i(TAG, "MLE-15 Found!!!");
                  Log.i(TAG, "¡Dispositivo encontrado!");
                  scanLeDevice(false);

                  connect(device.getAddress());
              }
          }
      }
  //};

  private boolean connect(final String address) {
      if (mBluetoothAdapter == null || address == null) {
          Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
          if (connectCallback != null) {
              connectCallback.error("No es posible utilizar el Bluetooth.");
          }
          return false;
      }

      // Previously connected device.  Try to reconnect.
      if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
          Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");

          if (mBluetoothGatt.connect()) {
              Log.i(TAG, "BLE is already connected.");
              return true;
          }
        /*else {
            Log.i(ma.TAG, "Can't connect to the existent BLE ["+mBluetoothDeviceAddress+"].");

            return false;
        }*/
      }

      final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

      if (device == null) {
          Log.w(TAG, "BLE Device not found.  Unable to connect.");
          if (connectCallback != null) {
              connectCallback.error("Imposible conectar, no se encuentra el dispositivo BLE.");
          }
          return false;
      }

      // We want to directly connect to the device, so we are setting the autoConnect
      // parameter to false.
      mBluetoothGatt = device.connectGatt(cordova.getActivity().getApplicationContext(), false, mGattCallback);
      Log.d(TAG, "Trying to create a new connection.");
      mBluetoothDeviceAddress = address;

      if (connectCallback != null) {
          try{
              JSONObject data = new JSONObject();
              data.put("name", (device.getName() != null ? device.getName().replace(" ","") : "Sin nombre"));
              data.put("address", device.getAddress());

              //Regresar los datos del BLE
              PluginResult result = new PluginResult(PluginResult.Status.OK, data);
              result.setKeepCallback(true);
              connectCallback.sendPluginResult(result);
          }
          catch(JSONException e){
              Log.d(TAG, "Error al convertir los datos a JSON.");
              if(connectCallback != null)
                  connectCallback.error("Error al convertir los datos a JSON.");
          }
      }

      return true;
  }

  // Implements callback methods for GATT events that the app cares about.  For example,
  // connection change and services discovered.
  private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
      @Override
      public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
          if (newState == BluetoothProfile.STATE_CONNECTED) {
              Log.i(TAG, "Connected to GATT server.");

              PluginResult result = new PluginResult(PluginResult.Status.OK, true);
              result.setKeepCallback(true);
              onConnectionStateChangeCallback.sendPluginResult(result);
              //final Intent intent = new Intent(ACTION_GATT_CONNECTED);
              //ma.getApplicationContext().sendBroadcast(intent);

              // Attempts to discover services after successful connection.
              Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
          }
          else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
              Log.i(TAG, "Disconnected from GATT server.");

              PluginResult result = new PluginResult(PluginResult.Status.OK, false);
              result.setKeepCallback(true);
              onConnectionStateChangeCallback.sendPluginResult(result);
              //final Intent intent = new Intent(ACTION_GATT_DISCONNECTED);
              //ma.getApplicationContext().sendBroadcast(intent);
          }
      }

      @Override
      public void onServicesDiscovered(BluetoothGatt gatt, int status) {
          if (status == BluetoothGatt.GATT_SUCCESS) {
              Log.i(TAG, "Service Discovered satus: " + status);
              //final Intent intent = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
              //ma.getApplicationContext().sendBroadcast(intent);

              List<BluetoothGattService> servicesList = mBluetoothGatt.getServices();
              for (BluetoothGattService service : servicesList) {
                  String s = service.getUuid().toString();
                  if(!s.equals("0000ffe0-0000-1000-8000-00805f9b34fb")){continue;}

                  Log.i(TAG, "|- Service: " + s);
                  List<BluetoothGattCharacteristic> characteristicsList = service.getCharacteristics();
                  for (BluetoothGattCharacteristic characteristic: characteristicsList) {
                      String c = characteristic.getUuid().toString();
                      if(!c.equals("0000ffe1-0000-1000-8000-00805f9b34fb")){continue;}
                      Log.i(TAG, "|--- characteristic: " + characteristic.getUuid().toString());
                      setCharacteristicNotification(characteristic, true);
                  }
              }
          }
          else {
              Log.w(TAG, "onServicesDiscovered received: " + status);
              Log.i(TAG, "NO Services Discovered??");
          }
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt,
                                       BluetoothGattCharacteristic characteristic,
                                       int status) {

          if (status == BluetoothGatt.GATT_SUCCESS) {
              Log.i(TAG, "*** Caracteristica leida: " + characteristic.getUuid());
          }
      }

      @Override
      public void onCharacteristicChanged(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic) {


          Log.i(TAG, "Caracteristica ["+characteristic.getUuid()+"] ha cambiado: ");
          byte[] rawValue = characteristic.getValue();
          //String strValue = null;
          int intValue = 0;

          if(rawValue.length > 0) intValue = (int)rawValue[0];
          if(rawValue.length > 1) intValue += ((int)rawValue[1] << 8);
          if(rawValue.length > 2) intValue += ((int)rawValue[2] << 8);
          if(rawValue.length > 3) intValue += ((int)rawValue[3] << 8);



          // not known type of characteristic, so we need to handle this in "general" way
          // get first four bytes and transform it to integer
        /*if (rawValue.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(rawValue.length);
            for (byte byteChar : rawValue) {
                stringBuilder.append(String.format("%c", byteChar));
            }
            strValue = stringBuilder.toString();
        }*/
          String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(new Date());
          //Log.i(TAG, "Nuevo Dato: " + strValue + "; at: " + timestamp);
          Log.i(TAG, "New Data: " + intValue + " at: " + timestamp);

          //final Intent intent = new Intent(ACTION_GATT_DATA_AVAILABLE);
          //ma.getApplicationContext().sendBroadcast(intent);

          PluginResult result = new PluginResult(PluginResult.Status.OK, 1);
          result.setKeepCallback(true);
          onButtonPressedCallback.sendPluginResult(result);

      }

      //@Override
      //public void onCharacteristicWrite(BluetoothGatt gatt,
      //BluetoothGattCharacteristic characteristic, int status) {
      //
      //}
  };

  /**
   * Enables or disables notification on a give characteristic.
   *
   * @param characteristic Characteristic to act on.
   * @param enabled If true, enable notification.  False otherwise.
   */
  public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {

      if (mBluetoothAdapter == null || mBluetoothGatt == null) {
          Log.w(TAG, "BluetoothAdapter not initialized");
          return;
      }

      mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

      if (enabled) {
          Log.w(TAG, "Notification to characteristic [" + characteristic.getUuid().toString() + "] has been SET!");
          //final Intent intent = new Intent(ACTION_GATT_NOTIFICATION_ENABLED);
          //ma.getApplicationContext().sendBroadcast(intent);
      }
      else{
          Log.w(TAG, "Notification to characteristic [" + characteristic.getUuid().toString() + "] has been UNSET!");
          //final Intent intent = new Intent(ACTION_GATT_NOTIFICATION_DISABLED);
          //ma.getApplicationContext().sendBroadcast(intent);
      }


      BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
              UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));

      if(descriptor == null){
          Log.w(TAG, "Characteristic ["+characteristic.getUuid().toString()+
                  "] does not have descriptor: "+CLIENT_CHARACTERISTIC_CONFIG);
          return;
      }

      if (enabled) {
          descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
          Log.w(TAG, "Notification to descriptor [" + descriptor.getUuid().toString() + "] has been SET!");
          //final Intent intent = new Intent(ACTION_GATT_NOTIFICATION_ENABLED);
          //ma.getApplicationContext().sendBroadcast(intent);
      }
      else {
          descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
          Log.w(TAG, "Notification to descriptor [" + descriptor.getUuid().toString() + "] has been UNSET!");
          //final Intent intent = new Intent(ACTION_GATT_NOTIFICATION_DISABLED);
          //ma.getApplicationContext().sendBroadcast(intent);
      }

      mBluetoothGatt.writeDescriptor(descriptor);
  }


}//End of Class
