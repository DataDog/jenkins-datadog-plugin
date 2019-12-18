/*
The MIT License

Copyright (c) 2010-2019, Datadog <info@datadoghq.com>
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

package org.datadog.jenkins.plugins.datadog.listeners;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;
import org.datadog.jenkins.plugins.datadog.events.ItemCRUDEventImpl;
import org.datadog.jenkins.plugins.datadog.events.ItemCopiedEventImpl;
import org.datadog.jenkins.plugins.datadog.events.ItemLocationChangedEventImpl;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class registers an {@link ItemListener} to trigger events and calculate metrics:
 * - When an item gets created, the {@link #onCreated(Item)} method will be invoked.
 * - When an item gets copied, the {@link #onCopied(Item, Item)} method will be invoked.
 * - When an item gets deleted, the {@link #onDeleted(Item)} method will be invoked.
 * - When an item gets updated, the {@link #onUpdated(Item)} method will be invoked.
 * - When an item gets their location changed, the {@link #onLocationChanged(Item, String, String)} method will be invoked.
 */
@Extension
public class DatadogItemListener extends ItemListener {

    private static final Logger logger = Logger.getLogger(DatadogItemListener.class.getName());

    @Override
    public void onCreated(Item item) {
        onCRUD(item, ItemCRUDEventImpl.CREATED);
    }

    @Override
    public void onDeleted(Item item) {
        onCRUD(item, ItemCRUDEventImpl.DELETED);
    }

    @Override
    public void onUpdated(Item item) {
        onCRUD(item, ItemCRUDEventImpl.UPDATED);
    }

    private void onCRUD(Item item, String action) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogItemListener#on" + action);

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of global tags to apply
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();

            // Send event
            DatadogEvent event = new ItemCRUDEventImpl(item, action, tags);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.item." + action.toLowerCase(), hostname, tags);

            logger.fine("End DatadogItemListener#on" + action);
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void onCopied(Item src, Item item) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogItemListener#onCopied");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of global tags to apply
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();

            // Send event
            DatadogEvent event = new ItemCopiedEventImpl(src, item, tags);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.item.copied", hostname, tags);

            logger.fine("End DatadogItemListener#onCopied");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Item item, String oldFullName, String newFullName) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogItemListener#onLocationChanged");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of global tags to apply
            Map<String, Set<String>> tags = DatadogUtilities.getTagsFromGlobalTags();

            // Send event
            DatadogEvent event = new ItemLocationChangedEventImpl(item, oldFullName, newFullName, tags);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.item.location_changed", hostname, tags);

            logger.fine("End DatadogItemListener#onLocationChanged");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

}
