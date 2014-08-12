package com.hexairbot.hexmini;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.hexairbot.hexmini.HexMiniApplication.AppStage;
import com.hexairbot.hexmini.modal.ApplicationSettings;
import com.hexairbot.hexmini.modal.OSDCommon;
import com.hexairbot.hexmini.modal.Transmitter;

@SuppressLint("NewApi")
public class HudActivity extends FragmentActivity implements SettingsDialogDelegate, OnTouchListener, HudViewControllerDelegate {
  private static final String TAG = HudActivity.class.getSimpleName();

  private SettingsDialog settingsDialog;
  private HudViewController hudVC;
  private View mDecorView;

  public static final int REQUEST_ENABLE_BT = 1;

  boolean isFirstRun = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    mDecorView = getWindow().getDecorView();
    hudVC = new HudViewController(this, this);
    hudVC.onCreate();
    hideSystemUI();

    ApplicationSettings settings = HexMiniApplication.sharedApplication().getAppSettings();

		/*
    if (settings.isFirstRun()) {
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			settings.setIsFirstRun(false);
			settings.save();
		}
		*/
  }

  @Override
  protected void onResume() {
    super.onResume();
    HexMiniApplication.sharedApplication().setAppStage(AppStage.HUD);
    hudVC.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    hudVC.onPause();
  }

  protected void showSettingsDialog() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    ft.addToBackStack(null);

    if (settingsDialog == null) {
      Log.d(TAG, "settingsDialog is null");
      settingsDialog = new SettingsDialog(this, this);
    }

    settingsDialog.show(ft, "settings");
  }

  @Override
  public void prepareDialog(SettingsDialog dialog) {

  }

  @Override
  public void onDismissed(SettingsDialog settingsDialog) {
    hudVC.setSettingsButtonEnabled(true);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return false;
  }

  private ApplicationSettings getSettings() {
    return ((HexMiniApplication) getApplication()).getAppSettings();
  }

  @Override
  public void settingsBtnDidClick(View settingsBtn) {
    hudVC.setSettingsButtonEnabled(false);
    showSettingsDialog();
  }

  public ViewController getViewController() {
    return hudVC;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
      finish();
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (Transmitter.sharedTransmitter().getBleConnectionManager() != null) {
      Transmitter.sharedTransmitter().transmitSimpleCommand(OSDCommon.MSPCommand.MSP_DISARM);
      Transmitter.sharedTransmitter().getBleConnectionManager().close();
    }

    hudVC.onDestroy();
  }

  protected void hideSystemUI() {
    // immersive sticky mode only available from Kitkat (API 19) onwards
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      Log.d(TAG, "About to hide system UI, current flags: " + mDecorView.getWindowSystemUiVisibility());
      // This snippet hides the system bars.
      int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LOW_PROFILE
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
      Log.d(TAG, "Hiding system UI, new flags: " + visibility);
      mDecorView.setSystemUiVisibility(visibility);
    } else {
      Log.d(TAG, "Not Android 4.4, not hiding system UI, current flags: " + mDecorView.getWindowSystemUiVisibility());
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    Log.d(TAG, "Window focus changed, has focus: " + hasFocus);
    if (hasFocus) {
      hideSystemUI();
    }
  }
}
