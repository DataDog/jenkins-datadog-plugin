package org.datadog.jenkins.plugins.datadog;

import static hudson.Util.fixEmptyAndTrim;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

/**
 * DatadogBuildListener {@link RunListener}.
 *
 * <p>When the user configures the project and runs a build,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link DatadogBuildListener} is created. The created instance is persisted to the project
 * configuration XML by using XStream, allowing you to use instance fields
 * (like {@literal}link #name) to remember the configuration.
 *
 * <p>When a build starts, the {@link #onStarted(Run, TaskListener)} method will be invoked. And
 * when a build finishes, the {@link #onCompleted(Run, TaskListener)} method will be invoked.
 *
 * @author John Zeller
 */

@Extension
public class DatadogBuildListener extends RunListener<Run>
                                  implements Describable<DatadogBuildListener> {
  /**
   * Static variables describing consistent plugin names, Datadog API endpoints/codes, and magic
   * numbers.
   */
  static final String DISPLAY_NAME = "Datadog Plugin";
  static final String BASEURL = "https://app.datadoghq.com/api/";
  static final String VALIDATE = "v1/validate";
  static final String METRIC = "v1/series";
  static final String EVENT = "v1/events";
  static final String SERVICECHECK = "v1/check_run";
  static final Integer OK = 0;
  static final Integer WARNING = 1;
  static final Integer CRITICAL = 2;
  static final Integer UNKNOWN = 3;
  static final double THOUSAND_DOUBLE = 1000.0;
  static final long THOUSAND_LONG = 1000L;
  static final float MINUTE = 60;
  static final float HOUR = 3600;
  static final Integer HTTP_FORBIDDEN = 403;
  static final Integer MAX_HOSTNAME_LEN = 255;
  private static final Logger logger =  Logger.getLogger(DatadogBuildListener.class.getName());

  /**
   * Runs when the {@link DatadogBuildListener} class is created.
   */
  public DatadogBuildListener() { }

  /**
   * Called when a build is first started.
   *
   * @param run - A Run object representing a particular execution of Job.
   * @param listener - A TaskListener object which receives events that happen during some
   *                   operation.
   */
  @Override
  public final void onStarted(final Run run, final TaskListener listener) {
    String jobName = run.getParent().getDisplayName();

    // Process only if job is NOT in blacklist
    if ( isJobTracked(jobName) ) {
      logger.fine("Started build!");

      // Grab environment variables
      EnvVars envVars = null;
      try {
        envVars = run.getEnvironment(listener);
      } catch (IOException e) {
        logger.severe(e.getMessage());
      } catch (InterruptedException e) {
        logger.severe(e.getMessage());
      }

      // Gather pre-build metadata
      JSONObject builddata = new JSONObject();
      builddata.put("hostname", getHostname(envVars)); // string
      builddata.put("job", jobName); // string
      builddata.put("number", run.number); // int
      builddata.put("result", null); // null
      builddata.put("duration", null); // null
      builddata.put("buildurl", envVars.get("BUILD_URL")); // string
      long starttime = run.getStartTimeInMillis() / DatadogBuildListener.THOUSAND_LONG; // ms to s
      builddata.put("timestamp", starttime); // string

      // Add event_type to assist in roll-ups
      builddata.put("event_type", "build start"); // string

      event(builddata);
    }
  }

  /**
   * Called when a build is completed.
   *
   * @param run - A Run object representing a particular execution of Job.
   * @param listener - A TaskListener object which receives events that happen during some
   *                   operation.
   */
  @Override
  public final void onCompleted(final Run run, @Nonnull final TaskListener listener) {
    final String jobName = run.getParent().getDisplayName();

    // Process only if job in NOT in blacklist
    if ( isJobTracked(jobName) ) {
      logger.fine("Completed build!");

      // Collect Data
      JSONObject builddata = gatherBuildMetadata(run, listener);

      // Add event_type to assist in roll-ups
      builddata.put("event_type", "build result"); // string

      // Report Data
      event(builddata);
      gauge("jenkins.job.duration", builddata, "duration");
      if ( "SUCCESS".equals(builddata.get("result")) ) {
        serviceCheck("jenkins.job.status", DatadogBuildListener.OK, builddata);
      } else {
        serviceCheck("jenkins.job.status", DatadogBuildListener.CRITICAL, builddata);
      }
    }
  }

  /**
   * Gathers build metadata, assembling it into a {@link JSONObject} before
   * returning it to the caller.
   *
   * @param run - A Run object representing a particular execution of Job.
   * @param listener - A TaskListener object which receives events that happen during some
   *                   operation.
   * @return a JSONObject containing a builds metadata.
   */
  private JSONObject gatherBuildMetadata(final Run run, @Nonnull final TaskListener listener) {
    // Grab environment variables
    EnvVars envVars = null;
    try {
      envVars = run.getEnvironment(listener);
    } catch (IOException e) {
      logger.severe(e.getMessage());
    } catch (InterruptedException e) {
      logger.severe(e.getMessage());
    }

    // Assemble JSON
    long starttime = run.getStartTimeInMillis() / DatadogBuildListener.THOUSAND_LONG; // ms to s
    double duration = run.getDuration() / DatadogBuildListener.THOUSAND_DOUBLE; // ms to s
    long endtime = starttime + (long) duration; // ms to s
    JSONObject builddata = new JSONObject();
    builddata.put("starttime", starttime); // long
    builddata.put("duration", duration); // double
    builddata.put("timestamp", endtime); // long
    builddata.put("result", run.getResult().toString()); // string
    builddata.put("number", run.number); // int
    builddata.put("job", run.getParent().getDisplayName()); // string
    builddata.put("hostname", getHostname(envVars)); // string
    builddata.put("buildurl", envVars.get("BUILD_URL")); // string
    builddata.put("node", envVars.get("NODE_NAME")); // string

    if ( envVars.get("GIT_BRANCH") != null ) {
      builddata.put("branch", envVars.get("GIT_BRANCH")); // string
    } else if ( envVars.get("CVS_BRANCH") != null ) {
      builddata.put("branch", envVars.get("CVS_BRANCH")); // string
    }

    return builddata;
  }

  /**
   * Assembles a {@link JSONArray} from metadata available in the
   * {@link JSONObject} builddata. Returns a {@link JSONArray} with the set
   * of tags.
   *
   * @param builddata - A JSONObject containing a builds metadata.
   * @return a JSONArray containing a specific subset of tags retrieved from a builds metadata.
   */
  private JSONArray assembleTags(final JSONObject builddata) {
    JSONArray tags = new JSONArray();
    tags.add("job:" + builddata.get("job"));
    if ( (builddata.get("node") != null) && getDescriptor().getTagNode() ) {
      tags.add("node:" + builddata.get("node"));
    }
    if ( builddata.get("result") != null ) {
      tags.add("result:" + builddata.get("result"));
    }
    if ( builddata.get("branch") != null ) {
      tags.add("branch:" + builddata.get("branch"));
    }

    return tags;
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
  public final Boolean post(final JSONObject payload, final String type) throws IOException {
    String urlParameters = "?api_key=" + getDescriptor().getApiKey();
    HttpURLConnection conn = null;

    try {
      // Make request
      conn = DatadogBuildListener.getHttpURLConnection(new URL(DatadogBuildListener.BASEURL + type
                                                               + urlParameters));
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setUseCaches(false);
      conn.setDoInput(true);
      conn.setDoOutput(true);

      // Send request
      DataOutputStream wr = new DataOutputStream( conn.getOutputStream() );
      wr.writeBytes( payload.toString() );
      wr.flush();
      wr.close();

      // Get response
      BufferedReader rd = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
      StringBuilder result = new StringBuilder();
      String line;
      while ( (line = rd.readLine()) != null ) {
        result.append(line);
      }
      rd.close();
      JSONObject json = (JSONObject) JSONSerializer.toJSON( result.toString() );
      if ( "ok".equals(json.getString("status")) ) {
        logger.finer(String.format("API call of type '%s' was sent successfully!", type));
        logger.finer(String.format("Payload: %s", payload));
        return true;
      } else {
        logger.fine(String.format("API call of type '%s' failed!", type));
        logger.fine(String.format("Payload: %s", payload));
        return false;
      }
    } catch (Exception e) {
      if ( conn.getResponseCode() == DatadogBuildListener.HTTP_FORBIDDEN ) {
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

  /**
   * Sends a metric to the Datadog API, including the gauge name, and value.
   *
   * @param metricName - A String with the name of the metric to record.
   * @param builddata - A JSONObject containing a builds metadata.
   * @param key - A String with the name of the build metadata to be found in the {@link JSONObject}
   *              builddata.
   */
  public final void gauge(final String metricName, final JSONObject builddata,
                          final String key) {
    String builddataKey = nullSafeGetString(builddata, key);
    logger.fine(String.format("Sending metric '%s' with value %s", metricName, builddataKey));

    // Setup data point, of type [<unix_timestamp>, <value>]
    JSONArray points = new JSONArray();
    JSONArray point = new JSONArray();
    point.add(System.currentTimeMillis() / DatadogBuildListener.THOUSAND_LONG); // current time, s
    point.add(builddata.get(key));
    points.add(point); // api expects a list of points

    // Build metric
    JSONObject metric = new JSONObject();
    metric.put("metric", metricName);
    metric.put("points", points);
    metric.put("type", "gauge");
    metric.put("host", builddata.get("hostname"));
    metric.put("tags", assembleTags(builddata));

    // Place metric as item of series list
    JSONArray series = new JSONArray();
    series.add(metric);

    // Add series to payload
    JSONObject payload = new JSONObject();
    payload.put("series", series);

    try {
      post(payload, DatadogBuildListener.METRIC);
    } catch (Exception e) {
      logger.severe(e.toString());
    }
  }

  /**
   * Sends a service check to the Datadog API, including the check name, and status.
   *
   * @param checkName - A String with the name of the service check to record.
   * @param status - An Integer with the status code to record for this service check.
   * @param builddata - A JSONObject containing a builds metadata.
   */
  public final void serviceCheck(final String checkName, final Integer status,
                                 final JSONObject builddata) {
    logger.fine(String.format("Sending service check '%s' with status %s", checkName, status));

    // Build payload
    JSONObject payload = new JSONObject();
    payload.put("check", checkName);
    payload.put("host_name", builddata.get("hostname"));
    payload.put("timestamp",
                System.currentTimeMillis() / DatadogBuildListener.THOUSAND_LONG); // current time, s
    payload.put("status", status);
    payload.put("tags", assembleTags(builddata));

    try {
      post(payload, DatadogBuildListener.SERVICECHECK);
    } catch (Exception e) {
      logger.severe(e.toString());
    }
  }

  /**
   * Sends a an event to the Datadog API, including the event payload.
   *
   * @param builddata - A JSONObject containing a builds metadata.
   */
  public final void event(final JSONObject builddata) {
    logger.fine("Sending event");

    // Gather data
    JSONObject payload = new JSONObject();
    String hostname = nullSafeGetString(builddata, "hostname");
    String number = nullSafeGetString(builddata, "number");
    String buildurl = nullSafeGetString(builddata, "buildurl");
    String job = nullSafeGetString(builddata, "job");
    long timestamp = builddata.getLong("timestamp");
    String message = "";

    // Setting source_type_name here, to allow modification based on type of event
    payload.put("source_type_name", "jenkins");

    // Build title
    StringBuilder title = new StringBuilder();
    title.append(job).append(" build #").append(number);
    if ( "SUCCESS".equals( builddata.get("result") ) ) {
      title.append(" succeeded");
      payload.put("alert_type", "success");
      message = "%%% \n [See results for build #" + number + "](" + buildurl + ") ";
    } else if ( builddata.get("result") != null ) {
      title.append(" failed");
      payload.put("alert_type", "failure");
      message = "%%% \n [See results for build #" + number + "](" + buildurl + ") ";
    } else {
      title.append(" started");
      payload.put("alert_type", "info");
      message = "%%% \n [Follow build #" + number + " progress](" + buildurl + ") ";
    }
    title.append(" on ").append(hostname);

    // Add duration
    if ( builddata.get("duration") != null ) {
      message = message + durationToString(builddata.getDouble("duration"));
    }

    // Close markdown
    message = message + " \n %%%";

    // Build payload
    payload.put("title", title.toString());
    payload.put("text", message);
    payload.put("date_happened", timestamp);
    payload.put("event_type", builddata.get("event_type"));
    payload.put("host", hostname);
    payload.put("result", builddata.get("result"));
    payload.put("tags", assembleTags(builddata));
    payload.put("aggregation_key", job); // Used for job name in event rollups

    try {
      post(payload, DatadogBuildListener.EVENT);
    } catch (Exception e) {
      logger.severe(e.toString());
    }
  }

  /**
   * Getter function to return either the saved hostname global configuration,
   * or the hostname that is set in the Jenkins host itself. Returns null if no
   * valid hostname is found.
   * <p>
   * Tries, in order:
   *    Jenkins configuration
   *    Jenkins hostname environment variable
   *    Unix hostname via `/bin/hostname -f`
   *    Localhost hostname
   *
   * @param envVars - An EnvVars object containing a set of environment variables.
   * @return a human readable String for the hostname.
   */
  public final String getHostname(final EnvVars envVars) {
    String[] UNIX_OS = {"mac", "linux", "freebsd", "sunos"};
    String hostname = null;

    // Check hostname configuration from Jenkins
    hostname = getDescriptor().getHostname();
    if ( (hostname != null) && isValidHostname(hostname) ) {
      logger.fine(String.format("Using hostname set in 'Manage Plugins'. Hostname: %s", hostname));
      return hostname;
    }

    // Check hostname using jenkins env variables
    if ( envVars.get("HOSTNAME") != null ) {
      hostname = envVars.get("HOSTNAME").toString();
    }
    if ( (hostname != null) && isValidHostname(hostname) ) {
      logger.fine(String.format("Using hostname found in $HOSTNAME host environment variable. "
                                + "Hostname: %s", hostname));
      return hostname;
    }

    // Check OS specific unix commands
    String os = getOS();
    if ( Arrays.asList(UNIX_OS).contains(os) ) {
      // Attempt to grab unix hostname
      try {
        String[] cmd = {"/bin/hostname", "-f"};
        Process proc = Runtime.getRuntime().exec(cmd);
        InputStream in = proc.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ( (line = reader.readLine()) != null ) {
          out.append(line);
        }

        hostname = out.toString();
      } catch (Exception e) {
        logger.severe(e.getMessage());
      }

      // Check hostname
      if ( (hostname != null) && isValidHostname(hostname) ) {
        logger.fine(String.format("Using unix hostname found via `/bin/hostname -f`. Hostname: %s",
                                  hostname));
        return hostname;
      }
    }

    // Check localhost hostname
    try {
      hostname = Inet4Address.getLocalHost().getHostName().toString();
    } catch (UnknownHostException e) {
      logger.fine(String.format("Unknown hostname error received for localhost. Error: %s", e));
    }
    if ( (hostname != null) && isValidHostname(hostname) ) {
      logger.fine(String.format("Using hostname found via "
                                + "Inet4Address.getLocalHost().getHostName()."
                                + " Hostname: %s", hostname));
      return hostname;
    }

    // Never found the hostname
    if ( (hostname == null) || "".equals(hostname) ) {
      logger.warning("Unable to reliably determine host name. You can define one in "
                     + "the 'Manage Plugins' section under the 'Datadog Plugin' section.");
    }
    return null;
  }

  /**
   * Validator function to ensure that the hostname is valid. Also, fails on
   * empty String.
   *
   * @param hostname - A String object containing the name of a host.
   * @return a boolean representing the validity of the hostname
   */
  public static final Boolean isValidHostname(final String hostname) {
    String[] localHosts = {"localhost", "localhost.localdomain",
                           "localhost6.localdomain6", "ip6-localhost"};
    String VALID_HOSTNAME_RFC_1123_PATTERN = "^(([a-zA-Z0-9]|"
                                             + "[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*"
                                             + "([A-Za-z0-9]|"
                                             + "[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
    String host = hostname.toLowerCase();

    // Check if hostname is local
    if ( Arrays.asList(localHosts).contains(host) ) {
      logger.fine(String.format("Hostname: %s is local", hostname));
      return false;
    }

    // Ensure proper length
    if ( hostname.length() > DatadogBuildListener.MAX_HOSTNAME_LEN ) {
      logger.fine(String.format("Hostname: %s is too long (max length is %s characters)",
                                hostname, DatadogBuildListener.MAX_HOSTNAME_LEN));
      return false;
    }

    // Check compliance with RFC 1123
    Pattern r = Pattern.compile(VALID_HOSTNAME_RFC_1123_PATTERN);
    Matcher m = r.matcher(hostname);
    if ( !m.find() ) {
      return false;
    }

    // Passed all checks, so the hostname is valid
    return true;
  }

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
   * Checks if a jobName is blacklisted, or not.
   *
   * @param jobName - A String containing the name of some job.
   * @return a boolean to signify if the jobName is or is not blacklisted.
   */
  private final boolean isJobTracked(final String jobName) {
    final String[] blacklist = blacklistStringtoArray( getDescriptor().getBlacklist() );
    return (blacklist == null) || !Arrays.asList(blacklist).contains(jobName.toLowerCase());
  }

  /**
   * Converts from a double to a human readable string, representing a time duration.
   *
   * @param duration - A Double with a duration in seconds.
   * @return a human readable String representing a time duration.
   */
  public final String durationToString(final double duration) {
    String output = "(";
    String format = "%.2f";
    if ( duration < DatadogBuildListener.MINUTE ) {
      output = output + String.format(format, duration) + " secs)";
    } else if ( (DatadogBuildListener.MINUTE <= duration)
                && (duration < DatadogBuildListener.HOUR) ) {
      output = output + String.format(format, duration / DatadogBuildListener.MINUTE)
               + " mins)";
    } else if ( DatadogBuildListener.HOUR <= duration ) {
      output = output + String.format(format, duration / DatadogBuildListener.HOUR)
               + " hrs)";
    }

    return output;
  }

  /**
  * Human-friendly OS name. Commons return values are windows, linux, mac, sunos, freebsd
  *
  * @return a String with a human-friendly OS name
  */
  public final String getOS() {
    String out = System.getProperty("os.name");
    String os = out.split(" ")[0];

    return os.toLowerCase();
  }

  /**
   * Safe getter function to make sure an exception is not reached.
   *
   * @param data - A JSONObject containing a set of key/value pairs.
   * @param key - A String to be used to lookup a value in the JSONObject data.
   * @return a String representing data.get(key), or "null" if it doesn't exist
   */
  public final String nullSafeGetString(final JSONObject data, final String key) {
    if ( data.get(key) != null ) {
      return data.get(key).toString();
    } else {
      return "null";
    }
  }

  /**
   * Converts a blacklist string into a String array.
   *
   * @param blacklist - A String containing a set of key/value pairs.
   * @return a String array representing the job names to be blacklisted. Returns
   *         empty string if blacklist is null.
   */
  public final String[] blacklistStringtoArray(final String blacklist) {
    if ( blacklist != null ) {
      return blacklist.split(",");
    }
    return ( new String[0] );
  }

  /**
   * Getter function for the {@link DescriptorImpl} class.
   *
   * @return a new {@link DescriptorImpl} class.
   */
  public final DescriptorImpl getDescriptor() {
    return new DescriptorImpl();
  }

  /**
   * Descriptor for {@link DatadogBuildListener}. Used as a singleton.
   * The class is marked as public so that it can be accessed from views.
   *
   * <p>See <tt>DatadogBuildListener/*.jelly</tt> for the actual HTML fragment
   * for the configuration screen.
   */
  @Extension // Indicates to Jenkins that this is an extension point implementation.
  public static final class DescriptorImpl extends Descriptor<DatadogBuildListener> {
    /**
     * Persist global configuration information by storing in a field and
     * calling save().
     */
    private Secret apiKey = null;
    private String hostname = null;
    private String blacklist = null;
    private Boolean tagNode = null;

    /**
     * Runs when the {@link DescriptorImpl} class is created.
     */
    public DescriptorImpl() {
      load(); // load the persisted global configuration
    }

    /**
     * Tests the {@link apiKey} from the configuration screen, to check its' validity.
     *
     * @param formApiKey - A String containing the apiKey submitted from the form on the
     *                     configuration screen, which will be used to authenticate a request to the
     *                     Datadog API.
     * @return a FormValidation object used to display a message to the user on the configuration
     *         screen.
     * @throws IOException if there is an input/output exception.
     * @throws ServletException if there is a servlet exception.
     */
    public FormValidation doTestConnection(@QueryParameter("apiKey") final String formApiKey)
        throws IOException, ServletException {
      String urlParameters = "?api_key=" + formApiKey;
      HttpURLConnection conn = null;

      try {
        // Make request
        conn = DatadogBuildListener.getHttpURLConnection(new URL(DatadogBuildListener.BASEURL
                                                                 + DatadogBuildListener.VALIDATE
                                                                 + urlParameters));
        conn.setRequestMethod("GET");

        // Get response
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
        rd.close();

        // Validate
        JSONObject json = (JSONObject) JSONSerializer.toJSON( result.toString() );
        if ( json.getBoolean("valid") ) {
          return FormValidation.ok("Great! Your API key is valid.");
        } else {
          return FormValidation.error("Hmmm, your API key seems to be invalid.");
        }
      } catch (Exception e) {
        if ( conn.getResponseCode() == DatadogBuildListener.HTTP_FORBIDDEN ) {
          return FormValidation.error("Hmmm, your API key may be invalid. "
                                      + "We received a 403 error.");
        }
        return FormValidation.error("Client error: " + e);
      } finally {
        if (conn != null) {
          conn.disconnect();
        }
      }
    }

    /**
     * Tests the {@link hostname} from the configuration screen, to determine if
     * the hostname is of a valid format, according to the RFC 1123.
     *
     * @param formHostname - A String containing the hostname submitted from the form on the
     *                     configuration screen, which will be used to authenticate a request to the
     *                     Datadog API.
     * @return a FormValidation object used to display a message to the user on the configuration
     *         screen.
     * @throws IOException if there is an input/output exception.
     * @throws ServletException if there is a servlet exception.
     */
    public FormValidation doTestHostname(@QueryParameter("hostname") final String formHostname)
        throws IOException, ServletException {
      if ( ( null != formHostname ) && DatadogBuildListener.isValidHostname(formHostname) ) {
        return FormValidation.ok("Great! Your hostname is valid.");
      } else {
        return FormValidation.error("Your hostname is invalid, likely because"
                                    + " it violates the format set in RFC 1123.");
      }
    }

    /**
     * Indicates if this builder can be used with all kinds of project types.
     *
     * @param aClass - An extension of the AbstractProject class representing a specific type of
     *                 project.
     * @return a boolean signifying whether or not a builder can be used with a specific type of
     *         project.
     */
    public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
      return true;
    }

    /**
     * Getter function for a human readable plugin name, used in the configuration screen.
     *
     * @return a String containing the human readable display name for this plugin.
     */
    public String getDisplayName() {
      return DatadogBuildListener.DISPLAY_NAME;
    }

    /**
     * Indicates if this builder can be used with all kinds of project types.
     *
     * @param req - A StaplerRequest object
     * @param formData - A JSONObject containing the submitted form data from the configuration
     *                   screen.
     * @return a boolean signifying the success or failure of configuration.
     * @throws FormException if the formData is invalid.
     */
    @Override
    public boolean configure(final StaplerRequest req, final JSONObject formData)
           throws FormException {
      // Grab apiKey and hostname
      apiKey = Secret.fromString(fixEmptyAndTrim(formData.getString("apiKey")));
      hostname = formData.getString("hostname");

      // Grab blacklist, strip whitespace, remove duplicate commas, and make lowercase
      blacklist = formData.getString("blacklist")
                          .replaceAll("\\s", "")
                          .replaceAll(",,", "")
                          .toLowerCase();

      // Grab tagNode and coerse to a boolean
      if ( formData.getString("tagNode").equals("true") ) {
        tagNode = true;
      } else {
        tagNode = false;
      }

      // Persist global configuration information
      save();
      return super.configure(req, formData);
    }

    /**
     * Getter function for the {@link apiKey} global configuration.
     *
     * @return a String containing the {@link apiKey} global configuration.
     */
    public String getApiKey() {
      return apiKey.getPlainText();
    }

    /**
     * Setter fuction for the {@link apiKey} global configuration.
     *
     * @param key = A string containing the plaintext representation of a
     *     DataDog API Key
     */
    public void setApiKey(final String key) {
      apiKey = Secret.fromString(fixEmptyAndTrim(key));
    }

    /**
     * Getter function for the {@link hostname} global configuration.
     *
     * @return a String containing the {@link hostname} global configuration.
     */
    public String getHostname() {
      return hostname;
    }

    /**
     * Setter function for the {@link hostname} global configuration.
     *
     * @param host - A String containing the hostname of the Jenkins host.
     */
    public void setHostname(final String host) {
      hostname = host;
    }

    /**
     * Getter function for the {@link blacklist} global configuration, containing
     * a comma-separated list of jobs to blacklist from monitoring.
     *
     * @return a String array containing the {@link blacklist} global configuration.
     */
    public String getBlacklist() {
      return blacklist;
    }

    /**
     * Setter function for the {@link blacklist} global configuration,
     * accepting a comma-separated string of jobs that will be sanitized.
     *
     * @param jobs - a comma-separated list of jobs to blacklist from monitoring.
     */
    public void setBlacklist(final String jobs) {
      // strip whitespace, remove duplicate commas, and make lowercase
      blacklist = jobs
        .replaceAll("\\s", "")
        .replaceAll(",,", "")
        .toLowerCase();
    }

    /**
     * Getter function for the optional tag {@link node} global configuration.
     *
     * @return a Boolean containing optional tag value for the {@link node}
     *     global configuration.
     */
    public Boolean getTagNode() {
      return tagNode;
    }

    /**
     * Setter function for the optional tag {@link node} global configuration.
     *
     * @param willTag - A Boolean expressing whether the {@link node} tag will
     *     be included.
     */
    public void setTagNode(final Boolean willTag) {
      tagNode = willTag;
    }
  }
}

