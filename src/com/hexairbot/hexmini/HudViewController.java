package com.hexairbot.hexmini;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Resources;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.BatteryManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import com.hexairbot.hexmini.HexMiniApplication.AppStage;
import com.hexairbot.hexmini.ble.BleConnectionManager;
import com.hexairbot.hexmini.gestures.EnhancedGestureDetector;
import com.hexairbot.hexmini.modal.ApplicationSettings;
import com.hexairbot.hexmini.modal.Channel;
import com.hexairbot.hexmini.modal.OSDCommon;
import com.hexairbot.hexmini.modal.Transmitter;
import com.hexairbot.hexmini.sensors.DeviceOrientationChangeDelegate;
import com.hexairbot.hexmini.sensors.DeviceOrientationManager;
import com.hexairbot.hexmini.sensors.DeviceSensorManagerWrapper;
import com.hexairbot.hexmini.ui.*;
import com.hexairbot.hexmini.ui.Image.SizeParams;
import com.hexairbot.hexmini.ui.Sprite.Align;
import com.hexairbot.hexmini.ui.joystick.*;
import com.hexairbot.hexmini.ui.joystick.JoystickFactory.JoystickType;
import com.hexairbot.hexmini.util.FontUtils;
import com.hexairbot.hexmini.util.StopWatch;
import com.hexairbot.hexmini.util.telemetry.TelemetryDataRequester;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class HudViewController extends ViewController
  implements OnTouchListener, OnGestureListener, SettingsViewControllerDelegate, DeviceOrientationChangeDelegate {

  private static final String TAG = HudViewController.class.getSimpleName();

  private static final int JOY_ID_LEFT = 1;
  private static final int JOY_ID_RIGHT = 2;
  private static final int MIDDLE_BG_ID = 3;
  private static final int TOP_BAR_ID = 4;
  private static final int BOTTOM_BAR_ID = 5;
  private static final int TAKE_OFF_BTN_ID = 6;
  private static final int STOP_BTN_ID = 7;
  private static final int SETTINGS_BTN_ID = 8;
  private static final int ALT_HOLD_TOGGLE_BTN = 9;
  private static final int STATE_TEXT_VIEW = 10;
  private static final int BATTERY_INDICATOR_ID = 11;
  private static final int HELP_BTN = 12;
  private static final int BOTTOM_LEFT_SCREW = 13;
  private static final int BOTTOM_RIGHT_SCREW = 14;
  private static final int LOGO = 15;
  private static final int STATUS_BAR = 16;
  private static final int STOPWATCH_BAR = 17;
  private static final int STOPWATCH_TEXT_VIEW = 18;
  private static final int STOPWATCH_RESET_BTN_ID = 19;

  private final float BEGINNER_ELEVATOR_CHANNEL_RATIO = 0.5f;
  private final float BEGINNER_AILERON_CHANNEL_RATIO = 0.5f;
  private final float BEGINNER_RUDDER_CHANNEL_RATIO = 0.0f;
  private final float BEGINNER_THROTTLE_CHANNEL_RATIO = 0.8f;

  private Image bottomBarBg;

  private Button stopBtn;
  private Button takeOffBtn;
  private Button settingsBtn;
  private Button stopWatchResetBtn;
  private ToggleButton altHoldToggleBtn;

  private Text stateTextView;

  private boolean isAltHoldMode;
  private boolean isAccMode;
  private boolean isCaptureTelemetryData;

  private Button[] buttons;

  private Indicator batteryIndicator;

  private Text txtBatteryStatus;

  private GLSurfaceView glView;

  private JoystickBase[] joysticks;   //[0]roll and pitch, [1]rudder and throttle
  private float joypadOpacity;
  private GestureDetector gestureDetector;

  private UIRenderer renderer;

  private HudViewControllerDelegate delegate;

  private boolean isLeftHanded;
  private JoystickListener rollPitchListener;
  private JoystickListener rudderThrottleListener;

  private ApplicationSettings settings;

  private Channel aileronChannel;
  private Channel elevatorChannel;
  private Channel rudderChannel;
  private Channel throttleChannel;
  private Channel aux1Channel;
  private Channel aux2Channel;
  private Channel aux3Channel;
  private Channel aux4Channel;

  private DeviceOrientationManager deviceOrientationManager;
  private static final float ACCELERO_THRESHOLD = (float) Math.PI / 180.0f * 2.0f;
  private static final int PITCH = 1;
  private static final int ROLL = 2;
  private float pitchBase;
  private float rollBase;
  private boolean rollAndPitchJoystickPressed;

  private Text stopWatchTextView;
  private StopWatch stopWatch;

  private TelemetryDataRequester telemetryDataCapturer = new TelemetryDataRequester();

  private LocalBroadcastManager mLocalBroadcastManager;

  public HudViewController(Activity context, HudViewControllerDelegate delegate) {
    this.delegate = delegate;
    this.context = context;
    Transmitter.sharedTransmitter().setBleConnectionManager(new BleConnectionManager(context));

    settings = ((HexMiniApplication) context.getApplication()).getAppSettings();

    joypadOpacity = settings.getInterfaceOpacity();
    isLeftHanded = settings.isLeftHanded();

    this.context = context;
    gestureDetector = new EnhancedGestureDetector(context, this);

    joysticks = new JoystickBase[2];

    glView = new GLSurfaceView(context);
    glView.setEGLContextClientVersion(2);

    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    //LinearLayout hud = (LinearLayout)inflater.inflate(R.layout.hud, null);
    //LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

    //hud.addView(glView, layoutParams);
    //glView.setBackgroundResource(R.drawable.settings_bg);

    context.setContentView(glView);

    renderer = new UIRenderer(context, null);

    initGLSurfaceView();

    Resources res = context.getResources();

    Image topBarBg = new Image(res, R.drawable.bar_top, Align.TOP_CENTER);
    topBarBg.setSizeParams(SizeParams.FILL_SCREEN, SizeParams.NONE);
    topBarBg.setAlphaEnabled(true);

    bottomBarBg = new Image(res, R.drawable.bar_bottom, Align.BOTTOM_CENTER);
    bottomBarBg.setSizeParams(SizeParams.FILL_SCREEN, SizeParams.NONE);
    bottomBarBg.setAlphaEnabled(true);

    Image middleBg = new Image(res, R.drawable.bg_tile, Align.CENTER);
    middleBg.setAlpha(1f);
    middleBg.setVisible(false);
    middleBg.setSizeParams(SizeParams.REPEATED, SizeParams.REPEATED);
    middleBg.setAlphaEnabled(true);

    Image bottomLeftSkrew = new Image(res, R.drawable.screw, Align.BOTTOM_LEFT);
    Image bottomRightSkrew = new Image(res, R.drawable.screw, Align.BOTTOM_RIGHT);

    Image logo = new Image(res, R.drawable.logo, Align.BOTTOM_LEFT);
    logo.setMargin(0, 0, (int) res.getDimension(R.dimen.hud_logo_margin_bottom), (int) res.getDimension(R.dimen.hud_logo_margin_left));

    Image statusBar = new Image(res, R.drawable.status_bar, Align.TOP_LEFT);
    statusBar.setMargin((int) res.getDimension(R.dimen.hud_status_bar_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_status_bar_margin_left));

    Image stopWatchBar = new Image(res, R.drawable.stopwatch_bar, Align.TOP_RIGHT);
    stopWatchBar.setMargin((int) res.getDimension(R.dimen.hud_stopwatch_bar_margin_top), (int) res.getDimension(R.dimen.hud_stopwatch_bar_margin_right), 0, 0);

    String stopWatchText = "00:00";
    stopWatchTextView = new Text(context, stopWatchText, Align.TOP_RIGHT);
    stopWatchTextView.setMargin((int) res.getDimension(R.dimen.hud_stopwatch_margin_top), (int) res.getDimension(R.dimen.hud_stopwatch_margin_right), 0, 0);
    stopWatchTextView.setTextColor(Color.WHITE);
    stopWatchTextView.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
    stopWatchTextView.setTextSize(res.getDimensionPixelSize(R.dimen.hud_state_text_size));

    stopWatchResetBtn = new Button(res, R.drawable.btn_stopwatchreset_normal, R.drawable.btn_stopwatchreset_hl, Align.TOP_RIGHT);
    stopWatchResetBtn.setMargin((int) res.getDimension(R.dimen.hud_btn_stopwatch_reset_margin_top), (int) res.getDimension(R.dimen.hud_btn_stopwatch_reset_margin_right), 0, 0);

    settingsBtn = new Button(res, R.drawable.btn_settings_normal, R.drawable.btn_settings_hl, Align.TOP_RIGHT);
    settingsBtn.setMargin((int) res.getDimension(R.dimen.hud_btn_settings_margin_top), (int) res.getDimension(R.dimen.hud_btn_settings_margin_right), 0, 0);

    Button helpBtn = new Button(res, R.drawable.btn_help_normal, R.drawable.btn_help_hl, Align.TOP_RIGHT);
    helpBtn.setMargin((int) res.getDimension(R.dimen.hud_btn_settings_margin_top), (int) res.getDimension(R.dimen.hud_btn_settings_margin_right) * 4, 0, 0);

    takeOffBtn = new Button(res, R.drawable.btn_take_off_normal, R.drawable.btn_take_off_hl, Align.BOTTOM_CENTER);
    takeOffBtn.setAlphaEnabled(true);
    stopBtn = new Button(res, R.drawable.btn_stop_normal, R.drawable.btn_stop_hl, Align.TOP_CENTER);
    stopBtn.setAlphaEnabled(true);

    String state = context.getResources().getString(R.string.settings_item_connection_state_not_conneceted);
    stateTextView = new Text(context, state, Align.TOP_LEFT);
    stateTextView.setMargin((int) res.getDimension(R.dimen.hud_state_text_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_state_text_margin_left));
    stateTextView.setTextColor(Color.WHITE);
    stateTextView.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
    stateTextView.setTextSize(res.getDimensionPixelSize(R.dimen.hud_state_text_size));

    int batteryIndicatorRes[] = {R.drawable.btn_battery_0,
      R.drawable.btn_battery_1,
      R.drawable.btn_battery_2,
      R.drawable.btn_battery_3,
      R.drawable.btn_battery_4
    };

    batteryIndicator = new Indicator(res, batteryIndicatorRes, Align.TOP_LEFT);
    batteryIndicator.setMargin((int) res.getDimension(R.dimen.hud_batterry_indicator_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_batterry_indicator_margin_left));

    altHoldToggleBtn = new ToggleButton(res, R.drawable.alt_hold_off, R.drawable.alt_hold_off_hl,
      R.drawable.alt_hold_on, R.drawable.alt_hold_on_hl,
      R.drawable.alt_hold_on, Align.TOP_LEFT);

    altHoldToggleBtn.setMargin(res.getDimensionPixelOffset(R.dimen.hud_alt_hold_toggle_btn_margin_top), 0, 0, res.getDimensionPixelOffset(R.dimen.hud_alt_hold_toggle_btn_margin_left));
    altHoldToggleBtn.setChecked(settings.isAltHoldMode());
    altHoldToggleBtn.setVisible(false);
    //altHoldToggleBtn.setAlphaEnabled(true);

    buttons = new Button[6];
    buttons[0] = settingsBtn;
    buttons[1] = takeOffBtn;
    buttons[2] = stopBtn;
    buttons[3] = altHoldToggleBtn;
    buttons[4] = helpBtn;
    buttons[5] = stopWatchResetBtn;

    renderer.addSprite(MIDDLE_BG_ID, middleBg);
    renderer.addSprite(TOP_BAR_ID, topBarBg);
    renderer.addSprite(BOTTOM_BAR_ID, bottomBarBg);
    renderer.addSprite(BOTTOM_LEFT_SCREW, bottomLeftSkrew);
    renderer.addSprite(BOTTOM_RIGHT_SCREW, bottomRightSkrew);
    renderer.addSprite(LOGO, logo);
    renderer.addSprite(STATUS_BAR, statusBar);
    renderer.addSprite(BATTERY_INDICATOR_ID, batteryIndicator);
    renderer.addSprite(TAKE_OFF_BTN_ID, takeOffBtn);
    renderer.addSprite(STOP_BTN_ID, stopBtn);
    renderer.addSprite(SETTINGS_BTN_ID, settingsBtn);
    renderer.addSprite(ALT_HOLD_TOGGLE_BTN, altHoldToggleBtn);
    renderer.addSprite(STATE_TEXT_VIEW, stateTextView);
    renderer.addSprite(STOPWATCH_BAR, stopWatchBar);
    renderer.addSprite(STOPWATCH_TEXT_VIEW, stopWatchTextView);
    renderer.addSprite(STOPWATCH_RESET_BTN_ID, stopWatchResetBtn);
    //renderer.addSprite(HELP_BTN, helpBtn);

    isAccMode = settings.isAccMode();
    deviceOrientationManager = new DeviceOrientationManager(new DeviceSensorManagerWrapper(this.context), this);
    deviceOrientationManager.onCreate();

    initJoystickListeners();

    helpBtn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent intent = new Intent(HudViewController.this.context, HelpActivity.class);
        HudViewController.this.context.startActivity(intent);
      }
    });

    if (isAccMode) {
      initJoysticks(JoystickType.ACCELERO);
    } else {
      initJoysticks(JoystickType.ANALOGUE);
    }

    stopWatch = new StopWatch();
    initStopWatch(stopWatch, stopWatchTextView);

    isCaptureTelemetryData = settings.isCaptureTelemetryData();
    initTelemetryDataCapturer(isCaptureTelemetryData);

    initListeners();

    initChannels();

    if (settings.isHeadFreeMode()) {
      aux1Channel.setValue(1);
    } else {
      aux1Channel.setValue(-1);
    }

    if (settings.isAltHoldMode()) {
      aux2Channel.setValue(1);
    } else {
      aux2Channel.setValue(-1);
    }

    if (settings.isBeginnerMode()) {
      new AlertDialog.Builder(context)
        .setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.dialog_title_info)
        .setMessage(R.string.beginner_mode_info)
        .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {

          }
        }).show();
    }

    mLocalBroadcastManager = LocalBroadcastManager.getInstance(this.context);
    registerAllBroadcastReceiver();
  }

  private void initStopWatch(final StopWatch stopWatch, final Text stopWatchTextView) {
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        long time = stopWatch.measure();
        String timeFormatted = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(time), TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));
        Log.d(TAG, "Stopwatch output: " + timeFormatted);
        stopWatchTextView.setText(timeFormatted);
      }
    }, 0, 1000);
  }

  private void initTelemetryDataCapturer(boolean captureTelemetryData) {
    if (captureTelemetryData) {
      telemetryDataCapturer.start();
    } else {
      telemetryDataCapturer.stop();
    }
  }

  private void initChannels() {
    aileronChannel = settings.getChannel(Channel.CHANNEL_NAME_AILERON);
    elevatorChannel = settings.getChannel(Channel.CHANNEL_NAME_ELEVATOR);
    rudderChannel = settings.getChannel(Channel.CHANNEL_NAME_RUDDER);
    throttleChannel = settings.getChannel(Channel.CHANNEL_NAME_THROTTLE);
    aux1Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX1);
    aux2Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX2);
    aux3Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX3);
    aux4Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX4);

    aileronChannel.setValue(0.0f);
    elevatorChannel.setValue(0.0f);
    rudderChannel.setValue(0.0f);
    throttleChannel.setValue(-1);
  }

  private void initJoystickListeners() {
    rollPitchListener = new JoystickListener() {
      public void onChanged(JoystickBase joy, float x, float y) {
        if (HexMiniApplication.sharedApplication().getAppStage() == AppStage.SETTINGS) {
          Log.d(TAG, "AppStage.SETTINGS ignore rollPitchListener onChanged");
          return;
        }

        if (!isAccMode && rollAndPitchJoystickPressed) {
          Log.v(TAG, "rollPitchListener onChanged x:" + x + "y:" + y);

          if (settings.isBeginnerMode()) {
            aileronChannel.setValue(x * BEGINNER_AILERON_CHANNEL_RATIO);
            elevatorChannel.setValue(y * BEGINNER_ELEVATOR_CHANNEL_RATIO);
          } else {
            aileronChannel.setValue(x);
            elevatorChannel.setValue(y);
          }
        }
      }

      @Override
      public void onPressed(JoystickBase joy) {
        rollAndPitchJoystickPressed = true;
      }

      @Override
      public void onReleased(JoystickBase joy) {
        rollAndPitchJoystickPressed = false;

        aileronChannel.setValue(0.0f);
        elevatorChannel.setValue(0.0f);

      }
    };

    rudderThrottleListener = new JoystickListener() {
      public void onChanged(JoystickBase joy, float x, float y) {
        if (HexMiniApplication.sharedApplication().getAppStage() == AppStage.SETTINGS) {
          Log.d(TAG, "AppStage.SETTINGS ignore rudderThrottleListener onChanged");
          return;
        }

        Log.v(TAG, "rudderThrottleListener onChanged x:" + x + "y:" + y);

        if (settings.yawEnable()) {
          rudderChannel.setValue(x);
        }
        else{
          rudderChannel.setValue(0);
        }

        if (settings.isBeginnerMode()) {
          //rudderChannel.setValue(x * BEGINNER_RUDDER_CHANNEL_RATIO);
          throttleChannel.setValue((BEGINNER_THROTTLE_CHANNEL_RATIO - 1) + y * BEGINNER_THROTTLE_CHANNEL_RATIO);

        } else {
          //rudderChannel.setValue(x);
          throttleChannel.setValue(y);
        }
      }

      @Override
      public void onPressed(JoystickBase joy) {
        if (settings.isHoverOnThrottleReleaseMode()) {
          // turning off hover mode
          aux2Channel.setValue(-1);
          Log.d(TAG, "Turning off hover mode");
        }
      }

      @Override
      public void onReleased(JoystickBase joy) {
        Log.v(TAG, "rudderThrottleListener onReleased: set rudder to 0, keep throttle as before");
        rudderChannel.setValue(0.0f);
        if (settings.isHoverOnThrottleReleaseMode()) {
          // put the copter into hover mode
          aux2Channel.setValue(1);
          Log.d(TAG, "Turning on hover mode");
        }
      }
    };
  }

  private void initListeners() {
    settingsBtn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {

        if (delegate != null) {
          delegate.settingsBtnDidClick(arg0);
        }

      }
    });

    stopWatchResetBtn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        stopWatch.reset();
      }
    });

    takeOffBtn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        throttleChannel.setValue(-1);
        getRudderAndThrottleJoystick().setYValue(-1);
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_ARM);
        stopWatch.start();
      }
    });

    stopBtn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_DISARM);
        stopWatch.pause();
      }
    });


    altHoldToggleBtn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        isAltHoldMode = !isAltHoldMode;
        settings.setIsAltHoldMode(isAltHoldMode);
        settings.save();

        altHoldToggleBtn.setChecked(isAltHoldMode);

        if (isAltHoldMode) {
          aux2Channel.setValue(1);
        } else {
          aux2Channel.setValue(-1);
        }
      }
    });
  }

  private void initGLSurfaceView() {
    if (glView != null) {
      glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
      glView.setRenderer(renderer);
      glView.setOnTouchListener(this);
    }
  }

  private void initJoysticks(JoystickType rollAndPitchType) {
    JoystickBase rollAndPitchJoystick = getRollAndPitchJoystick();
    JoystickBase rudderAndThrottleJoystick = getRudderAndThrottleJoystick();

    if (rollAndPitchType == JoystickType.ANALOGUE) {
      if (rollAndPitchJoystick == null || !(rollAndPitchJoystick instanceof AnalogueJoystick)) {
        rollAndPitchJoystick = JoystickFactory.createAnalogueJoystick(this.getContext(), false, rollPitchListener, true);
        rollAndPitchJoystick.setXDeadBand(settings.getAileronDeadBand());
        rollAndPitchJoystick.setYDeadBand(settings.getElevatorDeadBand());
      } else {
        rollAndPitchJoystick.setOnAnalogueChangedListener(rollPitchListener);
      }
    } else if (rollAndPitchType == JoystickType.ACCELERO) {
      if (rollAndPitchJoystick == null || !(rollAndPitchJoystick instanceof AcceleratorJoystick)) {
        rollAndPitchJoystick = JoystickFactory.createAcceleroJoystick(this.getContext(), false, rollPitchListener, true);
        //rollAndPitchJoystick.setXDeadBand(settings.getAileronDeadBand());
        //rollAndPitchJoystick.setYDeadBand(settings.getElevatorDeadBand());
      } else {
        rollAndPitchJoystick.setOnAnalogueChangedListener(rollPitchListener);
      }
    }

    if (rudderAndThrottleJoystick == null || !(rudderAndThrottleJoystick instanceof AnalogueJoystick)) {
      rudderAndThrottleJoystick = JoystickFactory.createAnalogueJoystick(this.getContext(), false, rudderThrottleListener, false);
      rudderAndThrottleJoystick.setXDeadBand(settings.getRudderDeadBand());
    } else {
      rudderAndThrottleJoystick.setOnAnalogueChangedListener(rudderThrottleListener);
    }

    rollAndPitchJoystick.setIsRollPitchJoystick(true);
    rudderAndThrottleJoystick.setIsRollPitchJoystick(false);

    joysticks[0] = rollAndPitchJoystick;
    joysticks[1] = rudderAndThrottleJoystick;

    setJoysticks();

    getRudderAndThrottleJoystick().setYValue(-1);
  }

  public void setJoysticks() {
    JoystickBase rollAndPitchJoystick = joysticks[0];
    JoystickBase rudderAndThrottleJoystick = joysticks[1];

    if (rollAndPitchJoystick != null) {
      if (isLeftHanded) {
        joysticks[0].setAlign(Align.BOTTOM_RIGHT);
        joysticks[0].setAlpha(joypadOpacity);
      } else {
        joysticks[0].setAlign(Align.BOTTOM_LEFT);
        joysticks[0].setAlpha(joypadOpacity);
      }

      rollAndPitchJoystick.setNeedsUpdate();
    }

    if (rudderAndThrottleJoystick != null) {
      if (isLeftHanded) {
        joysticks[1].setAlign(Align.BOTTOM_LEFT);
        joysticks[1].setAlpha(joypadOpacity);
      } else {
        joysticks[1].setAlign(Align.BOTTOM_RIGHT);
        joysticks[1].setAlpha(joypadOpacity);
      }

      rudderAndThrottleJoystick.setNeedsUpdate();
    }

    for (JoystickBase joystick : joysticks) {
      if (joystick != null) {
        joystick.setInverseYWhenDraw(true);

        int margin = context.getResources().getDimensionPixelSize(R.dimen.hud_joy_margin);

        joystick.setMargin(0, margin, bottomBarBg.getHeight() + margin, margin);
      }
    }

    renderer.removeSprite(JOY_ID_LEFT);
    renderer.removeSprite(JOY_ID_RIGHT);

    if (rollAndPitchJoystick != null) {
      if (isLeftHanded) {
        renderer.addSprite(JOY_ID_RIGHT, rollAndPitchJoystick);
      } else {
        renderer.addSprite(JOY_ID_LEFT, rollAndPitchJoystick);
      }
    }

    if (rudderAndThrottleJoystick != null) {
      if (isLeftHanded) {
        renderer.addSprite(JOY_ID_LEFT, rudderAndThrottleJoystick);
      } else {
        renderer.addSprite(JOY_ID_RIGHT, rudderAndThrottleJoystick);
      }
    }
  }

  public JoystickBase getRollAndPitchJoystick() {
    return joysticks[0];
  }

  public JoystickBase getRudderAndThrottleJoystick() {
    return joysticks[1];
  }

  public void setInterfaceOpacity(float opacity) {
    if (opacity < 0 || opacity > 100.0f) {
      Log.w(TAG, "Can't set interface opacity. Invalid value: " + opacity);
      return;
    }

    joypadOpacity = opacity / 100f;

    Sprite joystick = renderer.getSprite(JOY_ID_LEFT);
    joystick.setAlpha(joypadOpacity);

    joystick = renderer.getSprite(JOY_ID_RIGHT);
    joystick.setAlpha(joypadOpacity);
  }

  public void setBatteryValue(final int percent) {
    if (percent > 100 || percent < 0) {
      Log.w(TAG, "Can't set battery value. Invalid value " + percent);
      return;
    }

    int imgNum = Math.round((float) percent / 100.0f * 4.0f);

    //txtBatteryStatus.setText(percent + "%");

    if (imgNum < 0)
      imgNum = 0;

    if (imgNum > 4)
      imgNum = 4;

    if (batteryIndicator != null) {
      batteryIndicator.setValue(imgNum);
    }
  }

  public void setSettingsButtonEnabled(boolean enabled) {
    settingsBtn.setEnabled(enabled);
  }

  public void setDoubleTapClickListener(OnDoubleTapListener listener) {
    gestureDetector.setOnDoubleTapListener(listener);
  }

  public void onPause() {
    if (glView != null) {
      glView.onPause();
    }

    deviceOrientationManager.pause();
  }

  public void onResume() {
    if (glView != null) {
      glView.onResume();
    }

    deviceOrientationManager.resume();
  }

  //glView onTouch Event handler
  public boolean onTouch(View v, MotionEvent event) {
    boolean result = false;

    for (Button button : buttons) {
      if (button.processTouch(v, event)) {
        result = true;
        break;
      }
    }

    if (!result) {
      gestureDetector.onTouchEvent(event);

      for (JoystickBase joy : joysticks) {
        if (joy != null) {
          if (joy.processTouch(v, event)) {
            result = true;
          }
        }
      }
    }

    return result;
  }

  public void onDestroy() {
    renderer.clearSprites();
    deviceOrientationManager.destroy();
    unregisterAllBroadcastReceiver();
  }

  public boolean onDown(MotionEvent e) {
    return false;
  }

  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                         float velocityY) {
    return false;
  }

  public void onLongPress(MotionEvent e) {
    // Left unimplemented
  }

  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                          float distanceY) {
    return false;
  }

  public void onShowPress(MotionEvent e) {
    // Left unimplemented
  }

  public boolean onSingleTapUp(MotionEvent e) {
    return false;
  }

  public View getRootView() {
    if (glView != null) {
      return glView;
    }

    Log.w(TAG, "Can't find root view");
    return null;
  }

  @Override
  public void interfaceOpacityValueDidChange(float newValue) {
    setInterfaceOpacity(newValue);
  }

  @Override
  public void leftHandedValueDidChange(boolean isLeftHanded) {
    this.isLeftHanded = isLeftHanded;

    setJoysticks();

    Log.d(TAG, "THRO:" + throttleChannel.getValue());

    getRudderAndThrottleJoystick().setYValue(throttleChannel.getValue());
  }

  @Override
  public void accModeValueDidChange(boolean isAccMode) {
    this.isAccMode = isAccMode;

    initJoystickListeners();

    if (isAccMode) {
      initJoysticks(JoystickType.ACCELERO);
    } else {
      initJoysticks(JoystickType.ANALOGUE);
    }
  }

  @Override
  public void captureTelemetryDataValueDidChange(boolean isCaptureTelemetryData) {
    this.isCaptureTelemetryData = isCaptureTelemetryData;
    initTelemetryDataCapturer(isCaptureTelemetryData);
  }

  @Override
  public void headfreeModeValueDidChange(boolean isHeadfree) {
    if (settings.isHeadFreeMode()) {
      aux1Channel.setValue(1);
    } else {
      aux1Channel.setValue(-1);
    }
  }

  @Override
  public void aileronAndElevatorDeadBandValueDidChange(float newValue) {
    JoystickBase rollAndPitchJoyStick = getRollAndPitchJoystick();

    rollAndPitchJoyStick.setXDeadBand(newValue);
    rollAndPitchJoyStick.setYDeadBand(newValue);
  }

  @Override
  public void rudderDeadBandValueDidChange(float newValue) {
    JoystickBase rudderAndThrottleStick = getRudderAndThrottleJoystick();

    rudderAndThrottleStick.setXDeadBand(newValue);
  }

  @Override
  public void onDeviceOrientationChanged(float[] orientation,
                                         float magneticHeading, int magnetoAccuracy) {
    if (!rollAndPitchJoystickPressed) {
      pitchBase = orientation[PITCH];
      rollBase = orientation[ROLL];
      aileronChannel.setValue(0.0f);
      elevatorChannel.setValue(0.0f);

      Log.v(TAG, "before pressed ROLL:" + rollBase + ",PITCH:" + pitchBase);
    } else {
      float x = (orientation[PITCH] - pitchBase);
      float y = (orientation[ROLL] - rollBase);

      if (isAccMode) {
        Log.v(TAG, "ROLL:" + (-x) + ",PITCH:" + y);

        if (Math.abs(x) > ACCELERO_THRESHOLD || Math.abs(y) > ACCELERO_THRESHOLD) {
          if (settings.isBeginnerMode()) {
            aileronChannel.setValue(-x * BEGINNER_AILERON_CHANNEL_RATIO);
            elevatorChannel.setValue(y * BEGINNER_ELEVATOR_CHANNEL_RATIO);
          } else {
            aileronChannel.setValue(-x);
            elevatorChannel.setValue(y);
          }
        }
      }
    }
  }

  @Override
  public void didConnect() {
    String state = context.getResources().getString(R.string.settings_item_connection_state_conneceted);
    stateTextView.setText(state);
    // try to capture one-off telemetry data
    telemetryDataCapturer.initialRequest();
  }

  @Override
  public void didDisconnect() {
    String state = context.getResources().getString(R.string.settings_item_connection_state_not_conneceted);
    stateTextView.setText(state);
  }

  @Override
  public void didFailToConnect() {
    String state = context.getResources().getString(R.string.settings_item_connection_state_not_conneceted);
    stateTextView.setText(state);
  }

  @Override
  public void beginnerModeValueDidChange(boolean isBeginnerMode) {

  }

  @Override
  public void hoverOnThrottleReleaseModeValueDidChange(boolean isHoverOnThrottleReleaseMode) {

  }

  private void registerAllBroadcastReceiver() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);
    this.context.registerReceiver(receiver, filter);
    IntentFilter decodeFilter = new IntentFilter();
    mLocalBroadcastManager.registerReceiver(receiver, decodeFilter);
  }

  private void unregisterAllBroadcastReceiver() {
    this.context.unregisterReceiver(receiver);
    mLocalBroadcastManager.unregisterReceiver(receiver);
  }

  private BroadcastReceiver receiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context arg0, Intent intent) {
      String action = intent.getAction();
      if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
        final int level = intent.getIntExtra(
          BatteryManager.EXTRA_LEVEL, 0);
        final int scale = intent.getIntExtra(
          BatteryManager.EXTRA_SCALE, 0);
        final int status = intent.getIntExtra(
          BatteryManager.EXTRA_STATUS, 0);
        Log.d(TAG, String.format("level=%d,scale=%d,status=%d", level,
          scale, status));

        setBatteryValue(level);
        //battery_phone.setImageLevel(level / 25);
        //battery_phone_text.setText(level + "%");
      }
    }
  };

  @Override
  public void yawEnableValueDidChange(boolean isYawEnable) {
  }
}
