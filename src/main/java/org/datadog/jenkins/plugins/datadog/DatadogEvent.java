package org.datadog.jenkins.plugins.datadog;

import net.sf.json.JSONObject;

/**
 * Marker interface for Datadog events.
 */
public interface DatadogEvent {
    /**
     * @return The payload for the given event. Events usually have a custom message
     */
    JSONObject createPayload();
}
