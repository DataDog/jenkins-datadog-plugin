package org.datadog.jenkins.plugins.datadog.events;

import org.datadog.jenkins.plugins.datadog.model.BuildData;

public abstract class AbstractDatadogBuildEvent extends AbstractDatadogEvent {

    protected BuildData buildData;

    private static final float MINUTE = 60;
    private static final float HOUR = 3600;

    public AbstractDatadogBuildEvent(BuildData buildData) {
        this.buildData = buildData;
        setHost(buildData.getHostname(null));
        setAggregationKey(buildData.getJobName("unknown"));
        setDate(buildData.getEndTime(System.currentTimeMillis()) / 1000);
        setTags(buildData.getTags());
    }

    protected String getFormattedDuration() {
        Long duration = buildData.getDuration(null);
        if (duration != null) {
            String output = "(";
            String format = "%.2f";
            double d = duration.doubleValue() / 1000;
            if (d < MINUTE) {
                output = output + String.format(format, d) + " secs)";
            } else if (MINUTE <= d && d < HOUR) {
                output = output + String.format(format, d / MINUTE) + " mins)";
            } else if (HOUR <= d) {
                output = output + String.format(format, d / HOUR) + " hrs)";
            }
            return output;
        } else {
            return "";
        }
    }
}
