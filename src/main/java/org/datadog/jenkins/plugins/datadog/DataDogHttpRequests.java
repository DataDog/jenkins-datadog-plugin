package org.datadog.jenkins.plugins.datadog;

import hudson.ProxyConfiguration;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 *
 * This class is used to collect all methods that has to do with transmitting
 * data to Datadog.
 */
public class DataDogHttpRequests {

  private static final Logger logger =  Logger.getLogger(DataDogHttpRequests.class.getName());
  /**
   * Returns an HTTP url connection given a url object. Supports jenkins configured proxy.
   *
   * @param url - a URL object containing the URL to open a connection to.
   * @return a HttpURLConnection object.
   * @throws IOException
   */
  public static HttpURLConnection getHttpURLConnection(final URL url) throws IOException {
    HttpURLConnection conn = null;
    ProxyConfiguration proxyConfig = Jenkins.getInstance().proxy;
    if (proxyConfig != null) {
      Proxy proxy = proxyConfig.createProxy(url.getHost());
      if (proxy != null && proxy.type() == Proxy.Type.HTTP) {
        logger.fine("Attempting to use the Jenkins proxy configuration");
        conn = (HttpURLConnection) url.openConnection(proxy);
        if (conn == null) {
          logger.fine("Failed to use the Jenkins proxy configuration");
        }
      }
    }
    if (conn == null) {
      conn = (HttpURLConnection) url.openConnection();
      logger.fine("Using the Jenkins proxy configuration");
    }
    return conn;
  }

  /**
   * Sends a an event to the Datadog API, including the event payload.
   *
   * @param evt - The finished {@link DataDogEvent} to send
   */
  public static void sendEvent(DataDogEvent evt) {
    logger.fine("Sending event");
    try {
      DataDogHttpRequests.post(evt.createPayload(), DatadogBuildListener.EVENT);
    } catch (Exception e) {
      logger.severe(e.toString());
    }
  }

  /**
   * Posts a given {@link JSONObject} payload to the DataDog API, using the
   * user configured apiKey.
   *
   * @param payload - A JSONObject containing a specific subset of a builds metadata.
   * @param type - A String containing the URL subpath pertaining to the type of API post required.
   * @return a boolean to signify the success or failure of the HTTP POST request.
   * @throws IOException
   */
  public static Boolean post(final JSONObject payload, final String type) throws IOException {
    String urlParameters = "?api_key=" + DataDogUtilities.getApiKey();
    HttpURLConnection conn = null;
    try {
      conn = DataDogHttpRequests.getHttpURLConnection(new URL(DatadogBuildListener.BASEURL + type + urlParameters));
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setUseCaches(false);
      conn.setDoInput(true);
      conn.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
      wr.writeBytes(payload.toString());
      wr.flush();
      wr.close();
      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = rd.readLine()) != null) {
        result.append(line);
      }
      rd.close();
      JSONObject json = (JSONObject) JSONSerializer.toJSON(result.toString());
      if ("ok".equals(json.getString("status"))) {
        logger.finer(String.format("API call of type '%s' was sent successfully!", type));
        logger.finer(String.format("Payload: %s", payload));
        return true;
      } else {
        logger.fine(String.format("API call of type '%s' failed!", type));
        logger.fine(String.format("Payload: %s", payload));
        return false;
      }
    } catch (Exception e) {
      if (conn.getResponseCode() == DatadogBuildListener.HTTP_FORBIDDEN) {
        logger.severe("Hmmm, your API key may be invalid. We received a 403 error.");
      } else {
        logger.severe(String.format("Client error: %s", e));
      }
      return false;
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
      return true;
    }
  }

}
