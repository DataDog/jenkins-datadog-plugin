package org.datadog.jenkins.plugins.datadog;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static hudson.Util.fixEmptyAndTrim;

/**
 * DatadogBuildListener {@link RunListener}.
 * When the user configures the project and runs a build,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link DatadogBuildListener} is created. The created instance is persisted to the project
 * configuration XML by using XStream, allowing you to use instance fields
 * (like {@literal}link #name) to remember the configuration.
 * When a build starts, the {@link #onStarted(Run, TaskListener)} method will be invoked. And
 * when a build finishes, the {@link #onCompleted(Run, TaskListener)} method will be invoked.
 *
 * @author John Zeller
 */

@Extension
public class DatadogBuildListener extends RunListener<Run> implements Describable<DatadogBuildListener> {
    /**
     * Static variables describing consistent plugin names, Datadog API endpoints/codes, and magic
     * numbers.
     */
    static final String DISPLAY_NAME = "Datadog Plugin";
    static final Integer OK = 0;
    static final Integer WARNING = 1;
    static final Integer CRITICAL = 2;
    static final Integer UNKNOWN = 3;
    static final double THOUSAND_DOUBLE = 1000.0;
    static final long THOUSAND_LONG = 1000L;
    static final float MINUTE = 60;
    static final float HOUR = 3600;
    static final Integer MAX_HOSTNAME_LEN = 255;

    private static final Logger logger = Logger.getLogger(DatadogBuildListener.class.getName());
    private static final Queue queue = Queue.getInstance();

    /**
     * Runs when the {@link DatadogBuildListener} class is created.
     */
    public DatadogBuildListener() {
    }

    /**
     * Called when a build is first started.
     *
     * @param run      - A Run object representing a particular execution of Job.
     * @param listener - A TaskListener object which receives events that happen during some
     *                 operation.
     */
    @Override
    public final void onStarted(final Run run, final TaskListener listener) {
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }
        String jobName = run.getParent().getFullName();
        logger.fine(String.format("onStarted() called with jobName: %s", jobName));
        Map<String, String> tags = new HashMap<String, String>();

