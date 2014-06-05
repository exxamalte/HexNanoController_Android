package com.hexairbot.hexmini.util.telemetry;

import android.util.Log;

/**
 * Simple log dump.
 *
 * @author Malte Franken
 */
public class TelemetryDataLogger implements TelemetryDataListener {
  private static final String TAG = TelemetryDataLogger.class.getSimpleName();

  @Override
  public void receivedTelemetryData(CommandData commandData) {
    Log.i(TAG, commandData.toString());
  }
}
