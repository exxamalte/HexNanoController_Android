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
  public static final long LOG_PERIOD_IN_MILLIS = 100;

  private Timer timer = new Timer();

  public void start() {
    // stop all tasks just in case this method is called twice
    timer.cancel();
    // regular telemetry data
    timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_STATUS);
        //Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_SERVO);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_MOTOR);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_RC);
        //Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_ANALOG);
        //Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_RC_TUNING);
        //Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_PID);
        //Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_BOX);
        //Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_MISC);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_RAW_IMU);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_ALTITUDE);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_ATTITUDE);
      }
    }, 0, LOG_PERIOD_IN_MILLIS);
  }

  public void stop() {
    timer.cancel();
  }

  public void initialRequest() {
    // one-off telemetry data
    Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_IDENT);
    Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_BOXNAMES);
    Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_PIDNAMES);
  }
}
