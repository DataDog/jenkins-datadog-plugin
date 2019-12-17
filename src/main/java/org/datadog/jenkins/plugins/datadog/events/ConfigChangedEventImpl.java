package org.datadog.jenkins.plugins.datadog.events;

import hudson.XmlFile;
import hudson.model.Saveable;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ConfigChangedEventImpl extends AbstractDatadogSimpleEvent {

    public ConfigChangedEventImpl(Saveable config, XmlFile file, Map<String, Set<String>> tags) {
        super(tags);

        String fileName = DatadogUtilities.getFileName(file);
        String userId = DatadogUtilities.getUserId();
        setAggregationKey(fileName);

        String title = "User " + userId + " changed file " + fileName;
        setTitle(title);

        String text = "%%% \nUser " + userId + " changed file " + fileName + " \n%%%";
        setText(text);

        if (userId != null && "system".equals(userId.toLowerCase())){
            setPriority(Priority.LOW);
            setAlertType(AlertType.INFO);
        }else{
            setPriority(Priority.NORMAL);
            setAlertType(AlertType.WARNING);
        }
    }

}
