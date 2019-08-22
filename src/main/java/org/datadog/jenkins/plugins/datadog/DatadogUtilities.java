package org.datadog.jenkins.plugins.datadog;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatadogUtilities {

    private static final Logger logger = Logger.getLogger(DatadogUtilities.class.getName());

    private static final Integer MAX_HOSTNAME_LEN = 255;

    /**
     * @return - The descriptor for the Datadog plugin. In this case the global
     * - configuration.
     */
    public static DatadogBuildListener.DescriptorImpl getDatadogDescriptor() {
        return (DatadogBuildListener.DescriptorImpl) Jenkins.getInstance().
                getDescriptorOrDie(DatadogBuildListener.class);
    }

    /**
     * Check if apiKey is null
     *
     * @return boolean - apiKey is null
     */
    public static boolean isApiKeyNull() {
        return Secret.toString(DatadogUtilities.getDatadogDescriptor().getApiKey()).isEmpty();
    }

    /**
     * Builds extraTags if any are configured in the Job.
     *
     * @param run      - Current build
     * @param listener - Current listener
     * @return A {@link HashMap} containing the key,value pairs of tags if any.
     */
    public static HashMap<String, String> buildExtraTags(Run run, TaskListener listener) {
        String jobName = run.getParent().getFullName();
        HashMap<String, String> extraTags = new HashMap<>();
        try {
            extraTags = DatadogUtilities.parseTagList(run, listener);
        } catch (IOException | InterruptedException ex) {
            logger.severe(ex.getMessage());
        }
        extraTags.putAll(DatadogUtilities.getRegexJobTags(jobName));
        return extraTags;
    }

    /**
     * Checks if a jobName is blacklisted, whitelisted, or neither.
     *
     * @param jobName - A String containing the name of some job.
     * @return a boolean to signify if the jobName is or is not blacklisted or whitelisted.
     */
    public static boolean isJobTracked(final String jobName) {
        return !DatadogUtilities.isJobBlacklisted(jobName) && DatadogUtilities.isJobWhitelisted(jobName);
    }

    /**
     * Human-friendly OS name. Commons return values are windows, linux, mac, sunos, freebsd
     *
     * @return a String with a human-friendly OS name
     */
    private static String getOS() {
        String out = System.getProperty("os.name");
        String os = out.split(" ")[0];
        return os.toLowerCase();
    }

    /**
     * Retrieve the list of tags from the Config file if regex Jobs was checked
     *
     * @param jobName - A string containing the name of some job
     * @return - A Map of values containing the key and value of each Datadog tag to apply to the metric/event
     */
    private static Map<String, String> getRegexJobTags(String jobName) {
        Map<String, String> tags = new HashMap<>();
        final List<List<String>> globalTags = DatadogUtilities.regexJoblistStringtoList(
                DatadogUtilities.getDatadogDescriptor().getGlobalJobTags());

        logger.fine(String.format("The list of Global Job Tags are: %s", globalTags));

        // Each jobInfo is a list containing one regex, and a variable number of tags
        for (List<String> jobInfo : globalTags) {

            if (jobInfo.isEmpty()) {
                continue;
            }

            Pattern p = Pattern.compile(jobInfo.get(0));
            Matcher m = p.matcher(jobName);
            if (m.matches()) {
                for (int i = 1; i < jobInfo.size(); i++) {
                    String[] tagItem = jobInfo.get(i).split(":");
                    if (Character.toString(tagItem[1].charAt(0)).equals("$")) {
                        try {
                            tags.put(tagItem[0], m.group(Character.getNumericValue(tagItem[1].charAt(1))));
                        } catch (IndexOutOfBoundsException e) {
                            logger.fine(String.format(
                                    "Specified a capture group that doesn't exist, not applying tag: %s Exception: %s",
                                    Arrays.toString(tagItem), e));
                        }
                    } else {
                        tags.put(tagItem[0], tagItem[1]);
                    }
                }
            }
        }

        return tags;
    }

    /**
     * Checks if a jobName is blacklisted.
     *
     * @param jobName - A String containing the name of some job.
     * @return a boolean to signify if the jobName is or is not blacklisted.
     */
    private static boolean isJobBlacklisted(final String jobName) {
        final List<String> blacklist = DatadogUtilities.joblistStringtoList(
                DatadogUtilities.getDatadogDescriptor().getBlacklist());
        return blacklist.contains(jobName.toLowerCase());
    }

    /**
     * Checks if a jobName is whitelisted.
     *
     * @param jobName - A String containing the name of some job.
     * @return a boolean to signify if the jobName is or is not whitelisted.
     */
    private static boolean isJobWhitelisted(final String jobName) {
        final List<String> whitelist = DatadogUtilities.joblistStringtoList(
                DatadogUtilities.getDatadogDescriptor().getWhitelist());

        // Check if the user config is using regexes
        return whitelist.isEmpty() || whitelist.contains(jobName.toLowerCase());
    }

    /**
     * Converts a blacklist/whitelist string into a String array.
     *
     * @param joblist - A String containing a set of job names.
     * @return a String array representing the job names to be whitelisted/blacklisted. Returns
     * empty string if blacklist is null.
     */
    private static List<String> joblistStringtoList(final String joblist) {
        List<String> jobs = new ArrayList<>();
        if (joblist != null) {
            for (String job : joblist.trim().split(",")) {
                if (!job.isEmpty()) {
                    jobs.add(job.trim().toLowerCase());
                }
            }
        }
        return jobs;
    }

    /**
     * Converts a blacklist/whitelist string into a String array.
     * This is the implementation for when the Use Regex checkbox is enabled
     *
     * @param joblist - A String containing a set of job name regexes and tags.
     * @return a String List representing the job names to be whitelisted/blacklisted and its associated tags.
     * Returns empty string if blacklist is null.
     */
    private static List<List<String>> regexJoblistStringtoList(final String joblist) {
        List<List<String>> jobs = new ArrayList<>();
        if (joblist != null && joblist.length() != 0) {
            for (String job : joblist.split("\\r?\\n")) {
                List<String> jobAndTags = new ArrayList<>();
                for (String item : job.split(",")) {
                    if (!item.isEmpty()) {
                        jobAndTags.add(item);
                    }
                }
                jobs.add(jobAndTags);
            }
        }
        return jobs;
    }

    /**
     * This method parses the contents of the configured Datadog tags. If they are present.
     * Takes the current build as a parameter. And returns the expanded tags and their
     * values in a HashMap.
     *
     * Always returns a HashMap, that can be empty, if no tagging is configured.
     *
     * @param run      - Current build
     * @param listener - Current listener
     * @return A {@link HashMap} containing the key,value pairs of tags. Never null.
     * @throws IOException          if an error occurs when reading from any objects
     * @throws InterruptedException if an interrupt error occurs
     */
    @Nonnull
    private static HashMap<String, String> parseTagList(Run run, TaskListener listener) throws IOException,
            InterruptedException {
        HashMap<String, String> map = new HashMap<>();

        DatadogJobProperty property = DatadogUtilities.retrieveProperty(run);

        // If Null, nothing to retrieve
        if (property == null) {
            return map;
        }

        String prop = property.getTagProperties();

        if (!property.isTagFileEmpty()) {
            String dataFromFile = property.readTagFile(run);
            if (dataFromFile != null) {
                for (String tag : dataFromFile.split("\\r?\\n")) {
                    String[] expanded = run.getEnvironment(listener).expand(tag).split("=");
                    if (expanded.length > 1) {
                        map.put(expanded[0], expanded[1]);
                        logger.fine(String.format("Emitted tag %s:%s", expanded[0], expanded[1]));
                    } else {
                        logger.fine(String.format("Ignoring the tag %s. It is empty.", tag));
                    }
                }
            }
        }

        if (!property.isTagPropertiesEmpty()) {
            for (String tag : prop.split("\\r?\\n")) {
                String[] expanded = run.getEnvironment(listener).expand(tag).split("=");
                if (expanded.length > 1) {
                    map.put(expanded[0], expanded[1]);
                    logger.fine(String.format("Emitted tag %s:%s", expanded[0], expanded[1]));
                } else {
                    logger.fine(String.format("Ignoring the tag %s. It is empty.", tag));
                }
            }
        }

        return map;
    }

    /**
     * @param r - Current build.
     * @return - The configured {@link DatadogJobProperty}. Null if not there
     */
    @CheckForNull
    public static DatadogJobProperty retrieveProperty(Run r) {
        return  (DatadogJobProperty)r.getParent().getProperty(DatadogJobProperty.class);
    }

    /**
     * Getter function to return either the saved hostname global configuration,
     * or the hostname that is set in the Jenkins host itself. Returns null if no
     * valid hostname is found.
     *
     * Tries, in order:
     * Jenkins configuration
     * Jenkins hostname environment variable
     * Unix hostname via `/bin/hostname -f`
     * Localhost hostname
     *
     * @param envVarHostname - The Jenkins hostname environment variable
     * @return a human readable String for the hostname.
     */
    public static String getHostname(String envVarHostname) {
        String[] UNIX_OS = {"mac", "linux", "freebsd", "sunos"};

        // Check hostname configuration from Jenkins
        String hostname = DatadogUtilities.getDatadogDescriptor().getHostname();
        if (isValidHostname(hostname)) {
            logger.fine("Using hostname set in 'Manage Plugins'. Hostname: " + hostname);
            return hostname;
        }

        // Check hostname using jenkins env variables
        if (envVarHostname != null) {
            hostname = envVarHostname;
        }
        if (isValidHostname(hostname)) {
            logger.fine("Using hostname found in $HOSTNAME host environment variable. Hostname: " + hostname);
            return hostname;
        }

        // Check OS specific unix commands
        String os = getOS();
        if (Arrays.asList(UNIX_OS).contains(os)) {
            // Attempt to grab unix hostname
            try {
                String[] cmd = {"/bin/hostname", "-f"};
                Process proc = Runtime.getRuntime().exec(cmd);
                InputStream in = proc.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder out = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
                reader.close();

                hostname = out.toString();
            } catch (Exception e) {
                logger.severe(e.getMessage());
            }

            // Check hostname
            if (isValidHostname(hostname)) {
                logger.fine(String.format("Using unix hostname found via `/bin/hostname -f`. Hostname: %s",
                        hostname));
                return hostname;
            }
        }

        // Check localhost hostname
        try {
            hostname = Inet4Address.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.fine(String.format("Unknown hostname error received for localhost. Error: %s", e));
        }
        if (isValidHostname(hostname)) {
            logger.fine(String.format("Using hostname found via "
                    + "Inet4Address.getLocalHost().getHostName()."
                    + " Hostname: %s", hostname));
            return hostname;
        }

        // Never found the hostname
        if (hostname == null || "".equals(hostname)) {
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
    public static final Boolean isValidHostname(String hostname) {
        if (hostname == null){
            return false;
        }

        String[] localHosts = {"localhost", "localhost.localdomain",
                "localhost6.localdomain6", "ip6-localhost"};
        String VALID_HOSTNAME_RFC_1123_PATTERN = "^(([a-zA-Z0-9]|"
                + "[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*"
                + "([A-Za-z0-9]|"
                + "[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        String host = hostname.toLowerCase();

        // Check if hostname is local
        if (Arrays.asList(localHosts).contains(host)) {
            logger.fine(String.format("Hostname: %s is local", hostname));
            return false;
        }

        // Ensure proper length
        if (hostname.length() > MAX_HOSTNAME_LEN) {
            logger.fine(String.format("Hostname: %s is too long (max length is %s characters)",
                    hostname, MAX_HOSTNAME_LEN));
            return false;
        }

        // Check compliance with RFC 1123
        Pattern r = Pattern.compile(VALID_HOSTNAME_RFC_1123_PATTERN);
        Matcher m = r.matcher(hostname);

        // Final check: Hostname matches RFC1123?
        return m.find();
    }

}
