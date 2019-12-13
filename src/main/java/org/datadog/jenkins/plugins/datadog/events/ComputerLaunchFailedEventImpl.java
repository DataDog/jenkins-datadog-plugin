package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.Computer;
import hudson.model.TaskListener;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ComputerLaunchFailedEventImpl extends AbstractDatadogSimpleEvent {

    public ComputerLaunchFailedEventImpl(Computer computer, TaskListener listener, Map<String, Set<String>> tags) {
        super(tags);

        String nodeName = DatadogUtilities.getNodeName(computer);
        setAggregationKey(nodeName);

        String title = "Jenkins node " + nodeName + " failed to launch";
        setTitle(title);

        String text = "%%% \nJenkins node " + nodeName + " failed to launch \n%%%";
        setText(text);

        setPriority(Priority.NORMAL);
        setAlertType(AlertType.ERROR);
    }
}
