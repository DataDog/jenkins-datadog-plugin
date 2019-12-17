package org.datadog.jenkins.plugins.datadog;

import org.datadog.jenkins.plugins.datadog.logs.LogSender;
import com.timgroup.statsd.ServiceCheck;
import hudson.util.Secret;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface DatadogClient {

    public static enum ClientType {
        HTTP,
        DSD;

        private ClientType() { }
    }

    public static enum Status {
        OK(0),
        WARNING(1),
        CRITICAL(2),
        UNKNOWN(3);

        private final int val;

        private Status(int val) {
            this.val = val;
        }

        public int toValue(){
           return this.val;
        }

        public ServiceCheck.Status toServiceCheckStatus(){
            return ServiceCheck.Status.valueOf(this.name());
        }
    }

    public void setUrl(String url);

    public void setApiKey(Secret apiKey);

    public void setHostname(String hostname);

    public void setPort(int port);

    /**
     * Sends an event to the Datadog API, including the event payload.
     *
     * @param event - a DatadogEvent object
     * @return  a boolean to signify the success or failure of the HTTP POST request.
     */
    public boolean event(DatadogEvent event);

    /**
     * Increment a counter for the given metrics.
     * NOTE: To submit all counters you need to execute the flushCounters method.
     * This is to aggregate counters and submit them in batch to Datadog in order to minimize network traffic.
     * @param name - metric name
     * @param hostname - metric hostname
     * @param tags - metric tags
     */
    public void incrementCounter(String name, String hostname, Map<String, Set<String>> tags);

    /**
     * Submit all your counters as rate with 10 seconds intervals.
     */
    public void flushCounters();

    /**
     * Sends a metric to the Datadog API, including the gauge name, and value.
     *
     * @param name     - A String with the name of the metric to record.
     * @param value    - A long containing the value to submit.
     * @param hostname - A String with the hostname to submit.
     * @param tags     - A Map containing the tags to submit.
     * @return a boolean to signify the success or failure of the HTTP POST request.
     */
    public boolean gauge(String name, long value, String hostname, Map<String, Set<String>> tags);

    /**
     * Sends a service check to the Datadog API, including the check name, and status.
     *
     * @param name     - A String with the name of the service check to record.
     * @param status   - An Status with the status code to record for this service check.
     * @param hostname - A String with the hostname to submit.
     * @param tags     - A Map containing the tags to submit.
     * @return a boolean to signify the success or failure of the HTTP POST request.
     */
    public boolean serviceCheck(String name, Status status, String hostname, Map<String, Set<String>> tags);

    /**
     * Tests the apiKey is valid.
     *
     * @return a boolean to signify the success or failure of the HTTP GET request.
     * @throws IOException      if there is an input/output exception.
     * @throws ServletException if there is a servlet exception.
     */
    public boolean validate() throws IOException, ServletException;

    public boolean sendLogs(LogSender payloadLogs) throws IOException;
}
