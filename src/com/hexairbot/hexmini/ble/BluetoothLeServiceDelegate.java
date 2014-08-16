package com.hexairbot.hexmini.ble;

/**
 * Used to pass information from bluetooth le service on to interested parties.
 *
 * @author Malte Franken
 */
public interface BluetoothLeServiceDelegate {
  void onReadRemoteRssi(int rssi, int status);
}
