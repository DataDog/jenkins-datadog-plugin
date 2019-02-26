package org.datadog.jenkins.plugins.datadog;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatadogBuildStep extends Builder implements SimpleBuildStep {
    public static Map<String,String> tagPool = new ConcurrentHashMap<>();
    private final String tags;

    @DataBoundConstructor
    public DatadogBuildStep(String tags) {
        this.tags = tags;
    }

    public String getTags() {
        return tags;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        String jobName = run.getParent().getFullName();
        tagPool.put(jobName, tags);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getDisplayName() {
            return "DataDog Tagging";
        }
    }
}
