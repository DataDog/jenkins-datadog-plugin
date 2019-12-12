package org.datadog.jenkins.plugins.datadog.events;

import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.model.BuildData;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public abstract class AbstractDatadogSimpleEvent implements DatadogEvent {

    protected Map<String, Set<String>> tags;

    public AbstractDatadogSimpleEvent(Map<String, Set<String>> tags) {
        this.tags = tags;
    }

    public JSONObject createPayload(String aggregation_key) {
        JSONObject payload = new JSONObject();
        payload.put("host", DatadogUtilities.getHostname(null));
        payload.put("aggregation_key", aggregation_key);
        payload.put("date_happened", System.currentTimeMillis() / 1000);
        payload.put("tags", TagsUtil.convertTagsToJSONArray(tags));
        payload.put("source_type_name", "jenkins");

        return payload;
    }

}
