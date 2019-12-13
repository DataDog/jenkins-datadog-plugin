package org.datadog.jenkins.plugins.datadog.events;

import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload with a
 * with a proper description for a completed checkout.
 */
public class SCMCheckoutCompletedEventImpl extends AbstractDatadogBuildEvent {

    public SCMCheckoutCompletedEventImpl(BuildData buildData) {
        super(buildData);

        String buildNumber = buildData.getBuildNumber("unknown");
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 checkout finished hostname`
        String title = "Job " + jobName + " build #" + buildNumber + " checkout finished on " + hostname;
        setTitle(title);

        // Build Text
        // eg: `[Job <jobName> with build number #<buildNumber>] checkout successfully (1sec)`
        String text = "%%% \n[Job " + jobName + " build #" + buildNumber + "](" + buildUrl +
                ") checkout finished successfully on " + hostname + " " + getFormattedDuration() + " \n%%%";
        setText(text);

        setPriority(Priority.LOW);
        setAlertType(AlertType.SUCCESS);
    }
}
