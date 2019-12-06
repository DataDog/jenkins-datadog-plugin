package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.slaves.OfflineCause;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ComputerOfflineEventImpl implements DatadogEvent {

    private Computer computer;
    private OfflineCause cause;
    private Map<String, Set<String>> tags;
    private boolean isTemporarily;

    public ComputerOfflineEventImpl(Computer computer, OfflineCause cause, Map<String, Set<String>> tags, boolean isTemporarily) {
        this.computer = computer;
        this.cause = cause;
        this.tags = tags;
        this.isTemporarily = isTemporarily;
    }

    @Override
    public JSONObject createPayload() {
        String hostname = DatadogUtilities.getHostname(null);
        String nodeName = DatadogUtilities.getNodeName(computer);

        JSONObject payload = new JSONObject();
        payload.put("host", hostname);
        payload.put("aggregation_key", nodeName);
        payload.put("date_happened", System.currentTimeMillis() / 1000);
        payload.put("tags", TagsUtil.convertTagsToJSONArray(tags));
        payload.put("source_type_name", "jenkins");

        String title = "Jenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") + "Offline";
        payload.put("title", title);

        // TODO: Add more info about the case in the event in message.
        String message = "%%% \n Jenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") +
                "Offline \n %%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "warning");

        return payload;
    }
}