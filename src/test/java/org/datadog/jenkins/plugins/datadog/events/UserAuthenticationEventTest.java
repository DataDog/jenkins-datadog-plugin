package org.datadog.jenkins.plugins.datadog.events;

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
public class UserAuthenticationEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.currentTimeMillis()).thenReturn(0l);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);

        DatadogEvent event = new UserAuthenticationEventImpl(null, null, null);

        Assert.assertTrue(event.getHost() == null);
        Assert.assertTrue(event.getDate() == 0);
        Assert.assertTrue(event.getAggregationKey() == null);
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User null did something"));
        Assert.assertTrue(event.getText().contains("User null did something"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.ERROR));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        event = new UserAuthenticationEventImpl(null, "something", null);

        Assert.assertTrue(event.getHost() == null);
        Assert.assertTrue(event.getDate() == 0);
        Assert.assertTrue(event.getAggregationKey() == null);
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User null something"));
        Assert.assertTrue(event.getText().contains("User null something"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.ERROR));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));
    }

    @Test
    public void testWithEverythingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.currentTimeMillis()).thenReturn(System.currentTimeMillis());
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn("hostname");

        DatadogEvent event = new UserAuthenticationEventImpl("username", UserAuthenticationEventImpl.ACCESS_DENIED, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("username"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username failed to authenticate"));
        Assert.assertTrue(event.getText().contains("User username failed to authenticate"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.ERROR));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        event = new UserAuthenticationEventImpl("username", UserAuthenticationEventImpl.LOGOUT, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("username"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username logout"));
        Assert.assertTrue(event.getText().contains("User username logout"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.SUCCESS));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.LOW));

        event = new UserAuthenticationEventImpl("username", UserAuthenticationEventImpl.LOGIN, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("username"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username authenticated"));
        Assert.assertTrue(event.getText().contains("User username authenticated"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.SUCCESS));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.LOW));

    }
}
