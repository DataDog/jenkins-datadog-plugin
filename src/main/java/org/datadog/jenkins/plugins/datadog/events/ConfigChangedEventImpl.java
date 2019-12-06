package org.datadog.jenkins.plugins.datadog.events;

import hudson.XmlFile;
import hudson.model.Saveable;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ConfigChangedEventImpl implements DatadogEvent {

    private Saveable config;
    private XmlFile file;
    private Map<String, Set<String>> tags;

    public ConfigChangedEventImpl(Saveable config, XmlFile file, Map<String, Set<String>> tags) {
        this.config = config;
        this.file = file;
        this.tags = tags;
    }

    @Override
    public JSONObject createPayload() {
        String hostname = DatadogUtilities.getHostname(null);
        String fileName = file.getFile().getName();
        String userId = DatadogUtilities.getUserId();

        JSONObject payload = new JSONObject();
        payload.put("host", hostname);
        payload.put("aggregation_key", fileName);
        payload.put("date_happened", System.currentTimeMillis() / 1000);
        payload.put("tags", TagsUtil.convertTagsToJSONArray(tags));
        payload.put("source_type_name", "jenkins");

        String title = userId + " changed file " + fileName;
        payload.put("title", title);

        String message = "%%% \n " + userId + " changed " + fileName + " \n %%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "warning");

        return payload;
    }
}