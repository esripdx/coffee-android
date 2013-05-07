package com.geoloqi.coffee_android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.geoloqi.coffee_android.R;

public class LauncherActivity extends Activity {
  private static final String TAG = "coffee.LauncherActivity";

  /** The minimum amount of time that the splash screen will be displayed. */
  private static final int SPLASH_TIMEOUT = 3000;

  /** A runnable that will launch the main activity. */
  private Runnable mStartMainActivity = new Runnable() {
    @Override
    public void run() {
      startActivity(new Intent(LauncherActivity.this, MainActivity.class));
    }
  };

  private final Handler mHandler = new Handler();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.splash);

    // Show the splash screen then start the main activity
    mHandler.postDelayed(mStartMainActivity, SPLASH_TIMEOUT);
  }

  @Override
  public void onPause() {
    super.onPause();

    // If the user has stopped the launcher activity, don't
    // continue launching the main activity.
    mHandler.removeCallbacks(mStartMainActivity);
  }
}
