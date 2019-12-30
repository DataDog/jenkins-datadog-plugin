/*
The MIT License

Copyright (c) 2010-2020, Datadog <opensource@datadoghq.com>
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Create a job property for use with Datadog plugin.
 */
public class DatadogJobProperty<T extends Job<?, ?>> extends JobProperty<T> {

    private static final Logger logger = Logger.getLogger(DatadogJobProperty.class.getName());
    private static final String DISPLAY_NAME = "Datadog Job Tagging";

    private boolean enableFile = false;
    private String tagFile = null;
    private boolean enableProperty = false;
    private String tagProperties = null;
    private boolean emitSCMEvents = true;

    /**
     * Runs when the {@link DatadogJobProperty} class is created.
     */
    @DataBoundConstructor
    public DatadogJobProperty() { }

    /**
     * Gets a list of tag properties to be submitted with the Build to Datadog.
     *
     * @return a String representing a list of tag properties.
     */
    public String getTagProperties() {
        return isEnableProperty() ? tagProperties : null;
    }

    /**
     * @param tagProperties - The configured tag properties. Text area in the job configuration
     */
    @DataBoundSetter
    public void setTagProperties(final String tagProperties) {
        this.tagProperties = tagProperties;
    }

    /**
     * Gets the tagFile set in the job configuration.
     *
     * @return a String representing the relative path to a tagFile
     */
    public String getTagFile() {
        return isEnableFile() ? tagFile : null;
    }

    /**
     * Sets the tagFile set in the job configuration.
     *
     * @param tagFile - a String representing the relative path to a tagFile
     */
    @DataBoundSetter
    public void setTagFile(String tagFile) {
        this.tagFile = tagFile;
    }

    /**
     * Gets the enableFile set in the job configuration.
     *
     * @return a boolean representing the enableFile checkbox
     */
    public boolean isEnableFile() {
        return enableFile;
    }

    /**
     * Sets the enableFile set in the job configuration.
     *
     * @param enableFile - a boolean representing the enableFile checkbox
     */
    @DataBoundSetter
    public void setEnableFile(boolean enableFile) {
        this.enableFile = enableFile;
    }

    /**
     * Gets the enableProperty set in the job configuration.
     *
     * @return a boolean representing the enableProperty checkbox
     */
    public boolean isEnableProperty() {
        return enableProperty;
    }

    /**
     * Sets the enableProperty set in the job configuration.
     *
     * @param enableProperty - a boolean representing the enableProperty checkbox
     */
    @DataBoundSetter
    public void setEnableProperty(boolean enableProperty) {
        this.enableProperty = enableProperty;
    }

    /**
     * @return - A {@link Boolean} indicating if the user has configured Datadog to emit SCM related events.
     */
    public boolean isEmitSCMEvents() {
        return emitSCMEvents;
    }

    /**
     * Set the checkbox in the UI, used for Jenkins data binding
     *
     * @param emitSCMEvents - The checkbox status (checked/unchecked)
     */
    @DataBoundSetter
    public void setEmitSCMEvents(boolean emitSCMEvents) {
        this.emitSCMEvents = emitSCMEvents;
    }

    /**
     * Method to read the contents of the specified file in the {@link DatadogJobProperty}
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
            if (workspace != null && getTagFile() != null) {
                FilePath path = new FilePath(workspace, getTagFile());
                if (path.exists()) {
                    s = path.readToString();
                }
            }
        } catch (IOException | InterruptedException | NullPointerException ex) {
            logger.severe(ex.getMessage());
        }
        return s;
    }

    @Extension
    public static final class DatadogJobPropertyDescriptor extends JobPropertyDescriptor {

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
