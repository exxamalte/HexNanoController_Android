package com.hexairbot.hexmini.modal;

import android.util.Log;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListParser;
import com.hexairbot.hexmini.HexMiniApplication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ApplicationSettings {

  private static final String TAG = ApplicationSettings.class.getSimpleName();

  private final static String INTERFACE_OPACITY = "InterfaceOpacity";
  private final static String IS_LEFT_HANDED = "IsLeftHanded";
  public final static String IS_FIRST_RUN = "IsFirstRun";
  private final static String IS_ACC_MODE = "IsAccMode";
  private final static String IS_CAPTURE_TELEMETRY_DATA = "IsCaptureTelemetryData";
  private final static String IS_HEAD_FREE_MODE = "IsHeadFreeMode";
  private final static String YAW_ENABLE = "YawEnable";
  private final static String IS_ALT_HOLD_MODE = "IsAltHoldMode";
  private final static String IS_BEGINNER_MODE = "IsBeginnerMode";
  private final static String IS_HOVER_ON_THROTTLE_RELEASE_MODE = "IsHoverOnThrottleReleaseMode";
  private final static String AILERON_DEAD_BAND = "AileronDeadBand";
  private final static String ELEVATOR_DEAD_BAND = "ElevatorDeadBand";
  private final static String RUDDER_DEAD_BAND = "RudderDeadBand";
  private final static String TAKE_OFF_THROTTLE = "TakeOffThrottle";
  public final static String CHANNELS = "Channels";

  private String path;

  private NSDictionary data;

  private float interfaceOpacity;
  private boolean isLeftHanded;
  private boolean isAccMode;
  private boolean isCaptureTelemetryData;
  private boolean isFirstRun;
  private boolean isHeadFreeMode;
  private boolean yawEnable;
  private boolean isAltHoldMode;
  private boolean isBeginnerMode;
  private boolean isHoverOnThrottleReleaseMode;
  private float aileronDeadBand;
  private float elevatorDeadBand;
  private float rudderDeadBand;
  private float takeOffThrottle;


  private List<Channel> channels;

  public ApplicationSettings(String path) {
    this.path = path;

    try {
      data = (NSDictionary) PropertyListParser.parse(path);

      interfaceOpacity = ((NSNumber) data.objectForKey(INTERFACE_OPACITY)).floatValue();
      isLeftHanded = ((NSNumber) data.objectForKey(IS_LEFT_HANDED)).boolValue();
      isAccMode = ((NSNumber) data.objectForKey(IS_ACC_MODE)).boolValue();
      // special treatment for this setting as it was added later
      NSNumber isCaptureTelemetryDataNumber = (NSNumber) data.objectForKey(IS_CAPTURE_TELEMETRY_DATA);
      isCaptureTelemetryData = (isCaptureTelemetryDataNumber != null) && isCaptureTelemetryDataNumber.boolValue();
      isFirstRun = ((NSNumber) data.objectForKey(IS_FIRST_RUN)).boolValue();
      isHeadFreeMode = ((NSNumber) data.objectForKey(IS_HEAD_FREE_MODE)).boolValue();
      // special treatment for this setting as it was added later
      NSNumber yawEnableDataNumber = (NSNumber) data.objectForKey(YAW_ENABLE);
      yawEnable = (yawEnableDataNumber != null) && yawEnableDataNumber.boolValue();
      isAltHoldMode = ((NSNumber) data.objectForKey(IS_ALT_HOLD_MODE)).boolValue();
      isBeginnerMode = ((NSNumber) data.objectForKey(IS_BEGINNER_MODE)).boolValue();
      // special treatment for this setting as it was added later
      NSNumber isHoverOnThrottleReleaseModeNumber = (NSNumber) data.objectForKey(IS_HOVER_ON_THROTTLE_RELEASE_MODE);
      isHoverOnThrottleReleaseMode = (isHoverOnThrottleReleaseModeNumber != null) && isHoverOnThrottleReleaseModeNumber.boolValue();
      aileronDeadBand = ((NSNumber) data.objectForKey(AILERON_DEAD_BAND)).floatValue();
      elevatorDeadBand = ((NSNumber) data.objectForKey(ELEVATOR_DEAD_BAND)).floatValue();
      rudderDeadBand = ((NSNumber) data.objectForKey(RUDDER_DEAD_BAND)).floatValue();
      takeOffThrottle = ((NSNumber) data.objectForKey(TAKE_OFF_THROTTLE)).floatValue();

      NSArray rawChannels = (NSArray) data.objectForKey(ApplicationSettings.CHANNELS);
      int channelCount = rawChannels.count();

      channels = new ArrayList<Channel>(channelCount);

      for (int channelIdx = 0; channelIdx < channelCount; channelIdx++) {
        Channel oneChannel = new Channel(this, channelIdx);
        channels.add(oneChannel);
      }
    } catch (Exception e) {
      Log.e(TAG, "Failed to initialise application settings", e);
    }
  }

  public ApplicationSettings(InputStream inputStream) {
    try {
      data = (NSDictionary) PropertyListParser.parse(inputStream);
    } catch (Exception e) {
      Log.e(TAG, "Failed to initialise application settings", e);
    }
  }

  public boolean save() {
    File file = new File(path);
    try {
      //save as xml��be compatible with the plist of iOS
      PropertyListParser.saveAsXML(data, file);
    } catch (IOException e) {
      Log.e(TAG, "Failed to save data", e);
      return false;
    }
    return true;
  }

  public void resetToDefault() {
    ApplicationSettings defaultSettings = new ApplicationSettings(HexMiniApplication.sharedApplication().getFilesDir() + "/DefaultSettings.plist");

    this.data = defaultSettings.getData();

    this.interfaceOpacity = defaultSettings.getInterfaceOpacity();
    this.isLeftHanded = defaultSettings.isLeftHanded();
    this.isAccMode = defaultSettings.isAccMode();
    this.isCaptureTelemetryData = defaultSettings.isCaptureTelemetryData();
    this.isFirstRun = defaultSettings.isFirstRun();
    this.isHeadFreeMode = defaultSettings.isHeadFreeMode();
    this.yawEnable = defaultSettings.yawEnable();
    this.isAltHoldMode = defaultSettings.isAltHoldMode();
    this.isBeginnerMode = defaultSettings.isBeginnerMode();
    this.isHoverOnThrottleReleaseMode = defaultSettings.isHoverOnThrottleReleaseMode();
    this.aileronDeadBand = defaultSettings.getAileronDeadBand();
    this.elevatorDeadBand = defaultSettings.getElevatorDeadBand();
    this.rudderDeadBand = defaultSettings.getRudderDeadBand();
    this.takeOffThrottle = defaultSettings.getTakeOffThrottle();

    int channelCount = defaultSettings.getChannelCount();

    for (int defaultChannelIdx = 0; defaultChannelIdx < channelCount; defaultChannelIdx++) {
      Channel defaultChannel = new Channel(defaultSettings, defaultChannelIdx);
      Channel channel = this.getChannel(defaultChannel.getName());

      if (channel.getIdx() != defaultChannelIdx) {
        Channel needsReordedChannel = channels.get(defaultChannelIdx);
        needsReordedChannel.setIdx(channel.getIdx());

        Channel tmp = channels.get(defaultChannelIdx);

        channels.set(defaultChannelIdx, channels.get(channel.getIdx()));
        channels.set(channel.getIdx(), tmp);

        channel.setIdx(defaultChannelIdx);
      }

      channel.setReversed(defaultChannel.isReversed());
      channel.setTrimValue(defaultChannel.getTrimValue());
      channel.setOutputAdjustabledRange(defaultChannel.getOutputAdjustabledRange());
      channel.setDefaultOutputValue(defaultChannel.getDefaultOutputValue());
      channel.setValue(channel.getDefaultOutputValue());
    }
  }

  public NSDictionary getData() {
    return data;
  }

  public void setData(NSDictionary data) {
    this.data = data;
  }

  public float getInterfaceOpacity() {
    return interfaceOpacity;
  }

  public void setInterfaceOpacity(float interfaceOpacity) {
    this.interfaceOpacity = interfaceOpacity;
    data.put(INTERFACE_OPACITY, interfaceOpacity);
  }

  public boolean isLeftHanded() {
    return isLeftHanded;
  }

  public boolean isFirstRun() {
    return isFirstRun;
  }

  public void setLeftHanded(boolean isLeftHanded) {
    this.isLeftHanded = isLeftHanded;
    data.put(IS_LEFT_HANDED, isLeftHanded);
  }

  public void setIsFirstRun(boolean isFirstRun) {
    this.isFirstRun = isFirstRun;
    data.put(IS_FIRST_RUN, isFirstRun);
  }

  public boolean isAccMode() {
    return isAccMode;
  }

  public void setIsAccMode(boolean isAccMode) {
    this.isAccMode = isAccMode;
    data.put(IS_ACC_MODE, isAccMode);
  }

  public boolean isCaptureTelemetryData() {
    return isCaptureTelemetryData;
  }

  public void setIsCaptureTelemetryData(boolean isCaptureTelemetryData) {
    this.isCaptureTelemetryData = isCaptureTelemetryData;
    data.put(IS_CAPTURE_TELEMETRY_DATA, isCaptureTelemetryData);
  }

  public boolean isHeadFreeMode() {
    return isHeadFreeMode;
  }

  public void setIsHeadFreeMode(boolean isHeadFreeMode) {
    this.isHeadFreeMode = isHeadFreeMode;
    data.put(IS_HEAD_FREE_MODE, isHeadFreeMode);
  }
  
  public boolean yawEnable() {
    return yawEnable;
  }

  public void setYawEnable(boolean yawEnable) {
    this.yawEnable = yawEnable;
    data.put(YAW_ENABLE, yawEnable);
  }

  public boolean isAltHoldMode() {
    return isAltHoldMode;
  }

  public boolean isBeginnerMode() {
    return isBeginnerMode;
  }

  public void setIsBeginnerMode(boolean isBeginnerMode) {
    this.isBeginnerMode = isBeginnerMode;
    data.put(IS_BEGINNER_MODE, isBeginnerMode);
  }

  public boolean isHoverOnThrottleReleaseMode() {
    return isHoverOnThrottleReleaseMode;
  }

  public void setIsHoverOnThrottleReleaseMode(boolean isHoverOnThrottleReleaseMode) {
    this.isHoverOnThrottleReleaseMode = isHoverOnThrottleReleaseMode;
    data.put(IS_HOVER_ON_THROTTLE_RELEASE_MODE, isHoverOnThrottleReleaseMode);
  }

  public void setIsAltHoldMode(boolean isAltHoldMode) {
    this.isAltHoldMode = isAltHoldMode;
    data.put(IS_LEFT_HANDED, isAltHoldMode);
  }

  public float getAileronDeadBand() {
    return aileronDeadBand;
  }

  public void setAileronDeadBand(float aileronDeadBand) {
    this.aileronDeadBand = aileronDeadBand;
    data.put(AILERON_DEAD_BAND, aileronDeadBand);
  }

  public float getElevatorDeadBand() {
    return elevatorDeadBand;
  }

  public void setElevatorDeadBand(float elevatorDeadBand) {
    this.elevatorDeadBand = elevatorDeadBand;
    data.put(ELEVATOR_DEAD_BAND, elevatorDeadBand);
  }

  public float getRudderDeadBand() {
    return rudderDeadBand;
  }

  public void setRudderDeadBand(float rudderDeadBand) {
    this.rudderDeadBand = rudderDeadBand;
    data.put(RUDDER_DEAD_BAND, rudderDeadBand);
  }

  public float getTakeOffThrottle() {
    return takeOffThrottle;
  }

  public void setTakeOffThrottle(float takeOffThrottle) {
    this.takeOffThrottle = takeOffThrottle;
    data.put(TAKE_OFF_THROTTLE, takeOffThrottle);
  }

  public int getChannelCount() {
    return channels.size();
  }

  public Channel getChannel(int idx) {
    if (idx < channels.size()) {
      return channels.get(idx);
    } else {
      return null;
    }
  }

  public Channel getChannel(String name) {
    for (Channel oneChannel : channels) {
      if (name.equals(oneChannel.getName())) {
        return oneChannel;
      }
    }

    return null;
  }
}
