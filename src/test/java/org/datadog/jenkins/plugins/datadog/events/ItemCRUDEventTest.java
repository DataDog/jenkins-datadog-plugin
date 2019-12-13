package org.datadog.jenkins.plugins.datadog.events;

import hudson.model.*;
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
public class ItemCRUDEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.currentTimeMillis()).thenReturn(0l);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);
        when(DatadogUtilities.getNodeName(any(Computer.class))).thenReturn(null);
        when(DatadogUtilities.getItemName(any(Item.class))).thenReturn(null);

        DatadogEvent event = new ItemCRUDEventImpl(null, null, null);

        Assert.assertTrue(event.getHost() == null);
        Assert.assertTrue(event.getDate() == 0);
        Assert.assertTrue(event.getAggregationKey() == null);
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User null did something with the item null"));
        Assert.assertTrue(event.getText().contains("User null did something with the item null"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        event = new ItemCRUDEventImpl(null, "something", null);

        Assert.assertTrue(event.getHost() == null);
        Assert.assertTrue(event.getDate() == 0);
        Assert.assertTrue(event.getAggregationKey() == null);
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User null something the item null"));
        Assert.assertTrue(event.getText().contains("User null something the item null"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));
    }

    @Test
    public void testWithEverythingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.currentTimeMillis()).thenReturn(System.currentTimeMillis());
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn("hostname");
        when(DatadogUtilities.getUserId()).thenReturn("username");
        when(DatadogUtilities.getItemName(any(Item.class))).thenReturn("itemname");

        DatadogEvent event = new ItemCRUDEventImpl(null, ItemCRUDEventImpl.CREATED, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("itemname"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username created the item itemname"));
        Assert.assertTrue(event.getText().contains("User username created the item itemname"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        event = new ItemCRUDEventImpl(null, ItemCRUDEventImpl.UPDATED, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("itemname"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username updated the item itemname"));
        Assert.assertTrue(event.getText().contains("User username updated the item itemname"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        event = new ItemCRUDEventImpl(null, ItemCRUDEventImpl.DELETED, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("itemname"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username deleted the item itemname"));
        Assert.assertTrue(event.getText().contains("User username deleted the item itemname"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

    }
}
