package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Run;
import java.io.IOException;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Create a job property for use with DataDog plugin.
 * @param <T>
 */
public class DataDogJobProperty<T extends Job<?,?>> extends JobProperty<T> {
  private static final Logger LOGGER =  Logger.getLogger(DatadogBuildListener.class.getName());
  private static final String DISPLAY_NAME = "DataDog Job Tagging";
  private String tagProperties = null;
  private String tagFile = null;

  /**
   * Runs when the {@link DataDogJobProperty} class is created.
   */
  @DataBoundConstructor
  public DataDogJobProperty( ) { }

  /**
   * Gets a list of tag properties to be submitted with the Build to DataDog.
   *
   * @return a String representing a list of tag properties.
   */
  public String getTagProperties() {
    return tagProperties;
  }

  @DataBoundSetter
  public void setTagProperties(final String tagProperties) {
    this.tagProperties = tagProperties;
  }

  /**
   * This method is called whenever the Job form is saved. We use the 'on' property
   * to determine if the controls are selected.
   *
   * @param req - The request
   * @param form - A JSONObject containing the submitted form data from the job configuration
   * @return a {@link JobProperty} object representing the tagging added to the job
   * @throws hudson.model.Descriptor.FormException
   */
  @Override
  public JobProperty<?> reconfigure(StaplerRequest req, JSONObject form)
          throws Descriptor.FormException {

    DataDogJobProperty prop = (DataDogJobProperty) super.reconfigure(req, form);

    boolean isEnableFile = form.getBoolean("enableFile");
    boolean isEnableTagProperties = form.getBoolean("enableProperty");

    if(!isEnableFile) {
        prop.tagFile = null;
    }
    if(!isEnableTagProperties) {
        prop.tagProperties = null;
    }

    return prop;
  }

  /**
   * Gets the tagFile set in the job configuration.
   *
   * @return a String representing the relative path to a tagFile
   */
  public String getTagFile() {
    return tagFile;
  }

  /**
   * Sets the tagFile set in the job configration.
   *
   * @param tagFile - a String representing the relative path to a tagFile
   */
  @DataBoundSetter
  public void setTagFile(String tagFile) {
    this.tagFile = tagFile;
  }

  /**
   * Checks if tagFile was set in the job configuration.
   *
   * @return a boolean representing the state of the tagFile job configuration
   */
  public boolean isTagFileEmpty() {
    return StringUtils.isBlank(this.tagFile);
  }

  /**
   * Checks if the contents of the properties in the job tagging configuration section is empty
   *
   * @return a boolean representing the state of the properties job configuration
   */
  public boolean isTagPropertiesEmpty() {
    return StringUtils.isBlank(this.tagProperties);
  }

  /**
   * Method to read the contents of the specified file in the {@link DataDogJobProperty}
   *
   * @param r - Current build
   * @return - A String containing the contents of the scanned file. Returns null when
   * the file cannot be found.
   */
  public String readTagFile(Run r) {
    String s = null;
    try {
      //We need to make sure that the workspace has been created. When 'onStarted' is
      //invoked, the workspace has not yet been established, so this check is necessary.
      FilePath workspace = r.getExecutor().getCurrentWorkspace();
      if(workspace != null) {
        FilePath path = new FilePath(workspace,
                tagFile);
        if(path.exists()) {
          s = path.readToString();
        }
      }
    } catch (IOException ex) {
      LOGGER.severe(ex.getMessage());
    } catch (InterruptedException ex) {
      LOGGER.severe(ex.getMessage());
    }
    return s;
  }

  @Extension
  public static final class DataDogJobPropertyDescriptorImpl
    extends JobPropertyDescriptor {

    /**
     * Getter function for a human readable class display name.
     *
     * @return a String containing the human readable display name for the {@link JobProperty} class.
     */
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    /**
     * Indicates where this property can be used
     *
     * @param jobType - a Job object
     * @return Always true. This property can be set for all Job types.
     */
    @Override
    public boolean isApplicable(Class<? extends Job> jobType) {
        return true;
    }

  }
}
