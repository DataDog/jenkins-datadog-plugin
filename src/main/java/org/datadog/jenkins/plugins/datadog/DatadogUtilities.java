package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.ExtensionList;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import jenkins.model.Jenkins;
import org.datadog.jenkins.plugins.datadog.clients.DatadogHttpClient;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

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
     * @return - The descriptor for the Datadog plugin. In this case the global configuration.
     */
    public static DatadogGlobalConfiguration getDatadogGlobalDescriptor() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return null;
        }
        return ExtensionList.lookup(DatadogGlobalConfiguration.class).get(DatadogGlobalConfiguration.class);
    }

    /**
     * @return - The descriptor for the Datadog plugin. In this case the global configuration.
     */
    public static DatadogClient getDatadogClient() {
        DatadogGlobalConfiguration descriptor = getDatadogGlobalDescriptor();
        return DatadogHttpClient.getInstance(descriptor.getTargetMetricURL(), descriptor.getApiKey());
    }

    public static Map<String,Set<String>> getGlobalTags() {
        return getDatadogGlobalDescriptor().getGlobalTags();
    }

    /**
     * Builds extraTags if any are configured in the Job.
     *
     * @param run      - Current build
     * @param listener - Current listener
     * @return A {@link HashMap} containing the key,value pairs of tags if any.
     */
    public static Map<String, Set<String>> getBuildTags(Run run, @Nonnull TaskListener listener) {
        Map<String, Set<String>> result = new HashMap<>();
        String jobName = run.getParent().getFullName();
        final String globalJobTags = getDatadogGlobalDescriptor().getGlobalJobTags();
        final DatadogJobProperty property = DatadogJobProperty.retrieveProperty(run);
        final String workspaceTagFile = property.readTagFile(run);
        try {
            final EnvVars envVars = run.getEnvironment(listener);
            if (!property.isTagFileEmpty()) {
                if (workspaceTagFile != null) {
                    result = TagsUtil.merge(result, computeTagListFromVarList(envVars, workspaceTagFile));
                }
            }

            String prop = property.getTagProperties();
            if (!property.isTagPropertiesEmpty()) {
                result = TagsUtil.merge(result, computeTagListFromVarList(envVars, prop));
            }
        } catch (IOException | InterruptedException ex) {
            logger.severe(ex.getMessage());
        }

        result = TagsUtil.merge(result, getTagsFromGlobalJobTags(jobName, globalJobTags));
        return result;
    }

    /**
     * Checks if a jobName is blacklisted, whitelisted, or neither.
     *
     * @param jobName - A String containing the name of some job.
     * @return a boolean to signify if the jobName is or is not blacklisted or whitelisted.
     */
    public static boolean isJobTracked(final String jobName) {
        return !isJobBlacklisted(jobName) && isJobWhitelisted(jobName);
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
     * Retrieve the list of tags from the globalJobTagsLines param for jobName
     *
     * @param jobName - JobName to retrieve and process tags from.
     * @param globalJobTags - globalJobTags string
     * @return - A Map of values containing the key and values of each Datadog tag to apply to the metric/event
     */
    private static Map<String, Set<String>> getTagsFromGlobalJobTags(String jobName, final String globalJobTags) {
        Map<String, Set<String>> tags = new HashMap<>();
        List<String> globalJobTagsLines = linesToList(globalJobTags);
        logger.fine(String.format("The list of Global Job Tags are: %s", globalJobTagsLines));

        // Each jobInfo is a list containing one regex, and a variable number of tags
        for (String globalTagsLine : globalJobTagsLines) {
            List<String> jobInfo = cstrToList(globalTagsLine);
            if (jobInfo.isEmpty()) {
                continue;
            }
            Pattern jobNamePattern = Pattern.compile(jobInfo.get(0));
            Matcher jobNameMatcher = jobNamePattern.matcher(jobName);
            if (jobNameMatcher.matches()) {
                for (int i = 1; i < jobInfo.size(); i++) {
                    String[] tagItem = jobInfo.get(i).split(":");
                    if (tagItem.length == 2) {
                        String tagName = tagItem[0];
                        String tagValue = tagItem[1];
                        // Fills regex group values from the regex job name to tag values
                        // eg: (.*?)-job, owner:$1
                        if (Character.toString(tagValue.charAt(0)).equals("$")) {
                            try {
                                tagValue = jobNameMatcher.group(Character.getNumericValue(tagValue.charAt(1)));
                            } catch (IndexOutOfBoundsException e) {
                                logger.fine(String.format(
                                        "Specified a capture group that doesn't exist, not applying tag: %s Exception: %s",
                                        Arrays.toString(tagItem), e));
                            }
                        }
                        Set<String> tagValues = tags.containsKey(tagName) ? tags.get(tagName) : new HashSet<String>();
                        tagValues.add(tagValue.toLowerCase());
                        tags.put(tagName, tagValues);
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
        final String blacklistProp = getDatadogGlobalDescriptor().getBlacklist();
        List<String> blacklist = cstrToList(blacklistProp);
        for (String blacklistedJob : blacklist){
            Pattern blacklistedJobPattern = Pattern.compile(blacklistedJob);
            Matcher jobNameMatcher = blacklistedJobPattern.matcher(jobName);
            if (jobNameMatcher.matches()) {
                return true;
            }
        }
        return false;

    }

    /**
     * Checks if a jobName is whitelisted.
     *
     * @param jobName - A String containing the name of some job.
     * @return a boolean to signify if the jobName is or is not whitelisted.
     */
    private static boolean isJobWhitelisted(final String jobName) {
        final String whitelistProp = getDatadogGlobalDescriptor().getWhitelist();
        final List<String> whitelist = cstrToList(whitelistProp);
        for (String whitelistedJob : whitelist){
            Pattern whitelistedJobPattern = Pattern.compile(whitelistedJob);
            Matcher jobNameMatcher = whitelistedJobPattern.matcher(jobName);
            if (jobNameMatcher.matches()) {
                return true;
            }
        }
        return whitelist.isEmpty();
    }

    /**
     * Converts a Comma Separated List into a List Object
     *
     * @param str - A String containing a comma separated list of items.
     * @return a String List with all items transform with trim and lower case
     */
    public static List<String> cstrToList(final String str) {
        return convertRegexStringToList(str, ",");
    }

    /**
     * Converts a string List into a List Object
     *
     * @param str - A String containing a comma separated list of items.
     * @return a String List with all items
     */
    public static List<String> linesToList(final String str) {
        return convertRegexStringToList(str, "\\r?\\n");
    }

    /**
     * Converts a string List into a List Object
     *
     * @param str - A String containing a comma separated list of items.
     * @param regex - Regex to use to split the string list
     * @return a String List with all items
     */
    private static List<String> convertRegexStringToList(final String str, String regex) {
        List<String> result = new ArrayList<>();
        if (str != null && str.length() != 0) {
            for (String item : str.trim().split(regex)) {
                if (!item.isEmpty()) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }

    public static Map<String, Set<String>> computeTagListFromVarList(EnvVars envVars, final String varList) {
        HashMap<String, Set<String>> result = new HashMap<>();
        List<String> rawTagList = linesToList(varList);
        for (String tag : rawTagList) {
            String[] expanded = envVars.expand(tag).split("=");
            if (expanded.length > 1) {
                String name = expanded[0];
                String value = expanded[1];
                Set<String> values = result.containsKey(name) ? result.get(name) : new HashSet<String>();
                values.add(value);
                result.put(name, values);
                logger.fine(String.format("Emitted tag %s:%s", expanded[0], expanded[1]));
            } else {
                logger.fine(String.format("Ignoring the tag %s. It is empty.", tag));
            }
        }
        return result;
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
        String hostname = null;
        try {
            hostname = getDatadogGlobalDescriptor().getHostname();
        } catch (NullPointerException e){
            // noop
        }
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
    public static Boolean isValidHostname(String hostname) {
        if (hostname == null) {
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

    public static Map<String, Set<String>> getComputerTags(Computer computer) {
        Set<LabelAtom> labels = null;
        try {
            labels = computer.getNode().getAssignedLabels();
        } catch (NullPointerException e){
            logger.fine("Could not retrieve labels");
        }
        String nodeHostname = null;
        try {
            nodeHostname = computer.getHostName();
        } catch (IOException | InterruptedException e) {
            logger.fine("Could not retrieve hostname");
        }
        String nodeName = getNodeName(computer);
        Map<String, Set<String>> result = new HashMap<>();
        Set<String> nodeNameValues = new HashSet<>();
        nodeNameValues.add(nodeName);
        result.put("node_name", nodeNameValues);
        if(nodeHostname != null){
            Set<String> nodeHostnameValues = new HashSet<>();
            nodeHostnameValues.add(nodeHostname);
            result.put("node_hostname", nodeHostnameValues);
        }
        if(labels != null){
            Set<String> nodeLabelsValues = new HashSet<>();
            for (LabelAtom label: labels){
                nodeLabelsValues.add(label.getName());
            }
            result.put("node_label", nodeLabelsValues);
        }

        return result;
    }

    public static String getNodeName(Computer computer){
        if (computer instanceof Jenkins.MasterComputer) {
            return "master";
        } else {
            return computer.getName();
        }
    }

    public static String getUserId() {
        User user = User.current();
        if (user == null) {
            return "anonymous";
        } else {
            return user.getId();
        }
    }

    public static String getItemName(Item item) {
        if (item == null) {
            return "unknown";
        }
        return item.getName();
    }

    public static Long getRunStartTimeInMillis(Run run) {
        // getStartTimeInMillis wrapper in order to mock it in unit tests
        return run.getStartTimeInMillis();
    }

    public static long currentTimeMillis(){
        // This method exist so we can mock System.currentTimeMillis in unit tests
        return System.currentTimeMillis();
    }
}
