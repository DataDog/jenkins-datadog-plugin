package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.model.TaskListener;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ComputerOnlineEventImpl extends AbstractDatadogSimpleEvent {

    public ComputerOnlineEventImpl(Computer computer, TaskListener listener, Map<String, Set<String>> tags, boolean isTemporarily) {
        super(tags);

        String nodeName = DatadogUtilities.getNodeName(computer);
        setAggregationKey(nodeName);

        String title = "Jenkins node " + nodeName + " is" + (isTemporarily ? " temporarily " : " ") + "Online";
        setTitle(title);

        String text = "%%% \nJenkins node " + nodeName + " is" + (isTemporarily ? " temporarily " : " ") +
                "Online \n%%%";
        setText(text);

        setPriority(Priority.LOW);
        setAlertType(AlertType.SUCCESS);
    }
}
