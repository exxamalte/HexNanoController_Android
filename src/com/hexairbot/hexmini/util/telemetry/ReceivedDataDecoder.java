package com.hexairbot.hexmini.util.telemetry;

import android.util.Log;
import com.hexairbot.hexmini.modal.OSDCommon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Decodes data received from the copter via bluetooth.
 *
 * @author Malte Franken
 */
public class ReceivedDataDecoder {

  private static final String TAG = ReceivedDataDecoder.class.getSimpleName();

  public static final int CHECKSUM_MASK = 0xff;

  public static final byte MSP_HEADER_START = '$';
  public static final byte MSP_HEADER_M = 'M';
  public static final byte MSP_HEADER_ARROW = '>';

  private BlockingQueue<Byte> queue = new LinkedBlockingQueue<Byte>();
  private List<TelemetryDataListener> telemetryDataListeners = new ArrayList<TelemetryDataListener>();

  public ReceivedDataDecoder() {
    DataConsumer consumer = new DataConsumer(queue);
    new Thread(consumer).start();
  }

  public void add(String data) {
    byte[] bytes = data.getBytes();
    Log.v(TAG, "Adding data to queue: " + Arrays.toString(bytes));
    for (byte b : bytes) {
      queue.offer(b);
    }
  }

  protected void dataDecoded(CommandData commandData) {
    if (telemetryDataListeners != null) {
      for (TelemetryDataListener listener : telemetryDataListeners) {
        listener.receivedTelemetryData(commandData);
      }
    }
  }

  public void registerTelemetryDataListener(TelemetryDataListener listener) {
    telemetryDataListeners.add(listener);
  }

  public void unregisterTelemetryDataListener(TelemetryDataListener listener) {
    telemetryDataListeners.remove(listener);
  }

  protected class DataConsumer implements Runnable {
    private final BlockingQueue<Byte> queue;

    private static final int EXPECTING_START = 0,
      EXPECTING_M = 1,
      EXPECTING_ARROW = 2,
      EXPECTING_PAYLOAD_SIZE = 3,
      EXPECTING_COMMAND = 4,
      EXPECTING_PAYLOAD = 5;

    private int currentState = EXPECTING_START;
    private int payloadSize = 0;
    private byte[] payload;
    private int checksum = 0;
    OSDCommon.MSPCommand command = null;
    private int payloadOffset = 0;

    protected DataConsumer(BlockingQueue<Byte> q) {
      queue = q;
    }

    public void run() {
      try {
        while (true) {
          consume(queue.take());
        }
      } catch (InterruptedException ex) {
      }
    }

    /**
     * Command byte order:
     * <ul>
     * <li>$</li>
     * <li>M</li>
     * <li>&gt;</li>
     * <li>payload size: count of bytes which follow after the command byte</li>
     * <li>command</li>
     * <li>payload</li>
     * <li>checksum</li>
     * </ul>
     *
     * @param b the next byte to decode
     */
    protected void consume(byte b) {
      Log.v(TAG, "Decoding byte: " + b);
      switch (currentState) {
        case (EXPECTING_START):
          if (b == MSP_HEADER_START) {
            currentState = EXPECTING_M;
          }
          break;
        case (EXPECTING_M):
          if (b == MSP_HEADER_M) {
            currentState = EXPECTING_ARROW;
          }
          break;
        case (EXPECTING_ARROW):
          if (b == MSP_HEADER_ARROW) {
            Log.d(TAG, "Header detected");
            payloadSize = 0;
            checksum = 0;
            command = null;
            payloadOffset = 0;
            currentState = EXPECTING_PAYLOAD_SIZE;
          }
          break;
        case (EXPECTING_PAYLOAD_SIZE):
          // read payload size
          payloadSize = b;
          // and initialise the payload buffer
          payload = new byte[payloadSize];
          checksum ^= payloadSize;
          Log.d(TAG, "Payload size: " + payloadSize);
          currentState = EXPECTING_COMMAND;
          break;
        case (EXPECTING_COMMAND):
          Log.v(TAG, "Raw command: " + b);
          // decode command
          command = OSDCommon.MSPCommand.fromInt(b);
          checksum ^= b;
          Log.d(TAG, "Command: " + command);
          currentState = EXPECTING_PAYLOAD;
          break;
        case (EXPECTING_PAYLOAD):
          if (payloadOffset < payloadSize) {
            // store byte as payload
            payload[payloadOffset] = b;
            checksum ^= b;
            payloadOffset++;
          } else {
            // this must be our checksum
            if ((checksum & CHECKSUM_MASK) != b) {
              // TODO: do anything other than logging here?
              Log.w(TAG, "Checksum not matching, expected " + (checksum & CHECKSUM_MASK) + ", but received " + b);
            } else {
              Log.d(TAG, "Checksum matches");
            }
            if (command != null) {
              // now let anyone know who's interested in the result
              CommandData commandData = decode(command, payload, new Date());
              dataDecoded(commandData);
            }
            // and finally off to the next sequence
            currentState = EXPECTING_START;
          }
          break;
      }
    }
  }

