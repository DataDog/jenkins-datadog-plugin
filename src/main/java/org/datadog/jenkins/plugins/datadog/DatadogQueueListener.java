package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * This class registers a {@link PeriodicWork} with Jenkins to run periodically in order to enable
 * us to compute metrics related to the size of the queue.
 */
@Extension
public class DatadogQueueListener extends PeriodicWork {
  private static final String METRIC = "v1/series";
  private static final long RECURRENCE_PERIOD = TimeUnit.MINUTES.toMillis(1);

  private static final Logger logger = Logger.getLogger(DatadogQueueListener.class.getName());
  private static final Queue queue = Queue.getInstance();
  private static final EnvVars envVars = new EnvVars();

  @Override
  public long getRecurrencePeriod() {
    return RECURRENCE_PERIOD;
  }

  @Override
  protected void doRun() throws Exception {
    if ( DatadogUtilities.isApiKeyNull() ) {
      return;
    }
    logger.fine("doRun called: Computing queue metrics");
    gauge("jenkins.queue.size", queue.getApproximateItemsQuickly().size());
  }

  private void gauge(String name, int value) {
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

    // Place metric as item of series list
    JSONArray series = new JSONArray();
    series.add(metric);

    // Add series to payload
    JSONObject payload = new JSONObject();
    payload.put("series", series);

    logger.fine(String.format("Resulting payload: %s", payload.toString()));

    try {
      DatadogHttpRequests.post(payload, METRIC);
    } catch (Exception e) {
      logger.severe(e.toString());
    }
  }
}
