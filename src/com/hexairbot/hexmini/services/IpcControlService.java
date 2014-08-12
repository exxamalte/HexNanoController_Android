package com.hexairbot.hexmini.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import com.hexairbot.hexmini.util.DebugHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class IpcControlService extends Service {

  public final static String TAG = IpcControlService.class.getSimpleName();

  public final static String COMMAD_PARM = "command";
  public final static int COMMAND_CONNECT = 1000;
  public final static int COMMAND_DISCONNET = 2000;
  public final static int COMMAND_PAUSE = 3000;
  public final static int COMMAND_RESUME = 4000;

  private Binder localBinder = new LocalBinder();
  private ConnectStateManager mConnectStateManager = null;
  // private NavData preNavData = new NavData();
  // private String [] preNavData;
  private Map<String, String> preNavData = new HashMap<String, String>();

  private final Object navDataUpdateLock = new Object();
  private boolean stopNavDataUpdateThread = false;

  private WakeLock wakeLock;

  public final static String ACTION_NAVDATA_IPCREADY = "action_navdata_onipcready";
  public final static String ACTION_NAVDATA_CAMERAREADYCHANGED = "action_navdata_onCameraReadyChanged";
  public final static String ACTION_NAVDATA_RECORDREADYCHANGED = "action_navdata_onRecordReadyChangedd";
  public final static String ACTION_NAVDATA_BATTERYSTATECHANGED = "com.vmc.intent.action.action_navdata_onBatteryStateChanged";
  public final static String ACTION_NAVDATA_RECORDCHANGED = "action_navdata_onRecordChanged";

  public static final String EXTRA_STATE_IPCREADY = "extra_state_ipcready";
  public static final String EXTRA_STATE_RECORD = "extra_state_record";
  public static final String EXTRA_BATTERY_LEVEL = "extra_battery_level";
  public static final String EXTRA_STATE_RECORD_RAREADY = "extra_state_record_raready";
  public static final String EXTRA_STATE_CAMERA_READY = "extra_state_camera_ready";

  private Map<String, Intent> intentMap;
  private boolean mThreadStarted = false;

  @Override
  public void onCreate() {
    super.onCreate();

    mConnectStateManager = ConnectStateManager.getInstance(this
      .getApplication());
    mConnectStateManager.init();
    if (!DebugHandler.showServerSelect) {
      mConnectStateManager.connect("rtmp://192.168.1.1/live/stream");
//	    mConnectStateManager.connect("rtmp://10.0.12.191/live/stream");
    }
    mConnectStateManager
      .addConnectChangedListener(mOnIpcConnectChangedListener);

    // Preventing device from sleep
    PowerManager service = (PowerManager) getSystemService(POWER_SERVICE);
    wakeLock = service.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
      "DimWakeLock");
    wakeLock.acquire();

    initIntent();
  }

  private void initIntent() {
    intentMap = new HashMap<String, Intent>();
    intentMap.put(ACTION_NAVDATA_IPCREADY, new Intent(
      ACTION_NAVDATA_IPCREADY));
    intentMap.put(ACTION_NAVDATA_BATTERYSTATECHANGED, new Intent(
      ACTION_NAVDATA_BATTERYSTATECHANGED));
    intentMap.put(ACTION_NAVDATA_CAMERAREADYCHANGED, new Intent(
      ACTION_NAVDATA_CAMERAREADYCHANGED));
    intentMap.put(ACTION_NAVDATA_RECORDCHANGED, new Intent(
      ACTION_NAVDATA_RECORDCHANGED));
    intentMap.put(ACTION_NAVDATA_RECORDREADYCHANGED, new Intent(
      ACTION_NAVDATA_RECORDREADYCHANGED));
  }

  public ConnectStateManager getConnectStateManager() {
    if (mConnectStateManager == null)
      throw new IllegalArgumentException(
        "there is no ConnectStateManager!");
    return mConnectStateManager;
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return localBinder;
  }

  public class LocalBinder extends Binder {
    public IpcControlService getService() {
      return IpcControlService.this;
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    if (mConnectStateManager != null) {
      mConnectStateManager
        .removeConnectChangedListener(mOnIpcConnectChangedListener);
      mConnectStateManager.destroy();
    }

    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
    }

    stopNavDataUpdateThread();
    super.onDestroy();
  }

  private void stopNavDataUpdateThread() {
    stopNavDataUpdateThread = true;
    synchronized (navDataUpdateLock) {
      navDataUpdateLock.notify();
    }
    try {
      navDataUpdateThread.join(2000);
    } catch (InterruptedException ignored) {
    }
  }

  public static Map transStringToMap(String[] mapString) {
    if (mapString == null) {
      return null;
    }
    java.util.StringTokenizer items;
    Map<String, String> map = new HashMap<String, String>();

    int i;
    for (i = 0; i < mapString.length; i++) {
      items = new StringTokenizer(mapString[i], ",");
      map.put(items.nextToken(),
        items.hasMoreTokens() ? items.nextToken() : null
      );
    }
    return map;
  }

  private Thread navDataUpdateThread = new Thread() {
    @Override
    public void run() {
      DebugHandler.logInsist(TAG, "start updateNavDataThread ------");
      while (!stopNavDataUpdateThread) {
        if (mConnectStateManager != null) {
          IpcProxy ipc = mConnectStateManager.getIpcProxy();
          ipc.doUpdateNavData();
          // NavData currentNavData = ipc.getNavData();
          Map<String, String> currentNavData = transStringToMap(ipc.getNavData());
        /*
		     * if(currentNavData.initialized != preNavData.initialized)
		     * { onIpcReady(currentNavData.initialized); }
		     * if(currentNavData.batteryStatus !=
		     * preNavData.batteryStatus) {
		     * onBatteryStateChanged(currentNavData.batteryStatus); }
		     * if(currentNavData.cameraReady != preNavData.cameraReady)
		     * { onCameraReadyChanged(currentNavData.cameraReady); }
		     * if(currentNavData.recordReady != preNavData.recordReady)
		     * { onRecordReadyChanged(currentNavData.recordReady); }
		     * if(currentNavData.recording != preNavData.recording) {
		     * onRecordChanged(currentNavData.recording); }
		     */
          // preNavData.copyFrom(currentNavData);
          if (currentNavData != null) {
            String cur = currentNavData.get("battery");
            String old = preNavData.get("battery");
            if (cur == null) {
              cur = "";
            }
            if (old == null) {
              old = "";
            }
            if (!cur.equals(old)) {
              onBatteryStateChanged(Integer.parseInt(cur));
            }

            preNavData = currentNavData;
          }
        }

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          return;
        }

        if (mConnectStateManager != null
          && mConnectStateManager.getState() == ConnectStateManager.PAUSED
          && !stopNavDataUpdateThread) {
          synchronized (navDataUpdateLock) {
            try {
              navDataUpdateLock.wait();
            } catch (InterruptedException ignored) {
            }
          }
        }
      }
      DebugHandler.logInsist(TAG, "end updateNavDataThread ------");
    }
  };

  private void onIpcReady(boolean isReady) {
    Intent intent = intentMap.get(ACTION_NAVDATA_IPCREADY);
    intent.putExtra(EXTRA_STATE_IPCREADY, isReady);
    LocalBroadcastManager.getInstance(getApplicationContext())
      .sendBroadcast(intent);
  }

  private void onCameraReadyChanged(boolean isReady) {
    Intent intent = intentMap.get(ACTION_NAVDATA_CAMERAREADYCHANGED);
    intent.putExtra(EXTRA_STATE_CAMERA_READY, isReady);
    LocalBroadcastManager.getInstance(getApplicationContext())
      .sendBroadcast(intent);
  }

  private void onRecordReadyChanged(boolean isReady) {
    Intent intent = intentMap.get(ACTION_NAVDATA_RECORDREADYCHANGED);
    intent.putExtra(EXTRA_STATE_RECORD_RAREADY, isReady);
    LocalBroadcastManager.getInstance(getApplicationContext())
      .sendBroadcast(intent);
  }

  private void onBatteryStateChanged(int level) {
    Intent intent = intentMap.get(ACTION_NAVDATA_BATTERYSTATECHANGED);
    intent.putExtra(EXTRA_BATTERY_LEVEL, level);
    LocalBroadcastManager.getInstance(getApplicationContext())
      .sendBroadcast(intent);
  }

  private void onRecordChanged(boolean isProgress) {
    Intent intent = intentMap.get(ACTION_NAVDATA_RECORDCHANGED);
    intent.putExtra(EXTRA_STATE_RECORD, isProgress);
    LocalBroadcastManager.getInstance(getApplicationContext())
      .sendBroadcast(intent);
  }

  private OnIpcConnectChangedListener mOnIpcConnectChangedListener = new OnIpcConnectChangedListener() {

    @Override
    public void OnIpcConnected() {
      if (!mThreadStarted) {
        navDataUpdateThread.start();
        mThreadStarted = true;
      }
    }

    @Override
    public void OnIpcDisConnected() {
      synchronized (navDataUpdateLock) {
        navDataUpdateLock.notify();
      }
    }

    @Override
    public void onIpcPaused() {
      if (wakeLock != null && wakeLock.isHeld()) {
        wakeLock.release();
      }
    }

    @Override
    public void onIpcResumed() {
      synchronized (navDataUpdateLock) {
        navDataUpdateLock.notify();
      }

      if (wakeLock != null && !wakeLock.isHeld()) {
        wakeLock.acquire();
      }
    }

  };
}
