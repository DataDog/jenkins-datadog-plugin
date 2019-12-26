/*
The MIT License

Copyright (c) 2010-2020, Datadog <opensource@datadoghq.com>
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
public class ItemLocationChangedEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.currentTimeMillis()).thenReturn(0l);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);
        when(DatadogUtilities.getUserId()).thenReturn(null);
        when(DatadogUtilities.getItemName(any(Item.class))).thenReturn(null);

        DatadogEvent event = new ItemLocationChangedEventImpl(null, null, null, null);

        Assert.assertTrue(event.getHost() == null);
        Assert.assertTrue(event.getDate() == 0);
        Assert.assertTrue(event.getAggregationKey() == null);
        Assert.assertTrue(event.getTags() == null);
        Assert.assertTrue(event.getTitle().equals("User null changed the location of the item null"));
        Assert.assertTrue(event.getText().contains("User null changed the location of the item null from null to null"));
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

        DatadogEvent event = new ItemLocationChangedEventImpl(null, "from_loc", "to_loc", new HashMap<String, Set<String>>());

        Assert.assertTrue(event.getHost().equals("hostname"));
        Assert.assertTrue(event.getDate() != 0);
        Assert.assertTrue(event.getAggregationKey().equals("itemname"));
        Assert.assertTrue(event.getTags() != null);
        Assert.assertTrue(event.getTitle().equals("User username changed the location of the item itemname"));
        Assert.assertTrue(event.getText().contains("User username changed the location of the item itemname from from_loc to to_loc"));
        Assert.assertTrue(event.getAlertType().equals(DatadogEvent.AlertType.INFO));
        Assert.assertTrue(event.getPriority().equals(DatadogEvent.Priority.NORMAL));

    }
}
