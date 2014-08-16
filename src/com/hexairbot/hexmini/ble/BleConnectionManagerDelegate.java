package com.hexairbot.hexmini.ble;

public interface BleConnectionManagerDelegate {
  void didConnect(BleConnectionManager manager);
  void didDisconnect(BleConnectionManager manager);
  void didFailToConnect(BleConnectionManager manager);
  void didReceiveData(BleConnectionManager manager, byte[] data);
  void onReadRemoteRssi(int rssi, int status);
}
