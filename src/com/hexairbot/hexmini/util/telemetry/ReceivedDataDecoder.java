package com.hexairbot.hexmini.util.telemetry;

import android.util.Log;
import com.hexairbot.hexmini.modal.OSDCommon;

import java.util.Arrays;

/**
 * Decodes data received from the copter via bluetooth.
 *
 * @author Malte Franken
 */
public class ReceivedDataDecoder {

  private static final String TAG = ReceivedDataDecoder.class.getSimpleName();

  public static final int CHECKSUM_MASK = 0xff;
  public static final String MSP_HEADER_RECEIVING = "$M>";

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
   * @param data the data to decode
   */
  public static void decode(String data) {
    // if some data has been captured before, let's just concatenate everything and try to decode again
    //receivedDataBuffer.append(incoming);
    //String data = receivedDataBuffer.toString();
    //receivedDataBuffer.delete(0, receivedDataBuffer.length()-1);
    byte[] bytes = data.getBytes();
    Log.d(TAG, "Data package length: " + bytes.length);
    if (data.startsWith(MSP_HEADER_RECEIVING)) {
      Log.d(TAG, "Header detected");
      int payloadSize = 0;
      OSDCommon.MSPCommand mspCommand = null;
      int checksum = 0;
      byte[] payload = null;
      // check payload size
      if (bytes.length >= 4) {
        payloadSize = bytes[3];
        checksum = payloadSize;
        Log.d(TAG, "Payload size: " + payloadSize);
      }
      // check command
      if (bytes.length >= 5) {
        int command = bytes[4];
        checksum ^= command;
        mspCommand = OSDCommon.MSPCommand.fromInt(command);
        Log.d(TAG, "Command: " + mspCommand);
      }
      // check payload data
      if (payloadSize > 0) {
        // now check for payload
        if (bytes.length >= (6 + payloadSize)) {
          // all good - maybe too much data but at least just as much as promised
          payload = Arrays.copyOfRange(bytes, 5, 5 + payloadSize);
          Log.d(TAG, "Payload: " + Arrays.toString(payload));
          for (byte payloadByte : payload) {
            checksum ^= payloadByte;
          }
        } else {
          // something went wrong
          Log.w(TAG, "Payload size not matching, expected " + payloadSize + ", but received " + (bytes.length - 6));
          // maybe some more data is arriving soon; store data received so far
          // receivedDataBuffer.append(data);
        }
      } else {
        // probably command does not come with payload
        Log.d(TAG, "No payload expected");
      }
      if (bytes.length >= (6 + payloadSize)) {
        int checksumData = bytes[5 + payloadSize];
        if ((checksum & CHECKSUM_MASK) != checksumData) {
          Log.w(TAG, "Checksum not matching, expected " + (checksum & CHECKSUM_MASK) + ", but received " + checksumData);
        } else {
          Log.d(TAG, "Checksum matches");
        }
      } else {
        Log.w(TAG, "No checksum found, expected " + (checksum & CHECKSUM_MASK));
      }
      if (mspCommand != null) {
        decode(mspCommand, payload);
      }
    } else {
      Log.w(TAG, "Received data without header: " + data);
    }
  }

  public static void decode(OSDCommon.MSPCommand command, byte[] payload) {
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
          double vario = read32(Arrays.copyOfRange(payload, 4, 6));
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
