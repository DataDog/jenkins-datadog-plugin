package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ComputerOnlineEventImpl extends AbstractDatadogSimpleEvent {

    private Computer computer;
    private TaskListener listener;
    private boolean isTemporarily;

    public ComputerOnlineEventImpl(Computer computer, TaskListener listener, Map<String, Set<String>> tags, boolean isTemporarily) {
        super(tags);
        this.computer = computer;
        this.listener = listener;
        this.isTemporarily = isTemporarily;
    }

    @Override
    public JSONObject createPayload() {
        String nodeName = DatadogUtilities.getNodeName(computer);
        JSONObject payload = super.createPayload(nodeName);

        String title = "Jenkins node " + nodeName + " is" + (isTemporarily ? " temporarily " : " ") + "Online";
        payload.put("title", title);

        String message = "%%% \nJenkins node " + nodeName + " is" + (isTemporarily ? " temporarily " : " ") +
                "Online \n%%%";
        payload.put("text", message);

        payload.put("priority", "low");
        payload.put("alert_type", "success");

        return payload;
    }
}