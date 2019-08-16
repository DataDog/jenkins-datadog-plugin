package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class is used to collect all methods that has to do with transmitting
 * data to Datadog.
 */
public class DatadogHttpRequests {

    private static final Logger logger = Logger.getLogger(DatadogHttpRequests.class.getName());
    private static final EnvVars envVars = new EnvVars();
    private static final String EVENT = "v1/events";
    public static final String METRIC = "v1/series";
    private static final String SERVICECHECK = "v1/check_run";
    private static final String VALIDATE = "v1/validate";
    private static final Integer HTTP_FORBIDDEN = 403;

    /**
     * Returns an HTTP url connection given a url object. Supports jenkins configured proxy.
     *
     * @param url - a URL object containing the URL to open a connection to.
     * @return a HttpURLConnection object.
     * @throws IOException if HttpURLConnection fails to open connection
     */
    public static HttpURLConnection getHttpURLConnection(final URL url) throws IOException {
        HttpURLConnection conn = null;
        ProxyConfiguration proxyConfig = Jenkins.getInstance().proxy;

        /* Attempt to use proxy */
        if (proxyConfig != null) {
            Proxy proxy = proxyConfig.createProxy(url.getHost());
            if (proxy != null && proxy.type() == Proxy.Type.HTTP) {
                logger.fine("Attempting to use the Jenkins proxy configuration");
                conn = (HttpURLConnection) url.openConnection(proxy);
                if (conn == null) {
                    logger.fine("Failed to use the Jenkins proxy configuration");
                }
            }
        } else {
            logger.fine("Jenkins proxy configuration not found");
        }

        /* If proxy fails, use HttpURLConnection */
        if (conn == null) {
            conn = (HttpURLConnection) url.openConnection();
            logger.fine("Using HttpURLConnection, without proxy");
        }

        return conn;
    }

    /**
     * Sends an event to the Datadog API, including the event payload.
     *
     * @param event - The finished {@link DatadogEvent} to send
     * @return a boolean to signify the success or failure of the HTTP POST request.
     */
    public static boolean sendEvent(DatadogEvent event) {
        logger.fine("Sending event");
        boolean status;
        try {
            status = post(event.createPayload(), EVENT);
        } catch (Exception e) {
            logger.severe(e.toString());
            status = false;
        }
        return status;
    }

    /**
     * Sends a metric to the Datadog API, including the gauge name, and value.
     *
     * @param name  - A String with the name of the metric to record.
     * @param value - A Object containing the value to submit.
     * @return a boolean to signify the success or failure of the HTTP POST request.
     */
    public static boolean gauge(String name, Object value) {
        JSONObject payload = createMetricPayload(name, value, null);

        boolean status;
        try {
            status = post(payload, METRIC);
        } catch (Exception e) {
            logger.severe(e.toString());
            status = false;
        }
        return status;
    }

    /**
     * Sends a metric to the Datadog API, including the gauge name, and value.
     *
     * @param name      - A String with the name of the metric to record.
     * @param builddata - A JSONObject containing a builds metadata.
     * @param key       - A String with the name of the build metadata to be found in the {@link JSONObject} builddata.
     * @param extraTags - A list of tags, that are contributed via {@link DatadogJobProperty}.
     */
    public static void gauge(final String name, final JSONObject builddata,
                             final String key, final Map<String, String> extraTags) {
        String builddataKey = DatadogUtilities.nullSafeGetString(builddata, key);
        logger.fine(String.format("Sending metric '%s' with value %s", name, builddataKey));

        JSONObject payload = createMetricPayload(name, builddata.get(key),
                DatadogUtilities.assembleTags(builddata, extraTags));

        try {
            DatadogHttpRequests.post(payload, METRIC);
        } catch (Exception e) {
            logger.severe(e.toString());
        }
    }

    private static JSONObject createMetricPayload(String name, Object value, JSONArray tags) {
        // Setup data point, of type [<unix_timestamp>, <value>]
        JSONArray points = new JSONArray();
        JSONArray point = new JSONArray();

        long currentTime = System.currentTimeMillis() / DatadogBuildListener.THOUSAND_LONG;
        point.add(currentTime); // current time, s
        point.add(value);
        points.add(point); // api expects a list of points

        JSONObject metric = new JSONObject();
        metric.put("metric", name);
        metric.put("points", points);
        metric.put("type", "gauge");
        metric.put("host", DatadogUtilities.getHostname(envVars)); // string
        if (tags != null) {
            logger.fine(tags.toString());
            metric.put("tags", tags);
        }
        // Place metric as item of series list
        JSONArray series = new JSONArray();
        series.add(metric);

        // Add series to payload
        JSONObject payload = new JSONObject();
        payload.put("series", series);

        logger.fine(String.format("Resulting payload: %s", payload.toString()));
        return payload;
    }

