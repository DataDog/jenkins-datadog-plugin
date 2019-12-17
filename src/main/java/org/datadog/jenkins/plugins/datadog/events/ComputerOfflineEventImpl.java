package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.slaves.OfflineCause;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ComputerOfflineEventImpl extends AbstractDatadogSimpleEvent {

    public ComputerOfflineEventImpl(Computer computer, OfflineCause cause, Map<String, Set<String>> tags, boolean isTemporarily) {
        super(tags);

        String nodeName = DatadogUtilities.getNodeName(computer);
        setAggregationKey(nodeName);

        String title = "Jenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") + "offline";
        setTitle(title);

        // TODO: Add more info about the case in the event in message.
        String text = "%%% \nJenkins node " + nodeName + " is" + (isTemporarily? " temporarily ": " ") +
                "offline \n%%%";
        setText(text);

        setPriority(Priority.NORMAL);
        setAlertType(AlertType.WARNING);
    }
}
