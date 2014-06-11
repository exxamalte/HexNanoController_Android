package com.hexairbot.hexmini.util.telemetry;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple log dump.
 *
 * @author Malte Franken
 */
public class TelemetryDataLogger implements TelemetryDataListener {
  private static final String TAG = TelemetryDataLogger.class.getSimpleName();

  public static final String SIMPLE_TIMESTAMP_FORMAT = "yyyyMMddHHmmss";
  public static final char DATA_SEPARATOR = '\t';

  private Context context;

  private File file;

  public TelemetryDataLogger(Context context) {
    this.context = context;
    initLogfile();
  }

  protected void initLogfile() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)) {
      // filesystem writable
      DateFormat df = new SimpleDateFormat(SIMPLE_TIMESTAMP_FORMAT);
      String timestamp = df.format(new Date());
      file = new File(context.getExternalFilesDir(null), "flexbot-telemetry-" + timestamp + ".log");
      Log.i(TAG, "Log file: " + file.getAbsolutePath());
      if (!file.exists()) {
        try {
          boolean fileCreated = file.createNewFile();
          Log.d(TAG, "File created: " + fileCreated);
        } catch (IOException e) {
          Log.e(TAG, "Unable to open log file", e);
        }
      }
    }
  }

  @Override
  public void receivedTelemetryData(CommandData commandData) {
    Log.i(TAG, commandData.toString());
    // and now write to our own log file
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
      // Timestamp
      long timestamp = commandData.getReceived().getTime();
      writer.append(Long.toString(timestamp));
      writer.append(DATA_SEPARATOR);
      writer.append(toLog(commandData));
      writer.newLine();
      writer.close();
    } catch (IOException e) {
      Log.e(TAG, "Unable to write to log file", e);
    }
  }

  protected String toLog(CommandData commandData) {
    StringBuilder builder = new StringBuilder();
    builder.append(commandData.getCommand().toString());
    builder.append(DATA_SEPARATOR);
    Object payloadObject = commandData.getPayload();
    if (payloadObject != null) {
      // currently, only arrays of double values are stored
      if (payloadObject instanceof double[]) {
        double[] payload = (double[]) payloadObject;
        for (int i=0; i<payload.length; i++) {
          builder.append(String.format("%.2f", payload[i]));
          if (i<payload.length-1) {
            builder.append(DATA_SEPARATOR);
          }
        }
      }
    }
    return builder.toString();
  }
}
