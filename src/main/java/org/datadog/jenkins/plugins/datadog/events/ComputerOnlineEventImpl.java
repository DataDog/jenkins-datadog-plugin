package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ComputerOnlineEventImpl implements DatadogEvent {

    private Computer computer;
    private TaskListener listener;
    private Map<String, Set<String>> tags;
    private boolean isTemporarily;

    public ComputerOnlineEventImpl(Computer computer, TaskListener listener, Map<String, Set<String>> tags, boolean isTemporarily) {
        this.computer = computer;
        this.listener = listener;
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

        String title = "Jenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") + "Online";
        payload.put("title", title);

        String message = "%%% \n Jenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") +
                "Online \n %%%";
        payload.put("text", message);

        payload.put("priority", "low");
        payload.put("alert_type", "normal");

        return payload;
    }
}