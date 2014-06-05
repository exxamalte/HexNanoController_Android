package com.hexairbot.hexmini.util.telemetry;

import com.hexairbot.hexmini.modal.OSDCommon;

import java.util.Arrays;
import java.util.Date;

/**
 * The incoming command with its payload (if any).
 *
 * @author Malte Franken
 */
public class CommandData {

  private OSDCommon.MSPCommand command;
  private Object payload = null;
  private Date received = new Date();

  public CommandData(OSDCommon.MSPCommand command) {
    this.command = command;
  }

  public CommandData(OSDCommon.MSPCommand command, Date received) {
    this.command = command;
    this.received = received;
  }

  public OSDCommon.MSPCommand getCommand() {
    return command;
  }

  public Object getPayload() {
    return payload;
  }

  public void setPayload(Object payload) {
    this.payload = payload;
  }

  public Date getReceived() {
    return received;
  }

  public void setReceived(Date received) {
    this.received = received;
  }

  @Override
  public String toString() {
    String payloadOutput = "<empty>";
    if (payload != null) {
      if (payload instanceof double[]) {
        payloadOutput = Arrays.toString((double[]) payload);
      }
    }
    return "CommandData[" +
      "command=" + command +
      ", payload=" + payloadOutput +
      ']';
  }
}
