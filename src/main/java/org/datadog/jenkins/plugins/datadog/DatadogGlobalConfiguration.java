package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static hudson.Util.fixEmptyAndTrim;

@Extension
public class DatadogGlobalConfiguration extends GlobalConfiguration {

    private static final Logger logger = Logger.getLogger(DatadogGlobalConfiguration.class.getName());
    private static final String DISPLAY_NAME = "Datadog Plugin";

    private Secret apiKey = null;
    private String hostname = null;
    private String blacklist = null;
    private String whitelist = null;
    private String globalTagFile = null;
    private String globalTags = null;
    private String globalJobTags = null;
    private String targetMetricURL = "https://api.datadoghq.com/api/";
    private boolean emitSecurityEvents = true;
    private boolean emitSystemEvents = true;

    @DataBoundConstructor
    public DatadogGlobalConfiguration() {
        load(); // load the persisted global configuration
    }

    /**
     * Tests the apiKey field from the configuration screen, to check its' validity.
     * It is used in the config.jelly resource file. See method="testConnection"
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
        try {
            // Instantiate the Datadog Client
            DatadogClient client = DatadogHttpClient.getInstance(this.getTargetMetricURL(),
                    Secret.fromString(formApiKey));
            boolean status = client.validate();

            if (status) {
                return FormValidation.ok("Great! Your API key is valid.");
            } else {
                return FormValidation.error("Hmmm, your API key seems to be invalid.");
            }
        } catch (RuntimeException e){
            return FormValidation.error("Hmmm, your API key seems to be invalid.");
        }

    }

    /**
     * Tests the hostname field from the configuration screen, to determine if
     * the hostname is of a valid format, according to the RFC 1123.
     * It is used in the config.jelly resource file. See method="testHostname"
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
        if (DatadogUtilities.isValidHostname(formHostname)) {
            return FormValidation.ok("Great! Your hostname is valid.");
        } else {
            return FormValidation.error("Your hostname is invalid, likely because"
                    + " it violates the format set in RFC 1123.");
        }
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
        return DISPLAY_NAME;
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
        try {
            // Grab apiKey and hostname
            this.setApiKey(formData.getString("apiKey"));
            this.setHostname(formData.getString("hostname"));
            this.setBlacklist(formData.getString("blacklist"));
            this.setWhitelist(formData.getString("whitelist"));
            this.setGlobalTagFile(formData.getString("globalTagFile"));
            this.setGlobalTags(formData.getString("globalTags"));
            this.setGlobalJobTags(formData.getString("globalJobTags"));
            this.setTargetMetricURL(formData.getString("targetMetricURL"));
            this.setEmitSecurityEvents(formData.getBoolean("emitSecurityEvents"));
            this.setEmitSystemEvents(formData.getBoolean("emitSystemEvents"));

            //When form is saved...reinitialize the DatadogClient.
            DatadogHttpClient.getInstance(this.getTargetMetricURL(), this.getApiKey());

            // Persist global configuration information
            save();
            return super.configure(req, formData);
        } catch(Exception e){
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
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
    @DataBoundSetter
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
    @DataBoundSetter
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
    @DataBoundSetter
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
    @DataBoundSetter
    public void setWhitelist(final String jobs) {
        this.whitelist = jobs;
    }

    /**
     * Gets the globalTagFile set in the job configuration.
     *
     * @return a String representing the relative path to a globalTagFile
     */
    public String getGlobalTagFile() {
        return globalTagFile;
    }

    /**
     * Setter function for the globalFile global configuration,
     * accepting a comma-separated string of tags.
     *
     * @param globalTagFile - a comma-separated list of tags.
     */
    @DataBoundSetter
    public void setGlobalTagFile(String globalTagFile) {
        this.globalTagFile = globalTagFile;
    }

    /**
     * Getter function for the globalTags global configuration, containing
     * a comma-separated list of tags that should be applied everywhere.
     *
     * @return a String array containing the globalTags global configuration
     */
    public String getGlobalTags() {
        return globalTags;
    }

    /**
     * Setter function for the globalTags global configuration,
     * accepting a comma-separated string of tags.
     *
     * @param globalTags - a comma-separated list of tags.
     */
    @DataBoundSetter
    public void setGlobalTags(String globalTags) {
        this.globalTags = globalTags;
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
    @DataBoundSetter
    public void setGlobalJobTags(String globalJobTags) {
        this.globalJobTags = globalJobTags;
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
    @DataBoundSetter
    public void setTargetMetricURL(String targetMetricURL) {
        this.targetMetricURL = targetMetricURL;
    }

    /**
     * @return - A {@link Boolean} indicating if the user has configured Datadog to emit Security related events.
     */
    public boolean isEmitSecurityEvents() {
        return emitSecurityEvents;
    }

    /**
     * Set the checkbox in the UI, used for Jenkins data binding
     *
     * @param emitSecurityEvents - The checkbox status (checked/unchecked)
     */
    @DataBoundSetter
    public void setEmitSecurityEvents(boolean emitSecurityEvents) {
        this.emitSecurityEvents = emitSecurityEvents;
    }

    /**
     * @return - A {@link Boolean} indicating if the user has configured Datadog to emit System related events.
     */
    public boolean isEmitSystemEvents() {
        return emitSystemEvents;
    }

    /**
     * Set the checkbox in the UI, used for Jenkins data binding
     *
     * @param emitSystemEvents - The checkbox status (checked/unchecked)
     */
    @DataBoundSetter
    public void setEmitSystemEvents(boolean emitSystemEvents) {
        this.emitSystemEvents = emitSystemEvents;
    }

}
