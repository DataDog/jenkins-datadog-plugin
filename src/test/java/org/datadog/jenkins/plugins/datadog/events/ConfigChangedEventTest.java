package org.datadog.jenkins.plugins.datadog.events;

import hudson.XmlFile;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({DatadogUtilities.class})
public class ConfigChangedEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.currentTimeMillis()).thenReturn(0l);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);
        when(DatadogUtilities.getUserId()).thenReturn(null);
        when(DatadogUtilities.getFileName(any(XmlFile.class))).thenReturn(null);

        DatadogEvent event = new ConfigChangedEventImpl(null, null, null);

        Assert.assertTrue(event.getHost() == null);
        Assert.assertTrue(event.getDate() == 0);
        Assert.assertTrue(event.getAggregationKey() == null);
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User null changed file null"));
        Assert.assertTrue(event.getText().contains("User null changed file null"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.WARNING));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));
    }

    @Test
    public void testWithEverythingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.currentTimeMillis()).thenReturn(System.currentTimeMillis());
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn("hostname");
        when(DatadogUtilities.getUserId()).thenReturn("username");
        when(DatadogUtilities.getFileName(any(XmlFile.class))).thenReturn("filename");

        DatadogEvent event = new ConfigChangedEventImpl(null, null, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("filename"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username changed file filename"));
        Assert.assertTrue(event.getText().contains("User username changed file filename"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.WARNING));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        when(DatadogUtilities.getUserId()).thenReturn("SyStEm");
        event = new ConfigChangedEventImpl(null, null, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("filename"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User SyStEm changed file filename"));
        Assert.assertTrue(event.getText().contains("User SyStEm changed file filename"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.LOW));
    }
}
