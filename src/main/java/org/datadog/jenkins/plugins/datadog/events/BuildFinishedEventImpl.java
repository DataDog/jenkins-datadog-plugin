package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Result;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.model.BuildData;

/**
 * Class that implements the {@link DatadogEvent}. This event produces an event payload
 * with a proper description for a finished build.
 */
public class BuildFinishedEventImpl extends AbstractDatadogBuildEvent {

    public BuildFinishedEventImpl(BuildData buildData) {
        super(buildData);

        String buildNumber = buildData.getBuildNumber("unknown");
        String buildResult = buildData.getResult("UNKNOWN");
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 success on hostname`
        String title = "Job " + jobName + " build #" + buildNumber + " " + buildResult.toLowerCase() + " on " + hostname;
        setTitle(title);

        // Build Text
        // eg: `[Job <jobName> with build number #<buildNumber>] finished with status <buildResult> (1sec)`
        String text = "%%% \n[Job " + jobName + " build #" + buildNumber + "](" + buildUrl +
                ") finished with status " + buildResult.toLowerCase() + " " + getFormattedDuration() + " \n%%%";
        setText(text);

        if (Result.SUCCESS.toString().equals(buildResult)) {
            setPriority(Priority.LOW);
            setAlertType(AlertType.SUCCESS);
        } else if (Result.FAILURE.toString().equals(buildResult)) {
            setPriority(Priority.NORMAL);
            setAlertType(AlertType.ERROR);
        } else {
            setPriority(Priority.NORMAL);
            setAlertType(AlertType.WARNING);
        }
    }
}
