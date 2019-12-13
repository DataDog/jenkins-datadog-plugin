package org.datadog.jenkins.plugins.datadog.events;

import org.datadog.jenkins.plugins.datadog.model.BuildData;

/**
 * This event should contain all the data to construct a build started event. With
 * the right message for Datadog.
 */
public class BuildStartedEventImpl extends AbstractDatadogBuildEvent {

    public BuildStartedEventImpl(BuildData buildData) {
        super(buildData);

        String buildNumber = buildData.getBuildNumber("unknown");
        String userId = buildData.getUserId();
        String jobName = buildData.getJobName("unknown");
        String buildUrl = buildData.getBuildUrl("unknown");
        String hostname = buildData.getHostname("unknown");

        // Build title
        // eg: `job_name build #1 started on hostname`
        String title = jobName + " build #" + buildNumber + " started on " + hostname;
        setTitle(title);

        // Build Text
        // eg: User <userId> started the [job <jobName> with build number #<buildNumber>] (1sec)"
        String text = "%%% \nUser " + userId + " started the [job " + jobName + " build #" +
                buildNumber + "](" + buildUrl + ") " + getFormattedDuration() + " \n%%%";
        setText(text);

        setPriority(Priority.LOW);
        setAlertType(AlertType.INFO);
    }
}
