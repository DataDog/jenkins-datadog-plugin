package org.datadog.jenkins.plugins.datadog.events;

import hudson.XmlFile;
import hudson.model.Saveable;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;

import java.util.Map;
import java.util.Set;

public class ConfigChangedEventImpl extends AbstractDatadogSimpleEvent {

    public ConfigChangedEventImpl(Saveable config, XmlFile file, Map<String, Set<String>> tags) {
        super(tags);

        String fileName = file.getFile().getName();
        String userId = DatadogUtilities.getUserId();
        setAggregationKey(fileName);

        String title = userId + " changed file " + fileName;
        setTitle(title);

        String text = "%%% \nUser " + userId + " changed " + fileName + " \n%%%";
        setText(text);

        if ("system".equals(userId.toLowerCase())){
            setPriority(Priority.LOW);
            setAlertType(AlertType.INFO);
        }else{
            setPriority(Priority.NORMAL);
            setAlertType(AlertType.WARNING);
        }
    }

}
