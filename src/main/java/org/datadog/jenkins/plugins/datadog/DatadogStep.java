package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Run;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.inject.Inject;

/**
 * DatadogStep {@link AbstractStepImpl}.
 *
 * <p>When the user adds datadog step in Jenkinsfile,
 * {@link DescriptorImpl} is invoked and a new
 * {@link DatadogStep} is created.
 *
 * <p>When the step datadog is called, a job duration metric will be sent to datadog,
 * using the configuration set in General Settings, by invoking {@link DatadogUtilities}.
 *
 * @author David Guzman
 */

public final class DatadogStep extends AbstractStepImpl{
  
  private static final Logger logger =  Logger.getLogger(DatadogStep.class.getName());
  private HashMap<String, String> tags;
	
  /**
   * Runs when the {@link DatadogStep} class is created.
   * @param tags are the optional tags sent to datadog along with the metric value.
   */
  @DataBoundConstructor public DatadogStep(HashMap<String,String> tags) {
    this.tags = tags;
  }
  
  /**
   * Tags passed in pipeline as parameters to the build step, i.e. datadog([key1:'Value1', tag2:'Value2']).
   *
   * @return a HashMap with tag values
   */
  public HashMap<String, String> getTags(){
    return tags;
  }
  
  /**
   * Descriptor for {@link DatadogStep}. Used as a singleton.
   * The class is marked as public so that it can be accessed from views.
   */
  @Extension
  public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
  
    /**
     * Specifies the {@link Execution} for this DescriptorImpl
     */
    public DescriptorImpl() {
      super(Execution.class);
    }

    /**
     * Name used in pipeline script
     */
    @Override public String getFunctionName(){
      return "datadog";
    }
    
    /**
     * Name used in Snippet Generator
     */
    @Override public String getDisplayName(){
      return "Push job duration metric to Datadog";
    }
  }
  
  /**
   * This Execution class performs actions for {@link DatadogStep}.
   */
  public static class Execution extends AbstractSynchronousStepExecution<String> {

    /**
     * Injects tags defined in {@link DatadogStep}
     */
    @Inject(optional=true) DatadogStep datadog;
  
    /**
     * Runs the build step. It takes build information from {@link Run} and {@link TaskListener}.
     * @return a String with the build data sent to datadog as a map.
     * @throws Exception if data cannot be sent to datadog
     */
    @Override protected String run() throws Exception {
      Run run = this.getContext().get(Run.class);
      TaskListener listener = this.getContext().get(TaskListener.class);
      JSONObject buildData = gatherRunningBuildMetadata(run, listener);
  
      try {
        DatadogUtilities.gauge("jenkins.job.duration", buildData, "duration", datadog.getTags());
        logger.fine("Executed step to send metric");
      } catch (Exception e) {
        logger.severe(e.toString());
      }
      return buildData.toString();
    }
  
    /**
     * Gathers unfinished build metadata, assembling it into a {@link JSONObject} before
     * returning it to the caller.
     *
     * @param run - A Run object representing a particular execution of Job.
     * @param listener - A TaskListener object which receives events that happen during some
     *                   operation.
     * @return a JSONObject containing a builds metadata.
     */
    private JSONObject gatherRunningBuildMetadata(@Nonnull final Run run, @Nonnull final TaskListener listener) {
      // Assemble JSON
      long scheduledstarttime = run.getStartTimeInMillis();
      long timestamp = run.getTimeInMillis();
      double duration = (new GregorianCalendar().getTimeInMillis()-timestamp) / DatadogBuildListener.THOUSAND_DOUBLE;
      JSONObject builddata = new JSONObject();
      builddata.put("starttime", scheduledstarttime); // long ms
      builddata.put("timestamp", timestamp); // long ms
      builddata.put("duration", duration); // double ms
      builddata.put("number", run.number); // int
      builddata.put("job", run.getParent().getDisplayName()); // string

      // Grab environment variables
      try {
        EnvVars envVars = run.getEnvironment(listener);
        builddata.put("hostname", DatadogUtilities.getHostname(envVars)); // string
        builddata.put("buildurl", envVars.get("BUILD_URL")); // string
        builddata.put("node", envVars.get("NODE_NAME")); // string
        if ( envVars.get("GIT_BRANCH") != null ) {
          builddata.put("branch", envVars.get("GIT_BRANCH")); // string
        } else if ( envVars.get("CVS_BRANCH") != null ) {
          builddata.put("branch", envVars.get("CVS_BRANCH")); // string
        }
      } catch (IOException e) {
        logger.severe(e.getMessage());
      } catch (InterruptedException e) {
        logger.severe(e.getMessage());
      }
      return builddata;
    }
  } 
}