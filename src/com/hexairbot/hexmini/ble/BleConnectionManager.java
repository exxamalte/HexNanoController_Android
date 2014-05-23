package com.hexairbot.hexmini.ble;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * @author koupoo
 */
public class BleConnectionManager {
  private static final String TAG = BleConnectionManager.class.getSimpleName();

  private BluetoothDevice currentDevice;

  private Context context;
  private BleConnectionManagerDelegate delegate;

  private BluetoothLeService mBluetoothLeService;

  private boolean isConnected;

  // Code to manage Service lifecycle.
  private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
      if (!mBluetoothLeService.initialize()) {
        Log.e(TAG, "Unable to initialize Bluetooth");
        mBluetoothLeService = null;
      } else {
        Log.e(TAG, "mBluetoothLeService is okay");
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      Log.e(TAG, "onServiceDisconnected");
      // mBluetoothLeService = null;
    }
  };

  // Handles various events fired by the Service.
  // ACTION_GATT_CONNECTED: connected to a GATT server.
  // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
  // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
  // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
  //                        or notification operations.
  private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      final String action = intent.getAction();
      if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {  //���ӳɹ�
        Log.e(TAG, "Only gatt, just wait");
      } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //�Ͽ�����
        Log.e(TAG, "ACTION_GATT_DISCONNECTED");

        Log.e(TAG, "thread name:" + Thread.currentThread().getName());

        isConnected = false;

        if (delegate != null) {
          delegate.didDisconnect(BleConnectionManager.this);
        }
      } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) //���Կ�ʼ�ɻ���
      {
        //Toast.makeText(BleConnectionManager.this.context, "���ӳɹ������ڿ�����ͨ�ţ�", Toast.LENGTH_SHORT).show();
        isConnected = true;

            	/*
        		if(currentConnection != connection) {
        			Log.d(TAG, "didConnect:old connection, just ignore");
        		}
        		*/

        if (delegate != null) {
          delegate.didConnect(BleConnectionManager.this);
        }
      } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { //�յ����
        Log.e(TAG, "RECV DATA");
        String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);


        if (delegate != null) {
          delegate.didReceiveData(BleConnectionManager.this, data);
        }
      } else {
        Toast.makeText(BleConnectionManager.this.context, "Unkonwn��", Toast.LENGTH_SHORT).show();
      }
    }
  };


  public BleConnectionManager(Context context) {
    super();
    this.context = context;

    Intent gattServiceIntent = new Intent(this.context, BluetoothLeService.class);
    Log.d(TAG, "Try to bindService=" + this.context.bindService(gattServiceIntent, mServiceConnection, android.content.Context.BIND_AUTO_CREATE));

    this.context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
  }

  public BluetoothDevice getCurrentDevice() {
    return currentDevice;
  }

  public void connect(BluetoothDevice device) {
    Log.d(TAG, "try connect");

    if (device.equals(currentDevice)) {
      if (isConnected()) {
        return;
      } else {
        if (mBluetoothLeService != null) {
          mBluetoothLeService.connect(device.getAddress());
        }
      }
    } else {
      closeCurrentGatt();

      currentDevice = device;

      if (mBluetoothLeService != null) {
        mBluetoothLeService.connect(currentDevice.getAddress());
      }
    }
  }

  public void disconnect() {
    if (mBluetoothLeService != null) {
      mBluetoothLeService.disconnect();
    }
  }

  public void closeCurrentGatt() {
    if (mBluetoothLeService != null) {
      mBluetoothLeService.close();
      currentDevice = null;
    }
  }

  public void close() {
    Log.e(TAG, "release resource");

    if (mGattUpdateReceiver != null) {
      this.context.unregisterReceiver(mGattUpdateReceiver);
      mGattUpdateReceiver = null;
    }

    if (mServiceConnection != null) {
      this.context.unbindService(mServiceConnection);
      mServiceConnection = null;
    }

    if (mBluetoothLeService != null) {
      mBluetoothLeService.close();
      mBluetoothLeService = null;
    }

    Log.d(TAG, "releaseSource");
  }

  public void sendData(String data) {
    if (mBluetoothLeService != null && isConnected()) {
      mBluetoothLeService.WriteValue(data);
    }
  }

  public void sendData(byte[] data) {
    if (mBluetoothLeService != null && isConnected()) {
      mBluetoothLeService.WriteValue(data);
    }
  }

  public boolean isConnected() {
    return isConnected;
  }

  public BleConnectionManagerDelegate getDelegate() {
    return delegate;
  }

  public void setDelegate(BleConnectionManagerDelegate delegate) {
    this.delegate = delegate;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  private IntentFilter makeGattUpdateIntentFilter() {                        //ע����յ��¼�
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
    intentFilter.addAction(BluetoothDevice.ACTION_UUID);
    return intentFilter;
  }
}
