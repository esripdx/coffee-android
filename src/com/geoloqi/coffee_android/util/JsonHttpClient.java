package com.geoloqi.coffee_android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * A basic JSON HTTP client that handles GET and POST requests.
 */
public final class JsonHttpClient {
  private static final String TAG = "JsonHttpClient";

  /** A template for formatting a Unix timestamp as an ISO 8601 date String. */
  private static final String ISO_8601_DATE = "yyyy-MM-dd'T'HH:mm:ssZ";

  private JsonHttpClient() {}

  /**
   * <p>The interface that defines possible outcomes of an HTTP request so that custom behavior can be
   * implemented when those events occur.</p>
   */
  public interface JsonRequestListener {
    /**
     * The server returned an {@link org.apache.http.HttpStatus#SC_OK} response.
     *
     * @param json
     *         the {@link org.json.JSONObject} parsed from the raw HttpResponse.
     * @param headers
     *         the raw request headers returned from the HttpResponse.
     */
    public void onSuccess(JSONObject json, Header[] headers);

    /**
     * The request failed to be sent due to some local error condition
     * or the response could not be parsed.
     *
     * @param e
     *         the caught exception; otherwise null;
     */
    public void onFailure(Exception e);

    /**
     * The server did return a response, but the response code was not
     * {@link org.apache.http.HttpStatus#SC_OK} and indicates some error condition.
     *
     * @param json
     *         the {@link JSONObject} parsed from the raw HttpResponse.
     * @param headers
     *         the raw request headers returned from the HttpResponse.
     * @param status
     *         the StatusLine returned from the raw HttpResponse.
     */
    public void onComplete(JSONObject json, Header[] headers, StatusLine status);
  }

  /**
   * Send a GET request.
   *
   * @param args optional map of query arguments to be encoded and appended to the path.
   * @param headers optional headers to send with the request
   * @param listener
   */
  public static void runGetRequest(Context context, String url, Map<String, String> args, Header[] headers, JsonRequestListener listener) {
    HttpGet request = new HttpGet();
    try {
      String qs = urlencode(args);
      if (!TextUtils.isEmpty(qs)) {
        url += "?" + qs;
      }
      request.setURI(new URI(url));
      request.setHeaders(headers);
    } catch (URISyntaxException e) {
      listener.onFailure(new Exception(e));
      return;
    }
    runHttpRequest(context, request, listener);
  }

  /**
   * Send a POST request with a "Content-Type" value of "application/json".
   *
   * @param json json POST body
   * @param headers optional headers to send with the request
   * @param listener
   */
  public static void runPostRequest(Context context, String url, JSONObject json, Header[] headers, JsonRequestListener listener) {
    StringEntity entity;
    try {
      entity = new StringEntity(json.toString(), HTTP.UTF_8);
    } catch (UnsupportedEncodingException e) {
      listener.onFailure(new Exception(e));
      return;
    }
    runPostRequest(context, url, entity, headers, listener);
  }

  /**
   * Send a POST request with a "Content-Type" value of "application/json".
   *
   * @param json json POST body
   * @param headers optional headers to send with the request
   * @param listener
   */
  public static void runPostRequest(Context context, String url, JSONArray json, Header[] headers,
                                JsonRequestListener listener) {
    StringEntity entity;
    try {
      entity = new StringEntity(json.toString(), HTTP.UTF_8);
    } catch (UnsupportedEncodingException e) {
      listener.onFailure(new Exception(e));
      return;
    }
    runPostRequest(context, url, entity, headers, listener);
  }

  /**
   * <p>Send a POST request with a "Content-Type" value of "application/json".</p>
   *
   * <p>This method adds the Access Token header to the request.</p>
   *
   * @param entity a StringEntity containing a serialized JSONObject or JSONArray.
   * @param headers optional headers to send with the request
   * @param listener
   */
  private static void runPostRequest(Context context, String url, StringEntity entity, Header[] headers,
                              JsonRequestListener listener) {
    HttpPost request = new HttpPost();
    try {
      request.setURI(new URI(url));
      request.setEntity(entity);
      Log.d(TAG, "Post entity: " + EntityUtils.toString(entity));
      request.setHeaders(headers);
      // Ensure the Content-Type is set as expected.
      request.setHeader(HTTP.CONTENT_TYPE, "application/json");

    } catch (URISyntaxException e) {
      listener.onFailure(new Exception(e));
      return;
    } catch (IOException e) {
      // TODO: deleteme
    }
    runHttpRequest(context, request, listener);
  }

  /**
   * Run a raw HttpRequest on a background thread.
   *
   * @param request
   * @param listener
   */
  private static void runHttpRequest(Context context, final HttpRequestBase request, final JsonRequestListener listener) {
    // Initialize the request
    Log.d(TAG, "Executing new request.");

    // Check for a valid request URI
    if (request.getURI() == null) {
      throw new IllegalArgumentException("Cannot execute request with null URI!");
    }

    // Check for an active network connection
    if (!isConnected(context)) {
      // TODO: Pass a more appropriate exception type to the listener.
      Log.d(TAG, "Request failed! No active network connection.");
      listener.onFailure(new Exception("No active network connection!"));
      return;
    }

    // Execute the request on a background Thread
    // TODO: Investigate using ExecutorService.submit() and returning the Future.

    try {
      DefaultHttpClient client = new DefaultHttpClient();
      Log.d(TAG, String.format("Sending request to '%s'.", request.getURI()));

      // Execute the request
      HttpResponse response = client.execute(request);
      final Header[] headers = response.getAllHeaders();
      final StatusLine status = response.getStatusLine();

      String entity = EntityUtils.toString(response.getEntity());

      Log.d(TAG, String.format("Response received with status '%s'.", status));
      Log.d(TAG, String.format("Response entity: '%s'.", entity));

      // Consume the response content
      final JSONObject json = new JSONObject(entity);

      if (status.getStatusCode() == HttpStatus.SC_OK) {
        Log.v(TAG, "Request was successful!");
        listener.onSuccess(json, headers);
      } else {
        Log.v(TAG, String.format("Request completed with status '%s'!", status));
        listener.onComplete(json, headers, status);
      }
    } catch (final Exception e) {
      Log.d(TAG, String.format("Request failed with error '%s'!", e.getMessage()));
      listener.onFailure(new Exception(e));
    }
  }

  /**
   * Format a Unix timestamp as an
   * <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a> date String.
   *
   * @param time
   * @return the formatted time as a String.
   */
  public static String formatTimestamp(long time) {
    SimpleDateFormat dateFormat =
        new SimpleDateFormat(ISO_8601_DATE, Locale.US);
    return dateFormat.format(new Date(time));
  }

  /**
   * <p>Encode a {@link Map} of query arguments for use in a GET request.</p>
   *
   * <p>See {@link java.net.URLEncoder#encode}.</p>
   *
   * @param args
   * @return the URL encoded String.
   */
  public static String urlencode(Map<String, String> args) {
    String qs = "";
    if (args != null && args.size() > 0) {
      for (Map.Entry<String, String> arg : args.entrySet()) {
        qs += String.format("&%s=%s", URLEncoder.encode(arg.getKey()),
            URLEncoder.encode(arg.getValue()));
      }

      // Remove the leading ampersand
      qs = qs.substring(1);
    }
    return qs;
  }

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

