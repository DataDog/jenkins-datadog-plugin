package org.datadog.jenkins.plugins.datadog;

import hudson.EnvVars;
import hudson.model.*;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatadogHttpRequests.class, DatadogUtilities.class, Jenkins.class})
public class DatadogBuildListenerTest {
    @Mock
    private Jenkins jenkins;

    private DatadogBuildListener datadogBuildListener;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.isJobTracked(anyString())).thenReturn(true);
        when(DatadogUtilities.assembleTags(any(JSONObject.class), any(HashMap.class))).thenReturn(new JSONArray());
        PowerMockito.mockStatic(DatadogHttpRequests.class);

        datadogBuildListener = spy(new DatadogBuildListener());
        DatadogBuildListener.DescriptorImpl descriptorMock = descriptor();
        when(DatadogUtilities.getDatadogDescriptor()).thenReturn(descriptorMock);
    }

  
    @Test
    public void onCompleted_duration_fromRun() throws Exception {
        Run run = run();
        when(run.getDuration()).thenReturn(123000L);

        
        datadogBuildListener.onCompleted(run, mock(TaskListener.class));

        JSONObject series = capturePostMetricRequestPayload();
        assertEquals("jenkins.job.duration", series.getString("metric"));
        assertEquals(123L, valueOfFirstPoint(series), 0);
    }

    @Test
    public void onCompleted_duration_computedFromFallbackForPipelineJobs() throws Exception {
        Run run = run();
        when(run.getDuration()).thenReturn(0L); // pipeline jobs always return 0

        datadogBuildListener.onCompleted(run, mock(TaskListener.class));
        JSONObject series = capturePostMetricRequestPayload();
        assertEquals("jenkins.job.duration", series.getString("metric"));
        assertNotEquals(0, valueOfFirstPoint(series), 0);
    }



    private Run run() throws Exception {
        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(Result.SUCCESS);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars());

        Job job = job();
        when(run.getParent()).thenReturn(job);

        return run;
    }

    private Job job() {
        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("parent");

        Job job = mock(Job.class);
        when(job.getName()).thenReturn("test-job");
        when(job.getParent()).thenReturn(parent);

        return job;
    }

    private EnvVars envVars() {
        return new EnvVars();
    }

    private DatadogBuildListener.DescriptorImpl descriptor() {
        return mock(DatadogBuildListener.DescriptorImpl.class);
    }

    private JSONObject capturePostMetricRequestPayload() throws IOException {
        PowerMockito.verifyStatic();
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        DatadogHttpRequests.post(captor.capture(), eq(DatadogBuildListener.METRIC));

        return captor.getValue().getJSONArray("series").getJSONObject(0);
    }

    private double valueOfFirstPoint(JSONObject series) {
        return series.getJSONArray("points").getJSONArray(0).getDouble(1);
    }
}
