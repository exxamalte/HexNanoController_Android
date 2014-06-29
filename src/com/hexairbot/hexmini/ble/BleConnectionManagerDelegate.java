package com.hexairbot.hexmini.ble;

public interface BleConnectionManagerDelegate {
	public void didConnect(BleConnectionManager manager);
	public void didDisconnect(BleConnectionManager manager);
	public void didFailToConnect(BleConnectionManager manager);
	public void didReceiveData(BleConnectionManager manager, byte[] data);
}
