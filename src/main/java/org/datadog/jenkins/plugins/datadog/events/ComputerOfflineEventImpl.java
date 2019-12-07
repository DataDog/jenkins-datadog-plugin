package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.slaves.OfflineCause;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ComputerOfflineEventImpl extends AbstractDatadogSimpleEvent {

    private Computer computer;
    private OfflineCause cause;
    private boolean isTemporarily;

    public ComputerOfflineEventImpl(Computer computer, OfflineCause cause, Map<String, Set<String>> tags, boolean isTemporarily) {
        super(tags);
        this.computer = computer;
        this.cause = cause;
        this.isTemporarily = isTemporarily;
    }

    @Override
    public JSONObject createPayload() {
        String nodeName = DatadogUtilities.getNodeName(computer);
        JSONObject payload = super.createPayload(nodeName);

        String title = "Jenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") + "Offline";
        payload.put("title", title);

        // TODO: Add more info about the case in the event in message.
        String message = "%%% \nJenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") +
                "Offline \n%%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "warning");

        return payload;
    }
}