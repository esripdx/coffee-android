package com.geoloqi.coffee_android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class HttpClient {

  /**
   * Determine if the device has an active network connection.
   * @return true if the network is connected, false if otherwise.
   */
  public static boolean isConnected(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
        Context.CONNECTIVITY_SERVICE);
    if (cm != null) {
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
      if (activeNetwork != null) {
        return activeNetwork.isConnected();
      }
    }
    return false;
  }
}
