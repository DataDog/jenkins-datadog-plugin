/*
The MIT License

Copyright (c) 2010-2019, Datadog <info@datadoghq.com>
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

package org.datadog.jenkins.plugins.datadog.model;

import hudson.EnvVars;
import hudson.model.*;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.datadog.jenkins.plugins.datadog.DatadogUtilities;
import org.datadog.jenkins.plugins.datadog.util.TagsUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BuildData {

    private String buildNumber;
    private String buildId;
    private String buildUrl;
    private String nodeName;
    private String jobName;
    private String buildTag;
    private String jenkinsUrl;
    private String executorNumber;
    private String javaHome;
    private String workspace;
    // Branch contains either env variable - SVN_REVISION or CVS_BRANCH or GIT_BRANCH
    private String branch;
    private String gitUrl;
    private String gitCommit;
    // Environment variable from the promoted build plugin
    // - See https://plugins.jenkins.io/promoted-builds
    // - See https://wiki.jenkins.io/display/JENKINS/Promoted+Builds+Plugin
    private String promotedUrl;
    private String promotedJobName;
    private String promotedNumber;
    private String promotedId;
    private String promotedTimestamp;
    private String promotedUserName;
    private String promotedUserId;
    private String promotedJobFullName;

    private String result;
    private String hostname;
    private String userId;
    private Map<String, Set<String>> tags;

    private Long startTime;
    private Long endTime;
    private Long duration;

    public BuildData(Run run, TaskListener listener) throws IOException, InterruptedException {
        if (run == null) {
            return;
        }
        EnvVars envVars = null;
        if(listener != null){
            envVars = run.getEnvironment(listener);
            setTags(DatadogUtilities.getBuildTags(run, listener));
        }

        // Populate instance using environment variables.
        populateEnvVariables(envVars);

        // Populate instance using run instance
        // Set StartTime, EndTime and Duration
        long startTimeInMs = run.getStartTimeInMillis();
        setStartTime(startTimeInMs);
        long durationInMs = run.getDuration();
        if (durationInMs == 0 && startTimeInMs != 0) {
            durationInMs = System.currentTimeMillis() - startTimeInMs;
        }
        setDuration(durationInMs);
        if (durationInMs != 0 && startTimeInMs != 0) {
            Long endTimeInMs = startTimeInMs + durationInMs;
            setEndTime(endTimeInMs);
        }

        // Set UserId
        setUserId(getUserId(run));
        // Set Result
        setResult(run.getResult() == null ? null : run.getResult().toString());
        // Set Build Number
        setBuildNumber(String.valueOf(run.getNumber()));
        // Set Hostname
        setHostname(DatadogUtilities.getHostname(envVars == null ? null : envVars.get("HOSTNAME")));
        // Set Job Name
        String jobName = null;
        try {
            jobName = run.getParent().getFullName();
        } catch(NullPointerException e){
            //noop
        }
        setJobName(jobName == null ? null : jobName.
                replaceAll("»", "/").
                replaceAll(" ", ""));
    }

    private void populateEnvVariables(EnvVars envVars){
        if (envVars == null) {
            return;
        }
        setBuildId(envVars.get("BUILD_ID"));
        setBuildUrl(envVars.get("BUILD_URL"));
        setNodeName(envVars.get("NODE_NAME"));
        setBuildTag(envVars.get("BUILD_TAG"));
        setJenkinsUrl(envVars.get("JENKINS_URL"));
        setExecutorNumber(envVars.get("EXECUTOR_NUMBER"));
        setJavaHome(envVars.get("JAVA_HOME"));
        setWorkspace(envVars.get("WORKSPACE"));
        if (envVars.get("GIT_BRANCH") != null) {
            setBranch(envVars.get("GIT_BRANCH"));
            setGitUrl(envVars.get("GIT_URL"));
            setGitCommit(envVars.get("GIT_COMMIT"));
        } else if (envVars.get("CVS_BRANCH") != null) {
            setBranch(envVars.get("CVS_BRANCH"));
        } else if (envVars.get("SVN_REVISION") != null) {
            setBranch(envVars.get("SVN_REVISION"));
        }
        setPromotedUrl(envVars.get("PROMOTED_URL"));
        setPromotedJobName(envVars.get("PROMOTED_JOB_NAME"));
        setPromotedNumber(envVars.get("PROMOTED_NUMBER"));
        setPromotedId(envVars.get("PROMOTED_ID"));
        setPromotedTimestamp(envVars.get("PROMOTED_TIMESTAMP"));
        setPromotedUserName(envVars.get("PROMOTED_USER_NAME"));
        setPromotedUserId(envVars.get("PROMOTED_USER_ID"));
        setPromotedJobFullName(envVars.get("PROMOTED_JOB_FULL_NAME"));
    }

    /**
     * Assembles a map of tags containing:
     * - Build Tags
     * - Global Job Tags set in Job Properties
     * - Global Tag set in Jenkins Global configuration
     *
     * @return a map containing all tags values
     */
    public Map<String, Set<String>> getTags() {
        Map<String, Set<String>> mergedTags = new HashMap<>();
        try {
            mergedTags = DatadogUtilities.getTagsFromGlobalTags();
        } catch(NullPointerException e){
            //noop
        }
        mergedTags = TagsUtil.merge(mergedTags, tags);
        Map<String, Set<String>> additionalTags = new HashMap<>();
        Set<String> jobValues = new HashSet<>();
        jobValues.add(getJobName("unknown"));
        additionalTags.put("job", jobValues);
        if (nodeName != null) {
            Set<String> nodeValues = new HashSet<>();
            nodeValues.add(getNodeName("unknown"));
            additionalTags.put("node", nodeValues);
        }
        if (result != null) {
            Set<String> resultValues = new HashSet<>();
            resultValues.add(getResult("UNKNOWN"));
            additionalTags.put("result", resultValues);
        }
        if (branch != null) {
            Set<String> branchValues = new HashSet<>();
            branchValues.add(getBranch("unknown"));
            additionalTags.put("branch", branchValues);
        }
        mergedTags = TagsUtil.merge(mergedTags, additionalTags);

        return mergedTags;
    }

    public void setTags(Map<String, Set<String>> tags) {
        this.tags = tags;
    }

    private <A> A defaultIfNull(A value, A defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    public String getJobName(String value) {
        return defaultIfNull(jobName, value);
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getResult(String value) {
        return defaultIfNull(result, value);
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getHostname(String value) {
        return defaultIfNull(hostname, value);
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getBuildUrl(String value) {
        return defaultIfNull(buildUrl, value);
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public String getNodeName(String value) {
        return defaultIfNull(nodeName, value);
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getBranch(String value) {
        return defaultIfNull(branch, value);
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBuildNumber(String value) {
        return defaultIfNull(buildNumber, value);
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public Long getDuration(Long value) {
        return defaultIfNull(duration, value);
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getEndTime(Long value) {
        return defaultIfNull(endTime, value);
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getStartTime(Long value) {
        return defaultIfNull(startTime, value);
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public String getBuildId(String value) {
        return defaultIfNull(buildId, value);
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getBuildTag(String value) {
        return defaultIfNull(buildTag, value);
    }

    public void setBuildTag(String buildTag) {
        this.buildTag = buildTag;
    }

    public String getJenkinsUrl(String value) {
        return defaultIfNull(jenkinsUrl, value);
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public String getExecutorNumber(String value) {
        return defaultIfNull(executorNumber, value);
    }

    public void setExecutorNumber(String executorNumber) {
        this.executorNumber = executorNumber;
    }

    public String getJavaHome(String value) {
        return defaultIfNull(javaHome, value);
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getWorkspace(String value) {
        return defaultIfNull(workspace, value);
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getGitUrl(String value) {
        return defaultIfNull(gitUrl, value);
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getGitCommit(String value) {
        return defaultIfNull(gitCommit, value);
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getPromotedUrl(String value) {
        return defaultIfNull(promotedUrl, value);
    }

    public void setPromotedUrl(String promotedUrl) {
        this.promotedUrl = promotedUrl;
    }

    public String getPromotedJobName(String value) {
        return defaultIfNull(promotedJobName, value);
    }

    public void setPromotedJobName(String promotedJobName) {
        this.promotedJobName = promotedJobName;
    }

    public String getPromotedNumber(String value) {
        return defaultIfNull(promotedNumber, value);
    }

    public void setPromotedNumber(String promotedNumber) {
        this.promotedNumber = promotedNumber;
    }

    public String getPromotedId(String value) {
        return defaultIfNull(promotedId, value);
    }

    public void setPromotedId(String promotedId) {
        this.promotedId = promotedId;
    }

    public String getPromotedTimestamp(String value) {
        return defaultIfNull(promotedTimestamp, value);
    }

    public void setPromotedTimestamp(String promotedTimestamp) {
        this.promotedTimestamp = promotedTimestamp;
    }

    public String getPromotedUserName(String value) {
        return defaultIfNull(promotedUserName, value);
    }

    public void setPromotedUserName(String promotedUserName) {
        this.promotedUserName = promotedUserName;
    }

    public String getPromotedUserId(String value) {
        return defaultIfNull(promotedUserId, value);
    }

    public void setPromotedUserId(String promotedUserId) {
        this.promotedUserId = promotedUserId;
    }

    public String getPromotedJobFullName(String value) {
        return defaultIfNull(promotedJobFullName, value);
    }

    public void setPromotedJobFullName(String promotedJobFullName) {
        this.promotedJobFullName = promotedJobFullName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    private String getUserId(Run run) {
        if (promotedUserId != null){
            return promotedUserId;
        }
        String userName;
        for (CauseAction action : run.getActions(CauseAction.class)) {
            if (action != null && action.getCauses() != null) {
                for (Cause cause : action.getCauses()) {
                    userName = getUserId(cause);
                    if (userName != null) {
                        return userName;
                    }
                }
            }
        }
        if (run.getParent().getClass().getName().equals("hudson.maven.MavenModule")) {
            return "maven";
        }
        return "anonymous";
    }

    private String getUserId(Cause cause){
        if (cause instanceof TimerTrigger.TimerTriggerCause) {
            return "timer";
        } else if (cause instanceof SCMTrigger.SCMTriggerCause) {
            return "scm";
        } else if (cause instanceof Cause.UserIdCause) {
            String userName = ((Cause.UserIdCause) cause).getUserId();
            if (userName != null) {
                return userName;
            }
        } else if (cause instanceof Cause.UpstreamCause) {
            for (Cause upstreamCause : ((Cause.UpstreamCause) cause).getUpstreamCauses()) {
                String username = getUserId(upstreamCause);
                if (username != null) {
                    return username;
                }
            }
        }
        return null;
    }

}
