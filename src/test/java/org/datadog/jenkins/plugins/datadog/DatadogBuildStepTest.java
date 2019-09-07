package org.datadog.jenkins.plugins.datadog;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hamcrest.collection.IsMapContaining;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class})
public class DatadogBuildStepTest {
	@Mock
	private Jenkins jenkins;

	private DatadogBuildStep datadogBuildStep;

	@Before
	public void setUp() throws Exception {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
	}

	@Test
	public void perform() throws Exception {
		Run run = run();
		FilePath workspace = new FilePath(new File("/path/to/workspace"));
		Launcher launcher = mock(Launcher.class);
        TaskListener taskListener = mock(TaskListener.class);

        String pipelineTags = "myTag1=val1 myTag2=val2 myTag3=val3";
        String jobName = run.getParent().getFullName();

        String badPipelineTags = "tag1=val1, tag2=val2, tag3=val3";
        String fakeJobName = "parent/fake-job";

        datadogBuildStep = spy(new DatadogBuildStep(pipelineTags));
		datadogBuildStep.perform(run, workspace, launcher, taskListener);

		Map<String,String> expectedTagPool = new ConcurrentHashMap<>();
		expectedTagPool.put(jobName, pipelineTags);

		Map<String,String> notExpectedTagPool = new ConcurrentHashMap<>();
		notExpectedTagPool.put(fakeJobName, badPipelineTags);

		assertThat(datadogBuildStep.tagPool, is(expectedTagPool));
		assertThat(datadogBuildStep.tagPool, IsMapContaining.hasKey(jobName));
		assertThat(datadogBuildStep.tagPool, IsMapContaining.hasValue(pipelineTags));

		assertThat(datadogBuildStep.tagPool, not(is(notExpectedTagPool)));
		assertThat(datadogBuildStep.tagPool, not(IsMapContaining.hasValue(badPipelineTags)));
		assertThat(datadogBuildStep.tagPool, not(IsMapContaining.hasKey(fakeJobName)));

	}

	private Run run() throws Exception {
        Run run = mock(Run.class);

        Job job = job();
        when(run.getParent()).thenReturn(job);

        return run;
    }

    private Job job() {
        ItemGroup<?> parent = mock(ItemGroup.class);
        when(parent.getFullName()).thenReturn("parent");

        Job job = mock(Job.class);
        when(job.getName()).thenReturn("test-job");
        when(job.getParent()).thenReturn(parent);

        return job;
    }
}