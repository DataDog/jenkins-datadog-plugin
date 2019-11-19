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

        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.Counters.get();

        ConcurrentMetricCounters.Counters.set(new ConcurrentHashMap<CounterMetric, Integer>());

        // Test that both the metric name and tags in the counters cache match
        JSONArray tags = new JSONArray();
        tags.add("job:ParentFullName/JobName");
        tags.add("node:test-node");
        tags.add("result:SUCCESS");
        tags.add("branch:test-branch");

        Assert.assertTrue(counters.containsKey("jenkins.job.completed"));
        Assert.assertTrue(counters.containsValue(1));
        /*
        for (CounterMetric counterMetric: counters.keySet()) {
            System.out.println("In the loop");
            Assert.assertEquals(counterMetric.getMetricName(), "jenkins.job.completed");
            Assert.assertEquals(counterMetric.getTags(), tags);
        }
        */
    }

    @Test
    public void testOnTwoCompletedRuns() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("ParentFullName");

        Job job1 = mock(Job.class);
        when(job1.getParent()).thenReturn(parent);
        when(job1.getName()).thenReturn("JobName");

        Job job2 = mock(Job.class);
        when(job2.getParent()).thenReturn(parent);
        when(job2.getName()).thenReturn("JobName");

        Run run1 = mock(Run.class);
        when(run1.getResult()).thenReturn(Result.SUCCESS);
        Run run2 = mock(Run.class);
        when(run2.getResult()).thenReturn(Result.SUCCESS);

        datadogBuildListener.onCompleted(run1, mock(TaskListener.class));
        datadogBuildListener.onCompleted(run2, mock(TaskListener.class));

        ConcurrentMap<CounterMetric, Integer> counters = ConcurrentMetricCounters.Counters.get();

        ConcurrentMetricCounters.Counters.set(new ConcurrentHashMap<CounterMetric, Integer>());

        // 2 runs
        Assert.assertEquals(ConcurrentMetricCounters.Counters.get().size(), 2);

        // 2 completed jobs
        Assert.assertTrue(counters.containsKey("jenkins.job.completed"));
        Assert.assertTrue(counters.containsValue(2));

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
    public void testMtbf() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);
        DatadogBuildListener listener = new DatadogBuildListener();

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("ParentFullName");

        Job job = mock(Job.class);

        Run lastRun = mock(Run.class);
        Run currentRun = mock(Run.class);

        Assert.assertEquals(when(currentRun.getStartTimeInMillis()).thenReturn(123000L)
                - when(lastRun.getStartTimeInMillis()).thenReturn(120000L), 3000L);
    }

    @Test
    public void testCycleTime() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);

        Job job = mock(Job.class);

        Run lastRun = mock(Run.class);
        Run currentRun = mock(Run.class);

        Assert.assertEquals(when(lastRun.getStartTimeInMillis()).thenReturn(123000L)
                + when(currentRun.getDuration()).thenReturn(120000L)
                - (when(lastRun.getStartTimeInMillis()).thenReturn(10000L) +
                when(lastRun.getStartTimeInMillis()).thenReturn(10000L)), 1000L);
    }

    @Test
    public void testMttf() throws Exception {
        client = new DatadogClientStub();
        datadogBuildListener = mock(DatadogBuildListener.class);
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor(client);
        when(datadogBuildListener.getDescriptor()).thenReturn(descriptorMock);

        Job job = mock(Job.class);

        Run lastRun = mock(Run.class);
        Run currentRun = mock(Run.class);

        Assert.assertEquals(when(currentRun.getStartTimeInMillis()).thenReturn(123000L)
                - when(lastRun.getStartTimeInMillis()).thenReturn(120000L), 3000L);
    }


    private DatadogBuildListener.DescriptorImpl descriptor(DatadogClient client) {
        DatadogBuildListener.DescriptorImpl descriptor = mock(DatadogBuildListener.DescriptorImpl.class);
        when(descriptor.leaseDatadogClient()).thenReturn(client);
        return descriptor;
    }
}