  public static CommandData decode(OSDCommon.MSPCommand command, byte[] payload, Date received) {
    CommandData commandData = new CommandData(command, received);
    // Note: with regard to expected payload sizes, refer to corresponding code in firmware (Serial.ino)
    switch (command) {
      case MSP_RAW_IMU:
        if ((payload != null) && (payload.length == 18)) {
          // acc
          double ax = read16(Arrays.copyOfRange(payload, 0, 2));
          double ay = read16(Arrays.copyOfRange(payload, 2, 4));
          double az = read16(Arrays.copyOfRange(payload, 4, 6));
          // gyro
          double gx = read16(Arrays.copyOfRange(payload, 6, 8)) / 8;
          double gy = read16(Arrays.copyOfRange(payload, 8, 10)) / 8;
          double gz = read16(Arrays.copyOfRange(payload, 10, 12)) / 8;
          // mag
          double magx = read16(Arrays.copyOfRange(payload, 12, 14)) / 3;
          double magy = read16(Arrays.copyOfRange(payload, 14, 16)) / 3;
          double magz = read16(Arrays.copyOfRange(payload, 16, 18)) / 3;
          commandData.setPayload(new double[]{ax, ay, az, gx, gy, gz, magx, magy, magz});
          Log.i(TAG, "RAW_IMU: ax=" + String.format("%.2f", ax) + ", ay=" + String.format("%.2f", ay)
            + ", az=" + String.format("%.2f", az) + ", gx=" + String.format("%.2f", gx)
            + ", gy=" + String.format("%.2f", gy) + ", gz=" + String.format("%.2f", gz)
            + ", magx=" + String.format("%.2f", magx) + ", magy=" + String.format("%.2f", magy)
            + ", magz=" + String.format("%.2f", magz));
        } else {
          Log.w(TAG, "Command RAW_IMU expecting 18 bytes of data, found: " + (payload == null ? "<empty>" : payload.length));
        }
        break;
      case MSP_ALTITUDE:
        if ((payload != null) && (payload.length == 6)) {
          // altitude
          double altitude = read32(Arrays.copyOfRange(payload, 0, 4)) / 100;
          // vario
          double vario = read16(Arrays.copyOfRange(payload, 4, 6));
          commandData.setPayload(new double[]{altitude, vario});
          Log.i(TAG, "ALTITUDE: altitude=" + String.format("%.2f", altitude) + ", vario=" + String.format("%.2f", vario));
        } else {
          Log.w(TAG, "Command ALTITUDE expecting 6 bytes of data, found: " + (payload == null ? "<empty>" : payload.length));
        }
        break;
      case MSP_ATTITUDE:
        if ((payload != null) && (payload.length == 8)) {
          // angx
          double angx = read16(Arrays.copyOfRange(payload, 0, 2)) / 10;
          // angy
          double angy = read16(Arrays.copyOfRange(payload, 2, 4)) / 10;
          // heading
          double heading = read16(Arrays.copyOfRange(payload, 4, 6));
          // headFreeModeHold
          double headFreeModeHold = read16(Arrays.copyOfRange(payload, 6, 8));
          commandData.setPayload(new double[]{angx, angy, heading, headFreeModeHold});
          Log.i(TAG, "ATTITUDE: angx=" + String.format("%.2f", angx) + ", angy=" + String.format("%.2f", angy)
            + ", heading=" + String.format("%.2f", heading) + ", headFreeModeHold=" + String.format("%.2f", headFreeModeHold));
        } else {
          Log.w(TAG, "Command ATTITUDE expecting 8 bytes of data, found: " + (payload == null ? "<empty>" : payload.length));
        }
        break;
      case MSP_ARM:
        Log.i(TAG, "ARM: <no data>");
        break;
      case MSP_DISARM:
        Log.i(TAG, "DISARM: <no data>");
        break;
      case MSP_ACC_CALIBRATION:
        Log.i(TAG, "ACC_CALIBRATION: <no data>");
        break;
      case MSP_MAG_CALIBRATION:
        Log.i(TAG, "MAG_CALIBRATION: <no data>");
        break;
      default:
        Log.i(TAG, "Not handling commands of type " + command + " at the moment.");
    }
    return commandData;
  }

  protected static int read16(byte[] bytes) {
    assert bytes != null;
    assert bytes.length == 2;
    int result = bytes[0];
    result |= (bytes[1]) << 8;
    return result;
  }

  protected static int read32(byte[] bytes) {
    assert bytes != null;
    assert bytes.length == 4;
    int result = bytes[0];
    result |= (bytes[1]) << 8;
    result |= (bytes[2]) << 16;
    result |= (bytes[3]) << 24;
    return result;
  }

}
