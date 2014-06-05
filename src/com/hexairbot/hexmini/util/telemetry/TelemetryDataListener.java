package com.hexairbot.hexmini.util.telemetry;

/**
 * Implement this if you want to be notified if new data has arrived.
 *
 * @author Malte Franken
 */
public interface TelemetryDataListener {
  void receivedTelemetryData(CommandData commandData);
}
