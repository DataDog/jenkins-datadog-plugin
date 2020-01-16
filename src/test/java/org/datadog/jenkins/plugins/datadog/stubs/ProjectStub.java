package org.datadog.jenkins.plugins.datadog.stubs;

import hudson.model.ItemGroup;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;

import java.io.IOException;

public class ProjectStub extends Project<ProjectStub, BuildStub> implements TopLevelItem {
    public ProjectStub(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    protected Class<BuildStub> getBuildClass() {
        return null;
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return null;
    }

    public synchronized int assignBuildNumber() throws IOException {
        return 0;
    }
}
