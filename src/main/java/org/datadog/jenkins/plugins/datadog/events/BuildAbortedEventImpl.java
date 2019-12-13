package org.datadog.jenkins.plugins.datadog.events;

import org.datadog.jenkins.plugins.datadog.model.BuildData;

public class BuildAbortedEventImpl extends AbstractDatadogBuildEvent {

    public BuildAbortedEventImpl(BuildData buildData) {
        super(buildData);

        String number = buildData.getBuildNumber("unknown");
        String userId = buildData.getUserId();
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 aborted on hostname`
        String title = "Job " + jobName + " build #" + number + " aborted on " + hostname;
        setTitle(title);

        // Build Text
        // eg: `User <userId> aborted the [job with build number #<buildNumber>] (1sec)`
        String text = "%%% \nUser " + userId + " aborted the [job " + jobName + " build #" + number +
                "](" + buildUrl + ") " + getFormattedDuration() + " \n%%%";
        setText(text);

        setPriority(Priority.LOW);
        setAlertType(AlertType.INFO);
    }
}
