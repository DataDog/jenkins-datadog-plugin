package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import static org.datadog.jenkins.plugins.datadog.DatadogBuildListener.getOS;

public class DataDogUtilities {

  private static final Logger logger =  Logger.getLogger(DataDogSCMListener.class.getName());
  /**
   *
   * @return - The descriptor for the Datadog plugin. In this case the global
   *         - configuration.
   */
  public static DatadogBuildListener.DescriptorImpl getDataDogDescriptor() {
    DatadogBuildListener.DescriptorImpl desc = (DatadogBuildListener.DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(DatadogBuildListener.class);
    return desc;
  }

  public static String getHostName()  {
    return DataDogUtilities.getDataDogDescriptor().getHostname();
  }

  public static  String getApiKey() {
    return DataDogUtilities.getDataDogDescriptor().getApiKey();
  }

  public static String getBlacklist() {
    return DataDogUtilities.getDataDogDescriptor().getBlacklist();
  }

  /**
   * Checks if a jobName is blacklisted, or not.
   *
   * @param jobName - A String containing the name of some job.
   * @return a boolean to signify if the jobName is or is not blacklisted.
   */
  public static boolean isJobTracked(final String jobName) {
    final String[] blacklist = DataDogUtilities.blacklistStringtoArray( DataDogUtilities.getBlacklist() );
    return (blacklist == null) || !Arrays.asList(blacklist).contains(jobName.toLowerCase());
  }

  /**
   * Converts a blacklist string into a String array.
   *
   * @param blacklist - A String containing a set of key/value pairs.
   * @return a String array representing the job names to be blacklisted. Returns
   *         empty string if blacklist is null.
   */
  private static String[] blacklistStringtoArray(final String blacklist) {
    if ( blacklist != null ) {
      return blacklist.split(",");
    }
    return ( new String[0] );
  }

  /**
   * This method parses the contents of the configured DataDog tags. If they are present.
   * Takes the current build as a parameter. And returns the expanded tags and their
   * values in a HashMap.
   *
   * Always returns a HashMap, that can be empty, if no tagging is configured.
   *
   * @param run - Current build
   * @param listener - Current listener
   * @return A {@link HashMap} containing the key,value pairs of tags. Never null.
   * @throws IOException
   * @throws InterruptedException
   */
  @Nonnull
  public static HashMap<String,String> parseTagList(Run run, TaskListener listener) throws IOException,
          InterruptedException {
    HashMap<String,String> map = new HashMap<String, String>();

    DataDogJobProperty property = DataDogUtilities.retrieveProperty(run);
    String prop = property.getTagProperties();

    if( !property.isTagFileEmpty() ) {
      String dataFromFile = property.readTagFile(run);
      if(dataFromFile != null) {
        for(String tag : dataFromFile.split("\\r?\\n")) {
          String[] expanded = run.getEnvironment(listener).expand(tag).split("=");
          if( expanded.length > 1 ) {
            map.put(expanded[0], expanded[1]);
            logger.fine(String.format("Emitted tag %s:%s", expanded[0], expanded[1]));
          } else {
            logger.fine(String.format("Ignoring the tag %s. It is empty.", tag));
          }
        }
      }
    }

    if( !property.isTagPropertiesEmpty() ) {
      for(String tag : prop.split("\\r?\\n")) {
        String[] expanded = run.getEnvironment(listener).expand(tag).split("=");
        if( expanded.length > 1 ) {
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
   *
   * @param r - Current build.
   * @return - The configured {@link DataDogJobProperty}. Null if not there
   */
  public static DataDogJobProperty retrieveProperty(Run r) {
    DataDogJobProperty property = (DataDogJobProperty)r.getParent()
            .getProperty(DataDogJobProperty.class);
    return property;
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
  public static String getHostname(final EnvVars envVars) {
    String[] UNIX_OS = {"mac", "linux", "freebsd", "sunos"};
    String hostname = null;

    // Check hostname configuration from Jenkins
    hostname = DataDogUtilities.getHostName();
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
   * Safe getter function to make sure an exception is not reached.
   *
   * @param data - A JSONObject containing a set of key/value pairs.
   * @param key - A String to be used to lookup a value in the JSONObject data.
   * @return a String representing data.get(key), or "null" if it doesn't exist
   */
  public static String nullSafeGetString(final JSONObject data, final String key) {
    if ( data.get(key) != null ) {
      return data.get(key).toString();
    } else {
      return "null";
    }
  }

  /**
   * Assembles a {@link JSONArray} from metadata available in the
   * {@link JSONObject} builddata. Returns a {@link JSONArray} with the set
   * of tags.
   *
   * @param builddata - A JSONObject containing a builds metadata.
   * @param extra - A list of tags, that are contributed via {@link DataDogJobProperty}.
   * @return a JSONArray containing a specific subset of tags retrieved from a builds metadata.
   */
  public static JSONArray assembleTags(final JSONObject builddata, final HashMap<String,String> extra) {
    JSONArray tags = new JSONArray();

    tags.add("job:" + builddata.get("job"));
    if ( (builddata.get("node") != null) && DataDogUtilities.getDataDogDescriptor().getTagNode() ) {
      tags.add("node:" + builddata.get("node"));
    }

    if ( builddata.get("result") != null ) {
      tags.add("result:" + builddata.get("result"));
    }

    if ( builddata.get("branch") != null && !extra.containsKey("branch") ) {
      tags.add("branch:" + builddata.get("branch"));
    }

    //Add the extra tags here
    for(String key : extra.keySet()) {
      tags.add(String.format("%s:%s", key, extra.get(key)));
      logger.info(String.format("Emitted tag %s:%s", key, extra.get(key)));
    }

    return tags;
  }
  /**
   * Converts from a double to a human readable string, representing a time duration.
   *
   * @param duration - A Double with a duration in seconds.
   * @return a human readable String representing a time duration.
   */
  public static String durationToString(final double duration) {
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

}
