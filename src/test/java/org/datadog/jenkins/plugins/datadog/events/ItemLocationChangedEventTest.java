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

import hudson.model.FreeStyleProject;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemLocationChangedEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        DatadogEvent event = new ItemLocationChangedEventImpl(null, null, null, null);

        Assert.assertTrue(event.getHost().equals(DatadogUtilities.getHostname(null)));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("unknown"));
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User anonymous changed the location of the item unknown"));
        Assert.assertTrue(event.getText().contains("User anonymous changed the location of the item unknown from null to null"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));
    }

    @Test
    public void testWithEverythingSet() throws IOException, InterruptedException {
        FreeStyleProject item = mock(FreeStyleProject.class);
        when(item.getName()).thenReturn("itemname");

        DatadogEvent event = new ItemLocationChangedEventImpl(item, "from_loc", "to_loc",
                new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals(DatadogUtilities.getHostname(null)));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("itemname"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User anonymous changed the location of the item itemname"));
        Assert.assertTrue(event.getText().contains("User anonymous changed the location of the item itemname from from_loc to to_loc"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

    }
}
