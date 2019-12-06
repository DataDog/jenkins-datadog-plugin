package org.datadog.jenkins.plugins.datadog.listeners;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import net.sf.json.JSONArray;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.events.ConfigChangedEventImpl;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class registers an {@link SaveableListener} to trigger events and calculate metrics:
 * - When an saveable gets changed, the {@link #onChange(Saveable, XmlFile)} method will be invoked.
 */
@Extension
public class DatadogSaveableListener  extends SaveableListener {

    private static final Logger logger = Logger.getLogger(DatadogSaveableListener.class.getName());

    @Override
    public void onChange(Saveable config, XmlFile file) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogSaveableListener#onChange");

            // Get Datadog Client Instance
            DatadogClient client = DatadogUtilities.getDatadogClient();

            // Get the list of global tags to apply
            Map<String, Set<String>> tags = DatadogUtilities.getDatadogGlobalDescriptor().getGlobalTags();

            // Send event
            DatadogEvent event = new ConfigChangedEventImpl(config, file, tags);
            client.sendEvent(event.createPayload());

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.config.changed", hostname, tags);

            logger.fine("End DatadogSaveableListener#onChange");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }
}
