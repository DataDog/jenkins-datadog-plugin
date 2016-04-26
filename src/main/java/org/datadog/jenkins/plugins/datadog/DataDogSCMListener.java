package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import net.sf.json.JSONObject;

/**
 * This class registers an {@link SCMListener} with Jenkins which allows us to create
 * the "Checkout successful" event.
 *
 */
@Extension
public class DataDogSCMListener extends SCMListener {

  private static final Logger logger =  Logger.getLogger(DataDogSCMListener.class.getName());

  @Override
  public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener,
          File changelogFile, SCMRevisionState pollingBaseline) throws Exception {

    String jobName = build.getParent().getDisplayName();
    HashMap<String,String> tags = new HashMap<String,String>();
    // Process only if job is NOT in blacklist
    if ( DataDogUtilities.isJobTracked(jobName)
            && DataDogUtilities.retrieveProperty(build).isEmitOnCheckout() ) {
      logger.fine("Checkout! in onCheckout()");

      // Grab environment variables
      EnvVars envVars = null;
      try {
        envVars = build.getEnvironment(listener);
        tags = DataDogUtilities.parseTagList(build, listener);
      } catch (IOException e) {
        logger.severe(e.getMessage());
      } catch (InterruptedException e) {
        logger.severe(e.getMessage());
      }

      // Gather pre-build metadata
      JSONObject builddata = new JSONObject();
      builddata.put("hostname", DataDogUtilities.getHostname(envVars)); // string
      builddata.put("job", jobName); // string
      builddata.put("number", build.number); // int
      builddata.put("result", null); // null
      builddata.put("duration", null); // null
      builddata.put("buildurl", envVars.get("BUILD_URL")); // string
      long starttime = build.getStartTimeInMillis() / DatadogBuildListener.THOUSAND_LONG; // ms to s
      builddata.put("timestamp", starttime); // string

      DataDogEvent evt = new CheckoutCompletedEventImpl(builddata, tags);

      DataDogHttpRequests.sendEvent(evt);
    }
  }
}
