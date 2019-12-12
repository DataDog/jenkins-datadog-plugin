package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;

public class ComputerLaunchFailedEventImpl extends AbstractDatadogSimpleEvent {

    private Computer computer;
    private TaskListener listener;

    public ComputerLaunchFailedEventImpl(Computer computer, TaskListener listener, Map<String, Set<String>> tags) {
        super(tags);
        this.computer = computer;
        this.listener = listener;
    }

    @Override
    public JSONObject createPayload() {
        String nodeName = DatadogUtilities.getNodeName(computer);
        JSONObject payload = super.createPayload(nodeName);

        String title = "Jenkins node " + nodeName + " failed to launch";
        payload.put("title", title);

        String message = "%%% \nJenkins node " + nodeName + " failed to launch \n%%%";
        payload.put("text", message);

        payload.put("priority", "normal");
        payload.put("alert_type", "error");

        return payload;
    }
}