        // Process only if job is NOT in blacklist and is in whitelist
        if (DatadogUtilities.isJobTracked(jobName)) {
            logger.fine("Started build!");

            // Get the list of global tags to apply
            tags.putAll(DatadogUtilities.getRegexJobTags(jobName));

            Queue.Item item = queue.getItem(run.getQueueId());

            // Gather pre-build metadata
            JSONObject builddata = new JSONObject();
            Map<String, String> extraTags = DatadogUtilities.buildExtraTags(run, listener);
            extraTags.putAll(tags);
            builddata.put("job", DatadogUtilities.normalizeFullDisplayName(jobName)); // string
            builddata.put("number", run.number); // int
            builddata.put("result", null); // null
            builddata.put("duration", null); // null
            long starttime = run.getStartTimeInMillis() / DatadogBuildListener.THOUSAND_LONG; // ms to s
            builddata.put("timestamp", starttime); // string

            // Grab environment variables
            try {
                EnvVars envVars = run.getEnvironment(listener);
                tags.putAll(DatadogUtilities.parseTagList(run, listener));
                builddata.put("hostname", DatadogUtilities.getHostname(envVars)); // string
                builddata.put("buildurl", envVars.get("BUILD_URL")); // string
                builddata.put("node", envVars.get("NODE_NAME")); // string
            } catch (IOException | InterruptedException e) {
                logger.severe(e.getMessage());
            }

            BuildStartedEventImpl evt = new BuildStartedEventImpl(builddata, tags);
            DatadogHttpRequests.sendEvent(evt);

            // item.getInQueueSince() may raise a NPE if a worker node is spinning up to run the job.
            // This could be expected behavior with ec2 spot instances/ecs containers, meaning no waiting
            // queue times if the plugin is spinning up an instance/container for one/first job.
            try {
                builddata.put("waiting", (System.currentTimeMillis() - item.getInQueueSince()) / DatadogBuildListener.THOUSAND_LONG);
                DatadogHttpRequests.gauge("jenkins.job.waiting", builddata, "waiting", extraTags);
            } catch (NullPointerException e) {
                logger.warning("Unable to compute 'waiting' metric. item.getInQueueSince() unavailable, possibly due to worker instance provisioning");
            }

            logger.fine("Finished onStarted()");
        }
    }

    /**
     * Called when a build is completed.
     *
     * @param run      - A Run object representing a particular execution of Job.
     * @param listener - A TaskListener object which receives events that happen during some
     *                 operation.
     */

    @Override
    public final void onCompleted(final Run run, @Nonnull final TaskListener listener) {
        if (DatadogUtilities.isApiKeyNull()) {
            return;
        }
        String jobName = run.getParent().getFullName();

        // Process only if job in NOT in blacklist and is in whitelist
        if (DatadogUtilities.isJobTracked(jobName)) {
            logger.fine("Completed build!");

            // Collect Data
            JSONObject builddata = gatherBuildMetadata(run, listener);
            HashMap<String, String> extraTags = DatadogUtilities.buildExtraTags(run, listener);

            // Get the list of global tags to apply
            extraTags.putAll(DatadogUtilities.getRegexJobTags(jobName));
            JSONArray tagArr = DatadogUtilities.assembleTags(builddata, extraTags);
            DatadogEvent evt = new BuildFinishedEventImpl(builddata, extraTags);
            DatadogHttpRequests.sendEvent(evt);
            DatadogHttpRequests.gauge("jenkins.job.duration", builddata, "duration", extraTags);

            String buildResult = Result.NOT_BUILT.toString();
            if (builddata.get("result") != null) {
                buildResult = builddata.get("result").toString();
            }
            if (Result.SUCCESS.toString().equals(buildResult)) {
                DatadogHttpRequests.serviceCheck("jenkins.job.status", DatadogBuildListener.OK, builddata, extraTags);
            } else if (Result.UNSTABLE.toString().equals(buildResult) ||
                    Result.ABORTED.toString().equals(buildResult) ||
                    Result.NOT_BUILT.toString().equals(buildResult)) {
                DatadogHttpRequests.serviceCheck("jenkins.job.status", DatadogBuildListener.WARNING, builddata, extraTags);
            } else if (Result.FAILURE.toString().equals(buildResult)) {
                DatadogHttpRequests.serviceCheck("jenkins.job.status", DatadogBuildListener.CRITICAL, builddata, extraTags);
            } else {
                DatadogHttpRequests.serviceCheck("jenkins.job.status", DatadogBuildListener.UNKNOWN, builddata, extraTags);
            }

            // Setup tags for StatsDClient reporting
            String[] statsdTags = new String[tagArr.size()];
            for (int i = 0; i < tagArr.size(); i++) {
                statsdTags[i] = tagArr.getString(i);
            }

            // Report to StatsDClient
            if (isValidDaemon(getDescriptor().getDaemonHost())) {
                logger.fine(String.format("Sending 'completed' counter to %s ", getDescriptor().getDaemonHost()));
                StatsDClient statsd = null;
                try {
                    // The client is a threadpool so instead of creating a new instance of the pool
                    // we lease the exiting one registerd with Jenkins.
                    statsd = getDescriptor().leaseClient();
                    statsd.incrementCounter("completed", statsdTags);
                    logger.fine(String.format("Attempted to send 'completed' counter with tags: %s", Arrays.toString(statsdTags)));

                    logger.fine("Computing KPI metrics");
                    // Send KPIs
                    if (run.getResult() == Result.SUCCESS) {
                        long mttr = getMeanTimeToRecovery(run);
                        long cycleTime = getCycleTime(run);
                        long leadTime = run.getDuration() + mttr;

                        statsd.gauge("leadtime", leadTime / THOUSAND_DOUBLE, statsdTags);
                        if (cycleTime > 0) {
                            statsd.gauge("cycletime", cycleTime / THOUSAND_DOUBLE, statsdTags);
                        }
                        if (mttr > 0) {
                            statsd.gauge("mttr", mttr / THOUSAND_DOUBLE, statsdTags);
                        }
                    } else {
                        long feedbackTime = run.getDuration();
                        long mtbf = getMeanTimeBetweenFailure(run);

                        statsd.gauge("feedbacktime", feedbackTime / THOUSAND_DOUBLE, statsdTags);

                        if (mtbf > 0) {
                            statsd.gauge("mtbf", mtbf / THOUSAND_DOUBLE, statsdTags);
                        }
                    }

                } catch (StatsDClientException e) {
                    logger.severe(String.format("Runtime exception thrown using the StatsDClient. Exception: %s", e.getMessage()));
                } finally {
                    if (statsd != null) {
                        try {
                            // StatsDClient needs time to do its' thing. UDP messages fail to send at all without this sleep
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException ex) {
                            logger.severe(ex.getMessage());
                        }
                        statsd.stop();
                    }
                }
            } else {
                logger.warning("Invalid dogstats daemon host specificed");
            }
            logger.fine("Finished onCompleted()");
        }
    }

    /**
     * @param daemonHost - The host to check
     * @return - A boolean that checks if the daemonHost is valid
     */
    private boolean isValidDaemon(final String daemonHost) {
        if (!daemonHost.contains(":")) {
            logger.info("Daemon host does not contain the port seperator ':'");
            return false;
        }

        String hn = daemonHost.split(":")[0];
        String pn = daemonHost.split(":").length > 1 ? daemonHost.split(":")[1] : "";

        if (StringUtils.isBlank(hn)) {
            logger.info("Daemon host part is empty");
            return false;
        }

        //Match ports [1024-65535]
        Pattern p = Pattern.compile("^(102[4-9]|10[3-9]\\d|1[1-9]\\d{2}|[2-9]\\d{3}|[1-5]\\d{4}|6[0-4]"
                + "\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$");

        boolean match = p.matcher(pn).find();

        if (!match) {
            logger.info("Port number is invalid must be in the range [1024-65535]");
        }

        return match;
    }

    private long getMeanTimeBetweenFailure(Run<?, ?> run) {
        Run<?, ?> lastGreenRun = run.getPreviousNotFailedBuild();
        if (lastGreenRun != null) {
            return run.getStartTimeInMillis() - lastGreenRun.getStartTimeInMillis();
        }
        return 0;
    }

    private long getCycleTime(Run<?, ?> run) {
        Run<?, ?> previousSuccessfulBuild = run.getPreviousSuccessfulBuild();
        if (previousSuccessfulBuild != null) {
            return (run.getStartTimeInMillis() + run.getDuration()) -
                    (previousSuccessfulBuild.getStartTimeInMillis() + previousSuccessfulBuild.getDuration());
        }
        return 0;
    }

    private long getMeanTimeToRecovery(Run<?, ?> run) {
        if (buildFailed(run.getPreviousBuiltBuild())) {
            Run<?, ?> firstFailedRun = run.getPreviousBuiltBuild();

            while (buildFailed(firstFailedRun.getPreviousBuiltBuild())) {
                firstFailedRun = firstFailedRun.getPreviousBuiltBuild();
            }

            return run.getStartTimeInMillis() - firstFailedRun.getStartTimeInMillis();
        }
        return 0;
    }

    private boolean buildFailed(Run<?, ?> run) {
        return run != null && run.getResult() != Result.SUCCESS;
    }

    /**
     * Gathers build metadata, assembling it into a {@link JSONObject} before
     * returning it to the caller.
     *
     * @param run      - A Run object representing a particular execution of Job.
     * @param listener - A TaskListener object which receives events that happen during some
     *                 operation.
     * @return a JSONObject containing a builds metadata.
     */
    private JSONObject gatherBuildMetadata(final Run run, @Nonnull final TaskListener listener) {
        // Assemble JSON
        long startTime = run.getStartTimeInMillis() / DatadogBuildListener.THOUSAND_LONG; // ms to s
        double duration = duration(run);
        long endTime = startTime + (long) duration; // ms to s
        JSONObject builddata = new JSONObject();
        String jobName = run.getParent().getFullName();
        builddata.put("starttime", startTime); // long
        builddata.put("duration", duration); // double
        builddata.put("timestamp", endTime); // long
        builddata.put("result", run.getResult().toString()); // string
        builddata.put("number", run.number); // int
        builddata.put("job", DatadogUtilities.normalizeFullDisplayName(jobName)); // string

        // Grab environment variables
        try {
            EnvVars envVars = run.getEnvironment(listener);
            builddata.put("hostname", DatadogUtilities.getHostname(envVars)); // string
            builddata.put("buildurl", envVars.get("BUILD_URL")); // string
            builddata.put("node", envVars.get("NODE_NAME")); // string
            if (envVars.get("GIT_BRANCH") != null) {
                builddata.put("branch", envVars.get("GIT_BRANCH")); // string
            } else if (envVars.get("CVS_BRANCH") != null) {
                builddata.put("branch", envVars.get("CVS_BRANCH")); // string
            }
        } catch (IOException | InterruptedException e) {
            logger.severe(e.getMessage());
        }

        return builddata;
    }

    /**
     * Returns the duration of the run. For pipeline jobs, {@link Run#getDuration()} always returns 0,
     * in this case this method will calculate the duration of the run by using the current time as the
     * end time.
     *
     * @param run - A Run object representing a particular execution of Job.
     * @return the duration of the run
     */
    private double duration(final Run run) {
        if (run.getDuration() != 0) {
            return run.getDuration() / DatadogBuildListener.THOUSAND_DOUBLE; // ms to s
        } else {
            long durationMillis = System.currentTimeMillis() - run.getStartTimeInMillis();
            return durationMillis / DatadogBuildListener.THOUSAND_DOUBLE; // ms to s
        }
    }

    /**
     * Getter function for the {@link DescriptorImpl} class.
     *
     * @return a new {@link DescriptorImpl} class.
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }

    /**
     * Descriptor for {@link DatadogBuildListener}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * See <tt>DatadogBuildListener/*.jelly</tt> for the actual HTML fragment
     * for the configuration screen.
     */
    @Extension // Indicates to Jenkins that this is an extension point implementation.
    public static class DescriptorImpl extends Descriptor<DatadogBuildListener> {

        /**
         * @return - A {@link StatsDClient} lease for this registered {@link RunListener}
         */
        public StatsDClient leaseClient() {
            try {
                if (client == null) {
                    client = new NonBlockingStatsDClient("jenkins.job", daemonHost.split(":")[0],
                            Integer.parseInt(daemonHost.split(":")[1]));
                } else {
                    logger.warning("StatsDClient is null");
                }
            } catch (Exception e) {
                logger.severe(String.format("Error while configuring StatsDClient. Exception: %s", e.toString()));
            }
            return client;
        }

        /**
         * Persist global configuration information by storing in a field and
         * calling save().
         */
        private Secret apiKey = null;
        private String hostname = null;
        private String blacklist = null;
        private String whitelist = null;
        private String globalJobTags = null;
        private Boolean tagNode = false;
        private String daemonHost = "localhost:8125";
        private String targetMetricURL = "https://api.datadoghq.com/api/";

        //The StatsDClient instance variable. This variable is leased by the RunLIstener
        private StatsDClient client;

        /**
         * Runs when the {@link DescriptorImpl} class is created.
         */
        public DescriptorImpl() {
            load(); // load the persisted global configuration
        }

        @Override
        public DatadogBuildListener newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData); //To change body of generated methods, choose Tools | Templates.
        }

        /**
         * Tests the apiKey field from the configuration screen, to check its' validity.
         * It is used in the global.jelly resource file. See method="testConnection"
         *
         * @param formApiKey - A String containing the apiKey submitted from the form on the
         *                   configuration screen, which will be used to authenticate a request to the
         *                   Datadog API.
         * @return a FormValidation object used to display a message to the user on the configuration
         * screen.
         * @throws IOException      if there is an input/output exception.
         * @throws ServletException if there is a servlet exception.
         */
        public FormValidation doTestConnection(@QueryParameter("apiKey") final String formApiKey)
                throws IOException, ServletException {
            String urlParameters = "?api_key=" + Secret.fromString(formApiKey);
            String targetMetricURL = this.getTargetMetricURL();
            boolean status = DatadogHttpRequests.validate(targetMetricURL, urlParameters);

            if (status) {
                return FormValidation.ok("Great! Your API key is valid.");
            } else {
                return FormValidation.error("Hmmm, your API key seems to be invalid.");
            }
        }

        /**
         * Tests the hostname field from the configuration screen, to determine if
         * the hostname is of a valid format, according to the RFC 1123.
         * It is used in the global.jelly resource file. See method="testHostname"
         *
         * @param formHostname - A String containing the hostname submitted from the form on the
         *                     configuration screen, which will be used to authenticate a request to the
         *                     Datadog API.
         * @return a FormValidation object used to display a message to the user on the configuration
         * screen.
         * @throws IOException      if there is an input/output exception.
         * @throws ServletException if there is a servlet exception.
         */
        public FormValidation doTestHostname(@QueryParameter("hostname") final String formHostname)
                throws IOException, ServletException {
            if ((null != formHostname) && DatadogUtilities.isValidHostname(formHostname)) {
                return FormValidation.ok("Great! Your hostname is valid.");
            } else {
                return FormValidation.error("Your hostname is invalid, likely because"
                        + " it violates the format set in RFC 1123.");
            }
        }

        /**
         * @param daemonHost - The hostname for the dogstatsdaemon. Defaults to localhost:8125
         * @return a FormValidation object used to display a message to the user on the configuration
         * screen.
         */
        public FormValidation doCheckDaemonHost(@QueryParameter("daemonHost") final String daemonHost) {
            if (!daemonHost.contains(":")) {
                return FormValidation.error("The field must be configured in the form <hostname>:<port>");
            }

            String hn = daemonHost.split(":")[0];
            String pn = daemonHost.split(":").length > 1 ? daemonHost.split(":")[1] : "";

            if (StringUtils.isBlank(hn)) {
                return FormValidation.error("Empty hostname");
            }

            //Match ports [1024-65535]
            Pattern p = Pattern.compile("^(102[4-9]|10[3-9]\\d|1[1-9]\\d{2}|[2-9]\\d{3}|[1-5]\\d{4}|6[0-4]"
                    + "\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$");
            if (!p.matcher(pn).find()) {
                return FormValidation.error("Invalid port specified. Range must be 1024-65535");
            }

            return FormValidation.ok("Valid host specification");
        }

        /**
         * @param targetMetricURL - The API URL which the plugin will report to.
         * @return a FormValidation object used to display a message to the user on the configuration
         * screen.
         */
        public FormValidation doCheckTargetMetricURL(@QueryParameter("targetMetricURL") final String targetMetricURL) {
            if (!targetMetricURL.contains("http")) {
                return FormValidation.error("The field must be configured in the form <http|https>://<url>/");
            }

            if (StringUtils.isBlank(targetMetricURL)) {
                return FormValidation.error("Empty API URL");
            }

            return FormValidation.ok("Valid URL");
        }

        /**
         * Indicates if this builder can be used with all kinds of project types.
         *
         * @param aClass - An extension of the AbstractProject class representing a specific type of
         *               project.
         * @return a boolean signifying whether or not a builder can be used with a specific type of
         * project.
         */
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * Getter function for a human readable plugin name, used in the configuration screen.
         *
         * @return a String containing the human readable display name for this plugin.
         */
        @Override
        public String getDisplayName() {
            return DatadogBuildListener.DISPLAY_NAME;
        }

        /**
         * Indicates if this builder can be used with all kinds of project types.
         *
         * @param req      - A StaplerRequest object
         * @param formData - A JSONObject containing the submitted form data from the configuration
         *                 screen.
         * @return a boolean signifying the success or failure of configuration.
         * @throws FormException if the formData is invalid.
         */
        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            // Grab apiKey and hostname
            this.setApiKey(formData.getString("apiKey"));
            this.setHostname(formData.getString("hostname"));

            // Grab blacklist
            this.setBlacklist(formData.getString("blacklist"));

            // Grab whitelist
            this.setWhitelist(formData.getString("whitelist"));

            // Grab the Global Job Tags
            this.setGlobalJobTags(formData.getString("globalJobTags"));

            // Grab tagNode and coerse to a boolean
            if (formData.getString("tagNode").equals("true")) {
                this.setTagNode(true);
            } else {
                this.setTagNode(false);
            }

            daemonHost = formData.getString("daemonHost");
            //When form is saved...reinitialize the StatsDClient.
            //We need to stop the old one first. And create a new one with the new data from
            //The global configuration
            if (client != null) {
                try {
                    client.stop();
                    String hp = daemonHost.split(":")[0];
                    int pp = Integer.parseInt(daemonHost.split(":")[1]);
                    client = new NonBlockingStatsDClient("jenkins.job", hp, pp);
                    logger.finer(String.format("Created new DogStatsD client (%s:%S)!", hp, pp));
                } catch (Exception e) {
                    logger.severe(String.format("Unable to create new StatsDClient. Exception: %s", e.toString()));
                }
            }

            // Grab API URL
            targetMetricURL = formData.getString("targetMetricURL");

            // Persist global configuration information
            save();
            return super.configure(req, formData);
        }

        /**
         * Getter function for the apiKey global configuration.
         *
         * @return a Secret containing the apiKey global configuration.
         */
        public Secret getApiKey() {
            return apiKey;
        }

        /**
         * Setter function for the apiKey global configuration.
         *
         * @param key = A string containing the plaintext representation of a
         *            DataDog API Key
         */
        public void setApiKey(final String key) {
            this.apiKey = Secret.fromString(fixEmptyAndTrim(key));
        }

        /**
         * Getter function for the hostname global configuration.
         *
         * @return a String containing the hostname global configuration.
         */
        public String getHostname() {
            return hostname;
        }

        /**
         * Setter function for the hostname global configuration.
         *
         * @param hostname - A String containing the hostname of the Jenkins host.
         */
        public void setHostname(final String hostname) {
            this.hostname = hostname;
        }

        /**
         * Getter function for the blacklist global configuration, containing
         * a comma-separated list of jobs to blacklist from monitoring.
         *
         * @return a String array containing the blacklist global configuration.
         */
        public String getBlacklist() {
            return blacklist;
        }

        /**
         * Setter function for the blacklist global configuration,
         * accepting a comma-separated string of jobs.
         *
         * @param jobs - a comma-separated list of jobs to blacklist from monitoring.
         */
        public void setBlacklist(final String jobs) {
            this.blacklist = jobs;
        }

        /**
         * Getter function for the whitelist global configuration, containing
         * a comma-separated list of jobs to whitelist from monitoring.
         *
         * @return a String array containing the whitelist global configuration.
         */
        public String getWhitelist() {
            return whitelist;
        }

        /**
         * Setter function for the whitelist global configuration,
         * accepting a comma-separated string of jobs.
         *
         * @param jobs - a comma-separated list of jobs to whitelist from monitoring.
         */
        public void setWhitelist(final String jobs) {
            this.whitelist = jobs;
        }

        /**
         * Getter function for the globalJobTags global configuration, containing
         * a comma-separated list of jobs and tags that should be applied to them
         *
         * @return a String array containing the globalJobTags global configuration.
         */
        public String getGlobalJobTags() {
            return globalJobTags;
        }

        /**
         * Setter function for the globalJobTags global configuration,
         * accepting a comma-separated string of jobs and tags.
         *
         * @param globalJobTags - a comma-separated list of jobs to whitelist from monitoring.
         */
        public void setGlobalJobTags(String globalJobTags) {
            this.globalJobTags = globalJobTags;
        }

        /**
         * Getter function for the optional tag tagNode global configuration.
         *
         * @return a Boolean containing optional tag value for the tagNode
         * global configuration.
         */
        public Boolean getTagNode() {
            return tagNode;
        }

        /**
         * Setter function for the optional tag tagNode global configuration.
         *
         * @param willTag - A Boolean expressing whether the tagNode tag will
         *                be included.
         */
        public void setTagNode(final Boolean willTag) {
            this.tagNode = willTag;
        }

        /**
         * @return The host definition for the dogstats daemon
         */
        public String getDaemonHost() {
            return daemonHost;
        }

        /**
         * @param daemonHost - The host specification for the dogstats daemon
         */
        public void setDaemonHost(String daemonHost) {
            this.daemonHost = daemonHost;
        }

        /**
         * @return The target API URL
         */
        public String getTargetMetricURL() {
            return targetMetricURL;
        }

        /**
         * @param targetMetricURL - The target API URL
         */
        public void setTargetMetricURL(String targetMetricURL) {
            this.targetMetricURL = targetMetricURL;
        }
    }
}
