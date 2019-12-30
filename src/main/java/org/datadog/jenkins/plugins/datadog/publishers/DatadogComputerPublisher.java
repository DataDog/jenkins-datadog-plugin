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

package org.datadog.jenkins.plugins.datadog.publishers;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.PeriodicWork;
import jenkins.model.Jenkins;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class registers a {@link PeriodicWork} with Jenkins to run periodically in order to enable
 * us to compute metrics related to nodes and executors.
 */
@Extension
public class DatadogComputerPublisher extends PeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogComputerPublisher.class.getName());

    private static final long RECURRENCE_PERIOD = TimeUnit.MINUTES.toMillis(1);

    @Override
    public long getRecurrencePeriod() {
        return RECURRENCE_PERIOD;
    }

    @Override
    protected void doRun() throws Exception {
        try {
            logger.fine("doRun called: Computing Node metrics");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();
            String hostname = DatadogUtilities.getHostname("null");

            long nodeCount = 0;
            long nodeOffline = 0;
            long nodeOnline = 0;
            Computer[] computers = Jenkins.getInstance().getComputers();
            final Map<String, Set<String>> globalTags = DatadogUtilities.getTagsFromGlobalTags();
            for (Computer computer : computers) {
                nodeCount++;
                if (computer.isOffline()) {
                    nodeOffline++;
                }
                if (computer.isOnline()) {
                    nodeOnline++;
                }

                int executorCount = computer.countExecutors();
                int inUse = computer.countBusy();
                int free = computer.countIdle();

                Map<String, Set<String>> tags = TagsUtil.merge(
                        DatadogUtilities.getComputerTags(computer), globalTags);
                client.gauge("jenkins.executor.count", executorCount, hostname, tags);
                client.gauge("jenkins.executor.in_use", inUse, hostname, tags);
                client.gauge("jenkins.executor.free", free, hostname, tags);
            }
            client.gauge("jenkins.node.count", nodeCount, hostname, globalTags);
            client.gauge("jenkins.node.offline", nodeOffline, hostname, globalTags);
            client.gauge("jenkins.node.online", nodeOnline, hostname, globalTags);

        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }

    }

}
