package org.datadog.jenkins.plugins.datadog.events;

import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public abstract class AbstractDatadogSimpleEvent extends AbstractDatadogEvent {

    public AbstractDatadogSimpleEvent(Map<String, Set<String>> tags) {
        setHost(DatadogUtilities.getHostname(null));
        setDate(DatadogUtilities.currentTimeMillis() / 1000);
        setTags(tags);
    }

}
