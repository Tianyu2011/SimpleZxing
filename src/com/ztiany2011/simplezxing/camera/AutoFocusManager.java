
package com.ztiany2011.simplezxing.camera;

import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;


final class AutoFocusManager implements Camera.AutoFocusCallback {

  private static final String TAG = AutoFocusManager.class.getSimpleName();

  private static final long AUTO_FOCUS_INTERVAL_MS = 2000L;

  private boolean stopped;
  private boolean focusing;
  private final boolean useAutoFocus;
  private final Camera camera;
  private AsyncTask<?,?,?> outstandingTask;

  AutoFocusManager(Context context, Camera camera) {
    this.camera = camera;
    useAutoFocus = true;
    start();
  }

  @Override
  public synchronized void onAutoFocus(boolean success, Camera theCamera) {
    focusing = false;
    autoFocusAgainLater();
  }

  private synchronized void autoFocusAgainLater() {
    if (!stopped && outstandingTask == null) {
      AutoFocusTask newTask = new AutoFocusTask();
      try {
        newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        outstandingTask = newTask;
      } catch (RejectedExecutionException ree) {
        Log.w(TAG, "Could not request auto focus", ree);
      }
    }
  }

  synchronized void start() {
    if (useAutoFocus) {
      outstandingTask = null;
      if (!stopped && !focusing) {
        try {
          camera.autoFocus(this);
          focusing = true;
        } catch (RuntimeException re) {
          // Have heard RuntimeException reported in Android 4.0.x+; continue?
          Log.w(TAG, "Unexpected exception while focusing", re);
          // Try again later to keep cycle going
          autoFocusAgainLater();
        }
      }
    }
  }

  private synchronized void cancelOutstandingTask() {
    if (outstandingTask != null) {
      if (outstandingTask.getStatus() != AsyncTask.Status.FINISHED) {
        outstandingTask.cancel(true);
      }
      outstandingTask = null;
    }
  }

  synchronized void stop() {
    stopped = true;
    if (useAutoFocus) {
      cancelOutstandingTask();
      // Doesn't hurt to call this even if not focusing
      try {
        camera.cancelAutoFocus();
      } catch (RuntimeException re) {
        // Have heard RuntimeException reported in Android 4.0.x+; continue?
        Log.w(TAG, "Unexpected exception while cancelling focusing", re);
      }
    }
  }

  private final class AutoFocusTask extends AsyncTask<Object,Object,Object> {
    @Override
    protected Object doInBackground(Object... voids) {
      try {
        Thread.sleep(AUTO_FOCUS_INTERVAL_MS);
      } catch (InterruptedException e) {
        // continue
      }
      start();
      return null;
    }
  }

}
