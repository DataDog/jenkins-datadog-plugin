package org.datadog.jenkins.plugins.datadog;

import com.timgroup.statsd.StatsDClient;
import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.datadog.jenkins.plugins.datadog.clients.DatadogClientStub;
import org.datadog.jenkins.plugins.datadog.clients.DatadogStatsDClientStub;
import org.datadog.jenkins.plugins.datadog.model.LocalCacheCounters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatadogUtilities.class, Jenkins.class})
public class DatadogBuildListenerTest {
    @Mock
    private Jenkins jenkins;

    private DatadogClientStub client;
    private DatadogStatsDClientStub statsd;

    private DatadogBuildListener datadogBuildListener;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.isJobTracked(anyString())).thenReturn(true);
        when(DatadogUtilities.isApiKeyNull()).thenReturn(false);
        when(DatadogUtilities.isTagNodeEnable()).thenReturn(true);
    }

    @Test
    public void testOnCompletedWithNothing() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);

        EnvVars envVars = new EnvVars();

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(null);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(run.getParent()).thenReturn(job);

        datadogBuildListener.onCompleted(run, mock(TaskListener.class));

        client.assertedAllMetricsAndServiceChecks();

    }

    @Test
    public void testOnCompletedWithEverything() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("ParentFullName");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn("JobName");

        EnvVars envVars = new EnvVars();
        envVars.put("HOSTNAME", "test-hostname-2");
        envVars.put("NODE_NAME", "test-node");
        envVars.put("BUILD_URL", "http://build_url.com");
        envVars.put("GIT_BRANCH", "test-branch");

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(Result.SUCCESS);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(run.getDuration()).thenReturn(123000L);
        when(run.getNumber()).thenReturn(2);
        when(run.getParent()).thenReturn(job);

        datadogBuildListener.onCompleted(run, mock(TaskListener.class));
        Assert.assertEquals(LocalCacheCounters.Cache.get().size(), 1);

        String[] expectedTags = new String[4];
        expectedTags[0] = "job:ParentFullName/JobName";
        expectedTags[1] = "node:test-node";
        expectedTags[2] = "result:SUCCESS";
        expectedTags[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 123, "null", expectedTags);
        // client.assertMetric("jenkins.job.completed", 1.0, "null", expectedTags);
        client.assertServiceCheck("jenkins.job.status", 0, "null", expectedTags);
        // client.assertedAllMetricsAndServiceChecks();

    }

    @Test
    public void testOnCompletedWithDurationAsZero() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("ParentFullName");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn("JobName");

        EnvVars envVars = new EnvVars();
        envVars.put("HOSTNAME", "test-hostname-2");
        envVars.put("NODE_NAME", "test-node");
        envVars.put("BUILD_URL", "http://build_url.com");
        envVars.put("GIT_BRANCH", "test-branch");

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(Result.SUCCESS);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(run.getDuration()).thenReturn(0L); // pipeline jobs always return 0
        when(run.getNumber()).thenReturn(2);
        when(run.getParent()).thenReturn(job);

        datadogBuildListener.onCompleted(run, mock(TaskListener.class));
        Assert.assertEquals(LocalCacheCounters.Cache.get().size(), 1);

        String[] expectedTags = new String[4];
        expectedTags[0] = "job:ParentFullName/JobName";
        expectedTags[1] = "node:test-node";
        expectedTags[2] = "result:SUCCESS";
        expectedTags[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 0, "null", expectedTags);
        // client.assertMetric("jenkins.job.completed", 1.0, "null", expectedTags);
        client.assertServiceCheck("jenkins.job.status", 0, "null", expectedTags);
        // client.assertedAllMetricsAndServiceChecks();
    }

    private DatadogBuildListener.DescriptorImpl descriptor(DatadogClient client) {
        DatadogBuildListener.DescriptorImpl descriptor = mock(DatadogBuildListener.DescriptorImpl.class);
        when(descriptor.leaseDatadogClient()).thenReturn(client);
        return descriptor;
    }
}
