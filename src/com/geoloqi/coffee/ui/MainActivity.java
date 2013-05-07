package com.geoloqi.coffee.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import com.esri.android.geotrigger.GeotriggerService;
import com.geoloqi.coffee.R;
import com.geoloqi.coffee.util.JsonHttpClient;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
  private static final String TAG = "coffee.MainActivity";
  private static final String COFFEE_URL = "http://www.google.com"; //http://coffee.geoloqi.com/m;
  private View mParent;
  private WebView mWebView;
  private TextView mNoConnectionTextView;
  private View mLoadingView;
  private static final String AGO_APP_ID = "efcf47cc04e04de2bbd474a929c530d7";
  private static final String GCM_SENDER_ID = "685874447210";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    GeotriggerService.init(this, AGO_APP_ID, GCM_SENDER_ID, GeotriggerService.TRACKING_PROFILE_ADAPTIVE);
  }

  @Override
  public void onStart() {
    super.onStart();
    mParent = findViewById(android.R.id.content);

    // Build up our webview, but only if we haven't already done all this stuff already
    if (mParent != null) {
      mNoConnectionTextView = (TextView) mParent.findViewById(R.id.textview_no_connection);
      mLoadingView = mParent.findViewById(R.id.loading);

      if (mWebView == null) {
        // Get a reference to our webview
        mWebView = (WebView) mParent.findViewById(R.id.webview);

        // Set up client callback so that we can adjust visibility after page load
        mWebView.setWebViewClient(new WebViewClient() {
          private boolean mLoadSuccess = true;

          // This callback still gets hit even if an error has occurred, which is why this.mLoadSuccess exists
          @Override
          public void onPageFinished(WebView view, String url) {
            // Show the webview after load, if the load was successful
            if (this.mLoadSuccess) {
              // You might not think that webview could possibly be null here, but ...
              if (mWebView != null) {
                toggleNoConnectionTextView(false);
                toggleLoadingView(false);
                toggleWebView(true);
              }
            }
          }

          @Override
          public void onPageStarted(WebView view, String url, Bitmap favicon) {
            this.mLoadSuccess = true;
          }

          @Override
          public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            // We got some kind of unrecoverable error, make load success false and throw up the fail page
            this.mLoadSuccess = false;
          }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
          @Override
          public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            TextView progressText = null;

            if (mParent != null) {
              progressText = (TextView) mParent.findViewById(R.id.loading_text);
              progressText.setText(MainActivity.this.getText(R.string.loading) + " " + String.valueOf(newProgress) + "%");
            }

            if (newProgress >= 100) {
              if (progressText != null) {
                progressText.setText(MainActivity.this.getText(R.string.loading));
              }
            }
          }
        });

        // Even more stuff
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        // Attempt to load the page
        loadWebView();
      }
    }
  }

  private void loadWebView() {
    if (!JsonHttpClient.isConnected(this)) {
      toggleLoadingView(false);
      toggleWebView(false);
      toggleNoConnectionTextView(true);
    } else {
      toggleNoConnectionTextView(false);
      toggleWebView(false);
      toggleLoadingView(true);

      if (mWebView != null) {
        Map<String, String> headers = new HashMap<String, String>();
        mWebView.loadUrl(COFFEE_URL, headers);
      } else {
        Log.e(TAG, "Load attempt failed. Is the webview inflated?");
      }
    }
  }

  /**
   * Show or hide the webview.
   * @param shown
   */
  public void toggleWebView(boolean shown) {
    if (mWebView != null) {
      if (shown) {
        mWebView.setVisibility(View.VISIBLE);
      } else {
        mWebView.setVisibility(View.GONE);
      }
    }
  }

  /**
   * Show or hide the webview label.
   * @param shown
   */
  private void toggleNoConnectionTextView(boolean shown) {
    if (mNoConnectionTextView != null) {
      if (shown) {
        mNoConnectionTextView.setVisibility(View.VISIBLE);
      } else {
        mNoConnectionTextView.setVisibility(View.GONE);
      }
    }
  }

  /**
   * Show or hide the loading view.
   * @param shown
   */
  private void toggleLoadingView(boolean shown) {
    if (mLoadingView != null) {
      if (shown) {
        mLoadingView.setVisibility(View.VISIBLE);
      } else {
        mLoadingView.setVisibility(View.GONE);
      }
    }
  }
}
