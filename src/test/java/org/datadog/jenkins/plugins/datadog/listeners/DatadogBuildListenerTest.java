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

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.datadog.jenkins.plugins.datadog.stubs.BuildStub;
import org.datadog.jenkins.plugins.datadog.stubs.ProjectStub;
import org.datadog.jenkins.plugins.datadog.clients.DatadogClientStub;
import org.datadog.jenkins.plugins.datadog.clients.DatadogMetric;
import org.datadog.jenkins.plugins.datadog.stubs.QueueStub;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DatadogBuildListenerTest {

    @Test
    public void testOnCompletedWithNothing() throws Exception {
        DatadogClientStub client = new DatadogClientStub();
        DatadogBuildListener datadogBuildListener = new DatadogBuildListenerTestWrapper();
        ((DatadogBuildListenerTestWrapper)datadogBuildListener).setDatadogClient(client);

        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getFullName()).thenReturn(null);

        ProjectStub job = new ProjectStub(jenkins,null);

        EnvVars envVars = new EnvVars();

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(null);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(run.getParent()).thenReturn(job);

        datadogBuildListener.onCompleted(run, mock(TaskListener.class));

        client.assertedAllMetricsAndServiceChecks();

    }

    @Test
    public void testOnCompletedOnSuccessfulRun() throws Exception {
        DatadogClientStub client = new DatadogClientStub();
        DatadogBuildListener datadogBuildListener = new DatadogBuildListenerTestWrapper();
        ((DatadogBuildListenerTestWrapper)datadogBuildListener).setDatadogClient(client);

        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getFullName()).thenReturn("ParentFullName");

        ProjectStub project = new ProjectStub(jenkins,"JobName");

        EnvVars envVars = new EnvVars();
        envVars.put("HOSTNAME", "test-hostname-2");
        envVars.put("NODE_NAME", "test-node");
        envVars.put("BUILD_URL", "http://build_url.com");
        envVars.put("GIT_BRANCH", "test-branch");

        BuildStub previousSuccessfulRun = new BuildStub(project, Result.SUCCESS, envVars, null,
                121000L, 1, null, 1000000L, null);

        BuildStub previousFailedRun1 = new BuildStub(project, Result.FAILURE, envVars, previousSuccessfulRun,
                122000L, 2, previousSuccessfulRun, 2000000L, null);

        BuildStub previousFailedRun2 = new BuildStub(project, Result.FAILURE, envVars, previousSuccessfulRun,
                123000L, 3, previousFailedRun1, 3000000L, null);

        BuildStub successRun = new BuildStub(project, Result.SUCCESS, envVars, previousSuccessfulRun,
                124000L, 4, previousFailedRun2, 4000000L, null);

        datadogBuildListener.onCompleted(previousSuccessfulRun, mock(TaskListener.class));
        String[] expectedTags1 = new String[4];
        expectedTags1[0] = "job:ParentFullName/JobName";
        expectedTags1[1] = "node:test-node";
        expectedTags1[2] = "result:SUCCESS";
        expectedTags1[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 121, "test-hostname-2", expectedTags1);
        client.assertMetric("jenkins.job.leadtime", 121, "test-hostname-2", expectedTags1);
        client.assertServiceCheck("jenkins.job.status", 0, "test-hostname-2", expectedTags1);

        datadogBuildListener.onCompleted(previousFailedRun1, mock(TaskListener.class));
        String[] expectedTags2 = new String[4];
        expectedTags2[0] = "job:ParentFullName/JobName";
        expectedTags2[1] = "node:test-node";
        expectedTags2[2] = "result:FAILURE";
        expectedTags2[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 122, "test-hostname-2", expectedTags2);
        client.assertMetric("jenkins.job.feedbacktime", 122, "test-hostname-2", expectedTags2);
        client.assertServiceCheck("jenkins.job.status", 2, "test-hostname-2", expectedTags2);

        datadogBuildListener.onCompleted(previousFailedRun2, mock(TaskListener.class));
        client.assertMetric("jenkins.job.duration", 123, "test-hostname-2", expectedTags2);
        client.assertMetric("jenkins.job.feedbacktime", 123, "test-hostname-2", expectedTags2);
        client.assertMetric("jenkins.job.completed", 2, "test-hostname-2", expectedTags2);
        client.assertServiceCheck("jenkins.job.status", 2, "test-hostname-2", expectedTags2);

        datadogBuildListener.onCompleted(successRun, mock(TaskListener.class));
        client.assertMetric("jenkins.job.duration", 124, "test-hostname-2", expectedTags1);
        client.assertMetric("jenkins.job.leadtime", 2124, "test-hostname-2", expectedTags1);
        client.assertMetric("jenkins.job.cycletime", (4000+124)-(1000+121), "test-hostname-2", expectedTags1);
        client.assertMetric("jenkins.job.mttr", 4000-2000, "test-hostname-2", expectedTags1);
        client.assertServiceCheck("jenkins.job.status", 0, "test-hostname-2", expectedTags1);
        client.assertMetric("jenkins.job.completed", 2, "test-hostname-2", expectedTags1);
        client.assertedAllMetricsAndServiceChecks();
    }

    @Test
    public void testOnCompletedOnFailedRun() throws Exception {
        DatadogClientStub client = new DatadogClientStub();
        DatadogBuildListener datadogBuildListener = new DatadogBuildListenerTestWrapper();
        ((DatadogBuildListenerTestWrapper)datadogBuildListener).setDatadogClient(client);

        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getFullName()).thenReturn("ParentFullName");

        ProjectStub project = new ProjectStub(jenkins,"JobName");

        EnvVars envVars = new EnvVars();
        envVars.put("HOSTNAME", "test-hostname-2");
        envVars.put("NODE_NAME", "test-node");
        envVars.put("BUILD_URL", "http://build_url.com");
        envVars.put("GIT_BRANCH", "test-branch");

        BuildStub previousSuccessfulRun = new BuildStub(project, Result.SUCCESS, envVars, null,
                123000L, 1, null, 1000000L, null);

        BuildStub failedRun = new BuildStub(project, Result.FAILURE, envVars, null,
                124000L, 2, null, 2000000L, previousSuccessfulRun);;

        datadogBuildListener.onCompleted(previousSuccessfulRun, mock(TaskListener.class));
        String[] expectedTags1 = new String[4];
        expectedTags1[0] = "job:ParentFullName/JobName";
        expectedTags1[1] = "node:test-node";
        expectedTags1[2] = "result:SUCCESS";
        expectedTags1[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 123, "test-hostname-2", expectedTags1);
        client.assertMetric("jenkins.job.leadtime", 123, "test-hostname-2", expectedTags1);
        client.assertMetric("jenkins.job.completed", 1, "test-hostname-2", expectedTags1);
        client.assertServiceCheck("jenkins.job.status", 0, "test-hostname-2", expectedTags1);
        client.assertedAllMetricsAndServiceChecks();

        datadogBuildListener.onCompleted(failedRun, mock(TaskListener.class));
        String[] expectedTags2 = new String[4];
        expectedTags2[0] = "job:ParentFullName/JobName";
        expectedTags2[1] = "node:test-node";
        expectedTags2[2] = "result:FAILURE";
        expectedTags2[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 124, "test-hostname-2", expectedTags2);
        client.assertMetric("jenkins.job.mtbf", 1000, "test-hostname-2", expectedTags2);
        client.assertMetric("jenkins.job.feedbacktime", 124, "test-hostname-2", expectedTags2);
        client.assertMetric("jenkins.job.completed", 1, "test-hostname-2", expectedTags2);
        client.assertServiceCheck("jenkins.job.status", 2, "test-hostname-2", expectedTags2);
        client.assertedAllMetricsAndServiceChecks();
    }

    @Test
    public void testOnStarted() throws Exception {
        DatadogClientStub client = new DatadogClientStub();
        DatadogBuildListener datadogBuildListener = new DatadogBuildListenerTestWrapper();
        ((DatadogBuildListenerTestWrapper)datadogBuildListener).setDatadogClient(client);
        Queue queue = new QueueStub(mock(LoadBalancer.class));
        ((DatadogBuildListenerTestWrapper)datadogBuildListener).setQueue(queue);

        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getFullName()).thenReturn("ParentFullName");

        ProjectStub project = new ProjectStub(jenkins,"JobName");

        Queue.Item item = mock(Queue.Item.class);
        when(item.getId()).thenReturn(1L);
        when(item.getInQueueSince()).thenReturn(2000000L);

        ((QueueStub)queue).setItem(item);

        EnvVars envVars = new EnvVars();
        envVars.put("HOSTNAME", "test-hostname-2");
        envVars.put("NODE_NAME", "test-node");
        envVars.put("BUILD_URL", "http://build_url.com");
        envVars.put("GIT_BRANCH", "test-branch");

        BuildStub run = new BuildStub(project, Result.SUCCESS, envVars, null,
                123000L, 1, null, 1000000L, null);

        datadogBuildListener.onStarted(run, mock(TaskListener.class));
        String[] expectedTags = new String[4];
        expectedTags[0] = "job:ParentFullName/JobName";
        expectedTags[1] = "node:test-node";
        expectedTags[2] = "result:SUCCESS";
        expectedTags[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.started", 1, "test-hostname-2", expectedTags);
        Assert.assertTrue(client.metrics.size() == 1);
        DatadogMetric metric = client.metrics.get(0);
        Assert.assertTrue(metric.getName().equals("jenkins.job.waiting"));
        Assert.assertTrue(metric.getValue() > 0);
        Assert.assertTrue(metric.getHostname().equals("test-hostname-2"));
        Assert.assertTrue(metric.getTags().containsAll(Arrays.asList(expectedTags)));
        client.metrics.clear();
        client.assertedAllMetricsAndServiceChecks();
    }
}
