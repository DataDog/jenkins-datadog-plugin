/*
The MIT License

Copyright (c) 2015-Present Datadog, Inc <opensource@datadoghq.com>
All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package org.datadog.jenkins.plugins.datadog.events;

import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class UserAuthenticationEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        DatadogEvent event = new UserAuthenticationEventImpl(null, null, null);

        Assert.assertTrue(event.getHost().equals(DatadogUtilities.getHostname(null)));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("anonymous"));
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User anonymous did something"));
        Assert.assertTrue(event.getText().contains("User anonymous did something"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.ERROR));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        event = new UserAuthenticationEventImpl(null, "something", null);

        Assert.assertTrue(event.getHost().equals(DatadogUtilities.getHostname(null)));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("anonymous"));
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User anonymous something"));
        Assert.assertTrue(event.getText().contains("User anonymous something"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.ERROR));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));
    }

    @Test
    public void testWithEverythingSet() throws IOException, InterruptedException {
        DatadogEvent event = new UserAuthenticationEventImpl("username", UserAuthenticationEventImpl.ACCESS_DENIED, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals(DatadogUtilities.getHostname(null)));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("username"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username failed to authenticate"));
        Assert.assertTrue(event.getText().contains("User username failed to authenticate"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.ERROR));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

        event = new UserAuthenticationEventImpl("username", UserAuthenticationEventImpl.LOGOUT, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals(DatadogUtilities.getHostname(null)));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("username"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username logout"));
        Assert.assertTrue(event.getText().contains("User username logout"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.SUCCESS));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.LOW));

        event = new UserAuthenticationEventImpl("username", UserAuthenticationEventImpl.LOGIN, new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals(DatadogUtilities.getHostname(null)));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("username"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username authenticated"));
        Assert.assertTrue(event.getText().contains("User username authenticated"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.SUCCESS));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.LOW));

    }
}
