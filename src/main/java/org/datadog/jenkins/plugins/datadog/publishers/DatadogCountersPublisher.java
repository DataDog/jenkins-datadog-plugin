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
import hudson.model.*;
import org.datadog.jenkins.plugins.datadog.DatadogClient;
import org.datadog.jenkins.plugins.datadog.clients.ClientFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Extension
public class DatadogCountersPublisher extends AsyncPeriodicWork {

    private static final Logger logger = Logger.getLogger(DatadogCountersPublisher.class.getName());

    public DatadogCountersPublisher() {
        super("Datadog Counters Publisher");
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.SECONDS.toMillis(10);
    }

    @Override
    protected void execute(TaskListener taskListener) throws IOException, InterruptedException {
        try {
            logger.fine("Execute called: Publishing counters");

            // Get Datadog Client Instance
            DatadogClient client = ClientFactory.getClient();
            client.flushCounters();
        } catch (Exception e) {
            logger.warning("Unexpected exception occurred - " + e.getMessage());
        }
    }
}