    /**
     * Sends a service check to the Datadog API, including the check name, and status.
     *
     * @param checkName  - A String with the name of the service check to record.
     * @param statusCode - An Integer with the status code to record for this service check.
     * @param builddata  - A JSONObject containing a builds metadata.
     * @param extraTags  - A list of tags, that are contributed through the {@link DatadogJobProperty}.
     * @return a boolean to signify the success or failure of the HTTP POST request.
     */
    public static boolean serviceCheck(final String checkName, final Integer statusCode,
                                       final JSONObject builddata, final Map<String, String> extraTags) {
        logger.fine(String.format("Sending service check '%s' with status %s", checkName, statusCode));

        // Build payload
        JSONObject payload = new JSONObject();
        payload.put("check", checkName);
        payload.put("host_name", builddata.get("hostname"));
        payload.put("timestamp", System.currentTimeMillis() / DatadogBuildListener.THOUSAND_LONG); // current time, s
        payload.put("status", statusCode);

        // Remove result tag, so we don't create multiple service check groups
        builddata.remove("result");
        payload.put("tags", DatadogUtilities.assembleTags(builddata, extraTags));
        boolean status;
        try {
            status = post(payload, SERVICECHECK);
        } catch (Exception e) {
            logger.severe(e.toString());
            status = false;
        }
        return status;
    }

    /**
     * Posts a given {@link JSONObject} payload to the Datadog API, using the
     * user configured apiKey.
     *
     * @param payload - A JSONObject containing a specific subset of a builds metadata.
     * @param type    - A String containing the URL subpath pertaining to the type of API post required.
     * @return a boolean to signify the success or failure of the HTTP POST request.
     * @throws IOException if HttpURLConnection fails to open connection
     */
    public static boolean post(final JSONObject payload, final String type) throws IOException {
        String urlParameters = "?api_key=" + Secret.toString(DatadogUtilities.getApiKey());
        HttpURLConnection conn = null;
        boolean status = true;

        try {
            logger.finer("Setting up HttpURLConnection...");
            conn = DatadogHttpRequests.getHttpURLConnection(new URL(DatadogUtilities.getTargetMetricURL()
                    + type
                    + urlParameters));
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "utf-8");
            logger.finer("Writing to OutputStreamWriter...");
            wr.write(payload.toString());
            wr.close();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
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
            } else {
                logger.fine(String.format("API call of type '%s' failed!", type));
                logger.fine(String.format("Payload: %s", payload));
                status = false;
            }
        } catch (Exception e) {
            if (conn.getResponseCode() == HTTP_FORBIDDEN) {
                logger.severe("Hmmm, your API key may be invalid. We received a 403 error.");
            } else {
                logger.severe(String.format("Client error: %s", e.toString()));
            }
            status = false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return status;
    }

    /**
     * Tests the apiKey is valid.
     *
     * @param targetMetricURL - targetMetricUrl
     * @param urlParameters   - urlParameters
     * @return a boolean to signify the success or failure of the HTTP GET request.
     * @throws IOException      if there is an input/output exception.
     * @throws ServletException if there is a servlet exception.
     */
    public static boolean validate(String targetMetricURL, String urlParameters) throws IOException, ServletException {
        HttpURLConnection conn = null;
        boolean status = true;
        try {
            // Make request
            conn = DatadogHttpRequests.getHttpURLConnection(new URL(targetMetricURL + VALIDATE
                    + urlParameters));
            conn.setRequestMethod("GET");

            // Get response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();

            // Validate
            JSONObject json = (JSONObject) JSONSerializer.toJSON(result.toString());
            if (!json.getBoolean("valid")) {
                status = false;
            }
        } catch (Exception e) {
            if (conn.getResponseCode() == HTTP_FORBIDDEN) {
                logger.severe("Hmmm, your API key may be invalid. We received a 403 error.");
            } else {
                logger.severe(String.format("Client error: %s", e.toString()));
            }
            status = false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return status;
    }

}
