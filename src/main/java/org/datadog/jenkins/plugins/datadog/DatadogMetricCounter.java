package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.model.*;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;


public class DatadogMetricCounter extends PeriodicWork {
    // run frequency - 15 seconds
    @Override
    public long getRecurrencePeriod() {
        return PeriodicWork.MIN / 4;
    }

    @Override
    public void pwRun(final Run run, @Nonnull final TaskListener listener) throws Exception {

        @Override
        public DatadogBuildListener.DescriptorImpl getDescriptor() {
            return new DatadogBuildListener.DescriptorImpl();
        }

        // Collect Build Data
        BuildData buildData;
        try {
            buildData = new BuildData(run, listener);
        } catch (IOException | InterruptedException e) {
            logger.severe(e.getMessage());
            return;
        }

        // Instantiate the Datadog Client
        DatadogClient client = getDescriptor().leaseDatadogClient();

        // Get the list of global tags to apply
        Map<String, String> extraTags = DatadogUtilities.buildExtraTags(run, listener);

        client.gauge("jenkins.job.completed",
                // Get the value of the completed counter
                DatadogBuildListener.threadId.get(),
                buildData.getHostname("null"),
                buildData.getAssembledTags(extraTags));
    }
}
