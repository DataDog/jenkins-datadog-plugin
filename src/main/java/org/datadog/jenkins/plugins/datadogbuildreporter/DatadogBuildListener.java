package org.datadog.jenkins.plugins.datadogbuildreporter;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import java.io.BufferedReader;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import net.sf.json.JSON;

import javax.servlet.ServletException;
import javax.annotation.Nonnull;
import java.io.*;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.sf.json.JSONSerializer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;


/**
 * DatadogBuildListener {@link RunListener}.
 * 
 * <p>
 * When the user configures the project and runs a build,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link DatadogBuildListener} is created. The created instance is persisted
 * to the project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 *
 * <p>
 * When a build finishes, the {@link #onCompleted(Run, TaskListener)} method
 * will be invoked. 
 *
 * @author John Zeller
 */

@Extension
public class DatadogBuildListener extends RunListener<Run> implements Describable<DatadogBuildListener> {

    public static String DISPLAY_NAME = "Datadog Build Reporter";
    public static String BASEURL = "https://app.datadoghq.com/api/";
    public static String VALIDATE = "v1/validate";
    public static String METRIC = "v1/series";
    public static String EVENT = "v1/events";
    public static String SERVICECHECK = "v1/check_run";
    public static Integer OK = 0;
    public static Integer WARNING = 1;
    public static Integer CRITICAL = 2;
    public static Integer UNKNOWN = 3;
    private PrintStream logger = null;
    
    public DatadogBuildListener() { }

    /**
     * Called when a build is first started.
     */
    @Override
    public void onStarted(Run run, TaskListener listener) {
        logger = listener.getLogger();
        listener.getLogger().println("Started build!");
    }

    /**
     * Called when a build is completed.
     */
    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        logger = listener.getLogger();
        listener.getLogger().println("Completed build!");

        // Collect Data
        JSONObject builddata = gatherBuildMetadata(run, listener);
        JSONObject payload = assemblePayload(builddata);
        JSONArray tags = assembleTags(builddata);

