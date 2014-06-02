package com.hexairbot.hexmini.util.telemetry;

import com.hexairbot.hexmini.modal.OSDCommon;
import com.hexairbot.hexmini.modal.Transmitter;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Request telemetric data from the copter.
 *
 * @author Malte Franken
 */
public class TelemetryDataRequester {
  public static final long LOG_PERIOD_IN_MILLIS = 1000;

  private Timer timer = new Timer();

  public void start() {
    // stop all tasks just in case this method is called twice
    timer.cancel();
    timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_RAW_IMU);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_ALTITUDE);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_ATTITUDE);
      }
    }, 0, LOG_PERIOD_IN_MILLIS);
  }

  public void stop() {
    timer.cancel();
  }

}
