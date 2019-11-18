package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import org.datadog.jenkins.plugins.datadog.clients.DatadogClientStub;
import org.datadog.jenkins.plugins.datadog.model.ConcurrentMetricCounters;
import org.datadog.jenkins.plugins.datadog.model.CounterMetric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatadogUtilities.class, Jenkins.class})
public class DatadogBuildListenerTest {
    @Mock
    private Jenkins jenkins;

    private DatadogClientStub client;

    public DatadogBuildListener datadogBuildListener;

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
        when(datadogBuildListener.getMeanTimeBetweenFailure(run)).thenReturn(123000L);
        when(datadogBuildListener.getCycleTime(run)).thenReturn(123000L);
        when(datadogBuildListener.getMeanTimeToRecovery(run)).thenReturn(123000L);

        // First run
        datadogBuildListener.onCompleted(run, mock(TaskListener.class));
        Assert.assertEquals(ConcurrentMetricCounters.Counters.get().size(), 1);

        String[] expectedTags = new String[4];
        expectedTags[0] = "job:ParentFullName/JobName";
        expectedTags[1] = "node:test-node";
        expectedTags[2] = "result:SUCCESS";
        expectedTags[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 123, "null", expectedTags);
        client.assertMetric("jenkins.job.leadtime", 246, "null", expectedTags);
        client.assertMetric("jenkins.job.cycletime", 123, "null", expectedTags);
        client.assertMetric("jenkins.job.mttr", 123, "null", expectedTags);
        client.assertServiceCheck("jenkins.job.status", 0, "null", expectedTags);
        client.assertedAllMetricsAndServiceChecks();

        // Second run
        datadogBuildListener.onCompleted(run, mock(TaskListener.class));
        Assert.assertEquals(ConcurrentMetricCounters.Counters.get().size(), 1);

        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.Counters.get();

        ConcurrentMetricCounters.Counters.set(new ConcurrentHashMap<CounterMetric, Integer>());

        // Test that both the metric name and tags in the counters cache match
        JSONArray tags = new JSONArray();
        tags.add("job:ParentFullName/JobName");
        tags.add("node:test-node");
        tags.add("result:SUCCESS");
        tags.add("branch:test-branch");

        for (CounterMetric counterMetric: counters.keySet()) {
            Assert.assertEquals(counterMetric.getMetricName(), "jenkins.job.completed");
            Assert.assertEquals(counterMetric.getTags(), tags);
        }

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
        Assert.assertEquals(ConcurrentMetricCounters.Counters.get().size(), 1);

        String[] expectedTags = new String[4];
        expectedTags[0] = "job:ParentFullName/JobName";
        expectedTags[1] = "node:test-node";
        expectedTags[2] = "result:SUCCESS";
        expectedTags[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 0, "null", expectedTags);
        client.assertMetric("jenkins.job.leadtime", 0, "null", expectedTags);
        client.assertServiceCheck("jenkins.job.status", 0, "null", expectedTags);
        client.assertedAllMetricsAndServiceChecks();
    }

    @Test
    public void testOnFailures() throws Exception {
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

        Run runFailures = mock(Run.class);
        when(runFailures.getResult()).thenReturn(Result.FAILURE);
        when(runFailures.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(runFailures.getDuration()).thenReturn(123000L);
        when(runFailures.getNumber()).thenReturn(2);
        when(runFailures.getParent()).thenReturn(job);
        when(datadogBuildListener.getMeanTimeBetweenFailure(runFailures)).thenReturn(123000L);

        datadogBuildListener.onCompleted(runFailures, mock(TaskListener.class));

        String[] expectedTags = new String[4];
        expectedTags[0] = "job:ParentFullName/JobName";
        expectedTags[1] = "node:test-node";
        expectedTags[2] = "result:FAILURE";
        expectedTags[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 123, "null", expectedTags);
        client.assertMetric("jenkins.job.feedbacktime", 123, "null", expectedTags);
        client.assertMetric("jenkins.job.mtbf", 123, "null", expectedTags);
        client.assertServiceCheck("jenkins.job.status", 2, "null", expectedTags);
    }

    @Test
    public void testJenkinsRuns() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);

        // Mock returned values of getPreviousNotFailedBuild(), getPreviousSuccessfulBuild() and getPreviousBuiltBuild()
        // Mock 2 runs
        DatadogBuildListener listener = new DatadogBuildListener();
        Run lastRun = mock(Run.class);
        Run previousRun = mock(Run.class);

        // getPreviousNotFailedBuild()
        when(lastRun.getPreviousNotFailedBuild()).thenReturn(previousRun);
        Assert.assertNotNull(listener.getMeanTimeBetweenFailure(lastRun));

        // getPreviousSuccessfulBuild()
        when(lastRun.getPreviousSuccessfulBuild()).thenReturn(previousRun);
        Assert.assertNotNull(listener.getCycleTime(lastRun));

        // getPreviousBuiltBuild()
        when(lastRun.getPreviousSuccessfulBuild()).thenReturn(previousRun);
        Assert.assertNotNull(listener.getMeanTimeToRecovery(lastRun));
    }

    private DatadogBuildListener.DescriptorImpl descriptor(DatadogClient client) {
        DatadogBuildListener.DescriptorImpl descriptor = mock(DatadogBuildListener.DescriptorImpl.class);
        when(descriptor.leaseDatadogClient()).thenReturn(client);
        return descriptor;
    }
}
