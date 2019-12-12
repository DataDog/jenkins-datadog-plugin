package org.datadog.jenkins.plugins.datadog.events;

import hudson.XmlFile;
import hudson.model.Saveable;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ConfigChangedEventImpl extends AbstractDatadogSimpleEvent {

    private Saveable config;
    private XmlFile file;

    public ConfigChangedEventImpl(Saveable config, XmlFile file, Map<String, Set<String>> tags) {
        super(tags);
        this.config = config;
        this.file = file;
    }

    @Override
    public JSONObject createPayload() {
        String fileName = file.getFile().getName();
        String userId = DatadogUtilities.getUserId();
        JSONObject payload = super.createPayload(fileName);

        String title = userId + " changed file " + fileName;
        payload.put("title", title);

        String message = "%%% \nUser " + userId + " changed " + fileName + " \n%%%";
        payload.put("text", message);

        if ("system".equals(userId.toLowerCase())){
            payload.put("priority", "low");
            payload.put("alert_type", "info");
        }else{
            payload.put("priority", "normal");
            payload.put("alert_type", "warning");
        }

        return payload;
    }
}