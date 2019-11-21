package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.datadog.jenkins.plugins.datadog.clients.DatadogClientStub;
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
    public void testOnCompletedOnSuccessfulRun() throws Exception {
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

        Run previousSuccessfulRun = mock(Run.class);
        when(previousSuccessfulRun.getResult()).thenReturn(Result.SUCCESS);
        when(previousSuccessfulRun.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
//        when(previousSuccessfulRun.timestamp).thenReturn(1000000L);
        when(previousSuccessfulRun.getDuration()).thenReturn(121000L);
        when(previousSuccessfulRun.getNumber()).thenReturn(1);
        when(previousSuccessfulRun.getParent()).thenReturn(job);

        Run previousFailedRun1 = mock(Run.class);
        when(previousFailedRun1.getResult()).thenReturn(Result.FAILURE);
        when(previousFailedRun1.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
//        when(previousFailedRun1.getStartTimeInMillis()).thenReturn(2000000L);
        when(previousFailedRun1.getDuration()).thenReturn(122000L);
        when(previousFailedRun1.getNumber()).thenReturn(2);
        when(previousFailedRun1.getParent()).thenReturn(job);
        when(previousFailedRun1.getPreviousBuiltBuild()).thenReturn(previousSuccessfulRun);
        when(previousFailedRun1.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);

        Run previousFailedRun2 = mock(Run.class);
        when(previousFailedRun2.getResult()).thenReturn(Result.FAILURE);
        when(previousFailedRun2.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
//        when(previousFailedRun2.getStartTimeInMillis()).thenReturn(3000000L);
        when(previousFailedRun2.getDuration()).thenReturn(123000L);
        when(previousFailedRun2.getNumber()).thenReturn(3);
        when(previousFailedRun2.getParent()).thenReturn(job);
        when(previousFailedRun2.getPreviousBuiltBuild()).thenReturn(previousFailedRun1);
        when(previousFailedRun2.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);

        Run failedRun = mock(Run.class);
        when(failedRun.getResult()).thenReturn(Result.SUCCESS);
        when(failedRun.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(failedRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);
        when(failedRun.getDuration()).thenReturn(124000L);
//        when(failedRun.getStartTimeInMillis()).thenReturn(4000000L);
        when(failedRun.getNumber()).thenReturn(4);
        when(failedRun.getParent()).thenReturn(job);
        when(failedRun.getPreviousBuiltBuild()).thenReturn(previousFailedRun2);
        when(failedRun.getPreviousSuccessfulBuild()).thenReturn(previousSuccessfulRun);

        datadogBuildListener.onCompleted(previousSuccessfulRun, mock(TaskListener.class));
        String[] expectedTags1 = new String[4];
        expectedTags1[0] = "job:ParentFullName/JobName";
        expectedTags1[1] = "node:test-node";
        expectedTags1[2] = "result:SUCCESS";
        expectedTags1[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 121, "null", expectedTags1);
        client.assertMetric("jenkins.job.leadtime", 121, "null", expectedTags1);
        client.assertServiceCheck("jenkins.job.status", 0, "null", expectedTags1);

        datadogBuildListener.onCompleted(previousFailedRun1, mock(TaskListener.class));
        String[] expectedTags2 = new String[4];
        expectedTags2[0] = "job:ParentFullName/JobName";
        expectedTags2[1] = "node:test-node";
        expectedTags2[2] = "result:FAILURE";
        expectedTags2[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 122, "null", expectedTags2);
//        client.assertMetric("jenkins.job.mtbf", 2000-1000, "null", expectedTags2);
        client.assertMetric("jenkins.job.feedbacktime", 122, "null", expectedTags2);
        client.assertServiceCheck("jenkins.job.status", 2, "null", expectedTags2);

        datadogBuildListener.onCompleted(previousFailedRun2, mock(TaskListener.class));
        client.assertMetric("jenkins.job.duration", 123, "null", expectedTags2);
//        client.assertMetric("jenkins.job.mtbf", 3000-1000, "null", expectedTags2);
        client.assertMetric("jenkins.job.feedbacktime", 123, "null", expectedTags2);
        client.assertMetric("jenkins.job.completed", 2, "null", expectedTags2);
        client.assertServiceCheck("jenkins.job.status", 2, "null", expectedTags2);

        datadogBuildListener.onCompleted(failedRun, mock(TaskListener.class));
        client.assertMetric("jenkins.job.duration", 124, "null", expectedTags1);
        client.assertMetric("jenkins.job.leadtime", 124, "null", expectedTags1);
        client.assertMetric("jenkins.job.cycletime", (0+124)-(0+121), "null", expectedTags1);
//        client.assertMetric("jenkins.job.mttr", 4000-2000, "null", expectedTags1);
        client.assertServiceCheck("jenkins.job.status", 0, "null", expectedTags1);
        client.assertMetric("jenkins.job.completed", 2, "null", expectedTags1);
        client.assertedAllMetricsAndServiceChecks();

    }

    @Test
    public void testOnCompletedOnFailedRun() throws Exception {
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

        Run previousSuccessfulRun = mock(Run.class);
        when(previousSuccessfulRun.getResult()).thenReturn(Result.SUCCESS);
        when(previousSuccessfulRun.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(previousSuccessfulRun.getDuration()).thenReturn(123000L);
//        when(previousSuccessfulRun.getStartTimeInMillis()).thenReturn(1000000L);
        when(previousSuccessfulRun.getNumber()).thenReturn(1);
        when(previousSuccessfulRun.getParent()).thenReturn(job);

        Run failedRun = mock(Run.class);
        when(failedRun.getResult()).thenReturn(Result.FAILURE);
        when(failedRun.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(failedRun.getDuration()).thenReturn(124000L);
//        when(failedRun.getStartTimeInMillis()).thenReturn(2000000L);
        when(failedRun.getNumber()).thenReturn(2);
        when(failedRun.getParent()).thenReturn(job);
        when(failedRun.getPreviousNotFailedBuild()).thenReturn(previousSuccessfulRun);

        datadogBuildListener.onCompleted(previousSuccessfulRun, mock(TaskListener.class));
        String[] expectedTags1 = new String[4];
        expectedTags1[0] = "job:ParentFullName/JobName";
        expectedTags1[1] = "node:test-node";
        expectedTags1[2] = "result:SUCCESS";
        expectedTags1[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 123, "null", expectedTags1);
        client.assertMetric("jenkins.job.leadtime", 123, "null", expectedTags1);
        client.assertMetric("jenkins.job.completed", 1, "null", expectedTags1);
        client.assertServiceCheck("jenkins.job.status", 0, "null", expectedTags1);
        client.assertedAllMetricsAndServiceChecks();

        datadogBuildListener.onCompleted(failedRun, mock(TaskListener.class));
        String[] expectedTags2 = new String[4];
        expectedTags2[0] = "job:ParentFullName/JobName";
        expectedTags2[1] = "node:test-node";
        expectedTags2[2] = "result:FAILURE";
        expectedTags2[3] = "branch:test-branch";
        client.assertMetric("jenkins.job.duration", 124, "null", expectedTags2);
//        client.assertMetric("jenkins.job.mtbf", 2000, "null", expectedTags2);
        client.assertMetric("jenkins.job.feedbacktime", 124, "null", expectedTags2);
        client.assertMetric("jenkins.job.completed", 1, "null", expectedTags2);
        client.assertServiceCheck("jenkins.job.status", 2, "null", expectedTags2);
        client.assertedAllMetricsAndServiceChecks();
    }

    // TODO onCreated

    private DatadogBuildListener.DescriptorImpl descriptor(DatadogClient client) {
        DatadogBuildListener.DescriptorImpl descriptor = mock(DatadogBuildListener.DescriptorImpl.class);
        when(descriptor.leaseDatadogClient()).thenReturn(client);
        return descriptor;
    }
}
