package org.datadog.jenkins.plugins.datadog.clients;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.datadog.jenkins.plugins.datadog.DatadogClient;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * This class is used to collect all methods that has to do with transmitting
 * data to Datadog.
 */
public class DatadogHttpClient implements DatadogClient {

    private static DatadogClient instance;
    private static final Logger logger = Logger.getLogger(DatadogHttpClient.class.getName());

    private static final String EVENT = "v1/events";
    private static final String METRIC = "v1/series";
    private static final String SERVICECHECK = "v1/check_run";
    private static final String VALIDATE = "v1/validate";

    private static final Integer HTTP_FORBIDDEN = 403;

    public static boolean enableValidations = true;

    private String url;
    private Secret apiKey;

    /**
     * NOTE: Use DatadogUtilities.getDatadogClient method to instanciate the client in the Jenkins Plugin
     * This method is not recommended to be used because it misses some validations.
     * @param url - target url
     * @param apiKey - Secret api Key
     * @return an singleton instance of the DatadogClient.
     */
    public static DatadogClient getInstance(String url, Secret apiKey){
        if(enableValidations){
            if (url == null || url.isEmpty()) {
                logger.severe("Datadog Target URL is not set properly");
                throw new RuntimeException("Datadog Target URL is not set properly");
            }
            if (apiKey == null || Secret.toString(apiKey).isEmpty()){
                logger.severe("Datadog API Key is not set properly");
                throw new RuntimeException("Datadog API Key is not set properly");
            }
        }

        if(instance == null){
            synchronized (DatadogHttpClient.class) {
                if(instance == null){
                    instance = new DatadogHttpClient(url, apiKey);
                }
            }
        }

        // We reset param just in case we change values
        instance.setApiKey(apiKey);
        instance.setUrl(url);
        return instance;
    }

    private DatadogHttpClient(String url, Secret apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @Override
    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean sendEvent(JSONObject payload) {
        logger.fine("Sending event");
        boolean status;
        try {
            status = post(payload, EVENT);
        } catch (Exception e) {
            logger.severe(e.toString());
            status = false;
        }
        return status;
    }

    @Override
    public void incrementCounter(String name, String hostname, JSONArray tags) {
        ConcurrentMetricCounters.getInstance().increment(name, hostname, tags);
    }

    @Override
    public void flushCounters() {
        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.getInstance().getAndReset();

        logger.fine("Run flushCounters method");
        // Submit all metrics as gauge
        for (CounterMetric counterMetric: counters.keySet()) {
            int count = counters.get(counterMetric);
            logger.fine("Flushing: " + counterMetric.getMetricName() + " - " + count);
            // Since we submit a rate we need to divide the submitted value by the interval (10)
            this.postMetric(counterMetric.getMetricName(), count / 10, counterMetric.getHostname(),
                    counterMetric.getTags(), "rate");

        }
    }

    @Override
    public boolean gauge(String name, long value, String hostname, JSONArray tags) {
        return postMetric(name, value, hostname, tags, "gauge");
    }

    private boolean postMetric(String name, long value, String hostname, JSONArray tags, String type) {
        logger.fine(String.format("Sending metric '%s' with value %s", name, String.valueOf(value)));

        // Setup data point, of type [<unix_timestamp>, <value>]
        JSONArray points = new JSONArray();
        JSONArray point = new JSONArray();

        point.add(System.currentTimeMillis() / 1000); // current time, s
        point.add(value);
        points.add(point); // api expects a list of points

        JSONObject metric = new JSONObject();
        metric.put("metric", name);
        metric.put("points", points);
        metric.put("type", type);
        metric.put("host", hostname);
        if(type.equals("rate")){
            metric.put("interval", 10);
        }
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

        logger.fine(String.format("payload: %s", payload.toString()));

        boolean status;
        try {
            status = post(payload, METRIC);
        } catch (Exception e) {
            logger.severe(e.toString());
            status = false;
        }
        return status;
    }

    @Override
    public boolean serviceCheck(String name, int code, String hostname, JSONArray tags) {
        logger.fine(String.format("Sending service check '%s' with status %s", name, code));

        // Build payload
        JSONObject payload = new JSONObject();
        payload.put("check", name);
        payload.put("host_name", hostname);
        payload.put("timestamp", System.currentTimeMillis() / 1000); // current time, s
        payload.put("status", code);

        // Remove result tag, so we don't create multiple service check groups
        if (tags != null) {
            logger.fine(tags.toString());
            payload.put("tags", tags);
        }
        return post(payload, SERVICECHECK);
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
    private boolean post(final JSONObject payload, final String type) {
        String urlParameters = "?api_key=" + Secret.toString(apiKey);
        HttpURLConnection conn = null;
        boolean status = true;

        try {
            logger.finer("Setting up HttpURLConnection...");
            conn = getHttpURLConnection(new URL(url
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
            try {
                if (conn != null && conn.getResponseCode() == HTTP_FORBIDDEN) {
                    logger.severe("Hmmm, your API key may be invalid. We received a 403 error.");
                } else {
                    logger.severe(String.format("Client error: %s", e.toString()));
                }
            } catch (IOException ex) {
                logger.severe(ex.toString());
            }
            status = false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return status;
    }

    public boolean validate() throws IOException, ServletException {
        String urlParameters = "?api_key=" + apiKey;
        HttpURLConnection conn = null;
        boolean status = true;
        try {
            // Make request
            conn = getHttpURLConnection(new URL(url + VALIDATE + urlParameters));
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
            if (conn != null && conn.getResponseCode() == HTTP_FORBIDDEN) {
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
     * Returns an HTTP url connection given a url object. Supports jenkins configured proxy.
     *
     * @param url - a URL object containing the URL to open a connection to.
     * @return a HttpURLConnection object.
     * @throws IOException if HttpURLConnection fails to open connection
     */
    private HttpURLConnection getHttpURLConnection(final URL url) throws IOException {
        HttpURLConnection conn = null;
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null){
            return null;
        }
        ProxyConfiguration proxyConfig = jenkins.proxy;

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

        /* Timeout of 1 minutes for connecting and reading.
        * this prevents this plugin from causing jobs to hang in case of
        * flaky network or Datadog being down. Left intentionally long.
        */
        int timeoutMS = 1 * 60 * 1000;
        conn.setConnectTimeout(timeoutMS);
        conn.setReadTimeout(timeoutMS);

        return conn;
    }

}
