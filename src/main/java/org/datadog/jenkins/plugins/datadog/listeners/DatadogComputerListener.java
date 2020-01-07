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

package org.datadog.jenkins.plugins.datadog.listeners;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogEvent;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;
import org.datadog.jenkins.plugins.datadog.events.*;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class registers an {@link ComputerListener} to trigger events and calculate metrics:
 * - When a computer gets online, the {@link #onOnline(Computer, TaskListener)} method will be invoked.
 * - When a computer gets offline, the {@link #onOffline(Computer, OfflineCause)} method will be invoked.
 * - When a computer gets temporarily online, the {@link #onTemporarilyOnline(Computer)} method will be invoked.
 * - When a computer gets temporarily offline, the {@link #onTemporarilyOffline(Computer, OfflineCause)} method will be invoked.
 * - When a computer failed to launch, the {@link #onLaunchFailure(Computer, TaskListener)} method will be invoked.
 */
@Extension
public class DatadogComputerListener extends ComputerListener {

    private static final Logger logger = Logger.getLogger(DatadogComputerListener.class.getName());

    @Override
    public void onOnline(Computer computer, TaskListener listener) throws IOException, InterruptedException {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogComputerListener#onOnline");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of tags to apply
            Map<String, Set<String>> tags = TagsUtil.merge(
                    DatadogUtilities.getTagsFromGlobalTags(),
                    DatadogUtilities.getComputerTags(computer));

            // Send event
            DatadogEvent event = new ComputerOnlineEventImpl(computer, listener, tags, false);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.computer.online", hostname, tags);

            logger.fine("End DatadogComputerListener#onOnline");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void onOffline(@Nonnull Computer computer, @CheckForNull OfflineCause cause) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogComputerListener#onOffline");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of tags to apply
            Map<String, Set<String>> tags = TagsUtil.merge(
                    DatadogUtilities.getTagsFromGlobalTags(),
                    DatadogUtilities.getComputerTags(computer));

            // Send event
            DatadogEvent event = new ComputerOfflineEventImpl(computer, cause, tags, false);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.computer.offline", hostname, tags);

            logger.fine("End DatadogComputerListener#onOffline");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void onTemporarilyOnline(Computer computer) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogComputerListener#onTemporarilyOnline");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of tags to apply
            Map<String, Set<String>> tags = TagsUtil.merge(
                    DatadogUtilities.getTagsFromGlobalTags(),
                    DatadogUtilities.getComputerTags(computer));

            // Send event
            DatadogEvent event = new ComputerOnlineEventImpl(computer, null, tags, true);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.computer.temporarily_online", hostname, tags);

            logger.fine("End DatadogComputerListener#onTemporarilyOnline");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void onTemporarilyOffline(Computer computer, OfflineCause cause) {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogComputerListener#onTemporarilyOffline");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of tags to apply
            Map<String, Set<String>> tags = TagsUtil.merge(
                    DatadogUtilities.getTagsFromGlobalTags(),
                    DatadogUtilities.getComputerTags(computer));

            // Send event
            DatadogEvent event = new ComputerOfflineEventImpl(computer, cause, tags, true);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.computer.temporarily_offline", hostname, tags);

            logger.fine("End DatadogComputerListener#onTemporarilyOffline");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

    @Override
    public void onLaunchFailure(Computer computer, TaskListener taskListener) throws IOException, InterruptedException {
        try {
            final boolean emitSystemEvents = DatadogUtilities.getDatadogGlobalDescriptor().isEmitSystemEvents();
            if (!emitSystemEvents) {
                return;
            }
            logger.fine("Start DatadogComputerListener#onLaunchFailure");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();

            // Get the list of tags to apply
            Map<String, Set<String>> tags = TagsUtil.merge(
                    DatadogUtilities.getTagsFromGlobalTags(),
                    DatadogUtilities.getComputerTags(computer));

            // Send event
            DatadogEvent event = new ComputerLaunchFailedEventImpl(computer, taskListener, tags);
            client.event(event);

            // Submit counter
            String hostname = DatadogUtilities.getHostname("null");
            client.incrementCounter("jenkins.computer.launch_failure", hostname, tags);

            logger.fine("End DatadogComputerListener#onLaunchFailure");
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }

}
