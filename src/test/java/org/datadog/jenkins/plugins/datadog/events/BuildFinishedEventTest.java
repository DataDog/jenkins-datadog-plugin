package org.datadog.jenkins.plugins.datadog.events;

import hudson.EnvVars;
import hudson.model.*;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.model.BuildData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatadogUtilities.class})
public class BuildFinishedEventTest {

    @Test
    public void testWithNothingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn(null);

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(null);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(null);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);
        BuildData bd = new BuildData(run, listener);
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, null);
        JSONObject o = event.createPayload();

        try {
            o.getString("host");
            Assert.fail(o.getString("host"));
        }catch (JSONException e){
            //continue
        }
        Assert.assertTrue(Objects.equals(o.getString("aggregation_key"), "")); // TODO: IS THIS VALID?
        Assert.assertTrue(o.getLong("date_happened") != 0);
        Assert.assertTrue(o.getJSONArray("tags").size() == 1);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:")); //TODO: IS THIS VALID?
        Assert.assertTrue(Objects.equals(o.getString("source_type_name"), "jenkins"));
        Assert.assertTrue(Objects.equals(o.getString("title"), " build #0 unknown on unknown")); //TODO: IS THIS VALID?
        Assert.assertTrue(o.getString("text").contains( "[See results for build #0](unknown) (0.00 secs)"));
        Assert.assertTrue(Objects.equals(o.getString("alert_type"), "warning"));
        Assert.assertTrue(Objects.equals(o.getString("priority"), "normal"));
    }

    @Test
    public void testWithNothingSet_parentFullName() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("parentFullName");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn(null);

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(null);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(null);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);
        BuildData bd = new BuildData(run, listener);
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, null);
        JSONObject o = event.createPayload();

        Assert.assertTrue(Objects.equals(o.getString("aggregation_key"), "parentFullName/null"));
        Assert.assertTrue(o.getJSONArray("tags").size() == 1);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:parentFullName/null"));
        Assert.assertTrue(Objects.equals(o.getString("title"), "parentFullName/null build #0 unknown on unknown"));
    }

    @Test
    public void testWithNothingSet_parentFullName_2() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("parent»Full  Name");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn(null);

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(null);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(null);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);
        BuildData bd = new BuildData(run, listener);
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, null);
        JSONObject o = event.createPayload();

        Assert.assertTrue(Objects.equals(o.getString("aggregation_key"), "parent/FullName/null"));
        Assert.assertTrue(o.getJSONArray("tags").size() == 1);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:parent/FullName/null"));
        Assert.assertTrue(Objects.equals(o.getString("title"), "parent/FullName/null build #0 unknown on unknown"));
    }

    @Test
    public void testWithNothingSet_jobName() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("parentFullName");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn("jobName");

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(null);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(null);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);
        BuildData bd = new BuildData(run, listener);
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, null);
        JSONObject o = event.createPayload();

        Assert.assertTrue(Objects.equals(o.getString("aggregation_key"), "parentFullName/jobName"));
        Assert.assertTrue(o.getJSONArray("tags").size() == 1);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:parentFullName/jobName"));
        Assert.assertTrue(Objects.equals(o.getString("title"), "parentFullName/jobName build #0 unknown on unknown"));
    }

    @Test
    public void testWithNothingSet_result_failure() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("parentFullName");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn("jobName");

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(Result.FAILURE);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(null);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);
        BuildData bd = new BuildData(run, listener);
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, null);
        JSONObject o = event.createPayload();

        Assert.assertTrue(o.getJSONArray("tags").size() == 2);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:parentFullName/jobName"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(1), "result:FAILURE"));
        Assert.assertTrue(Objects.equals(o.getString("title"), "parentFullName/jobName build #0 failure on unknown"));
        Assert.assertTrue(Objects.equals(o.getString("alert_type"), "error"));
    }

    @Test
    public void testWithNothingSet_result_unstable() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn(null);

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("parentFullName");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn("jobName");

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(Result.UNSTABLE);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(null);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);
        BuildData bd = new BuildData(run, listener);
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, null);
        JSONObject o = event.createPayload();

        Assert.assertTrue(o.getJSONArray("tags").size() == 2);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:parentFullName/jobName"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(1), "result:UNSTABLE"));
        Assert.assertTrue(Objects.equals(o.getString("title"), "parentFullName/jobName build #0 unstable on unknown"));
        Assert.assertTrue(Objects.equals(o.getString("alert_type"), "warning"));
    }

    @Test
    public void testWithEverythingSet() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn("test-hostname-1");

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
        when(run.getDuration()).thenReturn(10l);
        when(run.getNumber()).thenReturn(2);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);

        BuildData bd = new BuildData(run, listener);
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, null);
        JSONObject o = event.createPayload();

        Assert.assertTrue(Objects.equals(o.getString("host"), "test-hostname-1"));
        Assert.assertTrue(Objects.equals(o.getString("aggregation_key"), "ParentFullName/JobName"));
        Assert.assertTrue(o.getLong("date_happened") == 0); //TODO: IS THIS VALID?
        Assert.assertTrue(o.getJSONArray("tags").size() == 4);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:ParentFullName/JobName"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(1), "node:test-node"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(2), "result:SUCCESS"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(3), "branch:test-branch"));
        Assert.assertTrue(Objects.equals(o.getString("source_type_name"), "jenkins"));
        Assert.assertTrue(Objects.equals(o.getString("title"), "ParentFullName/JobName build #2 success on test-hostname-1"));
        Assert.assertTrue(o.getString("text").contains("[See results for build #2](http://build_url.com) (0.00 secs)"));
        Assert.assertTrue(Objects.equals(o.getString("alert_type"), "success"));
        Assert.assertTrue(Objects.equals(o.getString("priority"), "low"));
    }

    @Test
    public void testWithEverythingSet_envVarsAndTags() throws IOException, InterruptedException {
        PowerMockito.mockStatic(DatadogUtilities.class);
        when(DatadogUtilities.getHostname(any(String.class))).thenReturn("test-hostname-1");

        ItemGroup parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("ParentFullName");

        Job job = mock(Job.class);
        when(job.getParent()).thenReturn(parent);
        when(job.getName()).thenReturn("JobName");

        EnvVars envVars = new EnvVars();
        envVars.put("BUILD_URL", "http://build_url.com");
        envVars.put("CVS_BRANCH", "csv-branch");
        envVars.put("SVN_BRANCH", "svn-branch");

        Run run = mock(Run.class);
        when(run.getResult()).thenReturn(Result.SUCCESS);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);
        when(run.getDuration()).thenReturn(10l);
        when(run.getNumber()).thenReturn(2);
        when(run.getParent()).thenReturn(job);

        TaskListener listener = mock(TaskListener.class);

        BuildData bd = new BuildData(run, listener);
        Map<String, String> tags = new HashMap<>();
        tags.put("tag1","value1");
        tags.put("tag2", "value2");
        BuildFinishedEventImpl event = new BuildFinishedEventImpl(bd, tags);
        JSONObject o = event.createPayload();

        Assert.assertTrue(Objects.equals(o.getString("host"), "test-hostname-1"));
        Assert.assertTrue(Objects.equals(o.getString("aggregation_key"), "ParentFullName/JobName"));
        Assert.assertTrue(o.getLong("date_happened") == 0);
        Assert.assertTrue(o.getJSONArray("tags").size() == 5);
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(0), "job:ParentFullName/JobName"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(1), "result:SUCCESS"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(2), "branch:csv-branch"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(3), "tag1:value1"));
        Assert.assertTrue(Objects.equals(o.getJSONArray("tags").getString(4), "tag2:value2"));
        Assert.assertTrue(Objects.equals(o.getString("source_type_name"), "jenkins"));
        Assert.assertTrue(Objects.equals(o.getString("title"), "ParentFullName/JobName build #2 success on test-hostname-1"));
        Assert.assertTrue(o.getString("text").contains("[See results for build #2](http://build_url.com) (0.00 secs)"));
        Assert.assertTrue(Objects.equals(o.getString("alert_type"), "success"));
        Assert.assertTrue(Objects.equals(o.getString("priority"), "low"));
    }
}