        // Report Data
        event(builddata, tags);
        gauge("jenkins.job.duration", builddata, "duration", tags);
        if ( "SUCCESS".equals(payload.get("result")) ) {
            serviceCheck("jenkins.job.status", DatadogBuildListener.OK, tags);
        } else {
            serviceCheck("jenkins.job.status", DatadogBuildListener.CRITICAL, tags);
        }
    }

    /**
     * Gathers build metadata, assembling it into a {@link JSONObject} before
     * returning it to the caller.
     */
    private JSONObject gatherBuildMetadata(Run run, @Nonnull TaskListener listener) {
        // Grab environment variables
        EnvVars envVars = null;
        try {
            envVars = run.getEnvironment(listener);
        } catch (IOException ex) {
            Logger.getLogger(DatadogBuildListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(DatadogBuildListener.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Assemble JSON
        double starttime = run.getStartTimeInMillis() / 1000.0;
        double duration = run.getDuration() / 1000.0;
        JSONObject builddata = new JSONObject();
        builddata.put("starttime", starttime); // double, adjusted from ms to s
        builddata.put("duration", duration); // double, adjusted from ms to s
        builddata.put("endtime", starttime + duration); // double, adjusted from ms to s
        builddata.put("result", run.getResult().toString()); // string
        builddata.put("number", run.number); // int
        builddata.put("job_name", run.getParent().getDisplayName()); // string
        builddata.put("hostname", envVars.get("HOSTNAME")); // string
        builddata.put("node", envVars.get("NODE_NAME")); // string

        if ( envVars.get("GIT_BRANCH") != null ) {
            builddata.put("branch", envVars.get("GIT_BRANCH")); // string
        } else if ( envVars.get("CVS_BRANCH") != null ) {
            builddata.put("branch", envVars.get("CVS_BRANCH")); // string
        }

        return builddata;
    }

    /**
     * Assembles a {@link JSONObject} payload from metadata available in the
     * {@link JSONObject} builddata. Returns a {@link JSONObject} with the new
     * payload.
     */
    private JSONObject assemblePayload(JSONObject builddata) {
        JSONObject payload = new JSONObject();
        payload.put("host", builddata.get("hostname"));
        payload.put("job_name", builddata.get("job_name"));
        payload.put("event_type", "build result");
        payload.put("timestamp", builddata.get("endtime"));
        payload.put("result", builddata.get("result"));
        payload.put("number", builddata.get("number"));
        payload.put("duration", builddata.get("duration"));
        payload.put("node", builddata.get("node"));

        if ( builddata.get("branch") != null ) {
            payload.put("branch", builddata.get("branch"));
        }

        return payload;
    }

    /**
     * Assembles a {@link JSONArray} from metadata available in the
     * {@link JSONObject} builddata. Returns a {@link JSONArray} with the set
     * of tags.
     */
    private JSONArray assembleTags(JSONObject builddata) {
        JSONArray tags = new JSONArray();
        tags.add("job_name:" + builddata.get("job_name"));
        tags.add("result:" + builddata.get("result"));
        tags.add("build_number:" + builddata.get("number"));
        if ( builddata.get("branch") != null ) {
            tags.add("branch:" + builddata.get("branch"));
        }

        return tags;
    };

    /**
     * Posts a given {@link JSONObject} payload to the DataDog API, using the
     * user configured apiKey.
     */
    public Boolean post(JSONObject payload, String type) {
        String urlParameters = "api_key=" + getDescriptor().getApiKey();
        HttpURLConnection conn = null;
        
        try {
            // Create connection
            BufferedReader rd;
            String line;
            StringBuilder result = new StringBuilder();

            // Make request
            URL url = new URL(DatadogBuildListener.BASEURL + type + "?" + urlParameters);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setUseCaches (false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream( conn.getOutputStream() );
            wr.writeBytes(payload.toString());
            wr.flush();
            wr.close();

            // Get response
            rd = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
            while ((line = rd.readLine()) != null) {
               result.append(line);
            }
            rd.close();
            JSONObject json = (JSONObject) JSONSerializer.toJSON( result.toString() );
            if ( "ok".equals(json.getString("status")) ) {
                logger.println("API call of type '" + type + "' was sent successfully!");
                logger.println("Payload: " + payload.toString());
                return true;
            } else {
                logger.println("API call of type '" + type + "' failed!");
                logger.println("Payload: " + payload.toString());
                return false;
            }
        } catch (Exception e) {
            if ( conn.getResponseCode() == 403 ) {
                logger.println("Hmmm, your API key may be invalid. We received a 403 error.");
                return false;
            }
            logger.println("Client error: " + e);
            return false;
        } finally {
            if(conn != null) {
                conn.disconnect(); 
            }
            return true;
        }
    }

    /**
     * Sends a metric to the Datadog API, including the gauge name, value, and
     * a set of tags.
     */
    public void gauge(String metricName, JSONObject builddata, String key, JSONArray tags) {
        JSONObject payload = new JSONObject();
        JSONArray series = new JSONArray();
        JSONObject metric = new JSONObject();

        // Setup data point, of type [UNIX_TIMESTAMP, VALUE]
        JSONArray points = new JSONArray();
        JSONArray point = new JSONArray();
        point.add(System.currentTimeMillis() / 1000L); // current time in seconds
        point.add(builddata.get(key));
        points.add(point); // api expects a list of points

        // Build metric
        metric.put("metric", metricName);
        metric.put("points", points);
        metric.put("type", "gauge");
        metric.put("host", builddata.get("hostname"));
        metric.put("tags", tags);

        // Place metric as item of series list
        series.add(metric);

        // Add series to payload
        payload.put("series", series);
        
        post(payload, DatadogBuildListener.METRIC);
    }

    /**
     * Sends a service check to the Datadog API, including the check name,
     * status, and a set of tags.
     */
    public void serviceCheck(String checkName, Integer status, JSONArray tags) {
        logger.println("Service check called for '" + checkName + "' with status " + status.toString());
    }

    /**
     * Sends a an event to the Datadog API, including the event payload, and a
     * set of tags.
     */
    public void event(JSONObject builddata, JSONArray tags) {
        // Build payload
        JSONObject payload = new JSONObject();
        String title = builddata.get("job_name").toString();
        if ( "SUCCESS".equals( builddata.get("result") ) ) {
            title = title + " succeeded";
        } else {
            title = title + " failed";
        }
        title = title + " on " + builddata.get("hostname").toString();
        payload.put("title", title);
        payload.put("text", "");
        payload.put("priority", "normal");
        payload.put("tags", tags);
        payload.put("alert_type", "info");
        
        post(payload, DatadogBuildListener.EVENT);
    }

    public DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }

    /**
     * Descriptor for {@link DatadogBuildListener}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>DatadogBuildListener/*.jelly</tt> for the actual HTML fragment
     * for the configuration screen.
     */
    @Extension // Indicates to Jenkins that this is an extension point implementation.
    public static final class DescriptorImpl extends Descriptor<DatadogBuildListener> {
        /**
         * Persist global configuration information by storing in a field and
         * calling save().
         */
        private String apiKey;

        public DescriptorImpl() {
            load(); // load the persisted global configuration
        }

        /**
         * Tests the apiKey from the configuration screen, to check its' validity.
         */
        public FormValidation doTestConnection(@QueryParameter("apiKey") final String apiKey)
                throws IOException, ServletException {
            String urlParameters = "api_key=" + apiKey;
            HttpURLConnection conn = null;
            
            try {
                // Create connection
                BufferedReader rd;
                String line;
                StringBuilder result = new StringBuilder();

                // Make request
                URL url = new URL(DatadogBuildListener.BASEURL + DatadogBuildListener.VALIDATE + "?" + urlParameters);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
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
                if ( conn.getResponseCode() == 403 ) {
                    return FormValidation.error("Hmmm, your API key may to be invalid. We received a 403 error.");
                }
                return FormValidation.error("Client error: " + e);
            } finally {
                if(conn != null) {
                    conn.disconnect(); 
                }
            }
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return DatadogBuildListener.DISPLAY_NAME;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            apiKey = formData.getString("apiKey");
            save(); // persist global configuration information
            return super.configure(req,formData);
        }

        public String getApiKey() {
            return apiKey;
        }
    }
}

